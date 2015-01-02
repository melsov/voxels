package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;

/**
 * Created by didyouloseyourdog on 10/13/14.
 */
public class TestBox extends TestCase {
    @Test
    public void testBoxContains() {
        Box b = new Box(new Coord3(-5), new Coord3(200));
        Coord3 negative = new Coord3(-1,2,2);
        assertTrue("does contain negative coord", b.contains(negative));

    }
}
