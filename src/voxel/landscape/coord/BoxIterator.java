package voxel.landscape.coord;

import java.util.Iterator;

/**
 * Created by didyouloseyourdog on 10/13/14.
 */
public class BoxIterator implements Iterator<Coord3> {

    public Box box;
    private Coord3 marker; // = new Coord3(0);

    public int getSize() { return box.dimensions.x * box.dimensions.y * box.dimensions.z; }

    public BoxIterator(Box _box) {
        box = _box;
        marker = box.start.clone();
    }

    @Override
    public boolean hasNext() {
        return marker.y < box.extent().y;
    }

    private void incrementMarker() {
        marker.x++;
        if (marker.x == box.extent().x) {
            marker.x = box.start.x;
            marker.z++;
            if (marker.z == box.extent().z) {
                marker.z = box.start.z;
                marker.y++;
            }
        }
    }
    @Override
    public Coord3 next() {
        if (!hasNext()) return null;
        Coord3 result = marker.clone();
        incrementMarker();
        return result;
    }

    @Override
    public void remove() {
        incrementMarker();
//        Asserter.assertFalseAndDie("one doesn't remove from a Box Iterator. Just not done.");
    }
}
