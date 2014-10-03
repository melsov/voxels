package voxel.landscape.collection.chunkface;

import voxel.landscape.chunkbuild.blockfacefind.BlockFaceRecord;
import voxel.landscape.chunkbuild.blockfacefind.ChunkBlockFaceCoord;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class ChunkBlockFaceMap {

    private Map<ChunkBlockFaceCoord, BlockFaceRecord> faces;

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
            setFace(localCoord, dir, false);
        }
    }
    public void removeFace(Coord3 localCoord, int direction) {
        setFace(localCoord, direction, false);
    }
    public void setFace(Coord3 localCoord, int direction, boolean exists) {
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

}
