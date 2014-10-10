package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice;

import voxel.landscape.coord.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/9/14.
 */
public class ChunkSliceBag {
    private List<ChunkSlice> slicesWithin = new ArrayList<ChunkSlice>(128);
    private List<ChunkSlice> slicesWithout = new ArrayList<ChunkSlice>(128);
    private Box bounds;
    public Box getBounds() { return bounds; }
    public void setBounds(Box _bounds) {
        //CONSIDER: for each out of bounds slice remove?? maybe not nec. we can sort out while iterating...
        bounds = _bounds;
    }
    private ChunkSliceBag() {}
    public static ChunkSliceBag ChunkSliceBagWithBounds(Box _bounds) {
        ChunkSliceBag chunkSliceBag = new ChunkSliceBag();
        chunkSliceBag.bounds = _bounds;
        return chunkSliceBag;
    }
    public static ChunkSliceBag UnboundedChunkSliceBag() {
        return new ChunkSliceBag();
    }
    public void add(ChunkSlice chunkSlice) {

    }
    public ChunkSlice removeNext() {
        if (slicesWithin.size() == 0) return null;
        return slicesWithin.remove(0);
    }
    public int size() { return slicesWithin.size(); }
}
