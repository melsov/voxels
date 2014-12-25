package voxel.landscape.noise.fake;

import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;

/**
 * Created by didyouloseyourdog on 12/23/14.
 */
public class BorderBox {
    public Box box;
    public boolean[] openFaces = new boolean[6];

    public BorderBox(Box b) {
        box = b;
    }

    public boolean isOnBorder(Coord3 co) {
        boolean onBorder = box.isOnBorder(co); // box.contains(co) && (box.start.x == co.x || box.start.y == co.y || box.start.z == co.z ||
//                box.extent().x - 1 == co.x || box.extent().y - 1 == co.y || box.extent().z - 1  == co.z);
        if (onBorder)
            for(int dir : Direction.Directions) {
                if (openFaces[dir]) {
                    onBorder = co.componentForDirection(dir) != box.borderLocation(dir);
                    if (!onBorder) break;
                }
            }
        return onBorder;
    }
}
