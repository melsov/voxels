package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Coord3;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by didyouloseyourdog on 10/26/14.
 */
public class TestCoord3HashCode extends TestCase {
    @Test
    public void testCoord3HashCode() {
        Set<Integer> hashcodes = new HashSet<>(64 * 500 * 500);
        Coord3 c; int hc;
        Random r = new Random();
        for(int x = 3095; x < 4096; x += r.nextInt(2) + 7)
            for(int y = -28; y < 38; y += 1)
                for(int z = 3095; z < 4096; z += r.nextInt(4) + 7) {
                    c = new Coord3(x,y,z);
                    hc = c.hashCode();
                    assertTrue(" not true for: " + c.toString(), !hashcodes.contains(hc));
                    hashcodes.add(hc);
                }
    }
}
