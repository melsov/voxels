package voxel.landscape;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import voxel.landscape.chunkbuild.*;
import voxel.landscape.chunkbuild.blockfacefind.BlockFaceFinder;
import voxel.landscape.chunkbuild.bounds.XZBounds;
import voxel.landscape.chunkbuild.meshbuildasync.AsyncMeshBuilder;
import voxel.landscape.chunkbuild.meshbuildasync.ChunkMeshBuildingSet;
import voxel.landscape.chunkbuild.unload.ColumnUnloader;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.coordmap.managepages.FurthestChunkFinder;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.debug.DebugGeometry;
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
        addToColumnPriorityQueue();
        if(!VoxelLandscape.DONT_BUILD_CHUNK_MESHES) {
            buildANearbyChunk();
        }
        checkAsyncCompletedChunkMeshes();
        cull();
    }

    private void addToColumnPriorityQueue() {
        if (columnsToBeBuilt == null) return;
        if (columnsToBeBuilt.size() > 10) return;
        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(camera, map, columnMap);
        if (emptyCol == null) return;
        removeFromUnloadLists(new Coord2(emptyCol.x, emptyCol.z));
        columnsToBeBuilt.add(new Coord2(emptyCol.x, emptyCol.z));
    }
    /*
     * TODO: make adding zones actually work
     */

    private void removeFromUnloadLists(Coord2 column) {
        for (Coord3 c = new Coord3(column.getX(), TerrainMap.MIN_CHUNK_COORD.y, column.getZ()) ; c.y < TerrainMap.MAX_CHUNK_COORD.y; c.y++){
//            deletableChunks.remove(c);
//            unloadChunks.remove(c);
        }
    }

    private void initUnloadService() {
        int CHUNK_UNLOAD_THREAD_COUNT = 5;
        chunkUnloadService = Executors.newFixedThreadPool(CHUNK_UNLOAD_THREAD_COUNT);
        for (int i = 0; i < CHUNK_UNLOAD_THREAD_COUNT; ++i) {
            ColumnUnloader columnUnloader= new ColumnUnloader(
                    map,
                    columnMap,
                    unloadChunks,
                    deletableChunks);
            chunkUnloadService.execute(columnUnloader);
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


    private void buildANearbyChunk() {
        Coord3 chcoord = shortOrderBlockFaceFinder.floodFilledChunkCoords.poll();
        if (chcoord == null) {
            chcoord = blockFaceFinder.floodFilledChunkCoords.poll();
        }
        if (chcoord == null){ return; }
        Asserter.assertTrue(map.GetChunk(chcoord) != null, "chunk not in map! at chunk coord: " + chcoord.toString());
        buildThisChunk(map.GetChunk(chcoord));
        return;
    }

    public void enqueueChunkMeshSets(ChunkMeshBuildingSet chunkMeshBuildingSet) {
        chunksToBeMeshed.add(chunkMeshBuildingSet);
    }

    private void buildThisChunk(Chunk ch) {
        ch.setHasEverStartedBuildingToTrue();
        if (TEST_DONT_RENDER) return;
//        ch.setHasGeneratedTrue();
        if (!ch.getIsAllAir()) {
            ch.getChunkBrain().SetDirty();
            ch.getChunkBrain().wakeUp();
            attachMeshToScene(ch); //note: no mesh geom yet
        } else {
            DebugGeometry.AddDebugChunk(ch.position, ColorRGBA.Orange);
            ch.getChunkBrain().setMeshEmpty();
        }
    }

    private void checkAsyncCompletedChunkMeshes() {
        for (int count = 0; count < 5; ++count) {
            ChunkMeshBuildingSet chunkMeshBuildingSet = completedChunkMeshSets.poll();
            if (TEST_DONT_RENDER) {
                return;
            }
            if (chunkMeshBuildingSet == null) return;
            Chunk chunk = map.GetChunk(chunkMeshBuildingSet.chunkPosition);
            if (chunk == null) {
                DebugGeometry.AddDebugChunk(chunkMeshBuildingSet.chunkPosition, ColorRGBA.Blue);
                Asserter.assertFalseAndDie("null chunk in check async...");
                return;
            }
            chunk.getChunkBrain().applyMeshBuildingSet(chunkMeshBuildingSet);
        }
    }

    /*
     * Remove columns
     */
    private void cull() {
        if (!VoxelLandscape.CULLING_ON) return;
        unloadChunks();
        removeAChunk();
    }
    private void unloadChunks() {
        int culled = 0;
//        while (columnMap.columnCount() > COLUMN_CULLING_MIN && culled++ < 1) {
            Coord3 furthest = furthestChunkFinder.furthestWriteDirtyButNotYetWritingChunk(map, camera, columnMap.getCoordXZSet().toArray());
            if (unloadChunks.remainingCapacity() == 0) {
                B.bugln("capacity is 0");
                return;
            }
            if (furthest != null && !BuildSettings.ChunkCoordWithinAddRadius(camera.getLocation(), furthest)) {
                Chunk chunk = map.GetChunk(furthest);
                if (chunk != null && !unloadChunks.contains(furthest)) {
                    chunk.hasStartedWriting.set(true);
                    unloadChunks.add(furthest);
                }
            }
//        }
    }
    private void removeAChunk() {
        Coord3 chunk = deletableChunks.poll();
        if (chunk == null) return;
        if (BuildSettings.ChunkCoordWithinAddRadius(camera.getLocation(), chunk)) {
            return;
        } else if (!BuildSettings.ChunkCoordOutsideOfRemoveRadius(camera.getLocation(), chunk)) {
            if (deletableChunks.remainingCapacity() != 0) deletableChunks.add(chunk);
        }

        if (map.removeColumn(chunk.getX(), chunk.getZ())) {
            columnMap.Destroy(chunk.getX(), chunk.getZ());
        }
    }

    private void attachMeshToScene(Chunk chunk) {
        chunk.getChunkBrain().attachTerrainMaterial(materialLibrarian.getBlockMaterial());
        chunk.getChunkBrain().attachWaterMaterial(materialLibrarian.getBlockMaterialTranslucentAnimated());
        chunk.getChunkBrain().attachToTerrainNode(worldNode);
    }

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
