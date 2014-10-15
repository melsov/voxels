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
import voxel.landscape.player.B;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class ChunkBlockFaceMap {

    private volatile Map<ChunkBlockFaceCoord, BlockFaceRecord> faces = new HashMap<ChunkBlockFaceCoord, BlockFaceRecord>(16*16*4);
    public volatile boolean deleteDirty; //True is face(s) has been deleted and map hasn't yet re-meshed

    private Map<ChunkBlockFaceCoord, BlockFaceRecord> getFaces() {
        return faces;
    }
    public boolean empty() {
        return getFaces().isEmpty();
    }
    public Iterator<Map.Entry<ChunkBlockFaceCoord, BlockFaceRecord>> iterator() {
        return faces.entrySet().iterator();
    }

    public void removeAllFacesUpdateNeighbors(Coord3 global, TerrainMap map) {
        Coord3 localCoord = Chunk.toChunkLocalCoord(global);
        removeAllFaces(localCoord);
        for (int dir : Direction.Directions) {
            Coord3 localNudge = localCoord.add(Direction.DirectionCoords[dir]);
            /* Which faceMap? Ours or a neighbors? */
            Map<ChunkBlockFaceCoord, BlockFaceRecord> faceMap;
            if (Chunk.ChunkLocalBox.contains(localNudge)) {
                faceMap = getFaces();
            } else {
                Chunk neighbor = map.GetChunk(Chunk.ToChunkPosition(global.add(Direction.DirectionCoords[dir])));
                faceMap = neighbor.chunkBlockFaceMap.getFaces();
            }
            BlockFaceRecord blockFaceRecord = faceMap.get(new ChunkBlockFaceCoord(localNudge));
            if (blockFaceRecord == null) {
                /* have we revealed a solid block here? */
                int blockType = map.lookupOrCreateBlock(global);
                if (BlockType.IsSolid(blockType)) {
                    blockFaceRecord = new BlockFaceRecord();
                    faceMap.put(new ChunkBlockFaceCoord(localCoord), blockFaceRecord);
                } else {
//                    continue;
                    // TODO: mechanism for adding a flood fill seed here: we could be opening up a view to a cave
                    // TODO: make this logic deal with with translucent but rendered types. glass-water, etc.
                    // they need to render but also should cause a flood fill seed.
                }
            }
            if (blockFaceRecord != null) {
                blockFaceRecord.setFace(Direction.OppositeDirection(dir), true);
            }
        }
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
        setFace(localCoord, direction, true);
    }
    private void setFace(Coord3 localCoord, int direction, boolean exists) {
        ChunkBlockFaceCoord bfCoord = new ChunkBlockFaceCoord(localCoord);
        BlockFaceRecord blockFaceRecord = getFaces().get(bfCoord);
        if (blockFaceRecord == null) {
            if (!exists) return;
            blockFaceRecord = new BlockFaceRecord();
            getFaces().put(bfCoord, blockFaceRecord);
        }
        blockFaceRecord.setFace(direction, exists);
        if (!exists && !blockFaceRecord.hasFaces()) {
            getFaces().remove(bfCoord);
        }
    }
    public boolean getFace(Coord3 localCoord, int direction) {
        BlockFaceRecord blockFaceRecord = getFaces().get(new ChunkBlockFaceCoord(localCoord));
        if (blockFaceRecord == null) return false;
        return blockFaceRecord.getFace(direction);
    }
    public String infoFor(Coord3 local) {
        BlockFaceRecord blockFaceRecord = getFaces().get(new ChunkBlockFaceCoord(local));
        if (blockFaceRecord == null) return "null block face record";
        return blockFaceRecord.toString();
    }

    public ChunkBlockFaceMap() {
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
            if(!faceRecord.DEBUGisOnlyXAxisFalse()) {
//            if(!faceRecord.DEBUGisOnlyZAxisFalse()) {
                B.bugln("non conformist block at " + blockWorldCoord.toString() + "\n" + faceRecord.toString());
            }
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
