package voxel.landscape.chunkbuild.blockfacefind.floodfill;

import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSlice;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.LightComputerUtils;
import voxel.landscape.util.Asserter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/5/14.
 */
public class FloodFill
{
    private final TerrainMap map;
    public final HashSet<Coord3> dirtyChunks = new HashSet<>(24);
    private AtomicBoolean shouldStop;

    private static List<Coord3> fakeChunkList = new ArrayList<Coord3>();
    private static int fakeRange = 2;
    static {
        for(int x = -fakeRange; x < fakeRange ; ++x ) {
            for(int z = -fakeRange; z < fakeRange; ++z) {
                for(int y = 0; y < TerrainMap.GetWorldHeightInChunks(); ++y) {
                    fakeChunkList.add(new Coord3(x,y,z));
                }
            }
        }
    }

    public FloodFill(TerrainMap _map, AtomicBoolean _shouldStop) {
        map = _map;
        shouldStop = _shouldStop;
    }



    public void flood(ChunkSlice[] chunkSliceShell, Coord3 seedGlobal) {
//        dirtyChunks.clear(); // dirty chunks keeps track of edited chunks
        dirtyChunks.add(Chunk.ToChunkPosition(seedGlobal));

        ChunkSlice yPosChunkSlice = new ChunkSlice(Chunk.ToChunkPosition(seedGlobal), Direction.YPOS);
        ChunkSlice yNegChunkSlice = new ChunkSlice(Chunk.ToChunkPosition(seedGlobal), Direction.YNEG);
        floodScanLines(chunkSliceShell,yPosChunkSlice, yNegChunkSlice, seedGlobal, FloodFill4D.UntouchedType);
        List<ChunkSlice> slices = new ArrayList<ChunkSlice>(25);
        slices.add(yNegChunkSlice);
        slices.add(yPosChunkSlice);

        while(slices.size() > 0) {
            ChunkSlice nextSlice = slices.remove(0);

            ChunkSlice yPosChunkSliceNext = new ChunkSlice(Chunk.ToChunkPosition(seedGlobal), Direction.YPOS);
            ChunkSlice yNegChunkSliceNext = new ChunkSlice(Chunk.ToChunkPosition(seedGlobal), Direction.YNEG);
            while(nextSlice.size() > 0) {
                Coord3 nextSeed = nextSlice.removeNext();
                floodScanLines(chunkSliceShell, yPosChunkSliceNext, yNegChunkSliceNext, nextSeed, FloodFill4D.UntouchedType );
            }
            if (yPosChunkSliceNext.size() > 0) {
                slices.add(yPosChunkSliceNext);
            }
            if (yNegChunkSliceNext.size() > 0) {
                slices.add(yNegChunkSliceNext);
            }
        }
    }


    private static void addSeed(ArrayList<Coord3> seeds, Coord3 seed) {
        seeds.add(seed);
    }

    private ChunkSlice chunkSliceFromShell(ChunkSlice[] shell, int direction) {
        return shell[direction];
    }

    private static boolean IsCoordInDebugArea(Coord3 co) {
        Box debugBox = new Box(
                new Coord3(0,14,14),
                new Coord3(17, 17, 5)
        );
        return debugBox.contains(co);
    }

    /*
     * Finds transparent blocks (mostly air) below the surface in a given chunk, starting at a given seed coord
     * @param initialSeedGlobal
     * Uses the scan lines algorithm.
     * New seeds one block above and one block below (in Y) are store in the yPos/Neg "chunk slices"
     * New seeds found outside the chunk are also stored in chunkSlices (the ones in @param chunkSliceShell )
     * the "untouchedType" is always BlockType.NON_EXISTENT. It's a parameter b/c this might help provide some flexibility to
     * flood fill non air in the future?? (this idea is not really born out anywhere else) (TODO: decide what we need vis-a-vis flood filling in general).
     */
    public void floodScanLines(ChunkSlice[] chunkSliceShell, ChunkSlice yPosChunkSlice, ChunkSlice yNegChunkSlice, Coord3 initialSeedGlobal, int untouchedType) {

        // TODO: deal with water!

        ArrayList<Coord3> seeds = new ArrayList<Coord3>(Chunk.XLENGTH * Chunk.YLENGTH * Chunk.ZLENGTH);

        Asserter.assertTrue(initialSeedGlobal != null, "initial seed null...not good");

        Chunk seedChunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(initialSeedGlobal));
        Box chunkBox = Box.WorldCoordBoxForChunk(seedChunk);

        /* wasIs arrays used to check whether blocks were unset before lookup:
         * we (mostly?) want to continue flooding only over air blocks that were of type "non_existant"
         * before we looked them up. *wasIs[0] stores the type before lookup.
         * *wasIs[1] stores the type after lookup */
        byte[] thisCoordWasIs = new byte[2];
        byte[] xNEGWasIs = new byte[2];
        byte[] xPOSWasIs = new byte[2];
        byte[] zNEGWasIs = new byte[2];
        byte[] zPOSWasIs = new byte[2];
        byte[] yNEGWasIs = new byte[2];
        byte[] yPOSWasIs = new byte[2];

        getCurrentWasIsWithinChunk(initialSeedGlobal, seedChunk, thisCoordWasIs, (byte) untouchedType);
        if (map.isAboveSurface(initialSeedGlobal) ||  !BlockType.IsNonExistentOrPlaceHolderAir(thisCoordWasIs[0])) {
            return;
        }

        seeds.add(initialSeedGlobal);
        int z1;
        boolean spanXNEG, spanXPOS;

        int xAreaStart = chunkBox.start.x, yAreaStart = chunkBox.start.y, zAreaStart = chunkBox.start.z;
        int xAreaEnd = chunkBox.extent().x, yAreaEnd = chunkBox.extent().y, zAreaEnd = chunkBox.extent().z;

        while(seeds.size() > 0) {
            Coord3 seed = seeds.remove(0);
            z1 = seed.z;


            Coord3 lessZNEGCoord = null;
            Coord3 previousZNEGForLight = null;
            int blockType = BlockType.NON_EXISTENT.ordinal();
            while (true) {
                if (shouldStop.get()) { return; }
                /*
                 * ZNEG (march backwards until we shouldn't anymore)
                 */
                int previousType = -1;
                if (lessZNEGCoord != null) {
                    previousZNEGForLight = lessZNEGCoord;
                    previousType = blockType;
                }
                lessZNEGCoord = new Coord3(seed.x, seed.y, z1);

                blockType = map.lookupOrCreateBlock(lessZNEGCoord);
                addFaceForType(seedChunk, chunkBox, lessZNEGCoord, Direction.ZPOS, blockType);
                if (BlockType.IsSolid(blockType)) {
                    z1++;
                    break;
                }
                else if (map.isAboveSurface(lessZNEGCoord)) {
                    z1++;
                    break;
                }
                else if (z1 == zAreaStart) {
                    Coord3 zNegNeighbor = new Coord3(seed.x, seed.y, z1 - 1);
                    map.setIsGetWasIsUnsetIfAir(zNegNeighbor, (byte) untouchedType, zNEGWasIs);

                    if (ShouldSeedBlock(zNEGWasIs)) {
                        chunkSliceFromShell(chunkSliceShell, Direction.ZNEG).addCoord(zNegNeighbor);
                    }
                    addFaceForType(seedChunk, chunkBox, zNegNeighbor, Direction.ZPOS, zNEGWasIs[1]);

                    offerLightBothWays(lessZNEGCoord, zNegNeighbor, (byte) blockType, zNEGWasIs[1]);
                    break;
                }
                /* light */
                if (previousZNEGForLight != null) {
                    offerLightBothWays(previousZNEGForLight, lessZNEGCoord, (byte) previousType, (byte) blockType);
                }
                z1--;
            }
            spanXNEG = spanXPOS = false;
            /************
             * Walk in ZPOS direction.
             * Add seeds and neighbor seeds as needed.
             * if wasIs[0] for x/y neg/pos != NON EXISTENT, don't seed. it's already been covered
             * On the other hand, if we hit AIR (as opposed to NON E) on THE CURRENT block, it doesn't mean we've covered
             * this area already, so keep going.
             * Set blocks back to NON EXISTENT when inspecting blocks beyond this chunk
             ************/
            Coord3 subject = null;
            Coord3 previousSubjectForLight = null;
            int previousSubjectBlockType = -1;
            int iterationSafetyCount = 0;
            while(true) {
                if (shouldStop.get()) { return; }
                if (iterationSafetyCount++ > 8000) { Asserter.assertFalseAndDie("death by iteration: z1 : " + z1 + "chunk box: " + chunkBox.toString()); return; }

                //DebugGeometry.AddDebugBlock(seed, ColorRGBA.Magenta);

                previousSubjectForLight = subject;
                previousSubjectBlockType = thisCoordWasIs[1];

                subject = new Coord3(seed.x, seed.y, z1);
                /*
                 * Look up the current block. And possibly set it in the map as a side effect.
                 */
                getCurrentWasIsWithinChunk(subject, seedChunk, thisCoordWasIs, (byte) untouchedType);

                /*
                 * ZPOS: hit a wall?
                 * */
                addFaceForType(seedChunk, chunkBox, subject, Direction.ZNEG, thisCoordWasIs[1]);
                if (BlockType.IsSolid(thisCoordWasIs[1]) || map.isAboveSurface(subject)) {
                    break;
                }
                if(previousSubjectForLight != null) {
                    offerLightBothWays(previousSubjectForLight, subject, (byte) previousSubjectBlockType, thisCoordWasIs[1]);
                }

                /*
                 * XNEG
                 */
                {
                    Coord3 xNegNeighbor = new Coord3(seed.x - 1, seed.y, z1);
                    boolean localXNeighbor = false;
                    if (seed.x > xAreaStart) {
                        getCurrentWasIsWithinChunk(xNegNeighbor, seedChunk, xNEGWasIs, (byte) untouchedType);
                        localXNeighbor = true;
                    } else {
                        map.setIsGetWasIsUnsetIfAir(xNegNeighbor, (byte) untouchedType, xNEGWasIs);
                    }
                    if (ShouldSeedBlock(xNEGWasIs, localXNeighbor)) {
                        if (seed.x == xAreaStart) {
                            chunkSliceFromShell(chunkSliceShell, Direction.XNEG).addCoord(xNegNeighbor);
                        } else if (!spanXNEG) {
                            addSeed(seeds, xNegNeighbor);
                            spanXNEG = true;
                        }
                    } else {
                        spanXNEG = false;
                    }
                    addFaceForType(seedChunk, chunkBox, xNegNeighbor, Direction.XPOS, xNEGWasIs[1]);
                    offerLightBothWays(subject, xNegNeighbor, thisCoordWasIs[1], xNEGWasIs[1]);
                }

                /*
                 * XPOS
                 */
                {
                    Coord3 xPosNeighbor = new Coord3(seed.x + 1, seed.y, z1);
                    boolean localXNeighbor = false;
                    if (seed.x < xAreaEnd - 1) {
                        getCurrentWasIsWithinChunk(xPosNeighbor, seedChunk, xPOSWasIs, (byte) untouchedType);
                        localXNeighbor = true;
                    } else {
                        map.setIsGetWasIsUnsetIfAir(xPosNeighbor, (byte) untouchedType, xPOSWasIs);
                    }
                    if (ShouldSeedBlock(xPOSWasIs, localXNeighbor)) {
                        if (seed.x == xAreaEnd - 1) {
                            chunkSliceFromShell(chunkSliceShell, Direction.XPOS).addCoord(xPosNeighbor);
                        } else if (!spanXPOS) {
                            addSeed(seeds, xPosNeighbor);
                            spanXPOS = true;
                        }
                    }
                    else {
                        spanXPOS = false;
                    }
                    addFaceForType(seedChunk, chunkBox, xPosNeighbor, Direction.XNEG, xPOSWasIs[1]);
                    offerLightBothWays(subject, xPosNeighbor, thisCoordWasIs[1], xPOSWasIs[1]);
                }

                /*
                 * YNEG
                 */
                Coord3 yNegNeighbor = new Coord3(seed.x, seed.y - 1, z1);
                map.setIsGetWasIsUnsetIfAir(yNegNeighbor, (byte) untouchedType, yNEGWasIs);
                if (ShouldSeedBlock(yNEGWasIs)) {
                    if (seed.y == yAreaStart) {
                        chunkSliceFromShell(chunkSliceShell, Direction.YNEG).addCoord(yNegNeighbor);
                    } else {
                        yNegChunkSlice.addCoord(yNegNeighbor);
                    }
                }
                addFaceForType(seedChunk, chunkBox, yNegNeighbor, Direction.YPOS, yNEGWasIs[1]);
                offerLightBothWays(subject, yNegNeighbor, thisCoordWasIs[1], yNEGWasIs[1]);

                /*
                 * YPOS
                 */
                Coord3 yPosNeighbor = new Coord3(seed.x, seed.y + 1, z1);
                map.setIsGetWasIsUnsetIfAir(yPosNeighbor, (byte) untouchedType, yPOSWasIs);

                if (ShouldSeedBlock(yPOSWasIs)){
                    if (seed.y == yAreaEnd - 1) {
                        chunkSliceFromShell(chunkSliceShell, Direction.YPOS).addCoord(yPosNeighbor);
                    } else {
                        yPosChunkSlice.addCoord(yPosNeighbor);
                    }
                }
                addFaceForType(seedChunk, chunkBox, yPosNeighbor, Direction.YNEG, yPOSWasIs[1]);
                offerLightBothWays(subject, yPosNeighbor, thisCoordWasIs[1], yPOSWasIs[1]);

                /*
                 * SURFACE CHECK
                 */
                if (map.isAboveSurface(new Coord3(seed.x, seed.y, z1 ))) {
                    break;
                }

                /*
                 * ZPOS
                 */
                if (z1 == zAreaEnd - 1) {
                    Coord3 zPosNeighbor = new Coord3(seed.x, seed.y, z1 + 1);
                    map.setIsGetWasIsUnsetIfAir(zPosNeighbor, (byte) untouchedType, zPOSWasIs);

                    if (ShouldSeedBlock(zPOSWasIs)) {
                        chunkSliceFromShell(chunkSliceShell, Direction.ZPOS).addCoord(zPosNeighbor);
                    }
                    addFaceForType(seedChunk, chunkBox, zPosNeighbor, Direction.ZNEG, zPOSWasIs[1]);
                    break;
                }

                z1++;
            }
        }
        return;
    }
    private static boolean ShouldSeedBlock(byte[] wasIs) {
        return ShouldSeedBlock(wasIs, false);
    }
    private static boolean ShouldSeedBlock(byte[] wasIs, boolean XNeighborCriteria) {
        return wasIs[0] == BlockType.NON_EXISTENT.ordinal() && wasIs[1] == BlockType.AIR.ordinal();
//        if (XNeighborCriteria) {
//            return wasIs[1] == BlockType.AIR.ordinal() || wasIs[1] == BlockType.PLACEHOLDER_AIR.ordinal();
//        }
//        return wasIs[1] == BlockType.PLACEHOLDER_AIR.ordinal();
    }

//    // add a face to a block in direction. Use seedChunk if it contains the block coord to cut down on looks up in map
    private void addFaceForType(Chunk seedChunk, Box chunkBounds, Coord3 globalBlockLocation, int direction, int blockType) {
        if (map.setBlockFaceForChunkIfType(seedChunk, chunkBounds, globalBlockLocation, direction, blockType)) {
            dirtyChunks.add(seedChunk.position);
        }
//        if (BlockType.IsRenderedType(blockType)) {
//            if (chunkBounds.contains(globalBlockLocation)) {
//                map.setBlockFaceForChunk(seedChunk, globalBlockLocation, direction, true);
////                seedChunk.chunkBlockFaceMap.addFace(Chunk.toChunkLocalCoord(globalBlockLocation), direction);
//                dirtyChunks.add(seedChunk.position);
//                return;
//            }
//            map.setBlockFace(globalBlockLocation, direction, true);
//            dirtyChunks.add(Chunk.ToChunkPosition(globalBlockLocation));
//        }
    }
    private void getCurrentWasIsWithinChunk(Coord3 global, Chunk chunk, byte[] wasIs, byte untouchedType) {
        wasIs[0] = chunk.blockAt(Chunk.toChunkLocalCoord(global));
        if (wasIs[0] != FloodFill4D.UntouchedType) {
            wasIs[1] = wasIs[0];
            return;
        }
        wasIs[1] = (byte) map.lookupOrCreateBlock(global);
    }

    public static final boolean FLOOD_FILL_HANDLES_LIGHT = true;
    private void offerLightBothWays(Coord3 from, Coord3 to, byte fromType, byte toType){
        if (FLOOD_FILL_HANDLES_LIGHT) {
            if (!BlockType.IsTranslucent(toType)) return;
            int fromLightLevel = map.GetSunLightmap().GetLight(from);
            int toLightLevel = map.GetSunLightmap().GetLight(to);
            if (fromLightLevel == toLightLevel) return;
            if (fromLightLevel > toLightLevel) {
                map.GetSunLightmap().SetMaxLight((byte) (fromLightLevel - LightComputerUtils.GetLightStep(fromType)), to);
            } else {
                map.GetSunLightmap().SetMaxLight((byte) (toLightLevel - LightComputerUtils.GetLightStep(toType)), from);
            }
        }
    }

    private void fakeFlood() {
        while(fakeChunkList.size() > 0) {
//            Coord3 camCoord = Coord3.FromVector3f(camera.getLocation());
            Coord3 chunkPos = fakeChunkList.remove(0); // Chunk.ToChunkPosition(camCoord);
            Chunk chunk = map.lookupOrCreateChunkAtPosition(chunkPos);
            if (chunk == null) continue;
            Coord3 origin = chunk.originInBlockCoords();
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    int k = (i + j) & 15;

                    map.setBlockFace(origin.add(new Coord3(i, k, j)), Direction.YPOS, true);
                    map.setBlockFace(origin.add(new Coord3(i, k, j)), Direction.ZPOS, true);
                    map.setBlockFace(origin.add(new Coord3(i, k, j)), Direction.XPOS, true);
                }
            }
        }
    }

}