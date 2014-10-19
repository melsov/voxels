package voxel.landscape;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import voxel.landscape.chunkbuild.*;
import voxel.landscape.chunkbuild.blockfacefind.BlockFaceFinder;
import voxel.landscape.chunkbuild.meshbuildasync.AsyncMeshBuilder;
import voxel.landscape.chunkbuild.meshbuildasync.ChunkMeshBuildingSet;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.coordmap.managepages.FurthestCoord3PseudoDelegate;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/16/14.
 * manages world generation related thread pools
 */
public class WorldGenerator {

    Camera camera;

    private BlockingQueue<Coord2> columnsToBeBuilt;
    private BlockingQueue<ChunkMeshBuildingSet> chunksToBeMeshed;
    private BlockingQueue<ChunkMeshBuildingSet> completedChunkMeshSets;
    private AtomicBoolean asyncChunkMeshThreadsShouldKeepGoing = new AtomicBoolean(true);

    private static final int COLUMN_DATA_BUILDER_THREAD_COUNT = 1;
    private static final int CHUNK_MESH_BUILD_THREAD_COUNT = 1;

    private ExecutorService colDataPool;
    private ExecutorService chunkMeshBuildPool;

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

    public WorldGenerator(Node _worldNode, Camera _camera, TerrainMap _map, final ColumnMap _columnMap, AssetManager _assetManager) {
        worldNode = _worldNode;
        camera = _camera;
        map = _map;
        columnMap = _columnMap;
        blockFaceFinder = new BlockFaceFinder(map, camera);
        materialLibrarian = new MaterialLibrarian(_assetManager);


        initThreadPools();
    }

    private void initThreadPools() {


        initColumnDataThreadExecutorService();
        blockFaceFinder.floodFind(); //TODO: ORDER OF THESE TWO MATTERS RIGHT NOW—AND SHOULDN'T
//        initColumnDataThreadExecutorServiceCHUNKWISE();
        initChunkMeshBuildThreadExecutorService();

    }

    public void update(float tpf) {
        addToColumnPriorityQueue();
        if(!VoxelLandscape.DONT_BUILD_CHUNK_MESHES && buildANearbyChunkCHUNKWISE()) {}
        checkAsyncCompletedChunkMeshes();
        cullAnExcessColumn(tpf);
    }

    private void addToColumnPriorityQueue() {
        if (columnsToBeBuilt == null) return;
        BlockingQueue<Coord2> queue = columnsToBeBuilt;
//        PriorityBlockingQueue<Coord2> queue = (PriorityBlockingQueue<Coord2>) columnsToBeBuilt;
        if (queue.size() > 10) return;
        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(camera, map, columnMap);
        if (emptyCol == null) {
            return;
        }
        queue.add(new Coord2(emptyCol.x, emptyCol.z));
    }

    private void initColumnDataThreadExecutorService() {
//        ColumnCamComparator columnCamComparator = new ColumnCamComparator(camera);
        columnsToBeBuilt = new ArrayBlockingQueue<Coord2>(100); // new PriorityBlockingQueue<Coord2>(100, columnCamComparator);
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

    private boolean buildANearbyChunkCHUNKWISE() {
//        Coord3 chcoord = chunkCoordsToBeMeshFromChunkWise.poll();
        Coord3 chcoord = null;
        chcoord = blockFaceFinder.floodFilledChunkCoords.poll();
//        try {
//            chcoord = blockFaceFinder.floodFilledChunkCoords.take(); //FREEZE!
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (chcoord == null) return false;

        if (map.GetChunk(chcoord) == null) {
            B.bugln(" build nearby chunk chunkWiSE null Chunk at " + chcoord.toString());
        }

        buildThisChunk(map.GetChunk(chcoord));
        return true;
    }

    public void enqueueChunkMeshSets(ChunkMeshBuildingSet chunkMeshBuildingSet) {
        chunksToBeMeshed.add(chunkMeshBuildingSet);
    }

    private void buildThisChunk(Chunk ch) {
//        if (ch == null) return;
        ch.setHasEverStartedBuildingToTrue();
        if (!ch.getIsAllAir()) {
            ch.getChunkBrain().SetDirty();
            ch.getChunkBrain().wakeUp();
            attachMeshToScene(ch); //note: no mesh geom yet
        } else {
            ch.getChunkBrain().setMeshEmpty();
        }
    }

    private void checkAsyncCompletedChunkMeshes() {
        int count = 0;
        while (count++ < 5) {
            ChunkMeshBuildingSet chunkMeshBuildingSet = completedChunkMeshSets.poll();
            if (chunkMeshBuildingSet == null) return;
            Chunk chunk = map.GetChunk(chunkMeshBuildingSet.chunkPosition);
            if (chunk == null) return;
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
