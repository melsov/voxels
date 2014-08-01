package voxel.landscape.tests;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.collection.ByteArray3D;
import voxel.landscape.collection.unused.ByteArray3DNoBitShifting;
import voxel.landscape.coord.Coord3;
import voxel.landscape.player.B;

import java.util.Random;

/**
 * Created by didyouloseyourdog on 7/31/14.
 */
public class ByteArray3DTest extends TestCase {

    @Test
    public void testByteArray3D() {
        Coord3 size = new Coord3(16,16,16);
        ByteArray3D ba = new ByteArray3D(size);
        ByteArray3DNoBitShifting bans = new ByteArray3DNoBitShifting(size);
        Random rand = new Random();
        for (int i=0; i<size.x; ++i) {
            for(int j=0; j<size.z; ++j) {
                for(int k=0; k<size.y; ++k) {
                    int r = rand.nextInt(128);
                    B.bug(i);
                    B.bug(j);
                    B.bug(k);
                    B.bugln("# random: " + r);
                    int baresult = ba.TestGetIndex(i, k, j);
                    int bansresult = ba.TestGetIndexNoShifting(i, k, j);
                    Assert.assertEquals(baresult, bansresult);
                    ba.Set((byte)r,i,k,j);
                    bans.Set((byte)r,i,k,j);
                    baresult = ba.Get(i,k,j);
                    bansresult = bans.Get(i,k,j);
                    Assert.assertEquals(baresult,bansresult);
                }
            }
        }

    }


//    public void testByteArray3D() {
//        Coord3 size = new Coord3(16,16,16);
//        ByteArray3D ba = new ByteArray3D(size);
//        ByteArray3DNoBitShifting bans = new ByteArray3DNoBitShifting(size);
//        Random rand = new Random();
//        for (int i=0; i<size.x; ++i) {
//            for(int j=0; j<size.z; ++j) {
//                for(int k=0; k<size.y; ++k) {
//                    int r = rand.nextInt(128);
//                    ba.Set((byte)r,i,k,j);
//                    bans.Set((byte)r,i,k,j);
//                    int baresult = ba.Get(i,k,j);
//                    int bansresult = bans.Get(i,k,j);
//                    Assert.assertEquals(baresult,bansresult);
//                }
//            }
//        }
//
//    }



}
