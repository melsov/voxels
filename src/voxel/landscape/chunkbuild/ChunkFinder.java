package voxel.landscape.chunkbuild;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import voxel.landscape.Chunk;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.jme3.math.FastMath.PI;

/**
 * Created by didyouloseyourdog on 7/31/14.
 */
public class ChunkFinder {

    private static List<Vector3f> lookDirections = new ArrayList<Vector3f>();

    static {
        Quaternion q = Quaternion.IDENTITY.clone();
        lookDirections.add(Vector3f.UNIT_Z.clone());
        Vector3f forward = Vector3f.UNIT_Z.clone();

        float turn_divisions = 12f;
        for(int j= (int) turn_divisions; j> 0 ; --j) {
            Quaternion turn = q.clone().mult(CreateFromAxisAngle(0,1,0, PI/2f *(j/turn_divisions)));
            float quarter_divisions = 12f;
            for (int i = 1; i < quarter_divisions; ++i) {
                Quaternion turnd = turn.clone().mult(CreateFromAxisAngle(1,0,0,PI/(2f)*(i/quarter_divisions)));

                Vector3f look = turnd.clone().mult(Vector3f.UNIT_Z.clone());
                Vector3f otherWay = look.clone();
                otherWay.x *= -1;
                Vector3f otherWayDown = look.clone();
                otherWayDown.y *= -1;
                Vector3f otherOtherWayDown = otherWay.clone();
                otherOtherWayDown.y *= -1;

                lookDirections.add(otherWayDown);
                lookDirections.add(otherOtherWayDown);
                lookDirections.add(look);
                lookDirections.add(otherWay);

            }
        }
        List<Vector3f> lookBack = new ArrayList<Vector3f>();
        for(Vector3f v : lookDirections) {
            Vector3f vb = v.clone();
            vb.z *=-1;
            lookBack.add(vb);
        }
        lookDirections.addAll(lookBack);

    }

    private static Quaternion CreateFromAxisAngle(double xx, double yy, double zz,  double a){
        // Here we calculate the sin( theta / 2) once for optimization
        float result = (float) Math.sin(a / 2.0);

        // Calculate the x, y and z of the quaternion
        float x = (float) (xx * result);
        float y = (float) (yy * result);
        float z = (float) (zz * result);

        // Calcualte the w value by cos( theta / 2 )
        float w = (float) Math.cos(a / 2.0);

        Quaternion q = new Quaternion();
        q.set(x,y,z,w);
        return q.normalizeLocal();
    }

    private static Coord3 NextChunkCoord(Camera Cam) {
        return null; //TODO:
    }

    public static Coord3 ClosestReadyToBuildChunk(Camera cam, TerrainMap terrainMap, ColumnMap columnMap)
    {
        Coord3 foundCoord = Chunk.ToChunkPosition(Coord3.FromVector3f(cam.getLocation()));
//        foundCoord.y = Math.min(foundCoord.y, terrainMap.getMaxChunkCoordY() - 1);
        foundCoord.y = terrainMap.getMinChunkCoordY();
        int count = 0;
        int lastX = 0;
        while(count++ < 40) {
            Chunk result = terrainMap.lookupOrCreateChunkAtPosition(foundCoord);
//            if (foundCoord.y == 0) {
//                if (lastX == 1) {
//                    foundCoord.x++;
//                    lastX = 0;
//                } else {
//                    lastX = 1;
//                }
//                foundCoord.z += lastX;
//            }


            if (columnMap.IsBuilt(foundCoord.x, foundCoord.z)){
                if (result != null) {
                    if(!result.getHasEverStartedBuilding().get()) {
                        return foundCoord.copy();
                    }
                }
            }

            foundCoord.y += 1;
            if (foundCoord.y ==terrainMap.getMaxChunkCoordY()) {
    //                foundCoord.y = terrainMap.getMaxChunkCoordY() - 1;
                return null;
            }

        }
        return null;
    }

    public static BufferedImage bufferedImageTest() {
        BufferedImage buff;
        buff = new BufferedImage(800,800, BufferedImage.TYPE_INT_ARGB);

        int c = 1;
        float count = (float) lookDirections.size();
        Vector2f center = new Vector2f(400,400);
        for (Vector3f v : lookDirections) {
            Vector2f off = center.add(new Vector2f(v.x, v.y).mult(200*v.z));
            c++;
            for(int i = 0; i < 1; ++i)
                for(int j = 0; j < 1; ++j)
                    buff.setRGB((int) off.x + i + j, (int) off.y + i + j, new Color(55, 128, 20).getRGB());
        }
        return buff;
    }
}
