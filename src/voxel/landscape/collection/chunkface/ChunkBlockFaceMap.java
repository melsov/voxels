package voxel.landscape.collection.chunkface;

import voxel.landscape.BlockMeshUtil;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.MeshSet;
import voxel.landscape.chunkbuild.blockfacefind.BlockFaceRecord;
import voxel.landscape.chunkbuild.blockfacefind.ChunkLocalCoord;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.fileutil.FileUtil;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.util.Asserter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class ChunkBlockFaceMap implements Serializable {

    private volatile ConcurrentHashMap<ChunkLocalCoord, BlockFaceRecord> faces = new ConcurrentHashMap<>(16*16*4);
    public volatile boolean deleteDirty; //True if a face has been deleted and map hasn't yet re-meshed
    public final AtomicBoolean writeDirty = new AtomicBoolean(true);

    private Map<ChunkLocalCoord, BlockFaceRecord> getFaces() {
        return faces;
    }
    public boolean empty() {
        return getFaces().isEmpty();
    }
    public Iterator<Map.Entry<ChunkLocalCoord, BlockFaceRecord>> iterator() {
        return faces.entrySet().iterator();
    }

    public void removeAllFacesUpdateNeighbors(Coord3 global, TerrainMap map) {
        Coord3 localCoord = Chunk.ToChunkLocalCoord(global);
        removeAllFaces(localCoord);
        for (int dir : Direction.Directions) {
            Coord3 globalNudge = global.add(Direction.DirectionCoords[dir]);
            /* Which (chunkBlock)FaceMap? Ours or a neighbors? */
            ChunkBlockFaceMap chunkBlockFaceMap;
//            Map<ChunkLocalCoord, BlockFaceRecord> faceMap;
            if (Chunk.ChunkLocalBox.contains(localCoord.add(Direction.DirectionCoords[dir]))) {
                chunkBlockFaceMap = this;
//                faceMap = getFaces();
            } else {
                Chunk neighbor = map.GetChunk(Chunk.ToChunkPosition(globalNudge));
                Asserter.assertTrue(neighbor != null, "null chunk!"); // TODO: FIX NULL P EXCEPTION HERE
                chunkBlockFaceMap = neighbor.chunkBlockFaceMap;
//                faceMap = neighbor.chunkBlockFaceMap.getFaces();
            }
            Coord3 localNudge = Chunk.ToChunkLocalCoord(globalNudge);
//            BlockFaceRecord blockFaceRecord = faceMap.get(new ChunkLocalCoord(localNudge));
            BlockFaceRecord blockFaceRecord = chunkBlockFaceMap.getFaces().get(new ChunkLocalCoord(localNudge));
            if (blockFaceRecord == null) {
                /* have we revealed a solid block here? */
                int blockType = map.lookupOrCreateBlock(globalNudge);
                if (BlockType.IsSolid(blockType)) {
                    blockFaceRecord = new BlockFaceRecord();
//                    faceMap.put(new ChunkLocalCoord(localNudge), blockFaceRecord);
                    chunkBlockFaceMap.getFaces().put(new ChunkLocalCoord(localNudge), blockFaceRecord);
                }
            }
            if (blockFaceRecord != null) {
                blockFaceRecord.setFace(Direction.OppositeDirection(dir), true);
                chunkBlockFaceMap.writeDirty.set(true);
            }
        }
    }
    public void addExposedFacesUpdateNeighbors(Coord3 global, TerrainMap map) {
        Coord3 localCoord = Chunk.ToChunkLocalCoord(global);
        for (int dir : Direction.Directions) {
            Coord3 globalNudge = global.add(Direction.DirectionCoords[dir]);
            /* Which faceMap? Ours or a neighbors? */
//            Map<ChunkLocalCoord, BlockFaceRecord> neighborFaceMap = null;
            ChunkBlockFaceMap chunkBlockFaceMap = null;
            if (Chunk.ChunkLocalBox.contains(localCoord.add(Direction.DirectionCoords[dir]))) {
                chunkBlockFaceMap = this;
//                neighborFaceMap = getFaces();
            } else {
                Chunk neighbor = map.GetChunk(Chunk.ToChunkPosition(globalNudge));
//                Asserter.assertTrue(neighbor != null, "null chunk!");
                if (neighbor != null) {
//                    neighborFaceMap = neighbor.chunkBlockFaceMap.getFaces();
                    chunkBlockFaceMap = neighbor.chunkBlockFaceMap;
                }

            }
            BlockFaceRecord blockFaceRecord = null;
            if (chunkBlockFaceMap != null && chunkBlockFaceMap.getFaces() != null) {
                blockFaceRecord = chunkBlockFaceMap.getFaces().get(new ChunkLocalCoord(Chunk.ToChunkLocalCoord(globalNudge)));
            }

            if (blockFaceRecord == null || !blockFaceRecord.getFace(Direction.OppositeDirection(dir))) {
                addFace(localCoord, dir);
            } else {
                blockFaceRecord.setFace(Direction.OppositeDirection(dir), false);
                chunkBlockFaceMap.writeDirty.set(true);
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
        ChunkLocalCoord bfCoord = new ChunkLocalCoord(localCoord);
        BlockFaceRecord blockFaceRecord = getFaces().get(bfCoord);
        if (blockFaceRecord == null) {
            if (!exists) return;
            blockFaceRecord = new BlockFaceRecord();
            getFaces().put(bfCoord, blockFaceRecord);
        }
        blockFaceRecord.setFace(direction, exists);
        writeDirty.set(true);
        if (!exists && !blockFaceRecord.hasFaces()) {
            getFaces().remove(bfCoord);
        }
    }
    public boolean getFace(Coord3 localCoord, int direction) {
        BlockFaceRecord blockFaceRecord = getFaces().get(new ChunkLocalCoord(localCoord));
        if (blockFaceRecord == null) return false;
        return blockFaceRecord.getFace(direction);
    }
    public String infoFor(Coord3 local) {
        BlockFaceRecord blockFaceRecord = getFaces().get(new ChunkLocalCoord(local));
        if (blockFaceRecord == null) return "null block face record";
        return blockFaceRecord.toString();
    }

    /*
     * Meshing
     */
    public void buildMeshFromMap(Chunk chunk, MeshSet mset, MeshSet waterMSet, boolean lightOnly, boolean liquidOnly) {
        TerrainMap map = chunk.getTerrainMap();
        Coord3 worldCoord = chunk.originInBlockCoords();
        int triIndex = 0, waterTriIndex = 0;

        Iterator<Map.Entry<ChunkLocalCoord, BlockFaceRecord>> iterator = chunk.chunkBlockFaceMap.iterator();

        while (iterator.hasNext())
        {
            Map.Entry<ChunkLocalCoord, BlockFaceRecord> entry = null;
            try {
                entry = iterator.next();
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
                Asserter.assertFalseAndDie("concurrent modification exception with chunk coord: " + chunk.position.toString());
            }
            ChunkLocalCoord blockFaceCoord = entry.getKey();
            BlockFaceRecord faceRecord =  entry.getValue(); //faces.get(blockFaceCoord);
            Coord3 blockWorldCoord = worldCoord.add(blockFaceCoord.toCoord3());

            int blockType = map.lookupOrCreateBlock(blockWorldCoord);

            if (BlockType.IsAirOrNonExistent(blockType)) continue;

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

    /*
     * Read/Write
     */
    public void readFromFile(Coord3 position) {
        Object facesO = FileUtil.DeserializeChunkObject(position, FileUtil.ChunkBlockFaceMapExtension);
        if (facesO != null) {
            faces = (ConcurrentHashMap<ChunkLocalCoord, BlockFaceRecord>) facesO;
            writeDirty.set(false);
            deleteDirty = true;
        }
    }

    public void writeToFile(Coord3 position) {
        try {
            FileUtil.SerializeChunkObject(faces, position, FileUtil.ChunkBlockFaceMapExtension);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        writeDirty.set(false);
        deleteDirty = true;
    }

}
