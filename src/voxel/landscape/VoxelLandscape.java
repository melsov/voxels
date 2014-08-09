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
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import voxel.landscape.chunkbuild.*;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.coordmap.managepages.FurthestCoord3PseudoDelegate;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.debugmesh.DebugChart;
import voxel.landscape.debugmesh.DebugChart.DebugShapeType;
import voxel.landscape.debugmesh.IDebugGet3D;
import voxel.landscape.debugutil.GUIInfo;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.debug.Array2DViewer;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.player.Audio;
import voxel.landscape.player.B;
import voxel.landscape.player.Player;
import voxel.landscape.util.Asserter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


// TODO: Separate world builder and game logic, everything else...
public class VoxelLandscape extends SimpleApplication implements ThreadCompleteListener
{
    public static boolean TESTING_DEBUGGING_ON = false;

	private static boolean UseTextureMap = false;
	private static boolean debugInfoOn = false;
    private static boolean ADD_CHUNKS_DYNAMICALLY = true;
    private static boolean COMPILE_CHUNK_DATA_ASYNC = false;
    public static boolean DO_USE_TEST_GEOMETRY = true;
    private boolean STOP_ADDING_COLUMNS_FOR_TESTING = false;
    private static boolean CULLING_ON = false;
    private static boolean BUILD_INITIAL_CHUNKS = true;
    private static boolean DONT_BUILD_CHUNK_MESHES = true;
    private static boolean SHOW_COLUMN_DEBUG_QUADS = false;

    private static int COLUMN_DATA_BUILDER_THREAD_COUNT = 4;
    public static int ADD_COLUMN_RADIUS = 3;
    private static int COLUMN_CULLING_MIN = (int) ((ADD_COLUMN_RADIUS * 1 + 1)*(ADD_COLUMN_RADIUS * 1 + 1));

	private TerrainMap terrainMap = new TerrainMap();
	private ColumnMap columnMap = new ColumnMap();
	private Player player;

    private Material blockMaterial;
    private Material blockMaterialTexMap;
    private Material waterMaterial;
    private Material debugColumnMat;
	private Node worldNode = new Node("world_node");
	private Node overlayNode = new Node("overlay_node");
    private volatile int genBlockDataThreadCount = 0;
	private BlockingQueue<Coord2> columnsToBeBuilt;

    private static float GameTime = 0f;
    private FurthestCoord3PseudoDelegate furthestDelegate = new FurthestCoord3PseudoDelegate();

	private static Coord2 screenDims;

//	private CameraNode camNode;
    private ExecutorService colDataPool;
    private AtomicBoolean keepGoingBoolean = new AtomicBoolean(true);

    private Geometry debugObject;


    private static void setupTestStateVariables()
    {
        if (TESTING_DEBUGGING_ON) {
            UseTextureMap = false;
            debugInfoOn = false;
            ADD_CHUNKS_DYNAMICALLY = true;
            COMPILE_CHUNK_DATA_ASYNC = true;
            DO_USE_TEST_GEOMETRY = false;
            CULLING_ON = true;
            BUILD_INITIAL_CHUNKS = false;
            DONT_BUILD_CHUNK_MESHES = false;
            SHOW_COLUMN_DEBUG_QUADS = false;
        } else {
            UseTextureMap = true;
            debugInfoOn = false;
            ADD_CHUNKS_DYNAMICALLY = true;
            COMPILE_CHUNK_DATA_ASYNC = true;
            DO_USE_TEST_GEOMETRY = false;
            CULLING_ON = true;
            BUILD_INITIAL_CHUNKS = true;
            DONT_BUILD_CHUNK_MESHES = false;
            SHOW_COLUMN_DEBUG_QUADS = false;
        }
    }

    private Material getBlockMaterialTexMap() {
        if (blockMaterialTexMap == null) {
            if (UseTextureMap) {
                blockMaterialTexMap = makeTexMapMaterial();
            } else {
                blockMaterialTexMap = blockMaterial;
            }
        }
        return blockMaterialTexMap;
    }
    private Material getWaterMaterial() {
        if (waterMaterial == null) {
            if (UseTextureMap) {
                waterMaterial = makeTexMapMaterial(); //FOR NOW
            } else {
                waterMaterial = blockMaterial;
            }
        }
        return waterMaterial;
    }

    public Coord2 getScreenDims() { return screenDims; }
	private void attachMeshToScene(Chunk chunk) {
//		addGeometryToScene(chunk.getGeometryObject());
        chunk.getChunkBrain().attachTerrainMaterial(getBlockMaterialTexMap());
        chunk.getChunkBrain().attachWaterMaterial(getWaterMaterial());
        chunk.getChunkBrain().attachToTerrainNode(worldNode);
	}

    private void detachFromScene(Chunk chunk) {
        Geometry g = chunk.getGeometryObject();
        if (g != null) g.removeFromParent();
    }

	private void addGeometryToScene(Geometry geo) {
		if (geo == null) {
            B.bug("Geom null??");
            return;
        }
//		Material mat;
//		if (UseTextureMap) {
//			mat = makeTexMapMaterial();
//		} else {
//            mat = blockMaterial;
//		}
//    	geo.setMaterial(mat);
//    	worldNode.attachChild(geo);
	}
    private void fakelyPopulateColumnsToBeBuilt() {
        int half_area = (int) (ADD_COLUMN_RADIUS * 1);
        Coord3 minChCo = new Coord3(-half_area, 0, -half_area);
        Coord3 maxChCo = new Coord3(half_area, 0, half_area);

        for (int i = minChCo.x; i < maxChCo.x; ++i) {
            for (int j = minChCo.z; j < maxChCo.z; ++j) {
                columnsToBeBuilt.add(new Coord2(i,j));
            }
        }
    }
	
	private void makeInitialWorld()
	{
		rootNode.attachChild(worldNode);

        if (!BUILD_INITIAL_CHUNKS) return;

        int half_area = (int) (ADD_COLUMN_RADIUS *.35f);
		Coord3 minChCo = new Coord3(-half_area, 0, -half_area);
		Coord3 maxChCo = new Coord3(half_area, 0, half_area);

        long startTime = System.currentTimeMillis();
		for(int i = minChCo.x; i < maxChCo.x; ++i) {
			for(int j = minChCo.z; j < maxChCo.z; ++j) {
				generateColumnData(i,j);
                attachDebugBuiltColumnAtChunkCoord(new Coord3(i,0,j)); //DEBUG
//				buildColumn(i,j);
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
        while(genBlockDataThreadCount < 3) {
            makeOneMoreColumn();
            genBlockDataThreadCount++;
        }
	}
    private void addToColumnPriorityQueue() {
        PriorityBlockingQueue<Coord2> queue = (PriorityBlockingQueue<Coord2>) columnsToBeBuilt;
        if (queue.size() > 10) return;
        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(cam,terrainMap,columnMap);
        if (emptyCol == null) {
            return;
        }
        queue.add(new Coord2(emptyCol.x, emptyCol.z));
        attachDebugBuiltColumnAtChunkCoord(emptyCol); //DEBUG
    }
    private void makeOneMoreColumn() {
        Coord3 camPosC = Coord3.FromVector3f(cam.getLocation()); //  player.getPlayerNode().getWorldTranslation());
        camPosC = Chunk.ToChunkPosition(camPosC);
//        Coord3 emptyCol = columnMap.GetClosestEmptyColumn(camPosC.x, camPosC.z, ADD_COLUMN_RADIUS);
        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(cam,terrainMap,columnMap);
        if (emptyCol == null) {
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
        columnMap.SetBuildingData(x,z);
        attachDebugBuiltColumnAtChunkCoord(new Coord3(x,0,z));
		runAsyncColumnData(x, z);	
	}
    private void initColumnDataThreadExecutorService() {
        colDataPool = Executors.newFixedThreadPool(COLUMN_DATA_BUILDER_THREAD_COUNT);
        for (int i = 0; i < COLUMN_DATA_BUILDER_THREAD_COUNT; ++i) {
            AsyncGenerateColumnDataInfinite infinColDataThread =
                    new AsyncGenerateColumnDataInfinite(terrainMap, columnMap, columnsToBeBuilt, keepGoingBoolean);

            colDataPool.execute(infinColDataThread);
        }
    }
    private void killThreadPools() {
        keepGoingBoolean.set(false);
        colDataPool.shutdownNow();
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
        if (responsiveRunnable instanceof AsyncGenerateColumnData) {
            // sleep?
            sleepForATime(100);
            AsyncGenerateColumnData async = (AsyncGenerateColumnData) responsiveRunnable;
//                columnsToBeBuilt.put(new Coord2(async.getX(), async.getZ()));
//            generatingBlockData = false;
            genBlockDataThreadCount--;
            columnMap.SetBuilt(async.getX(), async.getZ());
        } else if (responsiveRunnable instanceof AsyncGenerateColumnDataInfinite) {

        }
    }
	private void generateColumnData(int x, int z)  {
        columnMap.SetBuildingData(x,z);
		terrainMap.generateNoiseForChunkColumn(x, z);
        columnMap.SetBuilt(x,z);
		ChunkSunLightComputer.ComputeRays(terrainMap, x, z);
		ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z);
	}

    private boolean buildANearbyChunk() {
        Coord3 chcoord = ChunkFinder.ClosestReadyToBuildChunk(cam, terrainMap, columnMap);
        if (chcoord == null) {
            return false;
        }
        buildChunk(chcoord.x, chcoord.y, chcoord.z);
        return true;
    }

	private void buildColumn(int x, int z) {
        int minChunkY = terrainMap.getMinChunkCoordY();
        int maxChunkY = terrainMap.getMaxChunkCoordY();
		for (int k = minChunkY; k < maxChunkY; ++k ) {
			buildChunk(x, k, z);
		}
	}

    private void buildChunk(int x,int y,int z) {
        Chunk ch = terrainMap.GetChunk(x,y,z);
        Asserter.assertTrue(ch != null, "Unacceptable! chunks null when building mesh?");
        buildThisChunk(ch);
    }
    private void buildThisChunk(Chunk ch) {
        ch.setHasEverStartedBuildingToTrue();
        if (!ch.getIsAllAir()) {
            ch.getChunkBrain().SetDirty();
            ch.getChunkBrain().wakeUp();
            attachMeshToScene(ch);
        } else {
            ch.getChunkBrain().setMeshEmpty();
        }
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
                continue;
            }
            detachFromScene(ch);
            ch.getChunkBrain().clearMeshBuffersAndSetGeometryNull();
        }
        terrainMap.removeColumnData(x,z);
        columnMap.Destroy(x, z);
    }

    float artificialDelay = 0f;
	/* ***************************
	 * *** Main update loop ******
	   ****************************/
    @Override
    public void simpleUpdate(float tpf) 
    {
        if (!ADD_CHUNKS_DYNAMICALLY) return;
        GameTime += tpf;

        // TODO: get rid of artifical delay...(spread out the work more)
        if (artificialDelay < .15f) {
            artificialDelay += tpf;
            return;
        }
        artificialDelay = 0;

        if (!COMPILE_CHUNK_DATA_ASYNC) return;

        addToColumnPriorityQueue();
        if(!DONT_BUILD_CHUNK_MESHES && buildANearbyChunk()) {}
        cullAnExcessColumn(tpf);
    }

    //<editor-fold desc="Simple Init and main">
    /*******************************************
     * Do initialization related stuff here ****
     *******************************************/
    @Override
    public void simpleInitApp() 
    {
        BufferUtils.setTrackDirectMemoryEnabled(true);
        if (TESTING_DEBUGGING_ON) {
//            viewPort.addProcessor(new WireProcessor(assetManager));
//    	viewPort.removeProcessor(...); // KEEP FOR REFERENCE: COULD PERHAPS USE THIS TO TOGGLE WIRE FRAMES
        }
        ColumnCamComparator columnCamComparator = new ColumnCamComparator(cam);
        columnsToBeBuilt = new PriorityBlockingQueue<Coord2>(100, columnCamComparator);

        initColumnDataThreadExecutorService();

        setupTestStateVariables();
        Chunk.USE_TEST_GEOMETRY = DO_USE_TEST_GEOMETRY;
        inputManager.setCursorVisible(false);
        initBlockMaterial();
    	Audio audio = new Audio(assetManager, rootNode);

        flyCam.setMoveSpeed(55);
    	flyCam.setEnabled(false); // TESTING_DEBUGGING_ON);

    	rootNode.attachChild(overlayNode);

        player = new Player(terrainMap, cam, audio, this, overlayNode, rootNode);
        rootNode.attachChild(player.getPlayerNode());

        setupSkyColor();
        setupInfoView();
        setupPlayerDebugHat();
        makeInitialWorld();

        setupInputs();
        setupWASDInput();

    	initCrossHairs();

        setDisplayFps(true);
        setDisplayStatView(true);

        setupXPositiveMarker();
        setupZPositiveMarker();

        GUIInfo guiInfo = new GUIInfo(guiNode, assetManager.loadFont("Interface/Fonts/Console.fnt"));
        guiNode.addControl(guiInfo);

//        initDebugObject();
    }
	/*******************************
	 * Program starts here... ******
     *******************************/
    public static void main(String[] args)
    {
        VoxelLandscape app = new VoxelLandscape();
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
    @Override
    public void destroy() {
        super.destroy();
        killThreadPools();
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
        blockMaterial = wireFrameMaterialWithColor(new ColorRGBA(.3f,1f,.5f,.5f)); //  new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        blockMaterial.setBoolean("VertexColor", true);
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
    public Material makeTexMapMaterial() {
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
	private static BufferedImage OnePixelBufferedImage(java.awt.Color color) {
		BufferedImage image = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0 ; x < image.getWidth(); ++x) {
			  for (int y = 0; y < image.getHeight() ; ++y) {
				  image.setRGB(x, y, color.getRGB() );
			  }
		  }
		return image;
	}
    private void setupSkyColor() {
        ViewPort view_n = renderManager.createMainView("View of main camera", cam);
        view_n.setClearColor(true);
        view_n.attachScene(rootNode);
        java.awt.Color awtcolor = new java.awt.Color(0.26666668f, 0.77254903f,1f, 1f);
        float[] colors = awtcolor.getRGBColorComponents(null);
        ColorRGBA jmeColor = new ColorRGBA(colors[0],colors[1],colors[2],1f);
        view_n.setBackgroundColor(jmeColor);
    }
	private static Texture2D TexFromBufferedImage(BufferedImage bim) {
		AWTLoader awtl = new AWTLoader();
		Image im = awtl.load(bim, false);
		return new Texture2D(im);
	}
    private void sleepForATime(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    private void initDebugObject() {
        Mesh mesh = new Sphere(12,12,5);
        Geometry debugG = new Geometry("dbug",mesh);
        debugG.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Magenta));
        rootNode.attachChild(debugG);
        debugObject = debugG;
    }
    private void moveDebugObject(Vector3f worldPosition) {
        if (debugObject != null) debugObject.setLocalTranslation(worldPosition);
    }
    private void setupPlayerDebugHat() {
        float hatZLength = 20f;
        Mesh hat = new Box(new Vector3f(-.25f,-.25f, 0), new Vector3f(.25f, .25f, hatZLength ));
        Geometry g = new Geometry("hat", hat);
        g.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Red));
        player.getHeadNode().attachChild(g);
        g.setLocalTranslation(0,20f,0);
    }
    private void setupZPositiveMarker() {
        float markerLength = 20f;
        Mesh hat = new Box(new Vector3f(-.25f,-.25f, 0), new Vector3f(.25f, .25f, markerLength ));
        Geometry g = new Geometry("zpos_marker", hat);
        g.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Blue));
        rootNode.attachChild(g);
        g.setLocalTranslation(0,100f,0);
    }
    private void setupXPositiveMarker() {
        float markerLength = 20f;
        Mesh hat = new Box(new Vector3f(0,-.25f,-.25f), new Vector3f(markerLength, .25f, .25f));
        Geometry g = new Geometry("xpos_marker", hat);
        g.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Orange));
        rootNode.attachChild(g);
        g.setLocalTranslation(0,100f,0);
    }
    private void setupInfoView() {
        Camera cam2 = cam.clone();
        cam2.setViewPort(.6f, 1f, 0f, .4f);

        CameraNode camNode = new CameraNode("cam_node", cam2);
        player.getPlayerNode().attachChild(camNode);

        Vector3f ploc = player.getPlayerNode().getLocalTranslation();
        camNode.setLocalTranslation(ploc.x, 200, ploc.z);
        camNode.lookAt(ploc.clone(), Vector3f.UNIT_Y.clone());

//        cam2.setRotation(new Quaternion(0.00f, 0.99f, -0.04f, 0.02f));
        ViewPort viewPort2 = renderManager.createMainView("Info_view_port", cam2);
        viewPort2.setClearFlags(true, true, true);
        viewPort2.setBackgroundColor(ColorRGBA.Black);
        viewPort2.attachScene(rootNode);
    }
    private Material getDebugColumnMat() {
        if (debugColumnMat == null) debugColumnMat = wireFrameMaterialWithColor(ColorRGBA.Orange);
        return debugColumnMat;
    }
    public void attachDebugBuiltColumnAtChunkCoord(Coord3 chunkCo) {
        if (!SHOW_COLUMN_DEBUG_QUADS) return;
        chunkCo.y = 0;
        Mesh col = new Box(Vector3f.ZERO.clone(), Vector3f.UNIT_XYZ.clone().mult(new Vector3f((float)Chunk.XLENGTH,1f,(float)Chunk.ZLENGTH)));
        Vector3f pos = Chunk.ToWorldPosition(chunkCo).toVector3();
        Geometry g = new Geometry("col_mesh",col);
        g.setMaterial(getDebugColumnMat());
        rootNode.attachChild(g);
        g.setLocalTranslation(pos);
    }
    //endregion
    //</editor-fold>

}
