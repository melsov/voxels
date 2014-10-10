package voxel.landscape.coord;

/**
 * Created by didyouloseyourdog on 10/9/14.
 */
public class ChunkLocalCoord extends Coord3
{
    public ChunkLocalCoord(int _x, int _y, int _z) {
        super(_x, _y, _z);
    }
    public ChunkLocalCoord(Coord3 coord3){
        this(coord3.x, coord3.y, coord3.z);
    }

    @Override
    public int hashCode() {
        return ((z & 15) << 8 ) | ((y & 15) << 4) | (x & 15);
    }
}
