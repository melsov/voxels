package voxel.landscape.tests;

import org.junit.Test;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.MeshSet;
import voxel.landscape.chunkbuild.blockfacefind.ChunkLocalCoord;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;

/**
 * Created by didyouloseyourdog on 4/9/15.
 */
public class ChunkInterwovenReadWriteTest implements Runnable {

    private Chunk chunk;
    private Coord3 position = new Coord3(3, 2, -3);
    private static final int ITERATION_TIMES = 100;
    int bounds = -16;
    int start = -32;

    public static void main(String[] args) {
        ChunkInterwovenReadWriteTest chunkInterwovenReadWriteTest = new ChunkInterwovenReadWriteTest();
        new Thread(chunkInterwovenReadWriteTest).start();
    }

    @Test
    public void testSimulateInterwovenReadWrite() {
        TerrainMap map = new TerrainMap(null);
        chunk = new Chunk(position, map);
//        new Thread(new Writer()).start();
        new Thread(new Reader()).start();
        new Thread(new Meddler()).start();
    }

    private void populateChunk(Chunk chunk, Coord3 position) {
        for (int i = start; i < bounds; ++i) {
            for (int j = start; j < bounds; ++j) {
                for (int y = start; y < bounds; ++y) {
                    byte b = (byte) BlockType.get(Math.abs(i) % 7).ordinal();
                    chunk.setBlockAt(b, i, y, j);
                }
            }
        }
        chunk.chunkBlockFaceMap.buildMeshFromMap(chunk, new MeshSet(), new MeshSet(), false, false);
    }

    @Override
    public void run() {
        testSimulateInterwovenReadWrite();
    }

    class Meddler implements Runnable {
        @Override
        public void run() {
            int times = 0;
            while (times++ < ITERATION_TIMES) {
                B.bug("^");
                populateChunk(chunk, position);
            }
        }
    }

    class Writer implements Runnable {
        @Override
        public void run() {
            int times = 0;
            while (times++ < ITERATION_TIMES) {
                B.bug("*");
//                synchronized (chunk) {
                    chunk.readFromFile();
                    chunk.setBlockAt((byte)BlockType.get(times % 7).ordinal(), ChunkLocalCoord.IntToLocal(times) );
//                }
            }
        }
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            int times = 0;
            while (times++ < ITERATION_TIMES) {
                B.bug("-");
//                synchronized (chunk) {
                    chunk.readFromFile();
                    chunk.setBlockAt((byte)BlockType.get(times % 7).ordinal(), ChunkLocalCoord.IntToLocal(times) );
//                }
            }
        }
    }
}
