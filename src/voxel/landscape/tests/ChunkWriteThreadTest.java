package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.ChunkLocalCoord;
import voxel.landscape.collection.LocalBlockMap;
import voxel.landscape.coord.Coord3;
import voxel.landscape.fileutil.FileUtil;
import voxel.landscape.player.B;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by didyouloseyourdog on 4/8/15.
 */
public class ChunkWriteThreadTest extends TestCase {

    private LocalBlockMap localBlockMap;
    private Coord3 position = new Coord3(0);
    private static final int ITERATION_TIMES = 10;
    private Object synchro = new Object();

    public static void main(String[] args) {
        ChunkWriteThreadTest chunkWriteThreadTest = new ChunkWriteThreadTest();
        chunkWriteThreadTest.testSimulateInterwovenReadWrite();
        B.bugln("\n done");
    }

    @Test
    public void testSimulateInterwovenReadWrite() {
        int bounds = 16;
        localBlockMap = new LocalBlockMap(new Coord3(Chunk.XLENGTH));
        populateChunk(localBlockMap,position,bounds);
        new Thread(new Writer()).start();
        new Thread(new Reader()).start();
    }

    private void populateChunk(LocalBlockMap chunk, Coord3 position, int bounds) {
        for (int i = 0; i < bounds; ++i) {
            for (int j = 0; j < bounds; ++j) {
                for (int y = 0; y < bounds; ++y) {
                    chunk.Set(BlockType.get(i % 7).ordinal(), i, y, j);
                }
            }
        }
    }

    class Writer implements Runnable {
        @Override
        public void run() {
            int times = 0;
            while (times++ < ITERATION_TIMES) {
                try {
                    B.bug("*");
                    synchronized (synchro) {
                        FileUtil.SerializeChunkObjectTestPause(localBlockMap.map, position, FileUtil.LocalBlockMapExtension, 0L);
                        localBlockMap.Set(BlockType.get(times % 4).ordinal(), ChunkLocalCoord.IntToLocal(times));
                        localBlockMap.map.clear();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            int times = 0;
            while (times++ < ITERATION_TIMES) {
                B.bug("-");
                synchronized (synchro) {
                    localBlockMap.map = (HashMap<ChunkLocalCoord, Integer>) FileUtil.DeserializeChunkObjectTestPause(position, FileUtil.LocalBlockMapExtension, 2000L);
                }
            }
        }
    }
}
