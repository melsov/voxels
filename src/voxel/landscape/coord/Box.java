package voxel.landscape.coord;

import voxel.landscape.Chunk;

/**
 * Created by didyouloseyourdog on 10/5/14.
 */
public class Box {
    public Coord3 start;
    public Coord3 dimensions;
    public Coord3 extent() {
        return start.add(dimensions);
    }
    public Box(Coord3 start_, Coord3 dimensions_) {
        start = start_; dimensions = dimensions_;
    }
    public boolean contains(Coord3 co) {
        return dimensions.greaterThan(co.minus(start));
    }
    public int outsideOfInDirection(Coord3 co) {
        Coord3 dif = co.minus(start);
        if (dif.x < 0) {
            return Direction.XNEG;
        } else if (dif.y < 0) {
            return Direction.YNEG;
        } else if (dif.z < 0) {
            return Direction.ZNEG;
        }
        Coord3 outerDif = extent().minus(co);
        if (outerDif.x <= 0) {
            return Direction.XPOS;
        } else if (outerDif.y <= 0) {
            return Direction.YPOS;
        } else if (outerDif.z <= 0) {
            return Direction.ZPOS;
        }
        return -1;
    }
    public static Box WorldCoordBoxForChunkAtWorldCoord(Chunk chunk) {
        return new Box(chunk.originInBlockCoords(), Chunk.CHUNKDIMS);
    }
    public static Box WorldCoordBoxForChunkCoord(Coord3 chunkStart, Coord3 chunkEnd) {
        Coord3 start = Chunk.ToWorldPosition(chunkStart);
        return new Box(start, Chunk.ToWorldPosition(chunkEnd).minus(start));
    }

}
