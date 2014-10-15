package voxel.landscape.chunkbuild.blockfacefind.floodfill;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSlice;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSliceBag;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.BoxIterator;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;
import voxel.landscape.util.Asserter;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/9/14.
 * The fourth 'D' is time
 * Class to manage flood filling as
 * the camera moves around
 */
public class FloodFill4D
{
    private Camera camera;
    public BlockingQueue<Coord3> floodFilledChunkCoords;
//    public BlockingQueue<Coord3> floodFilledChunkCoords;

    private ChunkSliceBag inBoundsBag;
    private ChunkSliceBag outOfBoundsBag;
    private AtomicBoolean shouldStop;
    private TerrainMap map;
    public static final int UntouchedType = BlockType.NON_EXISTENT.ordinal();
    private static Coord3 FFBoundsHalf = new Coord3(1,1,2); // TerrainMap.MAX_CHUNK_COORD.minus(TerrainMap.MIN_CHUNK_COORD).divideBy(new Coord3(8));
    private FloodFill floodFill;

    public FloodFill4D(TerrainMap _map, Camera _camera, BlockingQueue<Coord3> _floodFilledChunkCoords, AtomicBoolean _shouldStop) {
        map = _map;
        camera = _camera; floodFilledChunkCoords = _floodFilledChunkCoords; shouldStop = _shouldStop;
        outOfBoundsBag = ChunkSliceBag.UnboundedChunkSliceBag();
        inBoundsBag = ChunkSliceBag.ChunkSliceBagWithBounds(boundsFromGlobal(camera.getLocation()));
        floodFill = new FloodFill(map);
    }
    private Box boundsFromGlobal(Vector3f global) {
        return boundsFromChunkCoord(Chunk.ToChunkPosition(Coord3.FromVector3f(global)));
    }
    private Box boundsFromChunkCoord(Coord3 chunkCo) {
        return new Box(Coord3.Zero.clone(), new Coord3(4, 2, 6)); //TEST
//        Coord3 start = chunkCo.minus(FFBoundsHalf);
//        start.y = Math.max(start.y, 0);
//        return new Box(start, FFBoundsHalf.multy(2)); //WANT
    }

    private boolean testSurfaceMapping() {
        for (int x = 0 ; x < 4; x++)
            for(int z = 0; z < 4; z++)
                for(int y = 0; y < 4; y++) {
                    Coord3 co = new Coord3(x,y,z);
                    Chunk chunk = map.lookupOrCreateChunkAtPosition(co);
                    try {
                        floodFilledChunkCoords.put(co);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        B.bugln("done");
        return true;
    }

    public void flood(boolean forever) {
        if (testSurfaceMapping()) return; //TEST!!!
        Coord3 initialSeed = new Coord3(14, 0, 14); //TEST WANT-> // Coord3.FromVector3f(camera.getLocation());

        initialSeed = floodFill.findAirCoord(initialSeed);
        B.bugln("initial seed: " + initialSeed.toString());
        Coord3 initialChunkCoord = Chunk.ToChunkPosition(initialSeed);
        /*
         * flood fill the camera's location chunk
         * and collect add it 'shell' to in-bounds bag
         */
        ChunkSlice[] seedChunkShell = new ChunkSlice[6];
        initializeChunkShell(seedChunkShell, initialSeed);

        inBoundsBag.setBounds(boundsFromChunkCoord(initialChunkCoord));
        floodFill.flood(seedChunkShell, initialSeed);

        try { floodFilledChunkCoords.put(initialChunkCoord);
        } catch (InterruptedException e) { e.printStackTrace(); }

        for(int i = 0; i <= Direction.ZPOS; ++i) { inBoundsBag.add(seedChunkShell[i]); }

        /*
         * get a slice from the in-bounds bag,
         * flood fill it and add it's shell sides
         * to either out or in bounds
         */
        HashSet<Coord3> testFloodedChunkCoords = new HashSet<Coord3>(64);
        depleteBag: while(inBoundsBag.size() > 0) {
            ChunkSlice chunkSlice;
            do {
                if (inBoundsBag.size() == 0) {
                    Asserter.assertFalseAndDie("didn't think this would happen? out of chunkslices. none with non zero size. breaking");
                    break depleteBag;
                }
                chunkSlice = inBoundsBag.removeNext();
            } while ( chunkSlice.size() == 0);

            /* ***** WANT *****
            if (!boundsFromGlobal(camera.getLocation()).contains(chunkSlice.getChunkCoord())){
                outOfBoundsBag.add(chunkSlice);
                continue;
            }
            */
            testFloodedChunkCoords.add(chunkSlice.getChunkCoord());

            while(chunkSlice.size() > 0) {
                Coord3 seed =  chunkSlice.removeNext();
                ChunkSlice[] chunkShell = new ChunkSlice[6];
                initializeChunkShell(chunkShell, seed);

                floodFill.flood(chunkShell, seed);
                // inBoundsBag.setBounds(boundsFromGlobal(camera.getLocation())); // WANT!!!
                for (int i = 0; i <= Direction.ZPOS; ++i) {
                    ChunkSlice chunkShellSlice = chunkShell[i];
                    if (chunkShellSlice.size() == 0) continue;
                    if (!inBoundsBag.add(chunkShellSlice)) {
                        outOfBoundsBag.add(chunkShellSlice);
                    }
                }
            }
            // CONSIDER: IF SAME CHUNK COORD IN NEXT CHUNK SLICE, DON'T ADD CHUNK YET
//            if (map.GetChunk(chunkSlice.getChunkCoord()) != null) {
//                try {
//                    floodFilledChunkCoords.put(chunkSlice.getChunkCoord());
//                    Asserter.assertTrue(floodFilledChunkCoords.size() < 200, " hmmm..");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

        }
        // add only after done
        for (Coord3 co : testFloodedChunkCoords) {
            if (map.GetChunk(co) != null) {
                try {
                    floodFilledChunkCoords.put(co);
                    Asserter.assertTrue(floodFilledChunkCoords.size() < 200, " hmmm..");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void debugPrintHashSet(HashSet<Coord3> coord3s, Box bounds) {
        BoxIterator bi = new BoxIterator(bounds);
        B.bugln("---COORDS---");
        while(bi.hasNext()) {
            Coord3 n = bi.next();
            if (n.x == bounds.start.x) {
                B.bugln("");
            }
            String r;
            if (coord3s.contains(n)) {
                r = "X-";
            } else {
                r = "0-";
            }
            B.bug(r);
        }
        B.bugln("");
    }

    private void initializeChunkShell(ChunkSlice[] chunkSlices, Coord3 globalBlockCoord) {
        Coord3 chunkPosition = Chunk.ToChunkPosition(globalBlockCoord);
        for(int i = 0; i <= Direction.ZPOS; ++i) {
            chunkSlices[i] = new ChunkSlice(chunkPosition.add(Direction.DirectionCoords[i]),i);
        }
    }

}
