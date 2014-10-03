package voxel.landscape.chunkbuild.blockfacefind;

import voxel.landscape.coord.Direction;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class BlockFaceRecord
{
    private int storage;
    public int hashCode() { return storage; }

    public void setFace(int direction, boolean exists) {
        if (exists) {
            storage |= Direction.BitMasks[direction];
        } else {
            storage &= Direction.NegativeBitMasks[direction];
        }
    }
    public boolean getFace(int direction) {
        return 0 < (storage & Direction.BitMasks[direction]);
    }
    public String toString() {
        return "BlockFace: " + Integer.toBinaryString(storage);
    }
}
