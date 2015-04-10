package voxel.landscape.settings;

import com.jme3.math.Vector3f;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord3;

/**
 * Created by didyouloseyourdog on 4/9/15.
 */
public class BuildSettings {

    public static int ADD_COLUMN_RADIUS = 6;
    public static int REMOVE_OUTSIDE_OF_COLUMN_RADIUS = ADD_COLUMN_RADIUS + 2;

    public static boolean ChunkCoordOutsideOfRemoveRadius(Vector3f camera, Coord3 chunkCoord) {
        return !ChunkCoordWithinRadius(camera, chunkCoord, REMOVE_OUTSIDE_OF_COLUMN_RADIUS);
    }
    
    public static boolean ChunkCoordWithinAddRadius(Vector3f camera, Coord3 chunkCoord) {
        return ChunkCoordWithinRadius(camera, chunkCoord, ADD_COLUMN_RADIUS);
    }

    private static boolean ChunkCoordWithinRadius(Vector3f camera, Coord3 chunkCoord, int radius) {
        Coord3 cameraCoord = Chunk.ToChunkPosition(Coord3.FromVector3f(camera));
        return cameraCoord.distanceXZSquared(chunkCoord) < radius * radius;
    }
    
    
    
}
