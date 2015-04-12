package voxel.landscape.tests;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.AsyncGenerateColumnData;
import voxel.landscape.chunkbuild.ResponsiveRunnable;
import voxel.landscape.chunkbuild.ThreadCompleteListener;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.debugmesh.DebugChart;
import voxel.landscape.debugmesh.DebugChart.DebugShapeType;
import voxel.landscape.debugmesh.IDebugGet3D;
import voxel.landscape.debugutil.GUIInfo;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.player.Audio;
import voxel.landscape.player.B;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;


// TODO: Separate world builder and game logic, everything else...
public class VoxelLandscapeTestChunkMgmt extends SimpleApplication implements ThreadCompleteListener
{
	private static boolean UseTextureMap =  false;
	private boolean debugInfoOn = false;
    private static boolean ADD_CHUNKS_DYNAMICALLY = true;
    private static boolean COMPILE_CHUNK_DATA_ASYNC = false;

	private TerrainMap terrainMap = new TerrainMap(null);
	private ColumnMap columnMap = new ColumnMap();
//	private Player player;

	private Node worldNode = new Node("world_node");
	private Node overlayNode = new Node("overlay_node");
	private boolean generatingBlockData = false;
	private List<Coord2> columnsToBeBuilt = new ArrayList<Coord2>();

	private static Coord2 screenDims;

	private CameraNode camNode;

    public Coord2 getScreenDims() { return screenDims; }
	private void attachMeshToScene(Chunk chunk)
	{
		Spatial geo = chunk.getRootSpatial();
		this.addGeometryToScene(geo);
	}
	public Material getTexMapMaterial() {
		Material mat = new Material(assetManager, "MatDefs/BlockTex2.j3md");

		Texture blockTex = assetManager.loadTexture("Textures/dog_64d_.jpg");
		blockTex.setMagFilter(Texture.MagFilter.Nearest);
		blockTex.setWrap(Texture.WrapMode.Repeat);

    	mat.setTexture("ColorMap", blockTex);
    	return mat;
	}
	private void addGeometryToScene(Spatial geo)
	{
		if (geo == null) return;
		Material mat;
		if (UseTextureMap)
		{
			mat = getTexMapMaterial();
		} else {
			mat = wireFrameMaterialWithColor(ColorRGBA.Pink);
		}

    	geo.setMaterial(mat);
    	worldNode.attachChild(geo);
	}

	private void makeInitialWorld()
	{
		rootNode.attachChild(worldNode);
		Texture2D skyTex = TexFromBufferedImage(OnePixelBufferedImage(new Color(0.019607844f, 0.5176471f,.6f, 1.0f)));
		rootNode.attachChild(SkyFactory.createSky( assetManager, skyTex , true));

		Coord3 minChCo = new Coord3(-1,0,-1);
		Coord3 maxChCo = new Coord3(1, 0, 1);

        long startTime = System.currentTimeMillis();
		for(int i = minChCo.x; i < maxChCo.x; ++i)
		{
			for(int j = minChCo.z; j < maxChCo.z; ++j)
			{
				generateColumnData(i,j);
				buildColumn(i,j);
			}
		}
        long endTime = System.currentTimeMillis();
        long genTime = endTime - startTime;
        float seconds = genTime/1000f;
        B.bugln("\ninitial gen took millis: " + genTime + "\n or seconds: " + seconds);

        /*
		 * debugging
		 */
		if (!debugInfoOn) return;
//		Array2DViewer.getInstance().saveToPNG("_debugPicture.png");
//		showImageOnScreen(Array2DViewer.getInstance().getImage());
		addDebugGeometry();
	}
	private void runAsyncColumnData(int x, int z) {
		AsyncGenerateColumnData asyncColumnData = new AsyncGenerateColumnData(terrainMap, columnMap, x,z);
		asyncColumnData.addListener(this);
		Thread t = new Thread(asyncColumnData);
		t.start();
	}
	@Override
	public void notifyThreadComplete(ResponsiveRunnable responsiveRunnable) {
		if (responsiveRunnable.getClass() == AsyncGenerateColumnData.class) {
            try {
                sleep(4000); //EXPERI: THIS SHOULD SLOW DOWN PRODUCTION
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AsyncGenerateColumnData async = (AsyncGenerateColumnData) responsiveRunnable;
			Coord2 asynccoord = new Coord2(async.getX(), async.getZ());
			columnsToBeBuilt.add(asynccoord);
			generatingBlockData = false;
		}
	}
	private void buildRestOfColumns() {
        if (!COMPILE_CHUNK_DATA_ASYNC) return;
		if (columnsToBeBuilt.size() == 0) return;
		Coord2 nextCol = columnsToBeBuilt.remove(0);
		buildRestOfColumn(nextCol.x, nextCol.y);
	}
	private void buildRestOfColumn(int x, int z) {
		buildColumn(x, z);
	}

	private void makeMoreWorld()
	{
        if (COMPILE_CHUNK_DATA_ASYNC) {
            if (generatingBlockData) return;
            generatingBlockData = true;
        }
		Vector3f camPos = cam.getLocation();
		Coord3 camPosC = Coord3.FromVector3f(camPos);
		camPosC = Chunk.ToChunkPosition(camPosC);
		Coord3 emptyCol = columnMap.GetClosestEmptyColumn(camPosC.x, camPosC.z, 7);
		if (emptyCol == null) return;
        if (COMPILE_CHUNK_DATA_ASYNC) {
            generateColumnDataAsync(emptyCol.x, emptyCol.z);
        } else {
            generateColumnData(emptyCol.x, emptyCol.z);
            buildColumn(emptyCol.x, emptyCol.z);
        }
	}
	private void generateColumnDataAsync(int x, int z)
	{
		columnMap.SetBuilt(x, z);
		runAsyncColumnData(x, z);
	}

	private void generateColumnData(int x, int z)
	{
		columnMap.SetBuilt(x, z);
		terrainMap.generateSurface(x, z);
		ChunkSunLightComputer.ComputeRays(terrainMap, x, z);
		ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z); //TEST WANT
	}
    private void buildColumn(int x, int z)
    {
        int minChunkY = terrainMap.getMinChunkCoordY();
        int maxChunkY = terrainMap.getMaxChunkCoordY();
        for (int k = minChunkY; k < maxChunkY; ++k )
        {
            Chunk ch = terrainMap.lookupOrCreateChunkAtPosition(x, k, z);
            if (ch == null) continue;
            ch.getChunkBrain().SetDirty();
            attachMeshToScene(ch);
        }
    }

	/* Use the main event loop to trigger repeating actions. */
    @Override
    public void simpleUpdate(float tpf)
    {
        if (ADD_CHUNKS_DYNAMICALLY) {
            makeMoreWorld();
            if (COMPILE_CHUNK_DATA_ASYNC) buildRestOfColumns();
        }
    }

    /*
     * Do initialization related stuff here
     */
    @Override
    public void simpleInitApp()
    {
        makeInitialWorld();
    	Audio audio = new Audio(assetManager, rootNode);
        inputManager.setCursorVisible(false);
    	rootNode.attachChild(overlayNode);

    	flyCam.setEnabled(true);
        flyCam.setMoveSpeed(25);

        setDisplayFps(true);
        setDisplayStatView(true);

        GUIInfo guiInfo = new GUIInfo(guiNode, assetManager.loadFont("Interface/Fonts/Console.fnt"));
        guiNode.addControl(guiInfo);
    }

	/*
	 * Program starts here...
	 */
    public static void main(String[] args)
    {
        VoxelLandscapeTestChunkMgmt app = new VoxelLandscapeTestChunkMgmt();
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode[] modes = device.getDisplayModes();

        int i=0; // note: there are usually several, let's pick the first
        AppSettings settings = new AppSettings(true);
        boolean FULL_SCREEN = false;
        float scale_screen = FULL_SCREEN ? 1f : .6f;
        screenDims = new Coord2((int)(modes[i].getWidth() * scale_screen ),(int)(modes[i].getHeight() * scale_screen ));
        settings.setResolution(screenDims.x, screenDims.y);

        settings.setFrequency(modes[i].getRefreshRate());
        settings.setBitsPerPixel(modes[i].getBitDepth());
        if (FULL_SCREEN) settings.setFullscreen(device.isFullScreenSupported());
        app.setSettings(settings);
        app.setShowSettings(false);

        app.start(); // start the game

    }

    public void setUpFrustum() {

    }


    /*
     * Debug helper methods
     */
	private void addDebugGeometry()
	{
        Coord3 min = Coord3.Zero;
        Coord3 max = Coord3.One;

        DebugChart debugChart = new DebugChart(min, max);
//		Geometry terrainHeights = debugChart.makeHeightMapVertsUVs(DebugShapeType.QUAD, 0f, wireFrameMaterialWithColor(ColorRGBA.Red), new IDebugGet2D() {
//			public float getAValue(int x, int z) {
//				return terrainMap.GetMaxY(x, z);
//			}
//		});
//		debugNode.attachChild(terrainHeights);
//		Geometry terrainSunHeight = debugChart.makeHeightMapVertsUVs(DebugShapeType.QUAD, 0f, wireFrameMaterialWithColor(ColorRGBA.Yellow), new IDebugGet2D() {
//			public float getAValue(int x, int z) {
//				return terrainMap.GetSunLightmap().GetSunHeight(x, z);
//			}
//		});
//		debugNode.attachChild(terrainSunHeight);
		Geometry terrainLight = debugChart.makeTerrainInfoVertsUVs3D(DebugShapeType.QUAD, 0f, wireFrameMaterialVertexColor(), new IDebugGet3D() {
			public float getAValue(int x, int y, int z) {
				return terrainMap.GetSunLightmap().GetLight(x, y, z);
			}
		});
		rootNode.attachChild(terrainLight); // make a 'debug node?'
	}
	public Material wireFrameMaterialWithColor(ColorRGBA color) {
		Material wireMaterial = new Material(assetManager, "/Common/MatDefs/Misc/Unshaded.j3md");
    	wireMaterial.setColor("Color", color);
    	wireMaterial.getAdditionalRenderState().setWireframe(true);
    	return wireMaterial;
	}
	private Material wireFrameMaterialVertexColor() {
		Material wireMaterial = new Material(assetManager, "/Common/MatDefs/Misc/Unshaded.j3md");
		wireMaterial.setBoolean("VertexColor", true);
    	wireMaterial.getAdditionalRenderState().setWireframe(true);
    	return wireMaterial;
	}
	private static BufferedImage OnePixelBufferedImage(Color color) {
		BufferedImage image = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0 ; x < image.getWidth(); ++x) {
			  for (int y = 0; y < image.getHeight() ; ++y) {
				  image.setRGB(x, y, color.getRGB() );
			  }
		  }
		return image;
	}
	private static Texture2D TexFromBufferedImage(BufferedImage bim) {
		AWTLoader awtl = new AWTLoader();
		Image im = awtl.load(bim, false);
		return new Texture2D(im);
	}
	private void showImageOnScreen(BufferedImage bim) {
		Texture2D tex = TexFromBufferedImage(bim);
		tex.setMagFilter(Texture.MagFilter.Nearest);
		tex.setWrap(Texture.WrapMode.Repeat);
    	
    	Picture pic = new Picture("Pic from BufferedImage");
    	pic.setTexture(assetManager, tex, false);
    	pic.setWidth(200);
    	pic.setHeight(200);
    	pic.setPosition(0f, 100f);
    	guiNode.attachChild(pic);
	}

}
