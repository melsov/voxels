package voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice;

import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/9/14.
 */
public class ChunkSliceBag {
    private List<ChunkSlice> slices = new ArrayList<ChunkSlice>(128);
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
    public boolean add(ChunkSlice chunkSlice) {
        if (contains(chunkSlice.getChunkCoord())) {
            slices.add(chunkSlice);
            return true;
        }
        return false;
    }
    private boolean contains(Coord3 co) {
        if (bounds == null) return true;
        return bounds.contains(co);
    }
    public ChunkSlice removeNext() {
        if (slices.size() == 0) return null;
        return slices.remove(0);
    }
    public int size() { return slices.size(); }


}
