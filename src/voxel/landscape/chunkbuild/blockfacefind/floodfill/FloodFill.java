package voxel.landscape.chunkbuild.blockfacefind.floodfill;

import com.jme3.renderer.Camera;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.player.B;
import voxel.landscape.util.Asserter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by didyouloseyourdog on 10/5/14.
 */
public class FloodFill
{
    private final TerrainMap map;
    private final Camera camera;
    private final List<Coord3> seeds = new ArrayList<Coord3>(30);
    private int steps;
    private static final int MAX_STEPS = 500;
    BlockingQueue<Coord3> floodFilledChunkCoords;

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


    public FloodFill(TerrainMap _map, Camera _cam, BlockingQueue<Coord3> _floodFilledChunkCoords) {
        map = _map; camera = _cam; floodFilledChunkCoords = _floodFilledChunkCoords;
    }

    public void flood() {
        Coord3 initialSeed = Coord3.FromVector3f(camera.getLocation());
        int initialType;
        int untouchedType = BlockType.NON_EXISTENT.ordinal();
        byte[] wasIs = new byte[2];
        int worldHeightBlocks = map.getMaxChunkCoordY() * Chunk.YLENGTH;
        while(initialSeed.y >= worldHeightBlocks) {
            initialSeed.y--;
        }

        // guarantee we start in AIR
        int count = 0;
        while(true) {
            Chunk chunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(initialSeed));
            if (chunk == null) {
                initialSeed.y = Math.max(0, initialSeed.y - 1);
                continue;
            }
            setIsGetWasIsUnsetIfAir(initialSeed, (byte) untouchedType, wasIs);
            if (wasIs[1] == BlockType.AIR.ordinal()) {
                break;
            }
            if (initialSeed.y < worldHeightBlocks - 1) initialSeed.y++;
            else initialSeed.x++;
        }
        int chunkRange = 5;
        Box tempFloodBounds = Box.WorldCoordBoxForChunkCoord(new Coord3(-chunkRange, 0, -chunkRange), new Coord3(chunkRange, chunkRange, chunkRange));
        ArrayList<Coord3> seeds = new ArrayList<Coord3>(48);
        seeds.add(initialSeed);
        int fakeCount = 0;
        while(seeds.size() > 0) {
            B.bugln("new seeds count: " + seeds.size());
            Coord3 seed = seeds.remove(0);
            Coord3 chunkSeed = Chunk.ToChunkPosition(seed);
            if (tempFloodBounds.contains(seed)) {
                ArrayList<Coord3> newSeeds = floodScanLines(seed, untouchedType);
                seeds.addAll(newSeeds);
            }else {
                B.bugln("temp flood bounds didn't contain: " + seed.toString() + "\n temp bounds start: " + tempFloodBounds.start.toString()
                 + " bounds dims: " + tempFloodBounds.dimensions.toString() + " bounds extent: \n " + tempFloodBounds.extent().toString());
                continue;
            }
            floodFilledChunkCoords.add(chunkSeed);
            if (fakeCount++ > chunkRange*chunkRange*chunkRange) break;
        }
//        floodScanLines(seeds.remove(0), untouchedType);
//        fakeFlood();
    }
    private static void addSeed(ArrayList<Coord3> seeds, Coord3 seed) {
        seeds.add(seed);
    }

    public ArrayList<Coord3> floodScanLines(Coord3 initialSeedGlobal, int untouchedType) {
        // TODO: deal with water!
        ArrayList<Coord3> neighborSeeds = new ArrayList<Coord3>(6 * 3);

        if (initialSeedGlobal == null) {
            Asserter.assertFalseAndDie("initial seed null...not good ");
            return neighborSeeds;
        }

        Chunk seedChunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(initialSeedGlobal));
        Box chunkBox = Box.WorldCoordBoxForChunkAtWorldCoord(seedChunk);

        if (!chunkBox.contains(initialSeedGlobal)) {
            Asserter.assertFalseAndDie("this line will never be called");
            return neighborSeeds;
        }

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
        if (thisCoordWasIs[0] != untouchedType) {
            return neighborSeeds;
        }

        ArrayList<Coord3> seeds = new ArrayList<Coord3>(Chunk.XLENGTH * Chunk.YLENGTH * Chunk.ZLENGTH);
        seeds.add(initialSeedGlobal);
        int z1 = 0;
        boolean spanXNEG = false, spanXPOS = false;
        boolean spanYNEG = false, spanYPOS = false;
        boolean spanZNEG = false, spanZPOS = false;

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
                if (z1 == zAreaStart) {
                    Coord3 zNegNeighbor = new Coord3(seed.x, seed.y, z1 - 1);
                    setIsGetWasIsUnsetIfAir(zNegNeighbor, (byte) untouchedType, zNEGWasIs );
                    if (!spanZNEG && zNEGWasIs[0] == untouchedType && zNEGWasIs[1] == BlockType.AIR.ordinal()) {
                        neighborSeeds.add(lessZNEGCoord);
                        spanZNEG = true;
                    }
                    if (BlockType.IsSolid(zNEGWasIs[1])) {
                        addFace(seedChunk, chunkBox, zNegNeighbor, Direction.ZPOS);
                        spanZNEG = false;
                    }
                    break;
                } else if (BlockType.IsSolid(blockType)) {
                    addFace(seedChunk, chunkBox, lessZNEGCoord, Direction.ZPOS);
                    z1++;
                    break;
                }
                z1--;
            }

            spanXNEG = spanXPOS = spanYNEG = spanYPOS = false;

            while(true) {
                getCurrentWasIsWithin(new Coord3(seed.x, seed.y, z1), seedChunk, thisCoordWasIs, (byte) untouchedType);
                if (BlockType.IsSolid(thisCoordWasIs[1])) {
                    addFace(seedChunk, chunkBox, new Coord3(seed.x, seed.y, z1), Direction.ZNEG);
                    break;
                }

                /*
                 * Add seeds and neighbor seeds where appropriate.
                 * if wasIs[0] for x/y neg/pos != NON EXISTENT, don't seed. it's already been covered
                 * On the other hand, if we hit AIR (as opposed to NON E) on this block, it doesn't mean we've covered
                 * this area already, so keep going.
                 * Set blocks back to NON E when inspecting blocks beyond this chunk
                 */

                /*
                 * XNEG
                 */
                {
                    Coord3 xNegNeighbor = new Coord3(seed.x - 1, seed.y, z1);
                    if (seed.x > xAreaStart) {
                        getCurrentWasIsWithin(xNegNeighbor, seedChunk, xNEGWasIs, (byte) untouchedType);
                    } else {
                        setIsGetWasIsUnsetIfAir(xNegNeighbor, (byte) untouchedType, xNEGWasIs);
                    }
                    if (!spanXNEG) {
                        if (untouchedType == xNEGWasIs[0] && xNEGWasIs[1] == BlockType.AIR.ordinal()) {
                            if (seed.x == xAreaStart) {
                                neighborSeeds.add(xNegNeighbor);
                            } else {
//                                seeds.add(xNegNeighbor);
                                addSeed(seeds, xNegNeighbor);
                            }
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
                    if (seed.x < xAreaEnd - 1) {
                        getCurrentWasIsWithin(xPosNeighbor, seedChunk, xPOSWasIs, (byte) untouchedType);
                    } else {
                        setIsGetWasIsUnsetIfAir(xPosNeighbor, (byte) untouchedType, xPOSWasIs);
                    }
                    if (!spanXPOS) {
                        if (untouchedType == xPOSWasIs[0] && xPOSWasIs[1] == BlockType.AIR.ordinal()) {
                            if (seed.x == xAreaEnd - 1) {
                                neighborSeeds.add(xPosNeighbor);
                            } else {
//                                seeds.add(xPosNeighbor);
                                addSeed(seeds, xPosNeighbor);
                            }
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
                {
                    Coord3 yNegNeighbor = new Coord3(seed.x, seed.y - 1, z1);
                    if (seed.y > yAreaStart) {
                        getCurrentWasIsWithin(yNegNeighbor, seedChunk, yNEGWasIs, (byte) untouchedType);
                    } else {
                        setIsGetWasIsUnsetIfAir(yNegNeighbor, (byte) untouchedType, yNEGWasIs);
                    }
                    if (!spanYNEG) {
                        if (untouchedType == yNEGWasIs[0] && yNEGWasIs[1] == BlockType.AIR.ordinal()) {
                            if (seed.y == yAreaStart) {
                                neighborSeeds.add(yNegNeighbor);
                            } else {
//                                seeds.add(yNegNeighbor);
                                addSeed(seeds, yNegNeighbor);
                            }
                            spanYNEG = true;
                        }
                    }
                    if (BlockType.IsSolid(yNEGWasIs[1])) {
                        addFace(seedChunk, chunkBox, yNegNeighbor, Direction.YPOS);
                        spanYNEG = false;
                    }
                }

                /*
                 * YPOS
                 */
                {
                    Coord3 yPosNeighbor = new Coord3(seed.x, seed.y + 1, z1);
                    if (seed.y < yAreaEnd - 1) {
                        getCurrentWasIsWithin(yPosNeighbor, seedChunk, yPOSWasIs, (byte) untouchedType);
                    } else {
                        setIsGetWasIsUnsetIfAir(yPosNeighbor, (byte) untouchedType, yPOSWasIs);
                    }
                    if (!spanYPOS) {
                        if (untouchedType == yPOSWasIs[0] && yPOSWasIs[1] == BlockType.AIR.ordinal()) {
                            if (seed.y == yAreaEnd - 1) {
                                neighborSeeds.add(yPosNeighbor);
                            } else {
//                                seeds.add(yPosNeighbor);
                                addSeed(seeds, yPosNeighbor);
                            }
                            spanYPOS = true;
                        }
                    }
                    if (BlockType.IsSolid(yPOSWasIs[1])) {
                        addFace(seedChunk, chunkBox, yPosNeighbor, Direction.YNEG);
                        spanYPOS = false;
                    }
                }


                /*
                 * ZPOS
                 */
                if (z1 == zAreaEnd - 1) {
                    Coord3 zPosNeighbor = new Coord3(seed.x, seed.y, z1 + 1);
                    setIsGetWasIsUnsetIfAir(zPosNeighbor, (byte) untouchedType, zPOSWasIs);
                    if (!spanZPOS && zPOSWasIs[0] == untouchedType && zPOSWasIs[1] == BlockType.AIR.ordinal()) {
                        neighborSeeds.add(zPosNeighbor);
                        spanZPOS = true;
                    }
                    if (BlockType.IsSolid(zPOSWasIs[1])) {
                        addFace(seedChunk, chunkBox, zPosNeighbor, Direction.ZNEG);
                        spanZPOS = false;
                    }
                    break;
                }
                z1++;
            }

        }
        return neighborSeeds;
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
    private void setIsGetWasIs(Coord3 woco, byte untouchedType, byte[] wasIs) {
        Chunk chunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(woco));
        if (chunk == null) {
            wasIs[0] = untouchedType; wasIs[1] = untouchedType;
            return;
        }
        Coord3 local = Chunk.toChunkLocalCoord(woco);
        wasIs[0] = chunk.blockAt(local);
        int nowIsType = map.lookupOrCreateBlock(woco);
        wasIs[1] = (byte) nowIsType;
    }
    private void setIsGetWasIsUnsetIfAir(Coord3 woco, byte untouchedType, byte[] wasIs) {
        setIsGetWasIs(woco, untouchedType, wasIs);
        if (wasIs[1] == BlockType.AIR.ordinal()) {
            map.setBlockAtWorldCoord((byte) untouchedType, woco);
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
            try {
                floodFilledChunkCoords.put(chunkPos);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}