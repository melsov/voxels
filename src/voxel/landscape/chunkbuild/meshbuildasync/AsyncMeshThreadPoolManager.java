package voxel.landscape.chunkbuild.meshbuildasync;

import voxel.landscape.coord.Coord3;

import java.util.concurrent.BlockingQueue;

/**
 * Created by didyouloseyourdog on 8/17/14.
 */
public class AsyncMeshThreadPoolManager
{
    BlockingQueue<Coord3> finishedMeshes;
    BlockingQueue<Coord3> awaitingBuildMeshSets;
    public AsyncMeshThreadPoolManager(BlockingQueue<Coord3> _builtMeshQueue) {
        finishedMeshes = _builtMeshQueue;
    }
}
