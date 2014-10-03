package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.chunkbuild.blockfacefind.BlockFaceRecord;
import voxel.landscape.chunkbuild.blockfacefind.ChunkBlockFaceCoord;
import voxel.landscape.collection.chunkface.ChunkBlockFaceMap;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.player.B;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class TestBlockFaceRecord extends TestCase {

    @Test
    public void testBlockFaceSetGet() {
        BlockFaceRecord blockFaceRecord = new BlockFaceRecord();

        for (int dir : Direction.Directions) {
            blockFaceRecord.setFace(dir, true);
            assertTrue("should be true. just set it", blockFaceRecord.getFace(dir));
            blockFaceRecord.setFace(dir, false);
        }
    }
    @Test
    public void testChunkBlockFaceCoord() {
        B.bug("test c block f");
        for(int i=0; i< 16; i++)
            for(int j=0; j < 16; j++)
                for(int k=0; k < 16; k++) {
                    Coord3 co = new Coord3(i,j,k);
                    ChunkBlockFaceCoord blockFaceCoord = new ChunkBlockFaceCoord(co);
                    String testStr = "coords should be equal " + co.toString() + " : bFToCoord: " + blockFaceCoord.toCoord3().toString();
                    assertTrue(testStr,
                            co.equal(blockFaceCoord.toCoord3()));
                    assertTrue("not equal", !blockFaceCoord.equals(new ChunkBlockFaceCoord(new Coord3(i, j, k + 1))));
                }
    }

    @Test
    public void testChunkBlockFaceMap() {
        ChunkBlockFaceMap blockFaceMap = new ChunkBlockFaceMap();
        B.bug("test c block f Map");
        for(int i=0; i< 16; i++)
            for(int j=0; j < 16; j++)
                for(int k=0; k < 16; k++) {
                    Coord3 co = new Coord3(i,j,k);
                    for (int dir : Direction.Directions) {
                        assertNotNull(co);
                        blockFaceMap.setFace(co, dir, true);
                        String testStr = "just set it: " + co.toString();
                        assertTrue(testStr,
                                blockFaceMap.getFace(co, dir));
                    }
                }
    }
}
