package voxel.landscape.tests;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.Chunk;
import voxel.landscape.collection.chunkarray.ChunkHalfNibble3D;
import voxel.landscape.player.B;

import java.util.Random;

/**
 * Created by didyouloseyourdog on 8/13/14.
 */
public class TestHalfNibble3D extends TestCase {

    @Test
    public void testHalfNibble3D () {
        ChunkHalfNibble3D halfNibble3D = new ChunkHalfNibble3D();
        byte[][][] normalByteArray = new byte[Chunk.XLENGTH][Chunk.YLENGTH][Chunk.ZLENGTH];

        Random rand = new Random();
        for (int x = 0 ; x < Chunk.XLENGTH; ++x ) {
            for (int y = 0; y < Chunk.YLENGTH; ++y) {
                for (int z = 0; z < Chunk.ZLENGTH; ++z) {
                    byte num = (byte) rand.nextInt(4);
                    normalByteArray[x][y][z] = num;
                    halfNibble3D.Set(num, x,y,z);
                    B.bugln("at: x: " + x + " y: " + y + " z: " + z + ", setting[][][] to: " + num);
                    B.bugln(" value of half: " + halfNibble3D.Get(x,y,z));

                    for(int zz = z - 1; zz >= 0; --zz) {
                        Assert.assertEquals(normalByteArray[x][y][zz], halfNibble3D.Get(x,y,zz));
                    }

                }
            }
        }
        for (int x = 0 ; x < Chunk.XLENGTH; ++x ) {
            for (int y = 0; y < Chunk.YLENGTH; ++y) {
                for (int z = 0; z < Chunk.ZLENGTH; ++z) {
                    Assert.assertEquals(normalByteArray[x][y][z], halfNibble3D.Get(x,y,z));
                }
            }
        }


    }
}
