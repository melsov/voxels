package voxel.landscape.collection.chunkface;

import voxel.landscape.BlockMeshUtil;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.MeshSet;
import voxel.landscape.chunkbuild.blockfacefind.BlockFaceRecord;
import voxel.landscape.chunkbuild.blockfacefind.ChunkBlockFaceCoord;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class ChunkBlockFaceMap {

    private Map<ChunkBlockFaceCoord, BlockFaceRecord> faces;
    public boolean deleteDirty; //True is face(s) has been deleted and map hasn't yet re-meshed

    private Map<ChunkBlockFaceCoord, BlockFaceRecord> getFaces() {
        if (faces == null) {
            faces = new HashMap<ChunkBlockFaceCoord, BlockFaceRecord>(16*16*4);
        }
        return faces;
    }
    public Iterator<Map.Entry<ChunkBlockFaceCoord, BlockFaceRecord>> iterator() {
        return faces.entrySet().iterator();
    }

    public void removeAllFaces(Coord3 localCoord) {
        for(int dir : Direction.Directions) {
            removeFace(localCoord, dir);
        }
    }
    public void removeFace(Coord3 localCoord, int direction) {
        setFace(localCoord, direction, false);
    }
    public void addFace(Coord3 localCoord, int direction) {
        setFace(localCoord, direction, false);
    }
    private void setFace(Coord3 localCoord, int direction, boolean exists) {
        BlockFaceRecord blockFaceRecord = getFaces().get(new ChunkBlockFaceCoord(localCoord));
        if (blockFaceRecord == null) {
            if (!exists) return;
            blockFaceRecord = new BlockFaceRecord();
            getFaces().put(new ChunkBlockFaceCoord(localCoord), blockFaceRecord);
        }
        blockFaceRecord.setFace(direction, exists);
    }
    public boolean getFace(Coord3 localCoord, int direction) {
        BlockFaceRecord blockFaceRecord = getFaces().get(new ChunkBlockFaceCoord(localCoord));
        if (blockFaceRecord == null) return false;
        return blockFaceRecord.getFace(direction);
    }

    public void buildMeshFromMap(Chunk chunk, MeshSet mset, MeshSet waterMSet, boolean lightOnly, boolean liquidOnly) {
        TerrainMap map = chunk.getTerrainMap();
        Coord3 worldCoord = chunk.originInBlockCoords();
        int triIndex = 0, waterTriIndex = 0;
        Iterator<Map.Entry<ChunkBlockFaceCoord, BlockFaceRecord>> iterator = chunk.chunkBlockFaceMap.iterator();
        while (iterator.hasNext())
        {
            Map.Entry<ChunkBlockFaceCoord, BlockFaceRecord> entry = iterator.next();
            ChunkBlockFaceCoord blockFaceCoord = entry.getKey();
            BlockFaceRecord faceRecord = entry.getValue();
            Coord3 blockWorldCoord = worldCoord.add(blockFaceCoord.toCoord3());
            byte blockType = (byte) map.lookupOrCreateBlock(blockWorldCoord);

            if (BlockType.AIR.equals(blockType)) continue;

            for (int dir : Direction.Directions)
            {
                if (faceRecord.getFace(dir)) {
                    if (BlockType.IsWaterType(blockType)) {
                        if (!lightOnly)
                            BlockMeshUtil.AddFaceMeshData(blockFaceCoord.toCoord3(), waterMSet, blockType, dir, waterTriIndex, map);
                        BlockMeshUtil.AddFaceMeshLightData(blockWorldCoord, waterMSet, dir, map);
                        waterTriIndex += 4;
                    } else {
                        if (!lightOnly)
                            BlockMeshUtil.AddFaceMeshData(blockFaceCoord.toCoord3(), mset, blockType, dir, triIndex, map);
                        BlockMeshUtil.AddFaceMeshLightData(blockWorldCoord, mset, dir, map);
                        triIndex += 4;
                    }
                }
            }
        }
        deleteDirty = false;
    }

}
