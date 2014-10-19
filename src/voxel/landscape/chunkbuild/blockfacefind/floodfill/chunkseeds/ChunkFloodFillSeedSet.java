package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkseeds;

import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/14/14.
 * A collection of ChunkFloodFillSeedBlob3Ds.
 * Handles adding blobs and combining contiguous blobs.
 */
public class ChunkFloodFillSeedSet {
    private Coord3 chunkCoord;
    public Coord3 getChunkCoord() { return chunkCoord; }
    private List<ChunkFloodFillSeedBlob3D> seeds = new ArrayList<>(7);

    public ChunkFloodFillSeedSet(Coord3 _chunkCoord) {
        chunkCoord = _chunkCoord;
    }

    public void addCoord(Coord3 global){
        Coord3 local = Chunk.toChunkLocalCoord(global);
        ChunkFloodFillSeedBlob3D adjacentFloodFillSeed = null;
        // Is this coord adjacent to an existing FloodFillSeedBlob?
        for (int i = 0; i < seeds.size(); ++i) {
            ChunkFloodFillSeedBlob3D floodFillSeed = seeds.get(i);
            if (adjacentFloodFillSeed == null && floodFillSeed.addCoord(local)) {
                adjacentFloodFillSeed = floodFillSeed;
            } else {
                if (floodFillSeed.isCoordAdjacent(global)) {
                    adjacentFloodFillSeed.addMembersOfSet(floodFillSeed);
                    seeds.remove(i--);
                }
            }
        }
        if (adjacentFloodFillSeed == null) {
            seeds.add(new ChunkFloodFillSeedBlob3D(local));
        }
    }

    public Coord3 removeNext() {
        if (seeds.size() == 0) return null;
        return Chunk.ToWorldPosition(chunkCoord, seeds.remove(0).getSeed());
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
