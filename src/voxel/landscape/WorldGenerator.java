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
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.coordmap.managepages.FurthestCoord3PseudoDelegate;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.debug.DebugGeometry;
import voxel.landscape.map.AsyncScatter;
import voxel.landscape.map.TerrainMap;
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
    private AtomicBoolean asyncChunkMeshThreadsShouldKeepGoing = new AtomicBoolean(true);

    private static final int COLUMN_DATA_BUILDER_THREAD_COUNT = 1;
    private static final int CHUNK_MESH_BUILD_THREAD_COUNT = 1;

    private ExecutorService colDataPool;
    private ExecutorService chunkMeshBuildPool;
    private ExecutorService scatterBuildPool;

    private final int COLUMN_DATA_BUILDER_THREAD_COUNT_CHUNKWISE = 1;
    private final BlockingQueue<Coord3> chunkCoordsToBeMeshFromChunkWise = new ArrayBlockingQueue<Coord3>(256);
    ExecutorService colDataPoolCHUNKWISE;

    private AtomicBoolean columnBuildingThreadsShouldKeepGoing = new AtomicBoolean(true);
    private static int COLUMN_CULLING_MIN = (int) ((VoxelLandscape.ADD_COLUMN_RADIUS * 1 + 2)*(VoxelLandscape.ADD_COLUMN_RADIUS * 1 + 2));

    private FurthestCoord3PseudoDelegate furthestDelegate = new FurthestCoord3PseudoDelegate();

    private Node worldNode;
    private TerrainMap map;
    public final ColumnMap columnMap;
    public final MaterialLibrarian materialLibrarian;

    public final BlockFaceFinder blockFaceFinder;
    public final XZBounds xzBounds;

    public WorldGenerator(Node _worldNode, Camera _camera, TerrainMap _map, final ColumnMap _columnMap, AssetManager _assetManager) {
        worldNode = _worldNode;
        camera = _camera;
        map = _map;
        columnMap = _columnMap;
        xzBounds = new XZBounds(camera, VoxelLandscape.ADD_COLUMN_RADIUS );
        blockFaceFinder = new BlockFaceFinder(map, camera, xzBounds);
        materialLibrarian = new MaterialLibrarian(_assetManager);

        initThreadPools();
    }

    private void initThreadPools() {
        initColumnDataThreadExecutorService();
        initLightAndWaterScatterService();
        blockFaceFinder.floodFind(); //TODO: ORDER OF THESE TWO MATTERS RIGHT NOW—AND SHOULDN'T
        initChunkMeshBuildThreadExecutorService();
    }

    public void update(float tpf) {
        addToColumnPriorityQueue();
        if(!VoxelLandscape.DONT_BUILD_CHUNK_MESHES && buildANearbyChunk()) {}
        checkAsyncCompletedChunkMeshes();
        cullAnExcessColumn(tpf);
    }

    private void addToColumnPriorityQueue() {
        if (columnsToBeBuilt == null) return;
        BlockingQueue<Coord2> queue = columnsToBeBuilt;
        if (queue.size() > 10) return;
        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(camera, map, columnMap);
        if (emptyCol == null) {
            return;
        }
        queue.add(new Coord2(emptyCol.x, emptyCol.z));
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

    private void initColumnDataThreadExecutorServiceCHUNKWISE() {
        colDataPoolCHUNKWISE = Executors.newFixedThreadPool(COLUMN_DATA_BUILDER_THREAD_COUNT_CHUNKWISE);
        for (int i = 0; i < COLUMN_DATA_BUILDER_THREAD_COUNT_CHUNKWISE; ++i) {
            AsyncGenerateColumnDataChunkWise asyncChunkWise = new AsyncGenerateColumnDataChunkWise(
                    map,
                    columnMap,
                    blockFaceFinder.floodFilledChunkCoords,
                    chunkCoordsToBeMeshFromChunkWise,
                    columnBuildingThreadsShouldKeepGoing );
            colDataPoolCHUNKWISE.execute(asyncChunkWise);
        }
    }
    private void initChunkMeshBuildThreadExecutorService() {
        ChunkCoordCamComparator chunkCoordCamComparator = new ChunkCoordCamComparator(camera);
        chunksToBeMeshed = new PriorityBlockingQueue<ChunkMeshBuildingSet>(50, chunkCoordCamComparator);
        completedChunkMeshSets = new LinkedBlockingQueue<ChunkMeshBuildingSet>(50);
        chunkMeshBuildPool = Executors.newFixedThreadPool(CHUNK_MESH_BUILD_THREAD_COUNT);
        for (int i = 0; i < CHUNK_MESH_BUILD_THREAD_COUNT; ++i) {
            AsyncMeshBuilder asyncMeshBuilder = new AsyncMeshBuilder(
                    map,
                    chunksToBeMeshed,
                    completedChunkMeshSets,
                    asyncChunkMeshThreadsShouldKeepGoing);
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

    private boolean buildANearbyChunk() {
        Coord3 chcoord = blockFaceFinder.floodFilledChunkCoords.poll();
        if (chcoord == null){ return false; }
        Asserter.assertTrue(map.GetChunk(chcoord) != null, "chunk not in map! at chunk coord: " + chcoord.toString());
        buildThisChunk(map.GetChunk(chcoord));
        return true;
    }

    public void enqueueChunkMeshSets(ChunkMeshBuildingSet chunkMeshBuildingSet) {
        chunksToBeMeshed.add(chunkMeshBuildingSet);
    }

    private void buildThisChunk(Chunk ch) {
        ch.setHasEverStartedBuildingToTrue();
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
        int count = 0;
        while (count++ < 5) {
            ChunkMeshBuildingSet chunkMeshBuildingSet = completedChunkMeshSets.poll();
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
    private void cullAnExcessColumn(float tpf) {
        if (!VoxelLandscape.CULLING_ON) return;
        int culled = 0;
        while (columnMap.columnCount() > COLUMN_CULLING_MIN) {
            Coord3 furthest = furthestDelegate.getFurthest2D(camera, columnMap.getCoordXZSet());
            removeColumn(furthest.x, furthest.z);
            if (culled++ > 10) break;
        }
    }
    private void removeColumn(int x, int z)
    {
        int minChunkY = map.getMinChunkCoordY();
        int maxChunkY = map.getMaxChunkCoordY();
        for (int k = minChunkY; k < maxChunkY; ++k )
        {
            Chunk ch = map.GetChunk(x, k, z);
            if (ch == null) {
                continue;
            }
            detachFromScene(ch);
            ch.getChunkBrain().clearMeshBuffersAndSetGeometryNull();
        }
        map.removeColumnData(x,z);
        columnMap.Destroy(x, z);
    }

    private void attachMeshToScene(Chunk chunk) {
        chunk.getChunkBrain().attachTerrainMaterial(materialLibrarian.getBlockMaterial());
        chunk.getChunkBrain().attachWaterMaterial(materialLibrarian.getBlockMaterialTranslucentAnimated());
        chunk.getChunkBrain().attachToTerrainNode(worldNode);
    }

    private void detachFromScene(Chunk chunk) {
        Node g = chunk.getRootSpatial();
        if (g != null) g.removeFromParent();
    }

    public void killThreadPools() {
        columnBuildingThreadsShouldKeepGoing.set(false);
        if (colDataPool != null)
            colDataPool.shutdownNow();
        if (colDataPoolCHUNKWISE != null)
            colDataPoolCHUNKWISE.shutdownNow();

        asyncChunkMeshThreadsShouldKeepGoing.set(false);
        chunkMeshBuildPool.shutdownNow();
    }



}
