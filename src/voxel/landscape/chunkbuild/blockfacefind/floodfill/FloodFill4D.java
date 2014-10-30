package voxel.landscape.chunkbuild.blockfacefind.floodfill;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSlice;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSliceBag;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.BoxIterator;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.debug.DebugGeometry;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;
import voxel.landscape.util.Asserter;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/9/14.
 * The fourth 'D' is time
 * Class to manage flood filling as
 * the camera moves around
 */
public class FloodFill4D implements Runnable
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

    // TODO: implement incoming seeds via a blocking queue (shared with TerrainMap) of ChunkFloodFillSeedSets

    public FloodFill4D(TerrainMap _map, Camera _camera, BlockingQueue<Coord3> _floodFilledChunkCoords, AtomicBoolean _shouldStop) {
        map = _map;
        camera = _camera; floodFilledChunkCoords = _floodFilledChunkCoords; shouldStop = _shouldStop;
        outOfBoundsBag = ChunkSliceBag.UnboundedChunkSliceBag();
        inBoundsBag = ChunkSliceBag.ChunkSliceBagWithBounds(boundsFromGlobal(camera.getLocation()));
        floodFill = new FloodFill(map);
    }

    private static int TestFloodFilledCoordsAddedCount = 0;
    private static int GotChunkCoordCount = 0;
    @Override
    public void run() {
        while (!shouldStop.get()) {
            Coord3 chunkCoord = null;
            //IN
            try {
                chunkCoord = map.chunkCoordsToBeFlooded.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Asserter.assertFalseAndDie("this doesn't happen...?");
                break;
            }
            startFlood(chunkCoord);
        }
    }
    private boolean fake() { return true; }

    public void startFlood(Coord3 chunkCoord) {
        Chunk chunk = map.GetChunk(chunkCoord);

        // * SHORT CIRCUIT THE WHOLE FLOOD FILL (DON'T FLOOD FILL) *
//        if (fake()) {
//            try { floodFilledChunkCoords.put(chunkCoord);
//            } catch (InterruptedException e) { e.printStackTrace(); }
//            DebugGeometry.AddDebugChunkSolidSkinny(chunkCoord, ColorRGBA.LightGray);
//            return;
//        }

        // if no seeds (no overhangs). we're done.
        if (chunk.chunkFloodFillSeedSet.size() == 0) {
            try { floodFilledChunkCoords.put(chunkCoord);
            } catch (InterruptedException e) { e.printStackTrace(); }
//            DebugGeometry.AddDebugChunkSolid(chunkCoord, ColorRGBA.Pink);
            return;
        }

        Coord3 seed;
        while(chunk.chunkFloodFillSeedSet.size() > 0) {
            seed = chunk.chunkFloodFillSeedSet.removeNext();
            Asserter.assertTrue(Box.WorldCoordBoxForChunkCoord(chunk.position).contains(seed), "seed not inside of chunk?");
            flood(seed);
        }
    }

    private void flood(Coord3 initialSeed) {
        Coord3 initialChunkCoord = Chunk.ToChunkPosition(initialSeed);

        /*
         * flood fill the initial chunk
         * and add its 'shell' to in-bounds bag
         */
        ChunkSlice[] seedChunkShell = new ChunkSlice[6];
        initializeChunkShell(seedChunkShell, initialSeed);

        inBoundsBag.setBounds(boundsFromChunkCoord(initialChunkCoord));
        floodFill.flood(seedChunkShell, initialSeed);


        try { floodFilledChunkCoords.put(initialChunkCoord);
        } catch (InterruptedException e) { e.printStackTrace(); }


        /* prime the inBoundsBag from chunkShell after flood filling the initial seed */
        for(int i = 0; i <= Direction.ZPOS; ++i) {
            if (seedChunkShell[i].size() > 0) inBoundsBag.add(seedChunkShell[i]);
            else { //DBUG?? or needed?
//                DebugGeometry.AddDebugChunkSolid(seedChunkShell[i].getChunkCoord(), new ColorRGBA(0f, .1f, .7f, .5f));
            }
        }

        // TODO: test flood lines ability to move down and across by chunks (it seems questionable)
        /*
         * get a slice from the in-bounds bag,
         * flood fill it and add it's shell sides
         * to either out or in bounds
         */
        depleteBag: while(inBoundsBag.size() > 0)
        {
            ChunkSlice chunkSlice = null;
            /*
             * get a chunk slice
             */
            ColumnMap columnMap = map.getApp().getColumnMap();
            findChunkSliceInBuiltColumn: do {
                if (inBoundsBag.size() == 0) {
                    B.bugln("....out of chunkslices. none with non zero size. breaking " + " initial seed: " + initialSeed.toString());
                    break depleteBag;
                }
                // find a slice whose column IS_BUILT
                List<ChunkSlice> iBBSlices = inBoundsBag.getSlices();
                for(int i = 0; i < iBBSlices.size(); ++i) {
                    ChunkSlice slice = iBBSlices.remove(i); // iBBSlices.get(i);
                    if (slice.size() == 0) {  Asserter.assertFalseAndDie("happens?"); continue; } //TEST? NOT SURE?

                    if (columnMap.IsBuilt(slice.getChunkCoord().x, slice.getChunkCoord().z)) {
                        chunkSlice = slice;
                        break findChunkSliceInBuiltColumn;
                    } else {
                        DebugGeometry.AddDebugChunkSolidSkinny(slice.getChunkCoord(), ColorRGBA.Red);
                        outOfBoundsBag.add(slice); //TODO: figure how to handle out of bounds becoming inbounds
                        --i;
                    }
                }
                try {
                    B.bugln("sleeping. no inBounds slices");
                    Thread.sleep(1); //TODO: consider a way around this? Is there anything else to do?
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (chunkSlice == null);

            /* ***** WANT *****
            if (!boundsFromGlobal(camera.getLocation()).contains(chunkSlice.getChunkCoord())){
                outOfBoundsBag.add(chunkSlice);
                continue;
            }
            */

            boolean didAddFaces = false;
            while(chunkSlice.size() > 0) {
                Coord3 seed =  chunkSlice.removeNext();
                ChunkSlice[] chunkShell = new ChunkSlice[6];
                initializeChunkShell(chunkShell, seed);

                didAddFaces = floodFill.flood(chunkShell, seed) ? true : didAddFaces;
                // inBoundsBag.setBounds(boundsFromGlobal(camera.getLocation())); // WANT!!!
                for (int i = 0; i <= Direction.ZPOS; ++i) {
                    ChunkSlice chunkShellSlice = chunkShell[i];
                    if (chunkShellSlice.size() == 0) {
                        continue;
                    }
                    if (!inBoundsBag.add(chunkShellSlice)) {
                        outOfBoundsBag.add(chunkShellSlice);
                    }
                }
            }

            // CONSIDER: IF SAME CHUNK COORD IN NEXT CHUNK SLICE, DON'T ADD CHUNK YET
            if (didAddFaces && map.GetChunk(chunkSlice.getChunkCoord()) != null) {
                try {floodFilledChunkCoords.put(chunkSlice.getChunkCoord());
                    Asserter.assertTrue(floodFilledChunkCoords.size() < 600, " hmmm..");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (map.GetChunk(chunkSlice.getChunkCoord()) == null) {
                DebugGeometry.AddDebugChunkSolidSkinny(chunkSlice.getChunkCoord(), ColorRGBA.Magenta);
            } else if (!didAddFaces) {
                DebugGeometry.AddDebugChunkSolidSkinny(chunkSlice.getChunkCoord(), ColorRGBA.Green);
            }
        }
    }

    private Box boundsFromGlobal(Vector3f global) {
        return boundsFromChunkCoord(Chunk.ToChunkPosition(Coord3.FromVector3f(global)));
    }
    private Box boundsFromChunkCoord(Coord3 chunkCo) {
        return new Box(Coord3.Zero.clone(), new Coord3(8, 4, 8)); //TEST
//        Coord3 start = chunkCo.minus(FFBoundsHalf);
//        start.y = Math.max(start.y, 0);
//        return new Box(start, FFBoundsHalf.multy(2)); //WANT
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
