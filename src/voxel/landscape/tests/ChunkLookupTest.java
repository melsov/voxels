package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Coord3;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by didyouloseyourdog on 4/4/15.
 */
public class ChunkLookupTest extends TestCase {

    @Test
    public void testLookup() {
        ConcurrentHashMap<Coord3,Thing> table = new ConcurrentHashMap<>(12);

        for(int i = 0; i < 12; ++i) {
            table.putIfAbsent(new Coord3(0), new Thing());
        }
    }

    class Thing {
        public Thing() {
            System.out.println("Constructor called");
        }

    }


}
