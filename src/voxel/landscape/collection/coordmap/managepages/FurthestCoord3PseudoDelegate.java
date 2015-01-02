package voxel.landscape.collection.coordmap.managepages;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.ICoordXZ;

import java.util.Set;

/**
 * Created by didyouloseyourdog on 7/29/14.
 */
public class FurthestCoord3PseudoDelegate
{
    public Coord3 getFurthest2D(Camera cam, Set<Coord2> coords) { return getFurthest2D(cam, coords.toArray()); }

    public Coord3 getFurthest2D(Camera cam, Object[] coords) {
        if (coords.length == 0) return null;
        if (!(coords[0] instanceof ICoordXZ)) return null;

        ICoordXZ coordxz = (ICoordXZ) coords[0];
        Vector3f vloc3 = cam.getLocation().clone(); //  getSpatial().getWorldTranslation();
        vloc3 = Chunk.ToChunkPosition(Coord3.FromVector3f(vloc3)).toVector3();
        Vector2f vloc = new Vector2f(vloc3.x, vloc3.z);

        float furthest = 0f;
        ICoordXZ furthestXZ = (ICoordXZ) Coord2.zero;
        for(int i=0; i < coords.length; ++i) {
            coordxz = (ICoordXZ) coords[i];
            float nextDistance = vloc.distanceSquared( new Vector2f(coordxz.getX(), coordxz.getZ()));
            if (nextDistance > furthest) {
                furthest = nextDistance;
                furthestXZ = coordxz;
            }
        }

        return new Coord3(furthestXZ.getX(),0,furthestXZ.getZ());
    }
}
