package voxel.landscape;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import voxel.landscape.chunkbuild.AsyncGenerateColumnDataInfinite;
import voxel.landscape.chunkbuild.ChunkCoordCamComparator;
import voxel.landscape.chunkbuild.ChunkFinder;
import voxel.landscape.chunkbuild.MaterialLibrarian;
import voxel.landscape.chunkbuild.blockfacefind.BlockFaceFinder;
import voxel.landscape.chunkbuild.bounds.XZBounds;
import voxel.landscape.chunkbuild.meshbuildasync.AsyncMeshBuilder;
import voxel.landscape.chunkbuild.meshbuildasync.ChunkMeshBuildingSet;
import voxel.landscape.chunkbuild.unload.ChunkUnloader;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.coordmap.managepages.FurthestChunkFinder;
import voxel.landscape.coord.ColumnRange;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.AsyncScatter;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;
import voxel.landscape.settings.BuildSettings;
import voxel.landscape.util.Asserter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/16/14.
 * manages world generation related thread pools
 */
public class WorldGenerator {

    private Camera camera;

    private BlockingQueue<Coord2> columnsToBeBuilt;
    private BlockingQueue<Coord2> columnsToBeScattered;
    private BlockingQueue<ChunkMeshBuildingSet> chunksToBeMeshed;
    private BlockingQueue<ChunkMeshBuildingSet> completedChunkMeshSets;

    //Chunk disposal
    private BlockingQueue<Coord3> unloadChunks = new ArrayBlockingQueue<>(80);
    private BlockingQueue<Coord3> deletableChunks = new ArrayBlockingQueue<>(80);
    ExecutorService chunkUnloadService;

    private static final int COLUMN_DATA_BUILDER_THREAD_COUNT = 1;
    private static final int CHUNK_MESH_BUILD_THREAD_COUNT = 1;

    private ExecutorService colDataPool;
    private ExecutorService chunkMeshBuildPool;
    private ExecutorService scatterBuildPool;


    private AtomicBoolean columnBuildingThreadsShouldKeepGoing = new AtomicBoolean(true);
    private static int COLUMN_CULLING_MIN = (int) ((BuildSettings.ADD_COLUMN_RADIUS * 1 + 2)*(BuildSettings.ADD_COLUMN_RADIUS * 1 + 2));
    private static int COLUMN_DELETING_MIN = (int)(COLUMN_CULLING_MIN * 1.2);

    private FurthestChunkFinder furthestChunkFinder = new FurthestChunkFinder();

    private Node worldNode;
    private TerrainMap map;
    public final ColumnMap columnMap;
    public final MaterialLibrarian materialLibrarian;

    public final BlockFaceFinder blockFaceFinder;
    public final BlockFaceFinder shortOrderBlockFaceFinder;
    public final XZBounds xzBounds;

    public static final String BGFloodFillThreadName = "Background Flood-Fill Thread";
    public static final String ShortOrderFloodFillThreadName = "Short-order Flood-Fill Thread";

    public static final boolean TEST_DONT_BUILD = false;
    public static final boolean TEST_DONT_RENDER = false;

    public WorldGenerator(Node _worldNode, Camera _camera, TerrainMap _map, final ColumnMap _columnMap, AssetManager _assetManager) {
        worldNode = _worldNode;
        camera = _camera;
        map = _map;
        columnMap = _columnMap;
        xzBounds = new XZBounds(camera, BuildSettings.ADD_COLUMN_RADIUS );
        blockFaceFinder = new BlockFaceFinder(map, map.chunkCoordsToBeFlooded, camera, xzBounds, BGFloodFillThreadName);
        shortOrderBlockFaceFinder = new BlockFaceFinder(map, map.chunkCoordsToBePriorityFlooded, camera, xzBounds, ShortOrderFloodFillThreadName);
        materialLibrarian = new MaterialLibrarian(_assetManager);

        initThreadPools();
    }

    private void initThreadPools() {
        initColumnDataThreadExecutorService();
//        initLightAndWaterScatterService();
        blockFaceFinder.start();
        shortOrderBlockFaceFinder.start();
        initChunkMeshBuildThreadExecutorService();
        initUnloadService();
    }

    public void update(float tpf) {
        addColumns();
        if(!VoxelLandscape.DONT_BUILD_CHUNK_MESHES) {
            buildAChunk();
        }
        renderChunks();
        cull();
    }


    private void initUnloadService() {
        int CHUNK_UNLOAD_THREAD_COUNT = 5;
        chunkUnloadService = Executors.newFixedThreadPool(CHUNK_UNLOAD_THREAD_COUNT);
        for (int i = 0; i < CHUNK_UNLOAD_THREAD_COUNT; ++i) {
            ChunkUnloader chunkUnloader = new ChunkUnloader(
                    map,
                    columnMap,
                    unloadChunks,
                    deletableChunks);
            chunkUnloadService.execute(chunkUnloader);
        }
    }

    private void initColumnDataThreadExecutorService() {
        columnsToBeBuilt = new ArrayBlockingQueue<Coord2>(100);
        colDataPool = Executors.newFixedThreadPool(COLUMN_DATA_BUILDER_THREAD_COUNT);
        for (int i = 0; i < COLUMN_DATA_BUILDER_THREAD_COUNT; ++i) {
            AsyncGenerateColumnDataInfinite infinColDataThread = new AsyncGenerateColumnDataInfinite(
                            map,
                            columnMap,
                            columnsToBeBuilt,
                            columnBuildingThreadsShouldKeepGoing );
            colDataPool.execute(infinColDataThread);
        }
    }

    private void initChunkMeshBuildThreadExecutorService() {
        ChunkCoordCamComparator chunkCoordCamComparator = new ChunkCoordCamComparator(camera);
        chunksToBeMeshed = new PriorityBlockingQueue<>(50, chunkCoordCamComparator);
        completedChunkMeshSets = new LinkedBlockingQueue<>(50);
        chunkMeshBuildPool = Executors.newFixedThreadPool(CHUNK_MESH_BUILD_THREAD_COUNT);
        for (int i = 0; i < CHUNK_MESH_BUILD_THREAD_COUNT; ++i) {
            AsyncMeshBuilder asyncMeshBuilder = new AsyncMeshBuilder(
                    map,
                    chunksToBeMeshed,
                    completedChunkMeshSets);
            chunkMeshBuildPool.execute(asyncMeshBuilder);
        }
    }
    //TODO: YA ASYNC CLASS/SERVICE: SCATTER LIGHT AND WATER... CONSUMES FROM FLOODFILLDCHUNKCOORDS. PRODUCES SCATTEREDCHUNKS
    //TODO: CHUNK BUILD TIMER TEST CLASS
    // OR>>> ADD AFTER? (AND THEN DO A LIGHT UPDATE????)
    private void initLightAndWaterScatterService() {
        columnsToBeScattered = new LinkedBlockingQueue<Coord2>(50);
        scatterBuildPool = Executors.newFixedThreadPool(COLUMN_DATA_BUILDER_THREAD_COUNT);
        for(int i = 0; i < COLUMN_DATA_BUILDER_THREAD_COUNT; ++i) {
            AsyncScatter asyncScatter = new AsyncScatter(
                    map,
                    columnMap,
                    columnsToBeScattered,
                    columnBuildingThreadsShouldKeepGoing);
            scatterBuildPool.execute(asyncScatter);
        }
    }


    /*
     * Update
     */
    private void addColumns() {
        if (columnsToBeBuilt == null) return;
        // TODO: new approach. in each update, go over a list of all columns within the add radius
        // TODO: clear columnsToBeBuilt, add all columns from the within-add-radius list

        if (columnsToBeBuilt.size() > 10) return;
        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(camera, map, columnMap, false);
        if (emptyCol == null) return;
        removeFromUnloadLists(new Coord2(emptyCol.x, emptyCol.z));
        if (!columnMap.IsBuiltOrIsBuilding(emptyCol.x, emptyCol.z)) {
            columnsToBeBuilt.add(new Coord2(emptyCol.x, emptyCol.z));
        } else if (map.columnHasHiddenChunk(emptyCol)) {
            for (Coord3 coord3 : new ColumnRange(emptyCol)) {
                Chunk chunk = map.GetChunk(coord3);
                if (chunk != null) {
                    buildThisChunk(chunk);
                }
            }
        }
    }

    private void removeFromUnloadLists(Coord2 column) {
        for (Coord3 c : new ColumnRange(column)) {
            deletableChunks.remove(c);
            unloadChunks.remove(c);
        }
    }

    private void buildAChunk() {
        Coord3 chunkCoord = shortOrderBlockFaceFinder.floodFilledChunkCoords.poll();
        if (chunkCoord == null) chunkCoord = blockFaceFinder.floodFilledChunkCoords.poll();
        if (chunkCoord == null){ return; }

        Asserter.assertTrue(map.GetChunk(chunkCoord) != null, "chunk not in map! at chunk coord: " + chunkCoord.toString());

        buildThisChunk(map.GetChunk(chunkCoord));
    }

    private void buildThisChunk(Chunk chunk) {
        chunk.setHasEverStartedBuildingToTrue();
        if (!chunk.getIsAllAir()) {
            chunk.getChunkBrain().SetDirty();
            chunk.getChunkBrain().wakeUp();
            attachMeshToScene(chunk); //no mesh geom yet
        } else {
            chunk.getChunkBrain().setMeshEmpty();
        }
    }

    private void attachMeshToScene(Chunk chunk) {
        chunk.getChunkBrain().attachTerrainMaterial(materialLibrarian.getBlockMaterial());
        chunk.getChunkBrain().attachWaterMaterial(materialLibrarian.getBlockMaterialTranslucentAnimated());
        chunk.getChunkBrain().attachToTerrainNode(worldNode);
    }

    public void enqueueChunkMeshSets(ChunkMeshBuildingSet chunkMeshBuildingSet) {
        chunksToBeMeshed.add(chunkMeshBuildingSet);
    }

    private void renderChunks() {
        for (int count = 0; count < 5; ++count) {
            ChunkMeshBuildingSet chunkMeshBuildingSet = completedChunkMeshSets.poll();
            if (chunkMeshBuildingSet == null) return;

            // write to file and don't mesh chunk?
            if (!BuildSettings.ChunkCoordWithinAddRadius(camera.getLocation(), chunkMeshBuildingSet.chunkPosition)) {
                slateForUnload(map.GetChunk(chunkMeshBuildingSet.chunkPosition));
                return;
            }
            Chunk chunk = map.GetChunk(chunkMeshBuildingSet.chunkPosition);
            Asserter.assertTrue(chunk != null, "null chunk in check async...");
            chunk.getChunkBrain().applyMeshBuildingSet(chunkMeshBuildingSet);
        }
    }

    /*
     * Remove columns
     */
    private void cull() {
        if (!VoxelLandscape.CULLING_ON) return;
        unloadChunks();
        removeChunks();
    }
    private void unloadChunks() {
        for (Coord3 chunkCoord : furthestChunkFinder.outsideOfAddRangeChunks(map, camera, columnMap.getCoordXZSet().toArray())) {
            if (chunkCoord == null) continue;
            if (unloadChunks.remainingCapacity() == 0) {
                B.bugln("capacity is 0");
                return;
            }
            if (!BuildSettings.ChunkCoordWithinAddRadius(camera.getLocation(), chunkCoord)) {
                slateForUnload(map.GetChunk(chunkCoord));
            }
        }
    }
    private void slateForUnload(Chunk chunk) {
        if (chunk != null && !unloadChunks.contains(chunk.position) && chunk.isWriteDirty() && !chunk.hasStartedWriting.get()) {
            chunk.hasStartedWriting.set(true);
            chunk.getChunkBrain().detachNodeFromParent();
            unloadChunks.add(chunk.position);
        }
    }
    private void removeChunks() {
        int addedBack = 0;
        while(deletableChunks.size() - addedBack > 0) {
            Coord3 chunkCoord = deletableChunks.poll();
            if (chunkCoord == null) return;
            if (BuildSettings.ChunkCoordWithinAddRadius(camera.getLocation(), chunkCoord)) {
                continue;
            }
            else if (!BuildSettings.ChunkCoordOutsideOfRemoveRadius(camera.getLocation(), chunkCoord)) {
                Chunk chunk = map.GetChunk(chunkCoord);
                if (chunk != null && !chunk.isWriteDirty() && deletableChunks.remainingCapacity() != 0) {
                    deletableChunks.add(chunkCoord);
                    addedBack++;
                }
                return;
            }

            if (map.removeColumn(chunkCoord)) {
                columnMap.Destroy(chunkCoord.getX(), chunkCoord.getZ());
            }
        }
    }

    /*
     * Clean-up
     */
    public void killThreadPools() {
        poisonThreads();

        columnBuildingThreadsShouldKeepGoing.set(false);
        if (colDataPool != null)
            colDataPool.shutdownNow();
        if (scatterBuildPool != null)
            scatterBuildPool.shutdownNow();

//        asyncChunkMeshThreadsShouldKeepGoing.set(false);
        chunkMeshBuildPool.shutdownNow();

        blockFaceFinder.shutdown();
        shortOrderBlockFaceFinder.shutdown();

//        columnRemovalShouldKeepGoing.set(false);
        if (chunkUnloadService != null) {
            chunkUnloadService.shutdownNow();
        }
    }


    private void poisonThreads() {
        columnsToBeBuilt.add(Coord2.SPECIAL_FLAG);
        map.chunkCoordsToBeFlooded.add(Coord3.SPECIAL_FLAG);
        map.chunkCoordsToBePriorityFlooded.add(Coord3.SPECIAL_FLAG);
        unloadChunks.add(Coord3.SPECIAL_FLAG);
        chunksToBeMeshed.add(ChunkMeshBuildingSet.POISON_PILL);
    }



}
