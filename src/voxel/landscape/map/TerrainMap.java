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

public class TerrainMap implements IBlockDataProvider {
	private static final int MIN_DIM_HORIZONTAL = -8;
	private static final int MAX_DIM_HORIZONTAL = 8; // limited dimension
														// world for now.
    private static final int MIN_DIM_VERTICAL = 0;
	private static final int MAX_DIM_VERTICAL = 4;

	public static Coord3 MIN_CHUNK_COORD = new Coord3(MIN_DIM_HORIZONTAL, MIN_DIM_VERTICAL, MIN_DIM_HORIZONTAL);
	public static Coord3 MAX_CHUNK_COORD = new Coord3(MAX_DIM_HORIZONTAL,MAX_DIM_VERTICAL, MAX_DIM_HORIZONTAL);

	TerrainDataProvider terrainDataProvider = new TerrainDataProvider();

//	List3D<Chunk> chunks = new List3D<Chunk>(MIN_CHUNK_COORD, MAX_CHUNK_COORD, Chunk.class);
    ConcurrentHashMapCoord3D<Chunk> chunks = new ConcurrentHashMapCoord3D<Chunk>(Chunk.class);
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

	public byte lookupBlock(Coord3 co) {
		return lookupBlock(co.x, co.y, co.z);
	}

    public byte lookupBlock(Vector3f v) {  return lookupBlock((int)v.x, (int)v.y, (int)v.z); }

	public byte lookupBlock(int x, int y, int z) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(x, y, z));
		if (chunk == null)
			return (byte) BlockType.NON_EXISTENT.ordinal();
		return chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z));
	}

	public boolean blockAtWorldCoordIsTranslucent(Coord3 co) {
		return blockAtWorldCoordIsTranslucent(co.x, co.y, co.z);
	}

	public boolean blockAtWorldCoordIsTranslucent(int x, int y, int z) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(x, y, z));
		if (chunk == null)
			return false;
		return BlockType.isTranslucent(chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z)));
	}

	public void setBlockAtWorldCoord(byte block, Coord3 pos) {
		setBlockAtWorldCoord(block, pos.x, pos.y, pos.z);
	}

	public void setBlockAtWorldCoord(byte block, int x, int y, int z) {
		Chunk chunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(x, y,
				z));
		if (chunk != null) {
			chunk.setBlockAt(block, Chunk.toChunkLocalCoord(x, y, z));
		}
	}

    public int getMinChunkCoordY() { return MIN_DIM_VERTICAL; }
    public int getMaxChunkCoordY() { return MAX_DIM_VERTICAL; }

	@Override
	public int lookupOrCreateBlock(Coord3 woco) {
		return lookupOrCreateBlock(woco.x, woco.y, woco.z);
	}

    public int lookupOrCreateBlock(Coord3 woco, TerrainDataProvider _terrainData) {
        return lookupOrCreateBlock(woco.x, woco.y, woco.z, _terrainData);
    }
	
	@Override
	public int lookupOrCreateBlock(int xin, int yin, int zin) {
//		byte block = lookupBlock(xin, yin, zin);
//		if (BlockType.NON_EXISTENT.equals((int) block) && ChunkCoordWithinWorldBounds(Chunk.ToChunkPosition(xin,yin,zin))) { // chunks.IndexWithinBounds(Chunk.ToChunkPosition(xin, yin, zin))) {
//			block = (byte) terrainDataProvider.getBlockDataAtPosition(xin, yin, zin);
//			setBlockAtWorldCoord(block, xin, yin, zin);
//		}
//		return block;
        return lookupOrCreateBlock(xin,yin,zin,terrainDataProvider);
	}
    public int lookupOrCreateBlock(int xin, int yin, int zin, TerrainDataProvider _terrainData) {
        byte block = lookupBlock(xin, yin, zin);
        if (BlockType.NON_EXISTENT.equals((int) block) && ChunkCoordWithinWorldBounds(Chunk.ToChunkPosition(xin,yin,zin))) { // chunks.IndexWithinBounds(Chunk.ToChunkPosition(xin, yin, zin))) {
            block = (byte) _terrainData.getBlockDataAtPosition(xin, yin, zin);
            setBlockAtWorldCoord(block, xin, yin, zin);
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
	private boolean generateNoiseForChunkAt(int x, int y, int z, TerrainDataProvider _dataProvider) {
		Chunk chunk = lookupOrCreateChunkAtPosition(x, y, z);
		Coord3 worldPos = Chunk.ToWorldPosition(new Coord3(x, y, z)); // ,

        /* TODO: check for all air */
        boolean foundDirt = false;
        if (chunk == null)
			return false;
		for (int i = -1; i < Chunk.CHUNKDIMS.x + 1; ++i) {
			for (int j = -1; j < Chunk.CHUNKDIMS.z + 1; ++j) {
				for (int k = -1; k < Chunk.CHUNKDIMS.y + 1; ++k) {
					Coord3 relPos = new Coord3(i, k, j);
					Coord3 absPos = worldPos.add(relPos);
					Chunk possibleChunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(absPos));

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
						Chunk oneBlockUpChunk = lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(upOne)); // maybe same chunk
                        if (oneBlockUpChunk == null) {
                            block = (byte) BlockType.GRASS.ordinal();
                        } else {
							byte oneBlockUp = (byte) lookupOrCreateBlock(upOne);
							if (BlockType.isTranslucent(oneBlockUp)) {
								block = (byte) BlockType.GRASS.ordinal();
							}
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
        chunk.setIsAllAir(!foundDirt);
		return true;
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
			return null; // Limited world right now...

		Chunk chunk = chunks.SafeGet(chunkPos); // GetChunk(chunkPos);
		if (chunk == null) {
			chunk = new Chunk(chunkPos, this);
			chunks.AddOrReplace(chunk, chunkPos);
		}
		return chunk;
	}

	public Chunk GetChunk(Coord3 chunkPos) {
		return chunks.SafeGet(chunkPos);
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
