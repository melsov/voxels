package voxel.landscape.collection.coordmap.managepages;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.ICoordXZ;
import voxel.landscape.map.TerrainMap;

/**
 * Created by didyouloseyourdog on 7/29/14.
 */
public class FurthestChunkFinder
{
    public Coord3 getFurthest2D(TerrainMap map, Camera cam, Object[] coords) {
        return getFurthest2D(map, cam, coords, false);
    }
    public Coord3 furthestWriteDirtyButNotYetWritingChunk(TerrainMap map, Camera cam, Object[] coords) {
        return getFurthest2D(map, cam, coords, true);
    }

    public Coord3 getFurthest2D(TerrainMap map, Camera cam, Object[] coords, boolean wantNotWritingData) {
        if (coords.length == 0) return null;
        if (!(coords[0] instanceof ICoordXZ)) return null;

        ICoordXZ coordxz;
        Vector3f vloc3 = cam.getLocation().clone();
        vloc3 = Chunk.ToChunkPosition(Coord3.FromVector3f(vloc3)).toVector3();
        Vector2f vloc = new Vector2f(vloc3.x, vloc3.z);

        float furthest = 0f;
        ICoordXZ furthestXZ = null; // (ICoordXZ) Coord2.zero;
        for(int i=0; i < coords.length; ++i) {
            coordxz = (ICoordXZ) coords[i];
//            if (wantNotWritingData && columnMap.IsWritingData(coordxz.getX(), coordxz.getZ())) {
//                B.bug((Coord3) coordxz);
//                continue;
//            }
            Coord3 writeDirtyChunk = !wantNotWritingData ? null : map.writeDirtyButNotWritingChunkInColumn(coordxz.getX(), coordxz.getZ());
            if (!wantNotWritingData || writeDirtyChunk != null) {
                float nextDistance = vloc.distanceSquared(new Vector2f(coordxz.getX(), coordxz.getZ()));
                if (nextDistance > furthest) {
                    furthest = nextDistance;
                    furthestXZ = writeDirtyChunk;
                }
            }
        }
        return (Coord3)furthestXZ;
    }
}
