package voxel.landscape.collection.coordmap;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.player.B;

/**
 * Created by didyouloseyourdog on 7/28/14.
 */
public class HashCoord3DTest extends TestCase {

    @Test
    public void testPutGet() {
        String put = "you there";
        Thing thing = new Thing(put);
        HashCoord3D<Thing> strings = new HashCoord3D<Thing>(Thing.class);
        strings.Set(thing, 0,0,0);
        Thing got = strings.Get(0,0,0);

        if (got == null) { B.bug("got null??/"); }
        B.bugln(got.name);
        Assert.assertTrue(got.equals(thing));
        B.bug(thing.toString());
        B.bug(got.toString());
    }
}

class Thing
{
    public String name;
    public Thing(String _name) {
        name = _name;
    }
//    @Override
//    public boolean equals(Object other) {
//        if (other.getClass() != Thing.class) {
//            return false;
//        }
//        return name.equals(((Thing) other).name);
//    }
}
