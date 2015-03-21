package voxel.landscape;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
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
import com.jme3.util.BufferUtils;
import com.jme3.util.SkyFactory;
import voxel.landscape.chunkbuild.AsyncGenerateColumnData;
import voxel.landscape.chunkbuild.ChunkFinder;
import voxel.landscape.chunkbuild.ResponsiveRunnable;
import voxel.landscape.chunkbuild.ThreadCompleteListener;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.coordmap.managepages.FurthestCoord3PseudoDelegate;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.debugmesh.DebugChart;
import voxel.landscape.debugmesh.DebugChart.DebugShapeType;
import voxel.landscape.debugmesh.IDebugGet3D;
import voxel.landscape.debugutil.GUIInfo;
import voxel.landscape.jmonrenderutil.WireProcessor;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.debug.Array2DViewer;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.player.Audio;
import voxel.landscape.player.B;
import voxel.landscape.player.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;


// TODO: Separate world builder and game logic, everything else...
public class VoxelLandscapePreNearby extends SimpleApplication implements ThreadCompleteListener
{
    public static boolean TESTING_DEBUGGING_ON = false;

	private static boolean UseTextureMap = false;
	private static boolean debugInfoOn = false;
    private static boolean ADD_CHUNKS_DYNAMICALLY = true;
    private static boolean COMPILE_CHUNK_DATA_ASYNC = false;
    public static boolean DO_USE_TEST_GEOMETRY = true;
    private boolean STOP_ADDING_COLUMNS_FOR_TESTING = false;
    private static boolean CULLING_ON = false;

    private static int ADD_COLUMN_RADIUS = 7;
    private static int COLUMN_CULLING_MIN = (ADD_COLUMN_RADIUS * 2 + 2)*(ADD_COLUMN_RADIUS * 2 + 2);

	private TerrainMap terrainMap = new TerrainMap(null);
	private ColumnMap columnMap = new ColumnMap();
	private Player player;

    private Material blockMaterial;
	private Node worldNode = new Node("world_node");
	private Node overlayNode = new Node("overlay_node");
	private volatile boolean generatingBlockData = false;
    private volatile int genBlockDataThreadCount = 0;
	private LinkedBlockingQueue<Coord2> columnsToBeBuilt = new LinkedBlockingQueue<Coord2>();

    private static float GameTime = 0f;
    private FurthestCoord3PseudoDelegate furthestDelegate = new FurthestCoord3PseudoDelegate();

	private static Coord2 screenDims;

	private CameraNode camNode;

    private static void setupTestStateVariables()
    {
        if (TESTING_DEBUGGING_ON) {
            UseTextureMap = false;
            debugInfoOn = false; //;)
            ADD_CHUNKS_DYNAMICALLY = true;
            COMPILE_CHUNK_DATA_ASYNC = true;
            DO_USE_TEST_GEOMETRY = true;
            CULLING_ON = true;
        } else {
            UseTextureMap = true;
            debugInfoOn = false;
            ADD_CHUNKS_DYNAMICALLY = true;
            COMPILE_CHUNK_DATA_ASYNC = true;
            DO_USE_TEST_GEOMETRY = false;
            CULLING_ON = true;
        }
    }

	private void attachMeshToScene(Chunk chunk) {
		addGeometryToScene(chunk.getChunkBrain().getGeometry());
	}

    private void detachFromScene(Chunk chunk) {
        chunk.getRootSpatial().removeFromParent();
    }

	private void addGeometryToScene(Spatial geo) {
		if (geo == null) {
            B.bug("Geom null??");
            return;
        }
//		Material mat;
//		if (UseTextureMap) {
//			mat = getTexMapMaterial();
//		} else {
//            mat = blockMaterial;
//		}
//    	geo.setMaterial(mat);
    	worldNode.attachChild(geo);
	}

	private void makeInitialWorld()
	{
		rootNode.attachChild(worldNode);
		Texture2D skyTex = TexFromBufferedImage(OnePixelBufferedImage(new Color(.3f,.6f,1f,1f)));
		rootNode.attachChild(SkyFactory.createSky( assetManager, skyTex , true));

		Coord3 minChCo = new Coord3(-1, 0, -1);
		Coord3 maxChCo = new Coord3(1, 0, 1);

        long startTime = System.currentTimeMillis();
		for(int i = minChCo.x; i < maxChCo.x; ++i) {
			for(int j = minChCo.z; j < maxChCo.z; ++j) {
				generateColumnData(i,j);
				buildColumn(i,j);
			}
		}
        long endTime = System.currentTimeMillis();
        long genTime = endTime - startTime;
        float seconds = genTime/1000f;
        Coord3 dif = maxChCo.minus(minChCo);
        dif.y = terrainMap.getMaxChunkCoordY() -terrainMap.getMinChunkCoordY();
        int chunkArea = dif.x*dif.y*dif.z;
        int colArea = dif.x*dif.z;
        B.bugln("\ninitial gen took millis: " + genTime + "\n or seconds: " + seconds + "\n " +
                (genTime/(float) chunkArea) + " millis per chunk " + (genTime/(float) colArea) + " millis per column");

        /*
		 * debugging
		 */
		if (!debugInfoOn) return;
		Array2DViewer.getInstance().saveToPNG("_debugPicture.png");
		showImageOnScreen(Array2DViewer.getInstance().getImage());
		addDebugGeometry();
	}

	private void makeMoreWorld()
	{
        if (COMPILE_CHUNK_DATA_ASYNC) {
            if (generatingBlockData) return;
            generatingBlockData = true;
        }
        while(genBlockDataThreadCount < 3) {
            makeOneMoreColumn();
            genBlockDataThreadCount++;
        }
	}
    private void makeOneMoreColumn() {
        Coord3 camPosC = Coord3.FromVector3f(cam.getLocation()); //  player.getPlayerNode().getWorldTranslation());
        camPosC = Chunk.ToChunkPosition(camPosC);
        Coord3 emptyCol = columnMap.GetClosestEmptyColumn(camPosC.x, camPosC.z, ADD_COLUMN_RADIUS);
        if (emptyCol == null) {
            generatingBlockData = false;
            return;
        }
        if (COMPILE_CHUNK_DATA_ASYNC) {
            generateColumnDataAsync(emptyCol.x, emptyCol.z);
        } else {
            generateColumnData(emptyCol.x, emptyCol.z);
            buildColumn(emptyCol.x, emptyCol.z);
        }
    }
	private void generateColumnDataAsync(int x, int z) {
//		columnMap.SetBuilt(x, z);
        columnMap.SetBuildingData(x,z);
		runAsyncColumnData(x, z);
	}
    private void runAsyncColumnData(int x, int z) {
        AsyncGenerateColumnData asyncColumnData = new AsyncGenerateColumnData(terrainMap, columnMap, x,z);
        asyncColumnData.addListener(this);
        Thread t = new Thread(asyncColumnData);
        t.setName("AsyncColumnGenAt"+x+","+z);
        t.start();
    }
    @Override
    public void notifyThreadComplete(ResponsiveRunnable responsiveRunnable) {
        if (responsiveRunnable.getClass() == AsyncGenerateColumnData.class) {
            // sleep?
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AsyncGenerateColumnData async = (AsyncGenerateColumnData) responsiveRunnable;

            try {
                columnsToBeBuilt.put(new Coord2(async.getX(), async.getZ()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            generatingBlockData = false;
            genBlockDataThreadCount--;
        }
    }
	private void generateColumnData(int x, int z)  {
//		columnMap.SetBuilt(x, z);
        columnMap.SetBuildingData(x,z);
		terrainMap.generateSurface(x, z);
		ChunkSunLightComputer.ComputeRays(terrainMap, x, z);
		ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z); //TEST WANT
	}
    private boolean buildANearbyChunk() {
        Coord3 chcoord = ChunkFinder.ClosestReadyToBuildChunk(cam, terrainMap, columnMap);
        if (chcoord == null) return false;
        buildChunk(chcoord.x, chcoord.y, chcoord.z);
        return true;
    }
    private boolean buildRestOfColumns() {
        if (!COMPILE_CHUNK_DATA_ASYNC) return false;
        Coord2 nextCol = columnsToBeBuilt.poll();
        if (nextCol == null) {
            return false;
        }
        buildColumn(nextCol.x, nextCol.y);
        return true;
    }
	private void buildColumn(int x, int z) {
        int minChunkY = terrainMap.getMinChunkCoordY();
        int maxChunkY = terrainMap.getMaxChunkCoordY();
		for (int k = minChunkY; k < maxChunkY; ++k ) {
//            if (k == minChunkY && ((x+z) & 0x1) < 1) //TEST
			buildChunk(x, k, z);
		}
	}
    private void buildChunk(int x,int y,int z) {
        Chunk ch = terrainMap.lookupOrCreateChunkAtPosition(x, y, z);
        if (ch == null) {
            return;
        }
        buildThisChunk(ch);
    }
    private void buildThisChunk(Chunk ch) {
        ch.setHasEverStartedBuildingToTrue();
        if (!ch.getIsAllAir()) ch.getChunkBrain().SetDirty();
        attachMeshToScene(ch);
    }
     /*
    Remove columns
     */
    private void cullAnExcessColumn(float tpf) {
        if (!CULLING_ON) return;
        int culled = 0;
        while (columnMap.columnCount() > COLUMN_CULLING_MIN) {
            Coord3 furthest = furthestDelegate.getFurthest2D(cam, columnMap.getCoordXZSet());
            removeColumn(furthest.x, furthest.z);
            if (culled++ > 10) break;
        }
    }
    private void removeColumn(int x, int z)
    {
        int minChunkY = terrainMap.getMinChunkCoordY();
        int maxChunkY = terrainMap.getMaxChunkCoordY();
        for (int k = minChunkY; k < maxChunkY; ++k )
        {
            Chunk ch = terrainMap.GetChunk(x, k, z);
            if (ch == null) {
                B.bug("chunk null. no clearing/detaching");
                continue;
            }
            ch.getChunkBrain().clearMeshBuffersAndSetGeometryNull();
            detachFromScene(ch);
        }
        terrainMap.removeColumnData(x,z);
        columnMap.Destroy(x, z);
    }

    float artificialDelay = 0f;
	/* Use the main event loop to trigger repeating actions. */
    @Override
    public void simpleUpdate(float tpf)
    {
        if (artificialDelay < .5f) {
            artificialDelay += tpf;
            return;
        }
        artificialDelay = 0;
        GameTime += tpf;
        if (ADD_CHUNKS_DYNAMICALLY) {
            makeMoreWorld();
            if (COMPILE_CHUNK_DATA_ASYNC) {
//                if(buildRestOfColumns()) {
                if(buildANearbyChunk()) {
//                    cullAnExcessColumn(tpf);
                }
            }
        }
    }

    //<editor-fold desc="Simple Init and main">
/*
     * Do initialization related stuff here
     */
    @Override
    public void simpleInitApp()
    {
        BufferUtils.setTrackDirectMemoryEnabled(true);
        if (TESTING_DEBUGGING_ON) {
            viewPort.addProcessor(new WireProcessor(assetManager));
//    	viewPort.removeProcessor(...); // KEEP FOR REFERENCE: COULD PERHAPS USE THIS TO TOGGLE WIRE FRAMES
        }
        setupTestStateVariables();
        Chunk.USE_TEST_GEOMETRY = DO_USE_TEST_GEOMETRY;
        inputManager.setCursorVisible(false);
        initBlockMaterial();
    	Audio audio = new Audio(assetManager, rootNode);

        flyCam.setMoveSpeed(55);
    	flyCam.setEnabled(false); // TESTING_DEBUGGING_ON);

    	rootNode.attachChild(overlayNode);

        player = null; // new Player(terrainMap, cam, audio, this, overlayNode, rootNode);
        rootNode.attachChild(player.getPlayerNode());

        makeInitialWorld();

        setupInputs();
        setupWASDInput();

    	initCrossHairs();

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
        VoxelLandscapePreNearby app = new VoxelLandscapePreNearby();
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
    //</editor-fold>

    //<editor-fold desc="Inputs / other Setup">
/*
     * Mouse inputs
     */
    private void setupInputs() {
        inputManager.addMapping("Break", new KeyTrigger(KeyInput.KEY_T), new MouseButtonTrigger(MouseInput.BUTTON_LEFT) );
        inputManager.addMapping("Place", new KeyTrigger(KeyInput.KEY_G), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT) );
        inputManager.addMapping("GoHome", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("Inventory", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(player.getUserInputListener(), "Break", "Place", "GoHome", "Up", "Down", "Right", "Left", "Inventory");
    }

    private void setupWASDInput() {
        inputManager.addMapping("moveForward", new KeyTrigger(keyInput.KEY_UP), new KeyTrigger(keyInput.KEY_W));
        inputManager.addMapping("moveBackward", new KeyTrigger(keyInput.KEY_DOWN), new KeyTrigger(keyInput.KEY_S));
        inputManager.addMapping("moveRight", new KeyTrigger(keyInput.KEY_RIGHT), new KeyTrigger(keyInput.KEY_D));
        inputManager.addMapping("moveLeft", new KeyTrigger(keyInput.KEY_LEFT), new KeyTrigger(keyInput.KEY_A));
        inputManager.addMapping("moveUp",  new KeyTrigger(keyInput.KEY_Q));
        inputManager.addMapping("moveDown",  new KeyTrigger(keyInput.KEY_Z));
        inputManager.addMapping("jump",  new KeyTrigger(keyInput.KEY_SPACE));
        inputManager.addMapping("lmb", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("rmb", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(player.getAnalogListener(),
                "moveForward", "moveBackward", "moveRight", "moveLeft", "moveDown", "moveUp", "jump",
                "lmb", "rmb");
    }
    private void initBlockMaterial() {
        blockMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blockMaterial.setBoolean("VertexColor", true);
    }
    /** A centred plus sign to help the player aim. */
    protected void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth()/2, settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
        guiNode.attachChild(ch);
    }

    //</editor-fold>

    //<editor-fold desc="Debug and helper">
    //region Debug and helper

/*
     * Debug and helper methods
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
//		rootNode.attachChild(terrainHeights);
//		Geometry terrainSunHeight = debugChart.makeHeightMapVertsUVs(DebugShapeType.QUAD, 0f, wireFrameMaterialWithColor(ColorRGBA.Yellow), new IDebugGet2D() {
//			public float getAValue(int x, int z) {
//				return terrainMap.GetSunLightmap().GetSunHeight(x, z);
//			}
//		});
//		rootNode.attachChild(terrainSunHeight);
		Geometry terrainLight = debugChart.makeTerrainInfoVertsUVs3D(DebugShapeType.QUAD, 0f, wireFrameMaterialVertexColor(), new IDebugGet3D() {
			public float getAValue(int x, int y, int z) {
				return terrainMap.GetSunLightmap().GetLight(x, y, z);
			}
		});
		rootNode.attachChild(terrainLight); // make a 'debug node?'
	}
    private Vector3f fakeCamLocation() {
        float theta = FastMath.PI * (GameTime/2f);
        float radius = (GameTime/.25f);
        return new Vector3f(radius * FastMath.cos(theta), 0, radius * FastMath.sin(theta));
    }
    public Material getTexMapMaterial() {
        Material mat = new Material(assetManager, "MatDefs/BlockTex2.j3md");

        Texture blockTex = assetManager.loadTexture("Textures/dog_64d_.jpg");
        blockTex.setMagFilter(Texture.MagFilter.Nearest);
        blockTex.setWrap(Texture.WrapMode.Repeat);

        mat.setTexture("ColorMap", blockTex);
        return mat;
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
    //endregion
    //</editor-fold>

}
