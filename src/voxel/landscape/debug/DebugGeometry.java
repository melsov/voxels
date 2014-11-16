package voxel.landscape.debug;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.MaterialLibrarian;
import voxel.landscape.coord.Coord3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by didyouloseyourdog on 10/18/14.
 */
public class DebugGeometry
{
    public static Node rootNode;
    public static MaterialLibrarian materialLibrarian;
    private static BlockingQueue<Geometry> geometries = new LinkedBlockingQueue<>(1024);
    private static Map<Coord3, Integer> addedCoords = new HashMap<>(1024);
    private final static Geometry blockCursor = new Geometry("cursor", new Sphere(18,16,.2f));

    private DebugGeometry() {
        blockCursor.setMaterial(materialLibrarian.wireFrameMaterialWithColor(ColorRGBA.Magenta));
        try {
            geometries.put(blockCursor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static int TimesAddedToCoord(Coord3 co) {
        Integer times = addedCoords.get(co);
        if (times == null) {
            times = new Integer(0);
        }
        times = new Integer(times.intValue() + 1);
        addedCoords.put(co, times);
        return times.intValue();
    }
    public static void AddDebugChunk(Coord3 position, ColorRGBA color) {
        AddDebugBlock(Chunk.ToWorldPosition(position), color, Chunk.XLENGTH, false);
    }
    public static void AddDebugChunkSolid(Coord3 position, ColorRGBA color) {
        AddDebugBlock(Chunk.ToWorldPosition(position), color, Chunk.XLENGTH, true);
    }
    public static void AddDebugChunkSolidSkinny(Coord3 position, ColorRGBA color) {
        AddDebugBlockSkinny(Chunk.ToWorldPosition(position), color, Chunk.XLENGTH, true);
    }
    public static void AddDebugBlock(Coord3 position, ColorRGBA color) {
        AddDebugBlock(position, color, 1f, false);
    }
    public static void AddDebugBlock(Coord3 position, ColorRGBA color, float size, boolean solid) {
        Box s = new Box(new Vector3f(1,1,1), new Vector3f(14, 5, 14));
        Geometry g = new Geometry();
        g.setMesh(s);
        AddDebugGeometry(g, position, color, solid);
    }
    public static void AddDebugBlockSkinny(Coord3 position, ColorRGBA color, float size, boolean solid) {
        Box s = new Box(new Vector3f(2,2,2), new Vector3f(3, 12, 4));
        Geometry g = new Geometry();
        g.setMesh(s);
        AddDebugGeometry(g, position, color, solid);
    }
    public static void AddDebugSphere(Coord3 position, ColorRGBA color, float radius) {
        Sphere s = new Sphere(24, 8, radius);
        Geometry g = new Geometry();
        g.setMesh(s);
        AddDebugGeometry(g, position, color);
    }
    private static final Vector3f half = new Vector3f(.5f, .5f, .5f);
    public static void AddDebugGeometry(Geometry g, Coord3 position, ColorRGBA color) {
        AddDebugGeometry(g, position, color, false);
    }
    public static void AddDebugGeometrySolidMaterial(Geometry g, Coord3 position, ColorRGBA color) {
        AddDebugGeometry(g, position, color, true);
    }
    public static void AddDebugGeometry(Geometry g, Coord3 position, ColorRGBA color, boolean solidMat) {
        int times = TimesAddedToCoord(position) - 1;
        Vector3f localTrans = position.toVector3().subtract(half).add(Vector3f.UNIT_XYZ.mult(times/100f));
        AddDebugGeometry(g, localTrans, color, solidMat);
    }
    public static void AddDebugGeometry(Geometry g, Vector3f localTrans, ColorRGBA color, boolean solidMat) {
        g.setLocalTranslation(localTrans);
        if (solidMat)
            g.setMaterial(materialLibrarian.solidMaterialWithColor(color));
        else
            g.setMaterial(materialLibrarian.wireFrameMaterialWithColor(color));
        try {
            geometries.put(g);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void Update(float tpf) {
        Geometry g = null;
        g = geometries.poll();
        if (g != null) {
            rootNode.attachChild(g);
        }
    }
    // block pointer cursor
    public static void AddVector3fCursor(Vector3f global) {
        AddDebugGeometry(blockCursor.clone(), global, ColorRGBA.randomColor(), false);
    }
}
