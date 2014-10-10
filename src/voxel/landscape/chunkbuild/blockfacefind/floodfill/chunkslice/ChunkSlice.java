package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice;

import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/9/14.
 * Manages a collection of ChunkSliceBlockSets
 * for a 16x16 slice of blocks,
 * representing the face of a chunk
 */

public class ChunkSlice
{
    private Coord3 chunkCoord;
    public Coord3 getChunkCoord() { return chunkCoord; }
    private int direction;
    public int getDirection() { return direction; }
    private List<ChunkSliceBlockSet> blockSets = new ArrayList<ChunkSliceBlockSet>(3);

    public void addCoord(Coord2 co){

    }
    public Coord3 removeNext() {
        if (blockSets.size() == 0) return null;
        return blockSets.remove(0).getSeed();
    }
    public int size() {
        return blockSets.size();
    }
}
