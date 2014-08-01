package voxel.landscape.chunkbuild;

import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import voxel.landscape.Chunk;
import voxel.landscape.MeshSet;
import voxel.landscape.VoxelLandscape;
import voxel.landscape.coord.Coord3;

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
	
	public ChunkBrain(Chunk _chunk, Coord3 worldPosition) {
		chunk = _chunk;
		Mesh mesh = new Box(Vector3f.ZERO.clone(), new Vector3f(1,1,1).mult(12f));
        mesh.setDynamic();
		Geometry geom = new Geometry("chunk_geom", mesh);
//		geom.setLocalTranslation(_chunk.originInBlockCoords().toVector3());

        geom.move(worldPosition.toVector3());
//        Node chunkBrainGeomHolder = new Node();
//        chunkBrainGeomHolder.attachChild(geom);
//        chunkBrainGeomHolder.setLocalTranslation(_chunk.originInBlockCoords().toVector3());
//        chunkBrainGeomHolder.addControl(this);
		geom.addControl(this);
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
        return (Geometry) getSpatial();
//        Node node = (Node) getSpatial();
//        if (node.getChildren().size() == 0) return null;
//        return (Geometry) node.getChild(0);
    }
//    public Node getNode() {
//        return (Node) getSpatial();
//    }

    public void clearMeshBuffers() {
        ChunkBuilder.ClearBuffers(getMesh());
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
		MeshSet mset = ChunkBuilder.buildMesh(chunk, getMesh(), onlyLight);
		ChunkBuilder.ApplyMeshSet(mset, getMesh(), onlyLight);
        /*
        Consider: could use mesh.setBound( bounding box ) to speed up updating model bounds...
         */

        //TODO: JVM may need 'help' flushing unused Geometries...
//        getGeometry().setModelBound(new BoundingBox(...));
//		getGeometry().updateModelBound();
//		getSpatial().updateGeometricState(); //don't rely on JMonkey collisions at all?
	}
	public class AsyncBuildMesh extends ResponsiveRunnable
	{

		private boolean onlyLight;
		private MeshSet mset;
		public AsyncBuildMesh(boolean _onlyLight) {
			onlyLight = _onlyLight;
		}
		public MeshSet getMeshSet() { return mset; }
		public boolean getOnlyLight() { return onlyLight; }
		@Override
		public void doRun() {
			mset = ChunkBuilder.buildMesh(chunk, onlyLight);
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
