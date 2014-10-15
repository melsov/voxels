package voxel.landscape.chunkbuild.blockfacefind;

import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord3;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */

public class ChunkBlockFaceCoord
{
    private int storage;

    private static final int localCoordBitMask = Chunk.XLENGTH - 1;

    public ChunkBlockFaceCoord(Coord3 localCoord ) {
        storage = ((localCoord.z & localCoordBitMask ) << (Chunk.SIZE_X_BITS * 2)) |
                ((localCoord.y & localCoordBitMask) << Chunk.SIZE_X_BITS) |
                (localCoord.x & localCoordBitMask);
    }
    public Coord3 toCoord3() {
        return new Coord3(
                storage & localCoordBitMask,
                (storage >>> Chunk.SIZE_X_BITS) & localCoordBitMask,
                (storage >>> Chunk.SIZE_X_BITS * 2) & localCoordBitMask
        );
    }
    @Override
    public boolean equals(Object other) {
        return this.storage == other.hashCode();
    }
    @Override
    public int hashCode() { return storage; }
}

