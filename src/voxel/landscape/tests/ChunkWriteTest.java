package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord3;
import voxel.landscape.fileutil.FileUtil;
import voxel.landscape.noise.TerrainDataProvider;
import voxel.landscape.player.B;

import java.io.File;

/**
 * Created by didyouloseyourdog on 4/7/15.
 */
public class ChunkWriteTest extends TestCase {

    @Test
    public void testChunkWrite() {
        B.bug(readTimeOverGenTime(16));

//        long oneSizePre = timeNanosForBounds(1);
//        long oneSize = timeNanosForBounds(1);
//        long twoSize = timeNanosForBounds(2);
//        long sixteenSize = timeNanosForBounds(16);
//        B.bug(oneSizePre, oneSize, twoSize, twoSize / oneSize, sixteenSize, sixteenSize / twoSize);
//        B.bug(sixteenSize/1000000D);
    }

    private long fileSizeForBounds(int boundsXZ) {
        Coord3 position = new Coord3(0);
        Chunk chunk = new Chunk(position, null);

        for (int i = 0; i < boundsXZ; ++i) {
            for (int j = 0; j < boundsXZ; ++j) {
                for (int y = 0; y < boundsXZ; ++y) {
                    chunk.setBlockAt((byte) BlockType.BEDROCK.ordinal(), i, y, j);
                }
            }
        }

        chunk.writeToFile();
        assertTrue("chunk at 0, 0 not write dirty. ", !chunk.isWriteDirty());
        chunk = new Chunk(position, null);

        chunk.readFromFile();


        for (int i = 0; i < boundsXZ; ++i) {
            for (int j = 0; j < boundsXZ; ++j) {
                for (int y = 0; y < boundsXZ; ++y) {
                    assertEquals(chunk.blockAt(i, y, j), BlockType.BEDROCK.ordinal());
                }
            }
        }
        String fileName = FileUtil.ChunkFile(position, FileUtil.LocalBlockMapExtension, true);
        File file = new File(fileName);
        return file.length();
    }

    private long timeNanosForBounds(int boundsXZ) {
        Coord3 position = new Coord3(0);
        Chunk chunk = new Chunk(position, null);

        for (int i = 0; i < boundsXZ; ++i) {
            for (int j = 0; j < boundsXZ; ++j) {
                for (int y = 0; y < boundsXZ; ++y) {
                    chunk.setBlockAt((byte) BlockType.get(i % 7).ordinal(), i, y, j);
                }
            }
        }

        chunk.writeToFile();
        assertTrue("chunk at 0, 0 not write dirty. ", !chunk.isWriteDirty());
        chunk = new Chunk(position, null);

        long start = System.nanoTime();
        chunk.readFromFile();
        long end = System.nanoTime();

        for (int i = 0; i < boundsXZ; ++i) {
            for (int j = 0; j < boundsXZ; ++j) {
                for (int y = 0; y < boundsXZ; ++y) {
                    assertEquals(chunk.blockAt(i, y, j), BlockType.get(i % 7).ordinal());
                }
            }
        }
        return end - start;
    }

    private double readTimeOverGenTime(int bounds) {
        long readTime = timeNanosForBounds(bounds);
        long genTime = terrainGenTime(bounds);
        return readTime/(double) genTime;
    }

    private long terrainGenTime(int boundsXZ) {
        long start = System.nanoTime();
        Coord3 position = new Coord3(0);
        Chunk chunk = new Chunk(position, null);
        TerrainDataProvider terrainDataProvider = new TerrainDataProvider(TerrainDataProvider.Mode.NoiseModule, -21234);
        for (int i = 0; i < boundsXZ; ++i) {
            for (int j = 0; j < boundsXZ; ++j) {
                for (int y = 0; y < boundsXZ; ++y) {
                    chunk.setBlockAt((byte) terrainDataProvider.getBlockDataAtPosition(i, y, j), i, y, j);
                }
            }
        }
        long end = System.nanoTime();
        return end - start;
    }

}
