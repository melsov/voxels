package voxel.landscape.collection.chunkarray;

import voxel.landscape.Chunk;

public class ChunkUByte2D extends ChunkPrimitiveType2D
{
    private byte[] chunk = new byte [Chunk.CHUNKDIMS.x*Chunk.CHUNKDIMS.z];

    @Override
    public void Set(int val, int x, int z) {
        chunk[z * Chunk.CHUNKDIMS.x + x] = (byte)val;
    }
    @Override
    public int Get(int x, int z) {
        return chunk[z * Chunk.CHUNKDIMS.x + x] & 255; //bitwise conversion to positive
    }

//	private byte[][] chunk = new byte [Chunk.CHUNKDIMS.x][Chunk.CHUNKDIMS.z];
//
//	@Override
//	public void Set(int val, int x, int z) {
//		chunk[z][x] = (byte)val;
//	}
//	@Override
//	public int Get(int x, int z) {
//		return chunk[z][x] & 255; //bitwise conversion to positive
//	}
	
}
