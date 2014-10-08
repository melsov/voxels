package voxel.landscape.chunkbuild.meshbuildasync;

import voxel.landscape.Chunk;
import voxel.landscape.MeshSet;
import voxel.landscape.VoxelLandscape;
import voxel.landscape.map.TerrainMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 8/17/14.
 */
public class AsyncMeshBuilder implements Runnable
{
    private TerrainMap terrainMap;
    private AtomicBoolean keepGoing;
    private BlockingQueue<ChunkMeshBuildingSet> chunksToBeMeshed;
    private BlockingQueue<ChunkMeshBuildingSet> completedChunkMeshSets;

    public AsyncMeshBuilder(TerrainMap _terrainMap, final BlockingQueue<ChunkMeshBuildingSet> _chunksToBeMeshed,
                            final BlockingQueue<ChunkMeshBuildingSet> _completedChunkMeshSet, AtomicBoolean _keepGoing) {
        terrainMap = _terrainMap;
        keepGoing = _keepGoing;
        chunksToBeMeshed = _chunksToBeMeshed;
        completedChunkMeshSets = _completedChunkMeshSet;
    }
    @Override
    public void run() {
        while (keepGoing.get()) {
            Chunk chunk = null;
            ChunkMeshBuildingSet chunkMeshBuildingSet = null;
            try {
                chunkMeshBuildingSet = chunksToBeMeshed.take();
                chunk = terrainMap.GetChunk(chunkMeshBuildingSet.chunkPosition);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (chunk == null) {
                continue;
            }

            if (VoxelLandscape.DO_USE_TEST_GEOMETRY) return;

            MeshSet mset = new MeshSet(false, chunkMeshBuildingSet.isOnlyLight);
            MeshSet waterMSet = new MeshSet(true, chunkMeshBuildingSet.isOnlyLight);
            chunkMeshBuildingSet.meshSet = mset;
            chunkMeshBuildingSet.liquidMeshSet = waterMSet;

            // STILL WANT TO TEST
            if (!chunk.chunkBlockFaceMap.empty()) {
                chunk.chunkBlockFaceMap.buildMeshFromMap(chunk, mset, waterMSet, chunkMeshBuildingSet.isOnlyLight, chunkMeshBuildingSet.isOnlyLiquid);
            }
            //TEST! WANT \|/
//            ChunkBuilder.buildMesh(chunk, mset, waterMSet, chunkMeshBuildingSet.isOnlyLight, chunkMeshBuildingSet.isOnlyLiquid);

            try {
                completedChunkMeshSets.put(chunkMeshBuildingSet);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
