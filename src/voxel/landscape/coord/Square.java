package voxel.landscape.coord;

/**
 * Created by didyouloseyourdog on 12/29/14.
 */
public class Square {
    public Coord2 start;
    public Coord2 dimensions;

    public Square(Coord2 _start, Coord2 _dims) {
        start = _start; dimensions = _dims;
    }
    public Coord2 extent() {
        return start.add(dimensions);
    }

    public boolean containsXZ(Coord3 co) {
        return contains(new Coord2(co.x, co.z));
    }
    public boolean contains(Coord2 coord2) {
        return coord2.greaterThanOrEqual(start) && extent().greaterThan(coord2);
    }

}
