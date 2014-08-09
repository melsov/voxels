package voxel.landscape.tests;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Coord3;
import voxel.landscape.player.B;

import java.util.HashMap;

/**
 * Created by didyouloseyourdog on 8/3/14.
 */
public class TestHashCodes extends TestCase {

    @Test
    public void testHash() {

        int xmin, zmin, ymin, xmax, zmax, ymax;
        xmin = -2;
        zmin = -1;
        ymin = -1;
        xmax = zmax = 2;
        ymax = 2;
        HashMap<Integer, Coord3> codes = new HashMap<Integer, Coord3>();

        for(int x = xmin; x < xmax; ++x) {
//            for(int y = ymin; y < ymax; ++y) {
//                for(int z = zmin; z < zmax; ++z){
                    Coord3 c = new Coord3(x,0,0);
                    int hash = c.hashCode();
//                    if (codes.containsKey(hash)) {
//
//                    B.bug(bitString(hash)+ " ");
                        B.bugln("coord: "+c.toString());
//                    B.bugln(" : key: "+ hash + " ");
//                    }
                    Assert.assertTrue(!codes.containsKey(hash));
                    codes.put(hash, c);

//                    if (x == 0) {
//                        continue;
//                    }
                    // bitwise flip of x
                    int bitflipx = (x ^ -1) & Integer.MAX_VALUE;
                    Coord3 cxflip = new Coord3(bitflipx,0,0);
                    B.bugln("cxflip: " + cxflip.toString());
                    if (codes.containsKey(cxflip.hashCode())){
                        int fliphash = cxflip.hashCode();
                        Coord3 already = codes.get(fliphash);
                        B.bugln("tried to add: \n" + cxflip.toString() +
                                "\n but this co already had key: \n" + already.toString());
                        B.bugln("flip hash: \n" + fliphash + "\n same as: \n" + already.hashCode());
                        B.bugln("bit flip x " + bitflipx + " x: " + x);
                    }
                    Assert.assertTrue("flip fail x: " +x+" flipX: "+bitflipx+
                            " \n"+bitString(x)+"\n"+bitString(bitflipx) , !codes.containsKey(cxflip.hashCode()));
//                }
//            }
        }
        B.bug("codes len: " + codes.size());
        Assert.assertEquals("ya", ((int) (1 * 1f)), 1);
    }

    private String bitString(int x) {
        char[] chars = new char[32];
        for(int i=31; i >= 0; --i) {
            chars[i] = (x & 1) == 1 ? '1' : '0';
            x = x >> 1;
        }
        return new String(chars);
    }
}
