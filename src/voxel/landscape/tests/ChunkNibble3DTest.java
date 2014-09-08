package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import voxel.landscape.collection.chunkarray.ChunkNibble3D;
import voxel.landscape.collection.unused.ChunkByte3D;

import java.util.Random;

/**
 * Created by didyouloseyourdog on 7/28/14.
 */
public class ChunkNibble3DTest extends TestCase {

    @Test
    public void testShiftingTest()
    {
        ChunkByte3D cb = new ChunkByte3D();
        ChunkNibble3D cn = new ChunkNibble3D();

        int dim = 16;
        float epsilon = 0f;
        Random generator = new Random();
        for (int i=0; i < dim; ++i) {
            for (int j=0; j < dim; ++j)
                for (int k = 0; k < dim; ++k) {
                    int val = generator.nextInt(16);
                    cb.Set(val,i,j,k);
                    cn.Set(val,i,j,k);
                    int byteval = cb.Get(i, j, k);
                    int nibbleval = cn.Get(i, j, k);
                    Assert.assertTrue(cb.Get(i, j, k) == cn.Get(i, j, k));
                }
        }
    }

}
