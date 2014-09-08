package voxel.landscape.tests;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.Chunk;
import voxel.landscape.collection.chunkarray.ChunkNibble3D;
import voxel.landscape.player.B;

import java.util.Random;

/**
 * Created by didyouloseyourdog on 8/13/14.
 */
public class ChunkNibble3DTest2 extends TestCase {

    @Test
    public void testChunkNib3D() {
        ChunkNibble3D nibble3D = new ChunkNibble3D();
        byte[][][] normalByteArray = new byte[Chunk.XLENGTH][Chunk.YLENGTH][Chunk.ZLENGTH];

        Random rand = new Random();
        for (int x = 0 ; x < Chunk.XLENGTH; ++x ) {
            for (int y = 0; y < Chunk.YLENGTH; ++y) {
                for (int z = 0; z < Chunk.ZLENGTH; ++z) {
                    byte num = (byte) rand.nextInt(16);
                    normalByteArray[x][y][z] = num;
                    nibble3D.Set(num, x, y, z);
                    B.bugln("at: x: " + x + " y: " + y + " z: " + z + ", setting[][][] to: " + num);
                    B.bugln(" value of half: " + nibble3D.Get(x, y, z));

                    for(int zz = z - 1; zz >= 0; --zz) {
                        Assert.assertEquals(normalByteArray[x][y][zz], nibble3D.Get(x, y, zz));
                    }

                }
            }
        }
        for (int x = 0 ; x < Chunk.XLENGTH; ++x ) {
            for (int y = 0; y < Chunk.YLENGTH; ++y) {
                for (int z = 0; z < Chunk.ZLENGTH; ++z) {
                    Assert.assertEquals(normalByteArray[x][y][z], nibble3D.Get(x, y, z));
                }
            }
        }
    }
}
