package voxel.landscape.chunkbuild.blockfacefind.floodfill;

import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSlice;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;
import voxel.landscape.util.Asserter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/5/14.
 */
public class FloodFill
{
    private final TerrainMap map;
//    private final Camera camera;
    private final List<Coord3> seeds = new ArrayList<Coord3>(30);
    private int steps;
    private static final int MAX_STEPS = 500;
//    BlockingQueue<Coord3> floodFilledChunkCoords;

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


    public FloodFill(TerrainMap _map) {
        map = _map; //camera = _cam; floodFilledChunkCoords = _floodFilledChunkCoords;
    }

    /* Will replace the parameter-less version below */
    public void flood(ChunkSlice[] chunkSliceShell, Coord3 seed) {
        ChunkSlice yPosChunkSlice = new ChunkSlice(Chunk.ToChunkPosition(seed), Direction.YPOS);
        ChunkSlice yNegChunkSlice = new ChunkSlice(Chunk.ToChunkPosition(seed), Direction.YNEG);

        floodScanLines(chunkSliceShell,yPosChunkSlice, yNegChunkSlice, seed, FloodFill4D.UntouchedType); // first time both y dirs

        List<ChunkSlice> slices = new ArrayList<ChunkSlice>(5);
        slices.add(yNegChunkSlice);
        //GO Y NEG within Chunk
        ChunkSlice slice = yNegChunkSlice; // slices.remove(0);
        int seed_Y = Chunk.toChunkLocalCoord(seed).y;
        int yCoordIter = seed_Y;
        while (yCoordIter >= 0) {
            ChunkSlice yNegChunkSliceNext = new ChunkSlice(Chunk.ToChunkPosition(seed), Direction.YNEG);
            // exhaust the seed regions of this chunk slice
            while (slice.size() > 0) {
                Coord3 nextSeed = slice.removeNext();
                floodScanLines(chunkSliceShell, null, yNegChunkSliceNext, nextSeed, FloodFill4D.UntouchedType);
            }
//            if (yNegChunkSliceNext.size() == 0) break;
            slice = yNegChunkSliceNext;
            if (yCoordIter == 0) {
                chunkSliceShell[Direction.YNEG] = yNegChunkSliceNext;
            }
            yCoordIter--;
        }
        yCoordIter = seed_Y;
        slice = yPosChunkSlice;
        while (yCoordIter < Chunk.YLENGTH) {
            ChunkSlice yPosChunkSliceNext = new ChunkSlice(Chunk.ToChunkPosition(seed), Direction.YPOS);
            while(slice.size() > 0){
                floodScanLines(chunkSliceShell, yPosChunkSliceNext, null, slice.removeNext(), FloodFill4D.UntouchedType);
            }
            if (yPosChunkSliceNext.size() == 0) break;
            slice = yPosChunkSliceNext;
            if (yCoordIter == Chunk.YLENGTH - 1) {
                chunkSliceShell[Direction.YPOS] = yPosChunkSliceNext;
            }
            yCoordIter++;
        }
    }
    public Coord3 findAirCoord(Coord3 initialSeed) {
        byte[] wasIs = new byte[2];
        int worldHeightBlocks = map.getMaxChunkCoordY() * Chunk.YLENGTH;
        while(initialSeed.y >= worldHeightBlocks) {
            initialSeed.y--;
        }
        // guarantee we start in AIR
        while(true) {
            Chunk chunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(initialSeed));
            if (chunk == null) {
                initialSeed.y = Math.max(0, initialSeed.y - 1);
                continue;
            }
            setIsGetWasIsUnsetIfAir(initialSeed, (byte) FloodFill4D.UntouchedType, wasIs);
            if (wasIs[1] == BlockType.AIR.ordinal()) {
                break;
            }
            if (initialSeed.y < worldHeightBlocks - 1) initialSeed.y++;
            else if ((initialSeed.x & 1) == (initialSeed.z & 1)) initialSeed.x++;
            else  initialSeed.z++;
        }
        return initialSeed;
    }

    private static void addSeed(ArrayList<Coord3> seeds, Coord3 seed) {
        seeds.add(seed);
    }

    private ChunkSlice chunkSliceFromShell(ChunkSlice[] shell, int direction) {
        return shell[direction];
    }

    public void floodScanLines(ChunkSlice[] chunkSliceShell, ChunkSlice yPosChunkSlice, ChunkSlice yNegChunkSlice, Coord3 initialSeedGlobal, int untouchedType) {
        boolean searchYPos = yPosChunkSlice != null;
        boolean searchYNeg = yNegChunkSlice != null;
        // like scan lines method below, except use chunk slices, not the array lists for
        // y direction searches — and outside-of-chunk searches
        // TODO: deal with water!
//        ArrayList<Coord3> resul = new ArrayList<Coord3>(6 * 3);
        ArrayList<Coord3> seeds = new ArrayList<Coord3>(Chunk.XLENGTH * Chunk.YLENGTH * Chunk.ZLENGTH);

        Asserter.assertTrue(initialSeedGlobal != null, "initial seed null...not good");

        Chunk seedChunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(initialSeedGlobal));
        Box chunkBox = Box.WorldCoordBoxForChunkAtWorldCoord(seedChunk);

        /* wasIs arrays used to check whether blocks were unset before lookup:
         * we (mostly) want to continue flooding only over air blocks that were of type "non_existant"
         * before we looked them up. *wasIs[0] stores the type before lookup.
         * *wasIs[1] stores the type after lookup */
        byte[] thisCoordWasIs = new byte[2];
        byte[] xNEGWasIs = new byte[2];
        byte[] xPOSWasIs = new byte[2];
        byte[] zNEGWasIs = new byte[2];
        byte[] zPOSWasIs = new byte[2];
        byte[] yNEGWasIs = new byte[2];
        byte[] yPOSWasIs = new byte[2];

        getCurrentWasIsWithin(initialSeedGlobal, seedChunk, thisCoordWasIs, (byte) untouchedType);
        if (!BlockType.IsNonExistentOrPlaceHolderAir(thisCoordWasIs[0])) {
//        if (!(thisCoordWasIs[0] != untouchedType || thisCoordWasIs[0] != BlockType.PLACEHOLDER_AIR.ordinal())) {
            B.bugln("--- block type != unTouched type! at " + initialSeedGlobal.toString());
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
            Coord3 lessZNEGCoord;
            while (true) {
                /*
                 * ZNEG
                 */
                lessZNEGCoord = new Coord3(seed.x, seed.y, z1);
                int blockType = map.lookupOrCreateBlock(lessZNEGCoord);
                if (BlockType.IsSolid(blockType)) {
                    addFace(seedChunk, chunkBox, lessZNEGCoord, Direction.ZPOS);
                    z1++;
                    break;
                } else if (z1 == zAreaStart) {
                    Coord3 zNegNeighbor = new Coord3(seed.x, seed.y, z1 - 1);
                    setIsGetWasIsUnsetIfAir(zNegNeighbor, (byte) untouchedType, zNEGWasIs );

                    if (ShouldSeedBlock(zNEGWasIs)) {
                        chunkSliceFromShell(chunkSliceShell, Direction.ZNEG).addCoord(zNegNeighbor);
                    }
                    if (BlockType.IsSolid(zNEGWasIs[1])) {
                        addFace(seedChunk, chunkBox, zNegNeighbor, Direction.ZPOS);
                    }
                    break;
                }
                z1--;
            }
            spanXNEG = spanXPOS = false;
            /*
             * Add seeds and neighbor seeds where appropriate.
             * if wasIs[0] for x/y neg/pos != NON EXISTENT, don't seed. it's already been covered
             * On the other hand, if we hit AIR (as opposed to NON E) on this block, it doesn't mean we've covered
             * this area already, so keep going.
             * Set blocks back to NON E when inspecting blocks beyond this chunk
             */
            while(true) {
                getCurrentWasIsWithin(new Coord3(seed.x, seed.y, z1), seedChunk, thisCoordWasIs, (byte) untouchedType);
                if (thisCoordWasIs[1] == BlockType.PLACEHOLDER_AIR.ordinal()) {
                    seedChunk.setBlockAt((byte) BlockType.AIR.ordinal(), Chunk.toChunkLocalCoord(seed.x, seed.y, z1) );
                } else if (BlockType.IsSolid(thisCoordWasIs[1])) {
                    addFace(seedChunk, chunkBox, new Coord3(seed.x, seed.y, z1), Direction.ZNEG);
                    break;
                }

                /*
                 * XNEG
                 */
                {
                    Coord3 xNegNeighbor = new Coord3(seed.x - 1, seed.y, z1);
                    boolean localXNeighbor = false;
                    if (seed.x > xAreaStart) {
                        getCurrentWasIsWithin(xNegNeighbor, seedChunk, xNEGWasIs, (byte) untouchedType);
                        localXNeighbor = true;
                    } else {
                        setIsGetWasIsUnsetIfAir(xNegNeighbor, (byte) untouchedType, xNEGWasIs);
                    }
                    if (ShouldSeedBlock(xNEGWasIs, localXNeighbor)) {
                        if (seed.x == xAreaStart) {
                            chunkSliceFromShell(chunkSliceShell, Direction.XNEG).addCoord(xNegNeighbor);
                        } else if (!spanXNEG) {
                            addSeed(seeds, xNegNeighbor);
                            spanXNEG = true;
                        }
                    }
                    if (BlockType.IsSolid(xNEGWasIs[1])) {
                        addFace(seedChunk, chunkBox, xNegNeighbor, Direction.XPOS);
                        spanXNEG = false;
                    }
                }

                /*
                 * XPOS
                 */
                {
                    Coord3 xPosNeighbor = new Coord3(seed.x + 1, seed.y, z1);
                    boolean localXNeighbor = false;
                    if (seed.x < xAreaEnd - 1) {
                        getCurrentWasIsWithin(xPosNeighbor, seedChunk, xPOSWasIs, (byte) untouchedType);
                        localXNeighbor = true;
                    } else {
                        setIsGetWasIsUnsetIfAir(xPosNeighbor, (byte) untouchedType, xPOSWasIs);
                    }
                    if (ShouldSeedBlock(xPOSWasIs, localXNeighbor)) {
                        if (seed.x == xAreaEnd - 1) {
                            chunkSliceFromShell(chunkSliceShell, Direction.XPOS).addCoord(xPosNeighbor);
                        } else if (!spanXPOS) {
                            addSeed(seeds, xPosNeighbor);
                            spanXPOS = true;
                        }
                    }

                    if (BlockType.IsSolid(xPOSWasIs[1])) {
                        addFace(seedChunk, chunkBox, xPosNeighbor, Direction.XNEG);
                        spanXPOS = false;
                    }
                }

                /*
                 * YNEG
                 */
                Coord3 yNegNeighbor = new Coord3(seed.x, seed.y - 1, z1);
                setIsGetWasIsUnsetIfAir(yNegNeighbor, (byte) untouchedType, yNEGWasIs);
                if (searchYNeg) {
                    if (ShouldSeedBlock(yNEGWasIs)) {
                        if (seed.y == yAreaStart) {
                            chunkSliceFromShell(chunkSliceShell, Direction.YNEG).addCoord(yNegNeighbor);
                        } else {
                            yNegChunkSlice.addCoord(yNegNeighbor);
                        }
                    }
                }
                if (BlockType.IsSolid(yNEGWasIs[1])) {
                    addFace(seedChunk, chunkBox, yNegNeighbor, Direction.YPOS);
                }

                /*
                 * YPOS
                 */
                Coord3 yPosNeighbor = new Coord3(seed.x, seed.y + 1, z1);
                setIsGetWasIsUnsetIfAir(yPosNeighbor, (byte) untouchedType, yPOSWasIs);
                if (searchYPos) {
                    if (ShouldSeedBlock(yPOSWasIs)){
                        if (seed.y == yAreaEnd - 1) {
                            chunkSliceFromShell(chunkSliceShell, Direction.YPOS).addCoord(yPosNeighbor);
                        } else {
                            yPosChunkSlice.addCoord(yPosNeighbor);
                        }
                    }
                }
                if (BlockType.IsSolid(yPOSWasIs[1])) {
                    addFace(seedChunk, chunkBox, yPosNeighbor, Direction.YNEG);
                }

                /*
                 * ZPOS
                 */
                if (z1 == zAreaEnd - 1) {
                    Coord3 zPosNeighbor = new Coord3(seed.x, seed.y, z1 + 1);
                    setIsGetWasIsUnsetIfAir(zPosNeighbor, (byte) untouchedType, zPOSWasIs);
                    if (ShouldSeedBlock(zPOSWasIs)) {
                        chunkSliceFromShell(chunkSliceShell, Direction.ZPOS).addCoord(zPosNeighbor);
                    }
                    if (BlockType.IsSolid(zPOSWasIs[1])) {
                        addFace(seedChunk, chunkBox, zPosNeighbor, Direction.ZNEG);
                    }
                    break;
                }

                z1++;
            }
        }
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

    // add a face to a block in direction. Use seedChunk if it contains the block coord
    private void addFace(Chunk seedChunk, Box chunkBounds, Coord3 globalBlockLocation, int direction) {
        if (chunkBounds.contains(globalBlockLocation)) {
            seedChunk.chunkBlockFaceMap.addFace(Chunk.toChunkLocalCoord(globalBlockLocation), direction);
            return;
        }
        map.setBlockFace(globalBlockLocation, direction, true);
    }
    private void getCurrentWasIsWithin(Coord3 global, Chunk chunk, byte[] wasIs, byte untouchedType) {
        wasIs[0] = chunk.blockAt(Chunk.toChunkLocalCoord(global));
        if (wasIs[0] != untouchedType) {
            wasIs[1] = wasIs[0];
            return;
        }
        wasIs[1] = (byte) map.lookupOrCreateBlock(global);
    }

    private void setIsGetWasIs(Coord3 global, byte untouchedType, byte[] wasIs) {
        Chunk chunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(global));
        if (chunk == null) {
            Asserter.assertTrue(global.y < 0 || global.y >= map.getMaxChunkCoordY() * Chunk.YLENGTH, "wha null chunk at global co: " + global.toString());
            wasIs[0] = untouchedType; wasIs[1] = untouchedType;
            return;
        }
        Coord3 local = Chunk.toChunkLocalCoord(global);
        wasIs[0] = chunk.blockAt(local);
        int nowIsType = map.lookupOrCreateBlock(global);
        wasIs[1] = (byte) nowIsType;
        Asserter.assertTrue(wasIs[1] != untouchedType, "block value shouldn't be unTouched now! was value: " + untouchedType + "\n at global: " + global.toString());
    }
    private void setIsGetWasIsUnsetIfAir(Coord3 woco, byte untouchedType, byte[] wasIs) {
        setIsGetWasIs(woco, untouchedType, wasIs);
        if (wasIs[0] == untouchedType && wasIs[1] == BlockType.AIR.ordinal()) {
            map.setBlockAtWorldCoord(untouchedType, woco);
//            map.setBlockAtWorldCoord((byte) BlockType.PLACEHOLDER_AIR.ordinal(), woco);
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