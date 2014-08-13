package voxel.landscape;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import java.util.Vector;

//import com.jme3.bounding.BoundingVolume.Type.Position;
//import com.jme3.bounding.BoundingVolume.Type;

public class MeshSet 
{
	public Vector<Vector3f> vertices = new Vector<Vector3f>();
	public Vector<Integer> indices = new Vector<Integer>();
	public Vector<Vector2f> uvs = new Vector<Vector2f>();
	public Vector<Vector2f> texMapOffsets = new Vector<Vector2f>();
	public Vector<Float> colors = new Vector<Float>();
	public Vector<Vector3f> normals = new Vector<Vector3f>();
    public boolean isLiquidMaterial;
}
