package voxel.landscape.chunkbuild;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import voxel.landscape.Chunk;
import voxel.landscape.MeshSet;
import voxel.landscape.VoxelLandscape;

/*
 * Build (or rebuild) a mesh for the chunk
 * and set and reset our geometry's ('spatial's') mesh to the (re)built mesh.
 */
public class ChunkBrain extends AbstractControl implements Cloneable, Savable, ThreadCompleteListener 
{
	private Chunk chunk;
	private boolean dirty, lightDirty;
	private AsyncBuildMesh asyncBuildMesh = null;
	private boolean shouldApplyMesh = false;
	
	public ChunkBrain(Chunk _chunk) {
		chunk = _chunk;
//		Mesh mesh = new Mesh(); // new Box(Vector3f.ZERO.clone(), new Vector3f(1,1,1).mult(12f));
//        mesh.setDynamic();
//		Geometry geom = new Geometry("chunk_geom", mesh);
//        geom.move(worldPosition.toVector3());
//		geom.addControl(this);
	}
    public void setMeshEmpty() {
        getGeometry().setMesh(makeEmptyMesh());
    }
    public void setMeshPlaceholder() {
        getGeometry().setMesh(makePlaceHolderMesh());
    }
	@Override
	protected void controlUpdate(float timePerFrame) {
		/*
		if (shouldApplyMesh) {
			ChunkBuilder.ApplyMeshSet(asyncBuildMesh.getMeshSet(), getMesh(), asyncBuildMesh.getOnlyLight());
			getGeometry().updateModelBound();
			asyncBuildMesh = null;
			shouldApplyMesh = false;
		}
		*/
		if(dirty) {
			buildMesh(false);
			dirty = lightDirty = false;
		}
		if(lightDirty) {
			buildMeshLight();
			lightDirty = false;
		}
//        if (chunk.getIsAllAir()) {
//            setMeshEmpty();
//        }
	}

	@Override
	protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
		//do nothing here
	}

    private Mesh makeEmptyMesh() {
        Mesh mesh = new Mesh();
        mesh.setDynamic();
        mesh.setMode(Mesh.Mode.Triangles);
        return mesh;
    }
    private Mesh makePlaceHolderMesh() {
        return new Box(Vector3f.ZERO.clone(), new Vector3f(1,1,1).mult(12f));
    }
	private Mesh getMesh() {
		Geometry geom = getGeometry();
        if (geom == null) return null;
		if (geom.getMesh() == null) {
			Mesh mesh = makeEmptyMesh();
			geom.setMesh(mesh);
		}
		return geom.getMesh();
	}
	public Geometry getGeometry() {
        Node node = (Node) getSpatial();
        if (node == null) return null;
        return (Geometry) node.getChild("chunk_geom");
    }
    public Geometry getWaterGeometry() {
        Node node = (Node) getSpatial();
        if (node == null) return null;
        Geometry waterGeom = (Geometry) node.getChild("water_geom");
        if (waterGeom == null) {
            waterGeom = new Geometry("water_geom", makePlaceHolderMesh());
            node.attachChild(waterGeom);
        }
        return waterGeom;
    }
    private Mesh getWaterMesh() {
        Geometry g = getWaterGeometry();
        return g.getMesh();
    }
    public Node getNode() {
        return (Node) getSpatial();
    }
    private void makeNodeIfNull() {
        Node node = (Node) getSpatial();
        if (node == null) {
            node = new Node();
            Geometry g = new Geometry("chunk_geom", makeEmptyMesh());
            g.move(Chunk.ToWorldPosition(chunk.position).toVector3());
            node.attachChild(g);
            node.addControl(this);
        }
    }
    public void wakeUp() { makeNodeIfNull(); }

    public void attachToTerrainNode(Node terrainNode) {
        terrainNode.attachChild(getNode());
    }
    public void attachTerrainMaterial(Material terrainMaterial) {
        getGeometry().setMaterial(terrainMaterial);
    }
    public void attachWaterMaterial(Material waterMaterial) {
        getWaterGeometry().setMaterial(waterMaterial);
    }

    public void clearMeshBuffersAndSetGeometryNull() {
        ChunkBuilder.ClearBuffers(getMesh());
        setSpatial(null);
    }

	private void buildMeshLight() { buildMesh(true); }
	
	private void buildMesh(boolean onlyLight) 
	{
        if (VoxelLandscape.DO_USE_TEST_GEOMETRY) return;
		/*
		if (asyncBuildMesh != null) return;
		asyncBuildMesh = new AsyncBuildMesh(onlyLight);
		asyncBuildMesh.addListener(this);
		Thread t = new Thread(asyncBuildMesh);
		t.start();
		*/
		MeshSet mset = new MeshSet();
        MeshSet waterMSet = new MeshSet();
        ChunkBuilder.buildMesh(chunk, mset, waterMSet, onlyLight);

		ChunkBuilder.ApplyMeshSet(mset, getMesh(), onlyLight);
        ChunkBuilder.ApplyMeshSet(waterMSet, getWaterMesh(), onlyLight);

        getGeometry().setModelBound(new BoundingBox(Vector3f.ZERO.clone(), new Vector3f(Chunk.XLENGTH,Chunk.YLENGTH,Chunk.ZLENGTH)));
		getGeometry().updateModelBound();

        //TODO: JVM may need 'help' flushing unused Geometries...
	}
	public class AsyncBuildMesh extends ResponsiveRunnable
	{
		private boolean onlyLight;
		private MeshSet mset = new MeshSet();
        private MeshSet waterMSet = new MeshSet();
		public AsyncBuildMesh(boolean _onlyLight) {
			onlyLight = _onlyLight;
		}
		public MeshSet getMeshSet() { return mset; }
		public boolean getOnlyLight() { return onlyLight; }
		@Override
		public void doRun() {
			ChunkBuilder.buildMesh(chunk, mset, waterMSet, onlyLight);
		}
	}
	@Override
	public void notifyThreadComplete(ResponsiveRunnable responsizeRunnable) {
		if (responsizeRunnable.getClass() == AsyncBuildMesh.class) {
//			shouldApplyMesh = true;
		}
	}
	

	public void SetLightDirty() {
		lightDirty=true;
	}
	
	public void SetDirty() {
		dirty=true;
	}

}
