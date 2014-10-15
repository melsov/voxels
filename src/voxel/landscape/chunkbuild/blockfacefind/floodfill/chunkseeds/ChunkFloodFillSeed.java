package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkseeds;

import voxel.landscape.coord.ChunkLocalCoord;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;

import java.util.HashSet;

/**
 * Created by didyouloseyourdog on 10/14/14.
 */
public class ChunkFloodFillSeed {

    private Coord3 seed;
    public Coord3 getSeed() { return  seed; }
    public final HashSet<ChunkLocalCoord> coords = new HashSet<ChunkLocalCoord>(128);

    public ChunkFloodFillSeed(Coord3 _seed) {
        seed = _seed;
        coords.add(new ChunkLocalCoord(seed));
    }

    public boolean addCoord(Coord3 co) {
        if (isCoordAdjacent(co)) {
            coords.add(new ChunkLocalCoord(co));
            return true;
        }
        return false;
    }

    public void addMembersOfSet(ChunkFloodFillSeed other) {
        coords.addAll(other.coords);
    }

    public boolean isCoordAdjacent(Coord3 co) {
        for(Coord3 nudge : Direction.DirectionCoords) {
            if (coords.contains(new ChunkLocalCoord(co.add(nudge)))) {
                return true;
            }
        }
        return false;
    }
}
