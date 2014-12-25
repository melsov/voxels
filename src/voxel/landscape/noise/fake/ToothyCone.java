package voxel.landscape.noise.fake;

import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;

/**
 * Created by didyouloseyourdog on 12/23/14.
 */
public class ToothyCone extends Cone {
    public ToothyCone(Box _box) {
        super(_box);
    }
    @Override
    public boolean isOnBorder(Coord3 co) {
        co = co.clone();
        co.y += co.y % 2 == 0 ? 1 : 0;
        return super.isOnBorder(co);
    }
}
