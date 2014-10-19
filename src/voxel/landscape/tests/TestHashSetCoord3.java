package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Coord3;

import java.util.HashSet;

/**
 * Created by didyouloseyourdog on 10/16/14.
 */
public class TestHashSetCoord3 extends TestCase {

    @Test
    public void testHashSetCoord3() {
        HashSet<Coord3> set = new HashSet<>(50);
        Coord3 co = new Coord3(-20, 10, 40);
        Coord3 co2 = new Coord3(-20, 10, 40);

        set.add(co);
        set.add(co2);
        assertTrue("wha?", set.size() == 1);
    }
}
