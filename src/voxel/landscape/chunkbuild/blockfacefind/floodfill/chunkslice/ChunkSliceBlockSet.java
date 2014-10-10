package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice;

import voxel.landscape.coord.ChunkLocalCoord;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;

import java.util.HashSet;

public class ChunkSliceBlockSet
{

    private Coord3 seed;
    public Coord3 getSeed() { return  seed; }
    private int axis;
    public int getAxis(){ return axis; }
    public final HashSet<ChunkLocalCoord> coords = new HashSet<ChunkLocalCoord>(128);

    public ChunkSliceBlockSet(Coord3 _seed, int _axis) {
        seed = _seed; axis = _axis;
        coords.add(new ChunkLocalCoord(seed));
    }

    public void addCoord(Coord3 co) {

    }

    public void addMembersOfSet(ChunkSliceBlockSet other) {

    }

    public boolean isCoordAdjacent(Coord3 co) {
        for(Coord3 nudge : Direction.NudgeDirectionsNormalToAxis(axis)) {
            // ...
        }
        return false;
    }
}
