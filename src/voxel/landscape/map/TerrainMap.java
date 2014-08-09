package voxel.landscape.map;

import com.jme3.math.Vector3f;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.VoxelLandscape;
import voxel.landscape.collection.coordmap.managepages.ConcurrentHashMapCoord3D;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.light.LightComputer;
import voxel.landscape.map.light.LightMap;
import voxel.landscape.map.light.SunLightComputer;
import voxel.landscape.map.light.SunLightMap;
import voxel.landscape.noise.IBlockDataProvider;
import voxel.landscape.noise.TerrainDataProvider;
import voxel.landscape.util.Asserter;

public class TerrainMap implements IBlockDataProvider {
	private static final int MIN_DIM_HORIZONTAL = -8;
	private static final int MAX_DIM_HORIZONTAL = 8; // for limited dimension world only

    private static final int MIN_DIM_VERTICAL = 0;
	private static final int MAX_DIM_VERTICAL = 1;

	public static Coord3 MIN_CHUNK_COORD = new Coord3(MIN_DIM_HORIZONTAL, MIN_DIM_VERTICAL, MIN_DIM_HORIZONTAL);
	public static Coord3 MAX_CHUNK_COORD = new Coord3(MAX_DIM_HORIZONTAL,MAX_DIM_VERTICAL, MAX_DIM_HORIZONTAL);

	TerrainDataProvider terrainDataProvider = new TerrainDataProvider();

    private ConcurrentHashMapCoord3D<Chunk> chunks = new ConcurrentHashMapCoord3D<Chunk>(Chunk.class);
	private SunLightMap sunLightmap = new SunLightMap();
	private LightMap lightmap = new LightMap();

	public TerrainMap() {
	}

	public static int GetWorldHeightInChunks() {
		return MAX_DIM_VERTICAL - MIN_DIM_VERTICAL;
	}

	public static int GetWorldHeightInBlocks() {
		return GetWorldHeightInChunks() * Chunk.YLENGTH;
	}

    public byte lookupBlock(int x, int y, int z) {
		return lookupBlock(new Coord3(x, y, z));
	}

    public byte lookupBlock(Vector3f v) {  return lookupBlock((int)v.x, (int)v.y, (int)v.z); }

    /* TODO: consider how to make this thread-safe */
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
		return BlockType.isTranslucent(chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z)));
	}

	public void setBlockAtWorldCoord(byte block, Coord3 pos) {
		setBlockAtWorldCoord(block, pos.x, pos.y, pos.z);
	}

	public void setBlockAtWorldCoord(byte block, int x, int y, int z) {
		Chunk chunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(x, y, z));
		if (chunk != null) chunk.setBlockAt(block, Chunk.toChunkLocalCoord(x, y, z));
	}

    public int getMinChunkCoordY() { return MIN_DIM_VERTICAL; }
    public int getMaxChunkCoordY() { return MAX_DIM_VERTICAL; }

//	public int lookupOrCreateBlock(Coord3 woco, TerrainDataProvider _terrainData) {
//		return lookupOrCreateBlock(woco, _terrainData);
//	}

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
            block = (byte) _terrainData.getBlockDataAtPosition(woco.x, woco.y, woco.z);
            setBlockAtWorldCoord(block, woco.x, woco.y, woco.z);
        }
        return block;
    }
    public boolean ChunkCoordWithinWorldBounds(Coord3 chunkCoord) {
        return chunkCoord.y >= MIN_DIM_VERTICAL && chunkCoord.y < MAX_DIM_VERTICAL;
    }

    /*
    Clean up
     */
    public void removeColumnData(int x, int z) {
        Coord3 chunkPos = new Coord3(x, MIN_DIM_VERTICAL, z);
        for (; chunkPos.y < MAX_DIM_VERTICAL; chunkPos.y++) {
            chunks.Remove(chunkPos);
            sunLightmap.RemoveLightData(chunkPos);
            lightmap.RemoveLightData(chunkPos);
        }
        sunLightmap.RemoveRays(x,z);
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

    }

    private void doGenerateNoiseForChunkColumn(int x, int z, TerrainDataProvider _dataProvider) {
        Coord3 chunkPos = new Coord3(x, MIN_DIM_VERTICAL, z);
        for (; true; chunkPos.y++) {
            boolean generated = generateNoiseForChunkAt(chunkPos.x, chunkPos.y,
                    chunkPos.z, _dataProvider);
            /* CONSIDER: thread sleep at some point? */

            if (!generated)
                break;
        }
    }


	/*
	 * get the terrain data inside the chunk and also in the -1/+1 box that
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
					byte block = (byte) lookupOrCreateBlock(absPos, _dataProvider);
					// should be grass?
					/*
					 * CONSIDER: this is too simplistic! grass should grow only
					 * where there's light... Also, put this logic somewhere
					 * where it will also apply to newly created, destroyed blocks
					 * may need a 'block time step' concept...
					 */
					if (BlockType.DIRT.ordinal() == block) {
						Coord3 upOne = absPos.add(Coord3.ypos);
//						Chunk oneBlockUpChunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)); // maybe same chunk
                        if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null || BlockType.isTranslucent((byte) lookupOrCreateBlock(upOne))) {
                            block = (byte) BlockType.GRASS.ordinal();
                        }
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
            if (lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)) == null || BlockType.isTranslucent((byte) lookupOrCreateBlock(upOne))) {
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

		SunLightComputer.RecomputeLightAtPosition(this, pos);
		LightComputer.RecomputeLightAtPosition(this, pos);
	}

	private void SetDirty(Coord3 chunkPos) {
		Chunk chunk = GetChunk(chunkPos);
		if (chunk != null)
			chunk.getChunkBrain().SetDirty();
	}

	public int GetMaxY(int x, int z) {
		Coord3 chunkPos = Chunk.ToChunkPosition(x, 0, z);
		chunkPos.y = MAX_DIM_VERTICAL; // chunks.GetMax().y;
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
        if (!ChunkCoordWithinWorldBounds(chunkPos))
            return null;

        Chunk result = chunks.putIfKeyIsAbsent(chunkPos, new Chunk(chunkPos, this));
        if (result == null) {
            result = chunks.Get(chunkPos);
        }
        return result;
	}

	public Chunk GetChunk(Coord3 chunkPos) {
		return chunks.Get(chunkPos);
	}
    public Chunk GetChunk(int x, int y, int z) {
        return GetChunk(new Coord3(x,y,z));
    }

//	public List3D<Chunk> GetChunks() {
//		return chunks;
//	}

//    public HashMapCoord3D<Chunk> GetChunks() {
//        return chunks;
//    }

	public SunLightMap GetSunLightmap() {
		return sunLightmap;
	}

	public LightMap GetLightmap() {
		return lightmap;
	}

}
