package voxel.landscape;

// TODO: Separate world builder and game logic, everything else...
public class VoxelLandscapePreFloodFill // extends SimpleApplication
{
//
//    //TEST a different commit & push
//	private static boolean USE_TEXTURE_MAP = false, DEBUG_INFO_ON = false, ADD_CHUNKS_DYNAMICALLY = true, COMPILE_CHUNK_DATA_ASYNC = false,
//            CULLING_ON = false, BUILD_INITIAL_CHUNKS = true, DONT_BUILD_CHUNK_MESHES = true, SHOW_COLUMN_DEBUG_QUADS = false;
//    private static boolean TEST_BLOCK_FACE_MESH_BUILDING = true;
//
//    public static boolean TESTING_DEBUGGING_ON = false, DO_USE_TEST_GEOMETRY = true, SHOULD_BUILD_CHUNK_MESH_ASYNC = true;
//
//    private static final int COLUMN_DATA_BUILDER_THREAD_COUNT = 9;
//    private static final int CHUNK_MESH_BUILD_THREAD_COUNT = 9;
//
//    public static int ADD_COLUMN_RADIUS = 12;
//    private static int COLUMN_CULLING_MIN = (int) ((ADD_COLUMN_RADIUS * 1 + 2)*(ADD_COLUMN_RADIUS * 1 + 2));
//
//	private TerrainMap terrainMap; // = new TerrainMap();
//	private ColumnMap columnMap = new ColumnMap();
//	private Player player;
//
//    private Material debugColumnMat;
//	private Node worldNode = new Node("world_node");
//	private Node overlayNode = new Node("overlay_node");
//	private BlockingQueue<Coord2> columnsToBeBuilt;
//    private BlockingQueue<ChunkMeshBuildingSet> chunksToBeMeshed;
//    private BlockingQueue<ChunkMeshBuildingSet> completedChunkMeshSets;
//    private AtomicBoolean asyncChunkMeshThreadsShouldKeepGoing = new AtomicBoolean(true);
//
//    private static float GameTime = 0f;
//    private FurthestCoord3PseudoDelegate furthestDelegate = new FurthestCoord3PseudoDelegate();
//
//	private static Coord2 screenDims;
//
//    private MaterialLibrarian materialLibrarian;
//    private ExecutorService colDataPool;
//    private ExecutorService chunkMeshBuildPool;
//
//    private AtomicBoolean columnBuildingThreadsShouldKeepGoing = new AtomicBoolean(true);
//
//    private BlockFaceFinder blockFaceFinder;
//
//    private static void setupTestStateVariables()
//    {
//        if (TESTING_DEBUGGING_ON) {
//            USE_TEXTURE_MAP = false;
//            DEBUG_INFO_ON = false;
//            ADD_CHUNKS_DYNAMICALLY = false;
//            COMPILE_CHUNK_DATA_ASYNC = true;
//            DO_USE_TEST_GEOMETRY = false;
//            CULLING_ON = true;
//            BUILD_INITIAL_CHUNKS = true;
//            DONT_BUILD_CHUNK_MESHES = false;
//            SHOW_COLUMN_DEBUG_QUADS = false;
//            TEST_BLOCK_FACE_MESH_BUILDING = false;
//        } else {
//            USE_TEXTURE_MAP = true;
//            DEBUG_INFO_ON = false;
//            ADD_CHUNKS_DYNAMICALLY = true;
//            COMPILE_CHUNK_DATA_ASYNC = true;
//            DO_USE_TEST_GEOMETRY = false;
//            CULLING_ON = true;
//            BUILD_INITIAL_CHUNKS = false;
//            DONT_BUILD_CHUNK_MESHES = false;
//            SHOW_COLUMN_DEBUG_QUADS = false;
//            TEST_BLOCK_FACE_MESH_BUILDING = true;
//        }
//    }
//
//    public Coord2 getScreenDims() { return screenDims; }
//	private void attachMeshToScene(Chunk chunk) {
//        chunk.getChunkBrain().attachTerrainMaterial(materialLibrarian.getBlockMaterial());
//        chunk.getChunkBrain().attachWaterMaterial(materialLibrarian.getBlockMaterialTranslucentAnimated());
//        chunk.getChunkBrain().attachToTerrainNode(worldNode);
//	}
//
//    private void detachFromScene(Chunk chunk) {
//        Node g = chunk.getRootSpatial();
//        if (g != null) g.removeFromParent();
//    }
//
//	private void makeInitialWorld()
//	{
//		rootNode.attachChild(worldNode);
//
//        blockFaceFinder.start();
//        if (!BUILD_INITIAL_CHUNKS) return;
//
//        int half_area = 1; // (int) (ADD_COLUMN_RADIUS *.35f);
//		Coord3 minChCo = new Coord3(-half_area, 0, -half_area);
//		Coord3 maxChCo = new Coord3(half_area, 0, half_area);
//
//        long startTime = System.currentTimeMillis();
//		for(int i = minChCo.x; i < maxChCo.x; ++i) {
//			for(int j = minChCo.z; j < maxChCo.z; ++j) {
//				generateColumnData(i,j);
//                attachDebugBuiltColumnAtChunkCoord(new Coord3(i,0,j));
////				buildColumn(i,j);
//			}
//		}
//        long endTime = System.currentTimeMillis();
//        long genTime = endTime - startTime;
//        float seconds = genTime/1000f;
//        Coord3 dif = maxChCo.minus(minChCo);
//        dif.y = terrainMap.getMaxChunkCoordY() -terrainMap.getMinChunkCoordY();
//        int chunkArea = dif.x*dif.y*dif.z;
//        int colArea = dif.x*dif.z;
//        B.bugln("\ninitial gen took millis: " + genTime + "\n or seconds: " + seconds + "\n " +
//                (genTime/(float) chunkArea) + " millis per chunk " + (genTime/(float) colArea) + " millis per column");
//
//        /*
//		 * debugging
//		 */
//		if (!DEBUG_INFO_ON) return;
//		Array2DViewer.getInstance().saveToPNG("_debugPicture.png");
//		showImageOnScreen(Array2DViewer.getInstance().getImage());
//		AddDebugGeometry();
//	}
//
//    private void addToColumnPriorityQueue() {
//        PriorityBlockingQueue<Coord2> queue = (PriorityBlockingQueue<Coord2>) columnsToBeBuilt;
//        if (queue.size() > 10) return;
//        Coord3 emptyCol = ChunkFinder.ClosestEmptyColumn(cam,terrainMap,columnMap);
//        if (emptyCol == null) {
//            return;
//        }
//        queue.add(new Coord2(emptyCol.x, emptyCol.z));
//        attachDebugBuiltColumnAtChunkCoord(emptyCol); //DEBUG
//    }
//
//    private void initColumnDataThreadExecutorService() {
//        ColumnCamComparator columnCamComparator = new ColumnCamComparator(cam);
//        columnsToBeBuilt = new PriorityBlockingQueue<Coord2>(100, columnCamComparator);
//        colDataPool = Executors.newFixedThreadPool(COLUMN_DATA_BUILDER_THREAD_COUNT);
//        for (int i = 0; i < COLUMN_DATA_BUILDER_THREAD_COUNT; ++i) {
//            AsyncGenerateColumnDataInfinite infinColDataThread =
//                    new AsyncGenerateColumnDataInfinite(terrainMap, columnMap, columnsToBeBuilt, columnBuildingThreadsShouldKeepGoing);
//            colDataPool.execute(infinColDataThread);
//        }
//    }
//    private void initChunkMeshBuildThreadExecutorService() {
//        ChunkCoordCamComparator chunkCoordCamComparator = new ChunkCoordCamComparator(cam);
//        chunksToBeMeshed = new PriorityBlockingQueue<ChunkMeshBuildingSet>(50, chunkCoordCamComparator);
//        completedChunkMeshSets = new LinkedBlockingQueue<ChunkMeshBuildingSet>(50);
//        chunkMeshBuildPool = Executors.newFixedThreadPool(CHUNK_MESH_BUILD_THREAD_COUNT);
//        for (int i = 0; i < CHUNK_MESH_BUILD_THREAD_COUNT; ++i) {
//            AsyncMeshBuilder asyncMeshBuilder = new AsyncMeshBuilder(terrainMap,chunksToBeMeshed,
//                    completedChunkMeshSets, asyncChunkMeshThreadsShouldKeepGoing);
//            chunkMeshBuildPool.execute(asyncMeshBuilder);
//        }
//    }
//
//    private void killThreadPools() {
//        columnBuildingThreadsShouldKeepGoing.set(false);
//        colDataPool.shutdownNow();
//
//        asyncChunkMeshThreadsShouldKeepGoing.set(false);
//        chunkMeshBuildPool.shutdownNow();
//    }
//
//	private void generateColumnData(int x, int z)  {
//        columnMap.SetBuildingData(x,z);
//		terrainMap.generateSurface(x, z);
//        columnMap.SetBuilt(x,z);
//		ChunkSunLightComputer.ComputeRays(terrainMap, x, z);
//		ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z);
//        ChunkWaterLevelComputer.Scatter(terrainMap, columnMap, x, z);
//	}
//
//    private boolean buildANearbyChunk() {
//        Coord3 chcoord = ChunkFinder.ClosestReadyToBuildChunk(cam, terrainMap, columnMap);
//        if (chcoord == null) return false;
//        buildThisChunk(terrainMap.GetChunk(chcoord));
//        return true;
//    }
//
//    public void enqueueChunkMeshSets(ChunkMeshBuildingSet chunkMeshBuildingSet) {
//        chunksToBeMeshed.add(chunkMeshBuildingSet);
//    }
//
//    private void buildThisChunk(Chunk ch) {
//        ch.setHasEverStartedBuildingToTrue();
//        if (!ch.getIsAllAir()) {
//            ch.getChunkBrain().SetDirty();
//            ch.getChunkBrain().wakeUp();
//            attachMeshToScene(ch);
//        } else {
//            ch.getChunkBrain().setMeshEmpty();
//        }
//    }
//
//    /*
//     * Remove columns
//     */
//    private void cullAnExcessColumn(float tpf) {
//        if (!CULLING_ON) return;
//        int culled = 0;
//        while (columnMap.columnCount() > COLUMN_CULLING_MIN) {
//            Coord3 furthest = furthestDelegate.getFurthest2D(cam, columnMap.getCoordXZSet());
//            removeColumn(furthest.x, furthest.z);
//            if (culled++ > 10) break;
//        }
//    }
//    private void removeColumn(int x, int z)
//    {
//        int minChunkY = terrainMap.getMinChunkCoordY();
//        int maxChunkY = terrainMap.getMaxChunkCoordY();
//        for (int k = minChunkY; k < maxChunkY; ++k )
//        {
//            Chunk ch = terrainMap.GetChunk(x, k, z);
//            if (ch == null) {
//                continue;
//            }
//            detachFromScene(ch);
//            ch.getChunkBrain().clearMeshBuffers();
//        }
//        terrainMap.removeColumn(x,z);
//        columnMap.Destroy(x, z);
//    }
//
//	/* ***************************
//	 * *** Main Update loop ******
//	   ****************************/
//    @Override
//    public void simpleUpdate(float tpf) {
//        GameTime += tpf;
//        terrainMap.mapUpdate(tpf);
//        if (!ADD_CHUNKS_DYNAMICALLY) return;
//        if (!COMPILE_CHUNK_DATA_ASYNC) return;
//        addToColumnPriorityQueue();
//        if(!DONT_BUILD_CHUNK_MESHES && buildANearbyChunk()) {}
//        checkAsyncCompletedChunkMeshes();
//        cullAnExcessColumn(tpf);
//    }
//
//    private void checkAsyncCompletedChunkMeshes() {
//        int count = 0;
//        while (count++ < 5) {
//            ChunkMeshBuildingSet chunkMeshBuildingSet = completedChunkMeshSets.poll();
//            if (chunkMeshBuildingSet == null) return;
//            Chunk chunk = terrainMap.GetChunk(chunkMeshBuildingSet.chunkPosition);
//            if (chunk == null) return;
//            chunk.getChunkBrain().applyMeshBuildingSet(chunkMeshBuildingSet);
//        }
//    }
//
//    private void initBlockFaceFinder() {
//        blockFaceFinder = new BlockFaceFinder(terrainMap, cam);
//    }
//
//    //<editor-fold desc="Simple Init and main">
//    /*******************************************
//     * Do initialization related stuff here ****
//     *******************************************/
//    @Override
//    public void simpleInitApp()
//    {
//        terrainMap = new TerrainMap(this);
//        BufferUtils.setTrackDirectMemoryEnabled(true);
//        if (TESTING_DEBUGGING_ON) {
////            viewPort.addProcessor(new WireProcessor(assetManager));
////    	viewPort.removeProcessor(...); // KEEP FOR REFERENCE: COULD PERHAPS USE THIS TO TOGGLE WIRE FRAMES
//        }
//
//        initBlockFaceFinder();
//
//        materialLibrarian = new MaterialLibrarian(assetManager);
//
//        initColumnDataThreadExecutorService();
//        initChunkMeshBuildThreadExecutorService();
//
//        setupTestStateVariables();
//        Chunk.USE_TEST_GEOMETRY = DO_USE_TEST_GEOMETRY;
//        inputManager.setCursorVisible(false);
//
//    	Audio audio = new Audio(assetManager, rootNode);
//
//    	flyCam.setEnabled(false);
//
//    	rootNode.attachChild(overlayNode);
//
//        player = new Player(terrainMap, cam, audio, this, overlayNode, rootNode);
//        rootNode.attachChild(player.getPlayerNode());
//
//        setupSkyTexture();
//        setupInfoView();
//        setupPlayerDebugHat();
//        makeInitialWorld();
//
//        setupInputs();
//        setupWASDInput();
//
//    	initCrossHairs();
//
//        setDisplayFps(true);
//        setDisplayStatView(true);
//
//        setupXPositiveMarker();
//        setupZPositiveMarker();
//
//        GUIInfo guiInfo = new GUIInfo(guiNode, assetManager.loadFont("Interface/Fonts/Console.fnt"));
//        guiNode.addControl(guiInfo);
//
////        initDebugObject();
//    }
//	/*******************************
//	 * Program starts here... ******
//     *******************************/
//    public static void main(String[] args)
//    {
//        VoxelLandscapePreFloodFill app = new VoxelLandscapePreFloodFill();
//        ScreenSettings(app, false); //<--- call new method here
//        app.start(); // start the game
//    }
//    private static void ScreenSettings(VoxelLandscapePreFloodFill app, boolean fullScreen) {
//        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        DisplayMode[] modes = device.getDisplayModes();
//        int SCREEN_MODE=0; // note: there are usually several, let's pick the first
//        AppSettings settings = new AppSettings(true);
//        float scale_screen = fullScreen ? 1f : .6f;
//        screenDims = new Coord2((int)(modes[SCREEN_MODE].getWidth() * scale_screen ),(int)(modes[SCREEN_MODE].getHeight() * scale_screen ));
//        settings.setResolution(screenDims.x, screenDims.y);
//        settings.setFrequency(modes[SCREEN_MODE].getRefreshRate());
//        settings.setBitsPerPixel(modes[SCREEN_MODE].getBitDepth());
//        if (fullScreen) {
//            settings.setFullscreen(device.isFullScreenSupported());
//        }
//        app.setSettings(settings);
//        app.setShowSettings(false);
//    }
//    @Override
//    public void destroy() {
//        super.destroy();
//        killThreadPools();
//    }
//    //</editor-fold>
//
//    //<editor-fold desc="INPUT AND SET-UP METHODS">
//    /*
//     * Mouse inputs
//     */
//    private void setupInputs() {
//        inputManager.addMapping("Break", new KeyTrigger(KeyInput.KEY_T), new MouseButtonTrigger(MouseInput.BUTTON_LEFT) );
//        inputManager.addMapping("Place", new KeyTrigger(KeyInput.KEY_G), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT) );
//        inputManager.addMapping("GoHome", new KeyTrigger(KeyInput.KEY_H));
//        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_I));
//        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_K));
//        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_J));
//        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_L));
//        inputManager.addMapping("Inventory", new KeyTrigger(KeyInput.KEY_E));
//        inputManager.addListener(player.getUserInputListener(), "Break", "Place", "GoHome", "Up", "Down", "Right", "Left", "Inventory");
//    }
//
//    private void setupWASDInput() {
//        inputManager.addMapping("moveForward", new KeyTrigger(keyInput.KEY_UP), new KeyTrigger(keyInput.KEY_W));
//        inputManager.addMapping("moveBackward", new KeyTrigger(keyInput.KEY_DOWN), new KeyTrigger(keyInput.KEY_S));
//        inputManager.addMapping("moveRight", new KeyTrigger(keyInput.KEY_RIGHT), new KeyTrigger(keyInput.KEY_D));
//        inputManager.addMapping("moveLeft", new KeyTrigger(keyInput.KEY_LEFT), new KeyTrigger(keyInput.KEY_A));
//        inputManager.addMapping("moveUp",  new KeyTrigger(keyInput.KEY_Q));
//        inputManager.addMapping("moveDown",  new KeyTrigger(keyInput.KEY_Z));
//        inputManager.addMapping("jump",  new KeyTrigger(keyInput.KEY_SPACE));
//        inputManager.addMapping("lmb", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
//        inputManager.addMapping("rmb", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
//        inputManager.addListener(player.getAnalogListener(),
//                "moveForward", "moveBackward", "moveRight", "moveLeft", "moveDown", "moveUp", "jump",
//                "lmb", "rmb");
//    }
//
//    /** A centred plus sign to help the player aim. */
//    private void initCrossHairs() {
//        setDisplayStatView(false);
//        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
//        BitmapText ch = new BitmapText(guiFont, false);
//        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
//        ch.setText("+"); // crosshairs
//        ch.setLocalTranslation( // center
//                settings.getWidth() / 2 - ch.getLineWidth()/2, settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
//        guiNode.attachChild(ch);
//    }
//
//    //</editor-fold>
//
//    //<editor-fold desc="DEBUG AND HELPER METHODS">
//    //region Debug and helper
//
///*
//     * Debug and helper methods
//     */
//	private void AddDebugGeometry()
//	{
//		Coord3 min = Coord3.Zero;
//		Coord3 max = Coord3.One;
//
//		DebugChart debugChart = new DebugChart(min, max);
////		Geometry terrainHeights = debugChart.makeHeightMapVertsUVs(DebugShapeType.QUAD, 0f, wireFrameMaterialWithColor(ColorRGBA.Red), new IDebugGet2D() {
////			public float getAValue(int x, int z) {
////				return terrainMap.GetMaxY(x, z);
////			}
////		});
////		rootNode.attachChild(terrainHeights);
////		Geometry terrainSunHeight = debugChart.makeHeightMapVertsUVs(DebugShapeType.QUAD, 0f, wireFrameMaterialWithColor(ColorRGBA.Yellow), new IDebugGet2D() {
////			public float getAValue(int x, int z) {
////				return terrainMap.GetSunLightmap().GetSunHeight(x, z);
////			}
////		});
////		rootNode.attachChild(terrainSunHeight);
//		Geometry terrainLight = debugChart.makeTerrainInfoVertsUVs3D(DebugShapeType.QUAD, 0f, wireFrameMaterialVertexColor(), new IDebugGet3D() {
//			public float getAValue(int x, int y, int z) {
//				return terrainMap.GetSunLightmap().GetLight(x, y, z);
//			}
//		});
//		rootNode.attachChild(terrainLight); // make a 'debug node?'
//	}
//    private Vector3f fakeCamLocation() {
//        float theta = FastMath.PI * (GameTime/2f);
//        float radius = (GameTime/.25f);
//        return new Vector3f(radius * FastMath.cos(theta), 0, radius * FastMath.sin(theta));
//    }
//
//    public Material makeTexMapMaterial() {
//        return materialLibrarian.getBlockMaterial();
//    }
//
//	public Material wireFrameMaterialWithColor(ColorRGBA color) {
//        return materialLibrarian.wireFrameMaterialWithColor(color);
//	}
//
//	private Material wireFrameMaterialVertexColor() {
//        return materialLibrarian.getVertexColorMaterial();
//	}
//
//
//    private void setupSkyTexture() {
//        Texture2D skyTex = TexFromBufferedImage(OnePixelBufferedImage(new Color(.3f,.6f,1f,1f)));
//        rootNode.attachChild(SkyFactory.createSky(assetManager, skyTex, true));
//    }
//    private static BufferedImage OnePixelBufferedImage(Color color) {
//        BufferedImage image = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
//        for (int x = 0 ; x < image.getWidth(); ++x) {
//            for (int y = 0; y < image.getHeight() ; ++y) {
//                image.setRGB(x, y, color.getRGB() );
//            }
//        }
//        return image;
//    }
//	private static Texture2D TexFromBufferedImage(BufferedImage bim) {
//		AWTLoader awtl = new AWTLoader();
//		Image im = awtl.load(bim, false);
//		return new Texture2D(im);
//	}
//
//	private void showImageOnScreen(BufferedImage bim) {
//		Texture2D tex = TexFromBufferedImage(bim);
//		tex.setMagFilter(Texture.MagFilter.Nearest);
//		tex.setWrap(Texture.WrapMode.Repeat);
//
//    	Picture pic = new Picture("Pic from BufferedImage");
//    	pic.setTexture(assetManager, tex, false);
//    	pic.setWidth(200);
//    	pic.setHeight(200);
//    	pic.setPosition(0f, 100f);
//    	guiNode.attachChild(pic);
//	}
//    private void setupPlayerDebugHat() {
//        float hatZLength = 20f;
//        Mesh hat = new Box(new Vector3f(-.25f,-.25f, 0), new Vector3f(.25f, .25f, hatZLength ));
//        Geometry g = new Geometry("hat", hat);
//        g.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Red));
//        player.getHeadNode().attachChild(g);
//        g.setLocalTranslation(0,20f,0);
//    }
//    private void setupZPositiveMarker() {
//        float markerLength = 20f;
//        Mesh hat = new Box(new Vector3f(-.25f,-.25f, 0), new Vector3f(.25f, .25f, markerLength ));
//        Geometry g = new Geometry("zpos_marker", hat);
//        g.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Blue));
//        rootNode.attachChild(g);
//        g.setLocalTranslation(0,100f,0);
//    }
//    private void setupXPositiveMarker() {
//        float markerLength = 20f;
//        Mesh hat = new Box(new Vector3f(0,-.25f,-.25f), new Vector3f(markerLength, .25f, .25f));
//        Geometry g = new Geometry("xpos_marker", hat);
//        g.setMaterial(wireFrameMaterialWithColor(ColorRGBA.Orange));
//        rootNode.attachChild(g);
//        g.setLocalTranslation(0,100f,0);
//    }
//    private void setupInfoView() {
//        Camera cam2 = cam.clone();
//        cam2.setViewPort(.6f, 1f, 0f, .4f);
//
//        CameraNode camNode = new CameraNode("cam2_node", cam2);
//        player.getPlayerNode().attachChild(camNode);
//
//        Vector3f ploc = player.getPlayerNode().getLocalTranslation();
//        camNode.setLocalTranslation(ploc.x, 200, ploc.z);
//        camNode.lookAt(ploc.clone(), Vector3f.UNIT_Y.clone());
//
//        ViewPort viewPort2 = renderManager.createMainView("Info_view_port", cam2);
//        viewPort2.setClearFlags(true, true, true);
//        viewPort2.setBackgroundColor(ColorRGBA.Black);
//        viewPort2.attachScene(rootNode);
//    }
//
//    private Material getDebugColumnMat() {
//        if (debugColumnMat == null) debugColumnMat = wireFrameMaterialWithColor(ColorRGBA.Orange);
//        return debugColumnMat;
//    }
//    public void attachDebugBuiltColumnAtChunkCoord(Coord3 chunkCo) {
//        if (!SHOW_COLUMN_DEBUG_QUADS) return;
//        chunkCo.y = 0;
//        Mesh col = new Box(Vector3f.ZERO.clone(), Vector3f.UNIT_XYZ.clone().mult(new Vector3f((float)Chunk.XLENGTH,1f,(float)Chunk.ZLENGTH)));
//        Vector3f pos = Chunk.ToWorldPosition(chunkCo).toVector3();
//        Geometry g = new Geometry("col_mesh",col);
//        g.setMaterial(getDebugColumnMat());
//        rootNode.attachChild(g);
//        g.setLocalTranslation(pos);
//    }
//    //endregion
//    //</editor-fold>
//
//    //<editor-fold desc="UNUSED">
//    private void buildColumn(int x, int z) {
//        int minChunkY = terrainMap.getMinChunkCoordY();
//        int maxChunkY = terrainMap.getMaxChunkCoordY();
//		for (int k = minChunkY; k < maxChunkY; ++k ) {
//			buildThisChunk(terrainMap.GetChunk(x, k, z));
//		}
//	}
//    private void setupSkyColor(Camera camera, Color awtcolor) {
//        ViewPort view_n = renderManager.createMainView("View of main camera", camera);
//        view_n.setClearFlags(true, false, false);
//        view_n.attachScene(rootNode);
////        java.awt.Color awtcolor = ;
//        float[] colors = awtcolor.getRGBColorComponents(null);
//        ColorRGBA jmeColor = new ColorRGBA(colors[0],colors[1],colors[2],1f);
//        view_n.setBackgroundColor(jmeColor);
//    }

    //</editor-fold>




}
