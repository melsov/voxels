package voxel.landscape.map;

import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.VoxelLandscape;
import voxel.landscape.collection.chunkface.ChunkBlockFaceMap;
import voxel.landscape.collection.coordmap.managepages.ConcurrentHashMapCoord3D;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.light.LightComputer;
import voxel.landscape.map.light.LightMap;
import voxel.landscape.map.light.SunLightComputer;
import voxel.landscape.map.light.SunLightMap;
import voxel.landscape.map.water.LiquidUpdater;
import voxel.landscape.map.water.WaterFlowComputer;
import voxel.landscape.map.water.WaterLevelMap;
import voxel.landscape.noise.IBlockDataProvider;
import voxel.landscape.noise.TerrainDataProvider;
import voxel.landscape.util.Asserter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TerrainMap implements IBlockDataProvider
{
	private static final int MIN_DIM_HORIZONTAL = -8;
	private static final int MAX_DIM_HORIZONTAL = 8;

    private static final int MIN_CHUNK_DIM_VERTICAL = 0;
	private static final int MAX_CHUNK_DIM_VERTICAL = 4;

	public static Coord3 MIN_CHUNK_COORD = new Coord3(MIN_DIM_HORIZONTAL, MIN_CHUNK_DIM_VERTICAL, MIN_DIM_HORIZONTAL);
	public static Coord3 MAX_CHUNK_COORD = new Coord3(MAX_DIM_HORIZONTAL, MAX_CHUNK_DIM_VERTICAL, MAX_DIM_HORIZONTAL);

	TerrainDataProvider terrainDataProvider = new TerrainDataProvider();

    private ConcurrentHashMapCoord3D<Chunk> chunks = new ConcurrentHashMapCoord3D<Chunk>(Chunk.class);
	private SunLightMap sunLightmap = new SunLightMap();
	private LightMap lightmap = new LightMap();
    private WaterLevelMap liquidLevelMap = new WaterLevelMap();
    private LiquidUpdater liquidUpdaterWater = new LiquidUpdater(BlockType.LiquidFlowTimeStepSeconds(BlockType.WATER.ordinal()));

    private final VoxelLandscape app;

    public final BlockingQueue<Coord3> floodFillSeedSetsToFlood = new ArrayBlockingQueue<Coord3>(128);

	public TerrainMap(VoxelLandscape _app) {
        app = _app;
	}

    public VoxelLandscape getApp() { return app; }


    /*
     * DYNAMIC MAP UPDATES
     */
    public void mapUpdate(float tpf) {
        liquidUpdaterWater.StepScatter(this, tpf);
    }

    /*
     * CHUNK BLOCK FACE
     */
    public ChunkBlockFaceMap blockFaceMap(Coord3 chunkCoord) {
        return lookupOrCreateChunkAtPosition(chunkCoord).chunkBlockFaceMap;
    }
    public void setBlockFace(Coord3 worldCoord, int direction, boolean shouldExist) {
        Coord3 chunkCo = Chunk.ToChunkPosition(worldCoord);
        Coord3 localCo = Chunk.toChunkLocalCoord(worldCoord);
        Chunk chunk = lookupOrCreateChunkAtPosition(chunkCo);
        if (shouldExist) {
            chunk.chunkBlockFaceMap.addFace(localCo, direction);
        } else {
            chunk.chunkBlockFaceMap.removeFace(localCo, direction);
        }
    }
    /*
     * WORLD INFO
     */
	public static int GetWorldHeightInChunks() {
		return MAX_CHUNK_DIM_VERTICAL - MIN_CHUNK_DIM_VERTICAL;
	}

	public static int GetWorldHeightInBlocks() {
		return GetWorldHeightInChunks() * Chunk.YLENGTH;
	}

    /*
     * BLOCK INFO
     */
    public byte lookupBlock(int x, int y, int z) {
		return lookupBlock(new Coord3(x, y, z));
	}

	public byte lookupBlock(Coord3 co) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(co));
		if (chunk == null)
			return (byte) BlockType.NON_EXISTENT.ordinal();
		return chunk.blockAt(Chunk.toChunkLocalCoord(co));
	}

	public boolean blockAtWorldCoordIsTranslucent(Coord3 co) {
		return blockAtWorldCoordIsTranslucent(co.x, co.y, co.z);
	}

	public boolean blockAtWorldCoordIsTranslucent(int x, int y, int z) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(x, y, z));
		if (chunk == null) return false;
		return BlockType.IsTranslucent(chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z)));
	}

	public void setBlockAtWorldCoord(byte block, Coord3 pos) {
		setBlockAtWorldCoord(block, pos.x, pos.y, pos.z);
	}

	public void setBlockAtWorldCoord(byte block, int x, int y, int z) {
		Chunk chunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(x, y, z));
		if (chunk != null) chunk.setBlockAt(block, Chunk.toChunkLocalCoord(x, y, z));
	}

    public int getMinChunkCoordY() { return MIN_CHUNK_DIM_VERTICAL; }
    public int getMaxChunkCoordY() { return MAX_CHUNK_DIM_VERTICAL; }

	@Override
    public int lookupOrCreateBlock(Coord3 woco) {
        return lookupOrCreateBlock(woco,  terrainDataProvider);
    }
	
	@Override
    public int lookupOrCreateBlock(int xin, int yin, int zin) {
        return lookupOrCreateBlock(new Coord3(xin,yin,zin), terrainDataProvider);
    }
    public int lookupOrCreateBlock(Coord3 woco, TerrainDataProvider _terrainData) {
        byte block = lookupBlock(woco);
        if (BlockType.NON_EXISTENT.equals((int) block) && ChunkCoordWithinWorldBounds(Chunk.ToChunkPosition(woco))) {
            //synchronized block
            Coord3 chunkPos = Chunk.ToChunkPosition(woco);
            Coord3 localPos = Chunk.toChunkLocalCoord(woco);
            Chunk chunk = lookupOrCreateChunkAtPosition(chunkPos);
            Object lock = chunk.blockLockInstanceAt(localPos);
            synchronized (lock) {
                block = (byte) _terrainData.getBlockDataAtPosition(woco.x, woco.y, woco.z);
                setBlockAtWorldCoord(block, woco.x, woco.y, woco.z);
            }
        }
        return block;
    }
    public boolean ChunkCoordWithinWorldBounds(Coord3 chunkCoord) {
        return chunkCoord.y >= MIN_CHUNK_DIM_VERTICAL && chunkCoord.y < MAX_CHUNK_DIM_VERTICAL;
    }
    public boolean isGlobalCoordWithingWorldBounds(Coord3 global) {
        return global.y >= MIN_CHUNK_DIM_VERTICAL * Chunk.YLENGTH && global.y < MAX_CHUNK_DIM_VERTICAL * Chunk.YLENGTH;
    }

    /*
     * Clean up
     */
    public void removeColumnData(int x, int z) {
        Coord3 chunkPos = new Coord3(x, MIN_CHUNK_DIM_VERTICAL, z);
        for (; chunkPos.y < MAX_CHUNK_DIM_VERTICAL; chunkPos.y++) {
            chunks.Remove(chunkPos);
            sunLightmap.RemoveLightData(chunkPos);
            lightmap.RemoveLightData(chunkPos);
        }
        sunLightmap.RemoveRays(x,z);
    }

    public void assertChunkNotNull(Coord3 co, String msg) {
        Asserter.assertTrue(GetChunk(co) != null, "chunk was null: " + co.toString() + " : " + msg);
    }

	/*
	 * populate chunk with block values
	 */
    public void generateNoiseForChunkColumn(int x, int z) {
        generateNoiseForChunkColumn(x,z,terrainDataProvider);
    }

	public void generateNoiseForChunkColumn(int x, int z, TerrainDataProvider _dataProvider) {
        doGenerateNoiseForChunkColumn(x,z,_dataProvider);
	}
    /* i.e. find the surface */
    private void generateEnoughNoiseForSunlightInColumn(int x, int z, TerrainDataProvider _dataProvider) {
        //CONSIDER: implement this
    }

    private void doGenerateNoiseForChunkColumn(int x, int z, TerrainDataProvider _dataProvider) {
        Coord3 chunkPos = new Coord3(x, MAX_CHUNK_DIM_VERTICAL - 1, z);
        InstantiateChunksInColumn(x,z);
        generateNoiseForColumnToSurface(chunkPos.x, chunkPos.z, _dataProvider);
        /*
        for (; true; chunkPos.y--) {
            boolean generated = generateNoiseForChunkAt(chunkPos.x, chunkPos.y, chunkPos.z, _dataProvider);
            if (!generated)
                break;
        }
        */ // ******* WANT ?
    }
    private void InstantiateChunksInColumn(int x, int z) {
        Coord3 chunkPos = new Coord3(x, MAX_CHUNK_DIM_VERTICAL - 1, z);
//        generateNoiseForColumnToSurface(chunkPos.x, chunkPos.z, _dataProvider);

        for (; chunkPos.y >= MIN_CHUNK_DIM_VERTICAL; chunkPos.y--) {
            lookupOrCreateChunkAtPosition(chunkPos);
        }
    }

    /*
     * get the terrain data inside the chunk and also in the -1/+1 box (in block units) that
     * surrounds it (within world limits if any)
     */
    private boolean generateNoiseForColumnToSurface(int cx, int cz, TerrainDataProvider _dataProvider) {

        Coord3 worldPos = Chunk.ToWorldPosition(new Coord3(cx, 0, cz));

        Coord3 relPos, absPos, chunkPos = null;
        Chunk possibleChunk = null;
        boolean foundDirt = false;
        for (int i = -1; i < Chunk.CHUNKDIMS.x + 1; ++i) {
            for (int j = -1; j < Chunk.CHUNKDIMS.z + 1; ++j) {
                for (int wocoY = MAX_CHUNK_DIM_VERTICAL * Chunk.YLENGTH - 1 ; wocoY >= MIN_CHUNK_DIM_VERTICAL * Chunk.YLENGTH ; --wocoY) {
                    int k = Chunk.ToChunkLocalY(wocoY);
                    worldPos.y = Chunk.ToWorldPositionY(Chunk.ToChunkPositionY(wocoY), 0);
                    relPos = new Coord3(i, k, j);
                    absPos = worldPos.add(relPos);
                    Coord3 nextChunkPos = Chunk.ToChunkPosition(absPos);
                    if (chunkPos == null || !chunkPos.equal(nextChunkPos)) {
                        chunkPos = nextChunkPos.clone();
                        possibleChunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(absPos));
                    }

                    if (VoxelLandscape.DO_USE_TEST_GEOMETRY) return false;

                    if (possibleChunk == null) continue; // must be at world limit?
                    byte block;

                    block = (byte) lookupOrCreateBlock(absPos, _dataProvider);

					/*
					 * CONSIDER: grass is too simplistic! grass should grow only
					 * where there's light... Also, put this logic somewhere
					 * where it will also apply to newly created, destroyed blocks
					 * may need a 'block time step' concept...
					 */
                    // should this block be grass?
                    if (BlockType.DIRT.ordinal() == block) {
                        Coord3 upOne = absPos.add(Coord3.ypos);
                        if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null ||
                                BlockType.IsTranslucent((byte) lookupOrCreateBlock(upOne))) {
                            block = (byte) BlockType.GRASS.ordinal();
                        }
                    } else if (BlockType.WATER.ordinal() == block) {
                        liquidLevelMap.SetWaterLevel(WaterFlowComputer.MAX_WATER_LEVEL, absPos);
                        possibleChunk.getChunkBrain().SetLiquidDirty();
                    }

                    relPos = Chunk.toChunkLocalCoord(relPos);
                    possibleChunk.setBlockAt(block, relPos);

                    if (!foundDirt && k > -1 && j > -1 && i > -1 && k <= Chunk.YLENGTH && j <= Chunk.ZLENGTH && i <= Chunk.XLENGTH) {
                        foundDirt = BlockType.IsSolid(block);
                    }
                    if (BlockType.IsSolid(block)) {
                        possibleChunk.chunkBlockFaceMap.addFace(relPos, Direction.YPOS);
                        /* Set sunlight map here */
                        sunLightmap.SetSunHeight(wocoY + 1, absPos.x, absPos.z );
                        break;
                    }
                }
            }
        }
//        targetChunk.setIsAllAir(!foundDirt);
        return true;
    }

    /*
     * populate chunk flood fill seed set
     */
    public void populateFloodFillSeedsUpdateFaceMapsInChunkColumn(int cx, int cz, TerrainDataProvider _terrainDataProvider) {
        Coord3 worldPos = Chunk.ToWorldPosition(new Coord3(cx, 0, cz));

        Coord3 surfacePos;
        /*
         * ONLY LOOK FOR COLUMNS THAT ARE HIGHER THAN 'US'
         * IN THE DIRECTION IN WHICH WE ARE LOOKING
         * GO THROUGH COLUMNS IN X/Z POS/NEG DIRECTIONS
         */
        // X and Z pos
        for (int i = -1; i < Chunk.XLENGTH; ++i) {
            for (int j = -1; j < Chunk.ZLENGTH; ++j) {
                if (i == -1 && j == -1) continue;
                surfacePos = worldPos.add(new Coord3(i, 0, j));
                surfacePos.y = getSurfaceHeight(surfacePos.x, surfacePos.z);

//                if (i > -1 && j < Chunk.ZLENGTH - 1) {
                    populateFloodFillSeedsUpdateFaceMaps(surfacePos, surfacePos.y, Direction.ZPOS, _terrainDataProvider );
//                }
//                if (j > -1 && i < Chunk.XLENGTH - 1) {
                    populateFloodFillSeedsUpdateFaceMaps(surfacePos, surfacePos.y, Direction.XPOS, _terrainDataProvider );
//                }
            }
        }
        // X and Z neg
        for (int i = Chunk.XLENGTH; i >= 0 ; --i) {
            for (int j = Chunk.ZLENGTH; j >= 0 ; --j) {
                if (i == 0 && j == 0) continue;
                surfacePos = worldPos.add(new Coord3(i, 0, j));
                surfacePos.y = getSurfaceHeight(surfacePos.x, surfacePos.z);

//                if (i < Chunk.XLENGTH && j > 0) {
                    populateFloodFillSeedsUpdateFaceMaps(surfacePos, surfacePos.y, Direction.ZNEG, _terrainDataProvider );
//                }
//                if (j < Chunk.ZLENGTH && i > 0) {
                    populateFloodFillSeedsUpdateFaceMaps(surfacePos, surfacePos.y, Direction.XNEG, _terrainDataProvider );
//                }
            }
        }
    }

    public int getSurfaceHeight(Coord3 coord3) { return getSurfaceHeight(coord3.x,coord3.z); }
    public int getSurfaceHeight(int x, int z) { return sunLightmap.GetSunHeight(x,z) - 1; }

    private void populateFloodFillSeedsUpdateFaceMaps(Coord3 absPos, int height, int direction, TerrainDataProvider _terrainDataProvider) {
        Coord3 neighbor = absPos.add(Direction.DirectionCoords[direction]);
        int neighborHeight = getSurfaceHeight(neighbor);
        if (height >= neighborHeight) {
            return;
        }
        Chunk chunk_ = GetChunk(Chunk.ToChunkPosition(neighbor));
        if (chunk_ == null) {
            return;
        }

        for(neighbor.y = height + 1; neighbor.y <= neighborHeight; ++neighbor.y ) {
            Chunk chunk = GetChunk(Chunk.ToChunkPosition(neighbor));
            if (chunk == null) continue;
            int was = chunk.blockAt(Chunk.toChunkLocalCoord(neighbor));
            int is;
            if (was == BlockType.NON_EXISTENT.ordinal()) {
                is = lookupOrCreateBlock(neighbor, _terrainDataProvider);
            } else {
                is = was;
            }

            if (BlockType.IsTranslucent(is)) {
                // TODO: deal with water
                chunk.chunkFloodFillSeedSet.addCoord(neighbor);
                try { floodFillSeedSetsToFlood.put(chunk.position); } catch (InterruptedException e) {  e.printStackTrace(); }
            } else if (BlockType.IsSolid(is)) {
                chunk.chunkBlockFaceMap.addFace(Chunk.toChunkLocalCoord(neighbor),Direction.OppositeDirection(direction));
            }

        }
    }

    /*
	 * get the terrain data inside the chunk and also in the -1/+1 box (in block units) that
	 * surrounds it (within world limits if any)
	 */
    private boolean generateNoiseForChunkAt(int cx, int cy, int cz, TerrainDataProvider _dataProvider) {
        Chunk targetChunk = lookupOrCreateChunkAtPosition(new Coord3(cx, cy, cz));
        if (targetChunk == null) {
            if (ChunkCoordWithinWorldBounds(new Coord3(cx,cy,cz))) {
                Asserter.assertFalseAndDie("how is it possible?? null chunk with world bounds? " + new Coord3(cx,cy,cz).toString());
            }
            return false;
        }
        Coord3 worldPos = Chunk.ToWorldPosition(new Coord3(cx, cy, cz));

        Coord3 relPos, absPos, chunkPos = null;
        Chunk possibleChunk = null;
        boolean foundDirt = false;
        for (int i = -1; i < Chunk.CHUNKDIMS.x + 1; ++i) {
            for (int j = -1; j < Chunk.CHUNKDIMS.z + 1; ++j) {
                for (int k = -1; k < Chunk.CHUNKDIMS.y + 1; ++k) {
                    relPos = new Coord3(i, k, j);
                    absPos = worldPos.add(relPos);
                    Coord3 nextChunkPos = Chunk.ToChunkPosition(absPos);
                    if (chunkPos == null || !chunkPos.equal(nextChunkPos)) {
                        chunkPos = nextChunkPos.clone();
                        possibleChunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(absPos));
                    }

                    if (VoxelLandscape.DO_USE_TEST_GEOMETRY) return false;

                    if (possibleChunk == null)
                        continue; // must be at world limit?
                    byte block;

                    // FAKE WATER
                    boolean IsFakeWaterChunk = cx == 0 && cy == MAX_CHUNK_DIM_VERTICAL - 1 && cz == 0;
                    if (IsFakeWaterChunk && i == 3 && j == 2 && k == Chunk.YLENGTH - 1) {
                        block = (byte) BlockType.WATER.ordinal();
                    } else if (IsFakeWaterChunk && k > Chunk.YLENGTH - 5 ) {
                        block = (byte) BlockType.AIR.ordinal();
                    }
                    else
                        // #FAKE WATER
                        block = (byte) lookupOrCreateBlock(absPos, _dataProvider);

                    // should be grass?
					/*
					 * CONSIDER: this is too simplistic! grass should grow only
					 * where there's light... Also, put this logic somewhere
					 * where it will also apply to newly created, destroyed blocks
					 * may need a 'block time step' concept...
					 */
                    if (BlockType.DIRT.ordinal() == block) {
                        Coord3 upOne = absPos.add(Coord3.ypos);
                        if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null ||
                                BlockType.IsTranslucent((byte) lookupOrCreateBlock(upOne))) {
                            block = (byte) BlockType.GRASS.ordinal();
                        }
                    } else if (BlockType.WATER.ordinal() == block) {
                        liquidLevelMap.SetWaterLevel(WaterFlowComputer.MAX_WATER_LEVEL, absPos);
                        possibleChunk.getChunkBrain().SetLiquidDirty();
                    }

                    relPos = Chunk.toChunkLocalCoord(relPos);
                    possibleChunk.setBlockAt(block, relPos);

                    if (!foundDirt && k > -1 && j > -1 && i > -1 && k <= Chunk.YLENGTH && j <= Chunk.ZLENGTH && i <= Chunk.XLENGTH) {
                        foundDirt = BlockType.IsSolid(block);
                    }
                }
            }
        }
        targetChunk.setIsAllAir(!foundDirt);
        return true;
    }

    private void generateNoiseForBlock(int x, int y, int z, Coord3 relPos,
                                          Coord3 absPos, Coord3 worldPos, Coord3 chunkPos, boolean foundDirt, Chunk possibleChunk, TerrainDataProvider _dataProvider)
    {
        relPos = new Coord3(x, y, z);
        absPos = worldPos.add(relPos);
        Coord3 nextChunkPos = Chunk.ToChunkPosition(absPos);
        if (chunkPos == null || !chunkPos.equal(nextChunkPos)) {
            chunkPos = nextChunkPos.clone();
            possibleChunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(absPos));
        }

        if (VoxelLandscape.DO_USE_TEST_GEOMETRY) return;

        if (possibleChunk == null)
            return; // must be at world limit?
        byte block = (byte) lookupOrCreateBlock(absPos, _dataProvider);
					/*
					 * CONSIDER: this is too simplistic! grass should grow only
					 * where there's light... Also, put this logic somewhere
					 * where it will also apply to newly created, destroyed blocks
					 * may need a 'block time step' concept...
					 */
        // should be grass?
        if (BlockType.DIRT.ordinal() == block) {
            Coord3 upOne = absPos.add(Coord3.ypos);
            if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null || BlockType.IsTranslucent((byte) lookupOrCreateBlock(upOne))) {
                block = (byte) BlockType.GRASS.ordinal();
            }
        }
        relPos = Chunk.toChunkLocalCoord(relPos);
        possibleChunk.setBlockAt(block, relPos);

        if (!foundDirt && y > -1 && z > -1 && x > -1 && y <= Chunk.YLENGTH && z <= Chunk.ZLENGTH && x <= Chunk.XLENGTH) {
            foundDirt = BlockType.IsSolid(block);
        }
        return;
    }

	/*
	 * Credit: Mr. Wishmaster methods (YouTube)
	 */
	public void SetBlockAndRecompute(byte block, Coord3 pos) {
		setBlockAtWorldCoord(block, pos);

		Coord3 chunkPos = Chunk.ToChunkPosition(pos);
		Coord3 localPos = Chunk.toChunkLocalCoord(pos);

        Chunk chunk = GetChunk(chunkPos);
        if (BlockType.IsTranslucent(block)) {
            chunk.chunkBlockFaceMap.removeAllFacesUpdateNeighbors(pos, this);
        } else {

        }
		SetDirty(chunkPos);

		if (localPos.x == 0)
			SetDirty(chunkPos.minus(Coord3.right));
		if (localPos.y == 0)
			SetDirty(chunkPos.minus(Coord3.up));
		if (localPos.z == 0)
			SetDirty(chunkPos.minus(Coord3.forward));

		if (localPos.x == Chunk.CHUNKDIMS.x - 1)
			SetDirty(chunkPos.add(Coord3.right));
		if (localPos.y == Chunk.CHUNKDIMS.y - 1)
			SetDirty(chunkPos.add(Coord3.up));
		if (localPos.z == Chunk.CHUNKDIMS.z - 1)
			SetDirty(chunkPos.add(Coord3.forward));

		SunLightComputer.RecomputeLightAtPosition(this, pos.clone());
		LightComputer.RecomputeLightAtPosition(this, pos.clone());

        //TODO: make logic more abstract so TerrainMap doesn't have to deal with...
        if (BlockType.WATER.ordinal() == block) {
            liquidUpdaterWater.addCoord(pos.clone(), WaterFlowComputer.MAX_WATER_LEVEL);
        } else {
            WaterFlowComputer.RecomputeWaterAtPosition(this, pos.clone());
        }
	}

	private void SetDirty(Coord3 chunkPos) {
		Chunk chunk = GetChunk(chunkPos);
		if (chunk != null)
			chunk.getChunkBrain().SetDirty();
	}

	public int GetMaxY(int x, int z, boolean forceLookup) {
        if (!forceLookup) {
            int height = sunLightmap.GetSunHeight(x, z);
            if (height > 0) {
                return height;
            }
        }
		Coord3 chunkPos = Chunk.ToChunkPosition(x, 0, z);
		chunkPos.y = MAX_CHUNK_DIM_VERTICAL; // chunks.GetMax().y;
		Coord3 localPos = Chunk.toChunkLocalCoord(x, 0, z);
		for (; chunkPos.y >= 0; chunkPos.y--) {
			localPos.y = Chunk.CHUNKDIMS.y - 1;
			for (; localPos.y >= 0; localPos.y--) {
				Chunk chunk = chunks.SafeGet(chunkPos);
				if (chunk == null)
					break;
				byte block = chunk.blockAt(localPos);
				if (!BlockType.IsAirOrNonExistent(block)) {
					return Chunk.ToWorldPosition(chunkPos, localPos).y;
				}
			}
		}
		return 0;
	}

	/*
	 * Chunk info
	 */
	public Chunk lookupOrCreateChunkAtPosition(int x, int y, int z) {
		return lookupOrCreateChunkAtPosition(new Coord3(x, y, z));
	}
	public Chunk lookupOrCreateChunkAtPosition(Coord3 chunkPos) {
        if (!ChunkCoordWithinWorldBounds(chunkPos)) return null;
        // Sets chunk at key if not there before. Returns previous value! (null if nothing was there)
        Chunk result = chunks.putIfKeyIsAbsent(chunkPos, new Chunk(chunkPos, this));
        if (result == null) {
            result = chunks.Get(chunkPos); // re-get the Chunk if it was null. now guaranteed to be there.
        }
        return result;
	}

	public Chunk GetChunk(Coord3 chunkPos) {
		return chunks.Get(chunkPos);
	}
    public Chunk GetChunk(int x, int y, int z) {
        return GetChunk(new Coord3(x,y,z));
    }

	public SunLightMap GetSunLightmap() {
		return sunLightmap;
	}

	public LightMap GetLightmap() {
		return lightmap;
	}

    public WaterLevelMap getLiquidLevelMap() {
        return liquidLevelMap;
    }

    public void setWater(Coord3 worldPos) {
        setBlockAtWorldCoord((byte) BlockType.WATER.ordinal(), worldPos);
    }

    public void setWaterRunOff(Coord3 worldPos) {
        setBlockAtWorldCoord((byte) BlockType.WATER_RUNOFF.ordinal(), worldPos);
    }

    public void unsetWater(Coord3 worldPos) {
        if (BlockType.IsWaterType(lookupBlock(worldPos)))
            setBlockAtWorldCoord((byte) BlockType.AIR.ordinal(), worldPos);
    }
    /*
     * Block debug info
     */
    public String getBlockInfoString(Coord3 global) {
        Coord3 chunkCo = Chunk.ToChunkPosition(global);
        Coord3 local = Chunk.toChunkLocalCoord(global);
        StringBuilder sb = new StringBuilder();
        sb.append("Block Info for: \n");
        sb.append(global.toString());
        sb.append("\n");
        Chunk ch = GetChunk(chunkCo);
        if (ch != null) {
            if (ch.chunkBlockFaceMap != null) {

                sb.append(ch.chunkBlockFaceMap.infoFor(local));
            } else {
                sb.append("null chunk block face");
            }
        } else {
            sb.append("null chunk");
        }
        sb.append("\n");
        sb.append(chunkCo.toString());
        sb.append("\n");
        sb.append(local.toString());
        sb.append("\n");
        return sb.toString();
    }


}
