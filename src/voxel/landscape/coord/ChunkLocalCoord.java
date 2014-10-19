package voxel.landscape.coord;

import voxel.landscape.Chunk;

/**
 * Created by didyouloseyourdog on 10/9/14.
 */
public class ChunkLocalCoord extends Coord3
{
//    public ChunkLocalCoord(int _x, int _y, int _z) {
//        super(_x, _y, _z);
//    }

    public ChunkLocalCoord(Coord3 coord3){
        super(0);
        Coord3 local = Chunk.toChunkLocalCoord(coord3);
        x = local.x; y = local.y; z =local.z;
    }

    @Override
    public int hashCode() {
        return ((z & 15) << 8 ) | ((y & 15) << 4) | (x & 15);
    }
}
