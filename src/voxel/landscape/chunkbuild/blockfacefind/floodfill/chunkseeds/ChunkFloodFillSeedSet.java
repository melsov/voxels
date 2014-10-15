package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkseeds;

import voxel.landscape.coord.Coord3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/14/14.
 */
public class ChunkFloodFillSeedSet {
    private Coord3 chunkCoord;
    public Coord3 getChunkCoord() { return chunkCoord; }
    private List<ChunkFloodFillSeed> seeds = new ArrayList<ChunkFloodFillSeed>(7);

    public ChunkFloodFillSeedSet(Coord3 _chunkCoord) {
        chunkCoord = _chunkCoord;
    }

    public void addCoord(Coord3 co){
        ChunkFloodFillSeed adjacentFloodFillSeed = null;
        for (int i = 0; i < seeds.size(); ++i) {
            ChunkFloodFillSeed floodFillSeed = seeds.get(i);
            if (adjacentFloodFillSeed == null) {
                if (floodFillSeed.addCoord(co)) {
                    adjacentFloodFillSeed = floodFillSeed;
                }
            } else {
                if (floodFillSeed.isCoordAdjacent(co)) {
                    adjacentFloodFillSeed.addMembersOfSet(floodFillSeed);
                    seeds.remove(i--);
                }
            }
        }
        if (adjacentFloodFillSeed == null) {
            seeds.add(new ChunkFloodFillSeed(co));
        }
    }

    public Coord3 removeNext() {
        if (seeds.size() == 0) return null;
        return seeds.remove(0).getSeed();
    }
    public int size() {
        return seeds.size();
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChunkSlice: Chunk coord: ");
        sb.append(chunkCoord.toString());
        sb.append(" block set size: ");
        sb.append(seeds.size());
        return sb.toString();
    }
}
