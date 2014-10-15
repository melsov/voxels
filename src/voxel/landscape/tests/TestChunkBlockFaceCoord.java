package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.ChunkBlockFaceCoord;
import voxel.landscape.coord.Coord3;

/**
 * Created by didyouloseyourdog on 10/15/14.
 */
public class TestChunkBlockFaceCoord extends TestCase {

    @Test
    public void testChunkBlockFaceCo() {
        for (int i = 16; i < 34; ++i) {
            Coord3 c = new Coord3(0,i,0);
            ChunkBlockFaceCoord cb  = new ChunkBlockFaceCoord(c);
            assertEquals(Chunk.toChunkLocalCoord(c), cb.toCoord3());
        }
    }
}
