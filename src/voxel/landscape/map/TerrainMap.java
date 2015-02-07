package voxel.landscape.map;

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

import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static voxel.landscape.BlockType.*;

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
    private LiquidUpdater liquidUpdaterWater = new LiquidUpdater(LiquidFlowTimeStepSeconds(WATER.ordinal()));

    private final VoxelLandscape app;

    public final BlockingQueue<Coord3> chunkCoordsToBeFlooded = new ArrayBlockingQueue<Coord3>(128);

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
			return (byte) NON_EXISTENT.ordinal();
		return chunk.blockAt(Chunk.toChunkLocalCoord(co));
	}

	public boolean blockAtWorldCoordIsTranslucent(Coord3 co) {
		return blockAtWorldCoordIsTranslucent(co.x, co.y, co.z);
	}

	public boolean blockAtWorldCoordIsTranslucent(int x, int y, int z) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(x, y, z));
		if (chunk == null) return false;
		return IsTranslucent(chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z)));
	}

	public void setBlockAtWorldCoord(byte block, Coord3 pos) {
		setBlockAtWorldCoord(block, pos.x, pos.y, pos.z);
	}

	public void setBlockAtWorldCoord(byte block, int x, int y, int z) {
		Chunk chunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(x, y, z));
		if (chunk != null) chunk.setBlockAt(block, Chunk.toChunkLocalCoord(x, y, z));
	}
    /*
     * GET IS SET WASIS (BLOCK LOOK UP)
     */
    public void setIsGetWasIs(Coord3 global, byte untouchedType, byte[] wasIs) {
        Chunk chunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(global));
        Coord3 local = Chunk.toChunkLocalCoord(global);
        wasIs[0] = chunk.blockAt(local);
        wasIs[1] = (byte) lookupOrCreateBlock(global);
    }

    public void setIsGetWasIsUnsetIfAir(Coord3 woco, byte untouchedType, byte[] wasIs) {
        setIsGetWasIs(woco, untouchedType, wasIs);
        if (wasIs[0] == untouchedType && wasIs[1] == AIR.ordinal()) {
            setBlockAtWorldCoord(untouchedType, woco);
        }
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
        if (NON_EXISTENT.equals((int) block) && ChunkCoordWithinWorldBounds(Chunk.ToChunkPosition(woco))) {
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
    public boolean isGlobalCoordWithinWorldBounds(Coord3 global) {
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
        generateNoiseForChunkColumn(x,z,terrainDataProvider, null);
    }

    public void generateNoiseForChunkColumn(int x, int z, TerrainDataProvider _dataProvider, HashSet<Coord3> touchedChunkCoords) {
        Coord3 chunkPos = new Coord3(x, MAX_CHUNK_DIM_VERTICAL - 1, z);
        instantiateChunksInColumn(x, z);
        generateNoiseForColumnToSurface(chunkPos.x, chunkPos.z, _dataProvider, touchedChunkCoords);
    }
    private void instantiateChunksInColumn(int x, int z) {
        Coord3 chunkPos = new Coord3(x, MAX_CHUNK_DIM_VERTICAL - 1, z);
        for (; chunkPos.y >= MIN_CHUNK_DIM_VERTICAL; chunkPos.y--) {
            lookupOrCreateChunkAtPosition(chunkPos);
        }
    }

    /*
     * get the terrain data inside the chunk and also in the -1/+1 box (in block units) that
     * surrounds it (within world limits if any)
     */
    private boolean generateNoiseForColumnToSurface(int cx, int cz, TerrainDataProvider _dataProvider, HashSet<Coord3> touchedChunkCoords) {

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

                    //THIS ADD OPERATION IS ONLY MEANINGFUL ONCE PER CHUNK BUT OH WELL
                    if (i > -1 && j > -1 && i < Chunk.ZLENGTH && j < Chunk.ZLENGTH)
                        touchedChunkCoords.add(nextChunkPos);

                    byte block;
                    block = (byte) lookupOrCreateBlock(absPos, _dataProvider);

					/*
					 * CONSIDER: grass is too simplistic! grass should grow only
					 * where there's light... Also, put this logic somewhere
					 * where it will also apply to newly created, destroyed blocks
					 * may need a 'block time step' concept...
					 */
                    // should this block be grass?
                    if (DIRT.ordinal() == block) {
                        Coord3 upOne = absPos.add(Coord3.ypos);
                        if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null ||
                                IsTranslucent((byte) lookupOrCreateBlock(upOne))) {
                            block = (byte) GRASS.ordinal();
                        }
                    } else if (WATER.ordinal() == block) {
                        liquidLevelMap.SetWaterLevel(WaterFlowComputer.MAX_WATER_LEVEL, absPos);
                        possibleChunk.getChunkBrain().SetLiquidDirty();
                    }

                    relPos = Chunk.toChunkLocalCoord(relPos);
                    possibleChunk.setBlockAt(block, relPos);

                    if (!foundDirt && k > -1 && j > -1 && i > -1 && k <= Chunk.YLENGTH && j <= Chunk.ZLENGTH && i <= Chunk.XLENGTH) {
                        foundDirt = IsSolid(block);
                    }
                    if (IsSolid(block)) {
                        possibleChunk.chunkBlockFaceMap.addFace(relPos, Direction.YPOS);
                        /* Set sunlight map */
                        sunLightmap.SetSunHeight(wocoY + 1, absPos.x, absPos.z);
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
    public void populateFloodFillSeedsUpdateFaceMapsInChunkColumn(int cx, int cz, TerrainDataProvider _terrainDataProvider, HashSet<Coord3> touchedChunkCoords) {
        Coord3 worldPos = Chunk.ToWorldPosition(new Coord3(cx, 0, cz));

        Coord3 surfacePos;
        /*
         * FIND OVERHANGS: SOLID COLUMNS WITH AIR UNDERNEATH THEM. AND THE AIR IS NOT COVERED BY THEIR NEIGHBOR COLUMNS
         */
        // X and Z pos
        for (int i = -1; i < Chunk.XLENGTH; ++i) {
            for (int j = -1; j < Chunk.ZLENGTH; ++j) {
                if (i == -1 && j == -1) continue;
                surfacePos = worldPos.add(new Coord3(i, 0, j));
                surfacePos.y = getSurfaceHeight(surfacePos.x, surfacePos.z);

                if (i > -1 && j < Chunk.ZLENGTH - 1) {
                    climbUpCliff(surfacePos, surfacePos.y, Direction.ZPOS, _terrainDataProvider, touchedChunkCoords);
                }
                if (j > -1 && i < Chunk.XLENGTH - 1) {
                    climbUpCliff(surfacePos, surfacePos.y, Direction.XPOS, _terrainDataProvider, touchedChunkCoords);
                }
            }
        }
        // X and Z neg
        for (int i = Chunk.XLENGTH; i >= 0 ; --i) {
            for (int j = Chunk.ZLENGTH; j >= 0 ; --j) {
                if (i == Chunk.XLENGTH && j == Chunk.ZLENGTH) continue;
                surfacePos = worldPos.add(new Coord3(i, 0, j));
                surfacePos.y = getSurfaceHeight(surfacePos.x, surfacePos.z);

                if (i < Chunk.XLENGTH && j > 0) {
                    climbUpCliff(surfacePos, surfacePos.y, Direction.ZNEG, _terrainDataProvider, touchedChunkCoords);
                }
                if (j < Chunk.ZLENGTH && i > 0) {
                    climbUpCliff(surfacePos, surfacePos.y, Direction.XNEG, _terrainDataProvider, touchedChunkCoords);
                }
            }
        }
        // TODO: add chunk coord to needs flood fill--here?
    }

    public boolean isAboveSurface(Coord3 coord3) { return getSurfaceHeight(coord3) < coord3.y;  }
    public int getSurfaceHeight(Coord3 coord3) { return getSurfaceHeight(coord3.x,coord3.z); }
    public int getSurfaceHeight(int x, int z) { return sunLightmap.GetSunHeight(x,z) - 1; }

    private void climbUpCliff(Coord3 absPos, int height, int direction, TerrainDataProvider _terrainDataProvider, HashSet<Coord3> touchedChunkCoords) {
        Coord3 neighbor = absPos.add(Direction.DirectionCoords[direction]);
        int neighborHeight = getSurfaceHeight(neighbor);
        if (height >= neighborHeight) {
            return;
        }

        for(neighbor.y = height + 1; neighbor.y <= neighborHeight; ++neighbor.y ) {
            Coord3 chunkCoord = Chunk.ToChunkPosition(neighbor);
            Chunk chunk = lookupOrCreateChunkAtPosition(chunkCoord);

            int was = chunk.blockAt(Chunk.toChunkLocalCoord(neighbor));
            int is = was;
            if (was == NON_EXISTENT.ordinal()) {
                is = lookupOrCreateBlock(neighbor, _terrainDataProvider);
            }

            if (was == NON_EXISTENT.ordinal() && IsTranslucent(is)) {
                // TODO: deal with water
                chunk.chunkFloodFillSeedSet.addCoord(neighbor);
                // INEFFICIENT BUT NECESSARY UNTIL WE HAVE 'PLACEHOLDER_AIR.' UNSET THE BLOCK!
                chunk.setBlockAt((byte) NON_EXISTENT.ordinal(), Chunk.toChunkLocalCoord(neighbor));

                //TACKING ON SUNLIGHT UPDATE
                sunLightmap.SetLight(SunLightComputer.OneStepDownFromMaxLight(AIR), neighbor);
            } //else
            if (IsSolid(is)) {
                touchedChunkCoords.add(chunkCoord);
                chunk.chunkBlockFaceMap.addFace(Chunk.toChunkLocalCoord(neighbor),Direction.OppositeDirection(direction));
            }
        }
    }
    public void updateChunksToBeFlooded(HashSet<Coord3> chunkCoords) {
        for (Coord3 co : chunkCoords) {
            try {
                chunkCoordsToBeFlooded.put(co);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
                        block = (byte) WATER.ordinal();
                    } else if (IsFakeWaterChunk && k > Chunk.YLENGTH - 5 ) {
                        block = (byte) AIR.ordinal();
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
                    if (DIRT.ordinal() == block) {
                        Coord3 upOne = absPos.add(Coord3.ypos);
                        if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null ||
                                IsTranslucent((byte) lookupOrCreateBlock(upOne))) {
                            block = (byte) GRASS.ordinal();
                        }
                    } else if (WATER.ordinal() == block) {
                        liquidLevelMap.SetWaterLevel(WaterFlowComputer.MAX_WATER_LEVEL, absPos);
                        possibleChunk.getChunkBrain().SetLiquidDirty();
                    }

                    relPos = Chunk.toChunkLocalCoord(relPos);
                    possibleChunk.setBlockAt(block, relPos);

                    if (!foundDirt && k > -1 && j > -1 && i > -1 && k <= Chunk.YLENGTH && j <= Chunk.ZLENGTH && i <= Chunk.XLENGTH) {
                        foundDirt = IsSolid(block);
                    }
                }
            }
        }
        targetChunk.setIsAllAir(!foundDirt);
        return true;
    }


	/*
	 * A block changed somewhere.
	 * Update the map and check if anything else needs updating
	 * Credit: Mr. Wishmaster methods (YouTube)
	 */
	public void SetBlockAndRecompute(byte block, Coord3 global) {
        if (!isGlobalCoordWithinWorldBounds(global)) return;

		Coord3 chunkPos = Chunk.ToChunkPosition(global);
		Coord3 localPos = Chunk.toChunkLocalCoord(global);

        Chunk chunk = GetChunk(chunkPos);
        byte wasType = chunk.blockAt(localPos);
        if (wasType == block) return;

		setBlockAtWorldCoord(block, global);

        if (IsTranslucent(block)) {
            chunk.chunkBlockFaceMap.removeAllFacesUpdateNeighbors(global, this);
        }
        if (block != AIR.ordinal()){
            if (IsSolid(block)) {
                chunk.chunkBlockFaceMap.addExposedFacesUpdateNeighbors(global, this);
            }
            //TODO: add faces for the new block. remove occluded faces.
            else {} // TODO: if liquid or glass...
        }
        //need to flood fill?
        if (false) // ****************** !
        if (!IsTranslucent(wasType) && AIR.ordinal() == block) {
            chunk.setBlockAt((byte) PLACEHOLDER_AIR.ordinal(), localPos); // must 'fool' flood fill
            chunk.chunkFloodFillSeedSet.addCoord(global);
            app.getWorldGenerator().blockFaceFinder.floodFill.startFlood(chunkPos);
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

		SunLightComputer.RecomputeLightAtPosition(this, global.clone());
		LightComputer.RecomputeLightAtPosition(this, global.clone());

        //TODO: make logic 'more abstract' so TerrainMap doesn't have to deal with the following...
        if (WATER.ordinal() == block) {
            liquidUpdaterWater.addCoord(global.clone(), WaterFlowComputer.MAX_WATER_LEVEL);
        } else {
            WaterFlowComputer.RecomputeWaterAtPosition(this, global.clone());
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
				if (!IsAirOrNonExistent(block)) {
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
        setBlockAtWorldCoord((byte) WATER.ordinal(), worldPos);
    }

    public void setWaterRunOff(Coord3 worldPos) {
        setBlockAtWorldCoord((byte) WATER_RUNOFF.ordinal(), worldPos);
    }

    public void unsetWater(Coord3 worldPos) {
        if (IsWaterType(lookupBlock(worldPos)))
            setBlockAtWorldCoord((byte) AIR.ordinal(), worldPos);
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

    /*
     * Look up any light
     */
    public int lightAt(Coord3 global) {
        return Math.max(sunLightmap.GetLight(global), lightmap.GetLight(global));
    }


}
