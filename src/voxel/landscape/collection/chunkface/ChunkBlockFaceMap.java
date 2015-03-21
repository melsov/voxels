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
import voxel.landscape.util.Asserter;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class ChunkBlockFaceMap {

    private volatile ConcurrentHashMap<ChunkBlockFaceCoord, BlockFaceRecord> faces = new ConcurrentHashMap<>(16*16*4);
    public volatile boolean deleteDirty; //True if a face has been deleted and map hasn't yet re-meshed

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
            Coord3 globalNudge = global.add(Direction.DirectionCoords[dir]);
            /* Which faceMap? Ours or a neighbors? */
            Map<ChunkBlockFaceCoord, BlockFaceRecord> faceMap;
            if (Chunk.ChunkLocalBox.contains(localCoord.add(Direction.DirectionCoords[dir]))) {
                faceMap = getFaces();
            } else {
                Chunk neighbor = map.GetChunk(Chunk.ToChunkPosition(globalNudge));
                Asserter.assertTrue(neighbor != null, "null chunk!");
                faceMap = neighbor.chunkBlockFaceMap.getFaces(); // TODO: FIX NULL P EXCEPTION HERE
            }
            Coord3 localNudge = Chunk.toChunkLocalCoord(globalNudge);
            BlockFaceRecord blockFaceRecord = faceMap.get(new ChunkBlockFaceCoord(localNudge));
            if (blockFaceRecord == null) {
                /* have we revealed a solid block here? */
                int blockType = map.lookupOrCreateBlock(globalNudge);
                if (BlockType.IsSolid(blockType)) {
                    blockFaceRecord = new BlockFaceRecord();
                    faceMap.put(new ChunkBlockFaceCoord(localNudge), blockFaceRecord);
                }
            }
            if (blockFaceRecord != null) {
                blockFaceRecord.setFace(Direction.OppositeDirection(dir), true);
            }
        }
    }
    public void addExposedFacesUpdateNeighbors(Coord3 global, TerrainMap map) {
        Coord3 localCoord = Chunk.toChunkLocalCoord(global);
        for (int dir : Direction.Directions) {
            Coord3 globalNudge = global.add(Direction.DirectionCoords[dir]);
            /* Which faceMap? Ours or a neighbors? */
            Map<ChunkBlockFaceCoord, BlockFaceRecord> neighborFaceMap;
            if (Chunk.ChunkLocalBox.contains(localCoord.add(Direction.DirectionCoords[dir]))) {
                neighborFaceMap = getFaces();
            } else {
                Chunk neighbor = map.GetChunk(Chunk.ToChunkPosition(globalNudge));
                Asserter.assertTrue(neighbor != null, "null chunk!");
                neighborFaceMap = neighbor.chunkBlockFaceMap.getFaces();
            }
            Coord3 localNudge = Chunk.toChunkLocalCoord(globalNudge);
            ChunkBlockFaceCoord neighborBFCoord = new ChunkBlockFaceCoord(localNudge);
            BlockFaceRecord blockFaceRecord = neighborFaceMap.get(neighborBFCoord);

            if (blockFaceRecord == null || !blockFaceRecord.getFace(Direction.OppositeDirection(dir))) {
                addFace(localCoord, dir);
            } else {
                blockFaceRecord.setFace(Direction.OppositeDirection(dir), false);
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
            Map.Entry<ChunkBlockFaceCoord, BlockFaceRecord> entry = null;
            try {
                entry = iterator.next();
            } catch (ConcurrentModificationException e) {
                B.bug("concurrent modif exception with chunk Coord: " + chunk.position.toString());
                e.printStackTrace();
                Asserter.assertFalseAndDie("death");
            }
            ChunkBlockFaceCoord blockFaceCoord = entry.getKey();
            BlockFaceRecord faceRecord =  entry.getValue(); //faces.get(blockFaceCoord);
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
