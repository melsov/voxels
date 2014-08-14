package voxel.landscape.player;

import com.jme3.export.Savable;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.CameraControl;
import com.jme3.scene.shape.Box;
import voxel.landscape.BlockType;
import voxel.landscape.VoxelLandscape;
import voxel.landscape.coord.*;
import voxel.landscape.map.TerrainMap;

import java.util.ArrayList;
import java.util.List;

public class Player 
{
    private TerrainMap terrainMap;
    private Audio audio;
    private VoxelLandscape app;
    private PlayerControl playerControl = new PlayerControl();
    private Geometry blockCursor;
    private Node playerNode;
    private Node headNode;

    private static float height = 1.0f;
    private static float halfWidthXZ = .40f;
    private static Vector3f playerBodyOffset = new Vector3f(halfWidthXZ,-height*.85f,halfWidthXZ);

    private static int AUTO_MOVE = 0;
    private static int FLY_MODE = 1;
    private static final float NORMAL_MOVE_SPEED = 6f;
    private static final float FLY_MODE_MOVE_SPEED = 15f;
	private static float MOVE_SPEED = NORMAL_MOVE_SPEED;
    private Vector3f inputVelocity = Vector3f.ZERO.clone();
    private float jumpVelocity = 0f;
    private static final float JUMP_VELOCITY= 11f;
    private static final float REAL_GRAVITY = 10f;
    private static float gravity = REAL_GRAVITY;
    private boolean grounded = false;
    private boolean jumping = false;
    private boolean headBump = false;
    private byte blockInHandType = (byte) BlockType.SAND.ordinal();

    private boolean debugPlaceCoord = false;

//TODO: add fly cam mode...
    private ActionListener userInputListener = new ActionListener() {
    	public void onAction(String name, boolean keyPressed, float tpf) {
    		if (name.equals("Break") && !keyPressed) {
    			handleBreakBlock();
    		}
    		else if (name.equals("Place") && !keyPressed) {
    			handlePlaceBlock();
    		} else if (name.equals("GoHome") && !keyPressed) {
                teleportHome();
            }
            else if (name.equals("Up") && !keyPressed) {
                debugPlaceCoord = !debugPlaceCoord;
            }
            else if (name.equals("Down") && !keyPressed) {
                toggleBlockInHandType();
            }
            else if (name.equals("Right") && !keyPressed) {
            }
            else if (name.equals("Left") && !keyPressed) {
                toggleFlyMode();
            }
            else if (name.equals("Inventory") && !keyPressed) {
            }

    	}
    };
    private void teleportHome() {
        Coord3 home = new Coord3(1,0,1);
        playerNode.setLocalTranslation(new Vector3f(home.x,terrainMap.GetMaxY(home.x,home.z) + 4,home.z));
    }
    private void toggleFlyMode() {
        FLY_MODE = FLY_MODE == 1 ? 0 : 1;
        doSettingsForFlyMode();
    }
    private void doSettingsForFlyMode() {
        if (FLY_MODE==1) {
            gravity = 0f;
            MOVE_SPEED = FLY_MODE_MOVE_SPEED;
        } else {
            gravity = REAL_GRAVITY;
            MOVE_SPEED = NORMAL_MOVE_SPEED;
        }

    }
    private void toggleBlockInHandType() {
        blockInHandType = (byte)(blockInHandType == BlockType.SAND.ordinal() ? BlockType.LANTERN.ordinal() : BlockType.SAND.ordinal());
    }
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float keyPressed, float tpf) {

            if (FLY_MODE != 1 && name.equals("jump") && grounded && !jumping && jumpVelocity < .01  ) {
                jumping = true;
                jumpVelocity = JUMP_VELOCITY;
            }

            Vector3f move = Vector3f.ZERO.clone();
            if (name.equals("moveForward") ) {
                move.z = MOVE_SPEED;
            }
            else if (name.equals("moveBackward") ) {
                move.z = -MOVE_SPEED;
            }
            if (name.equals("moveRight") ) {
                move.x = -MOVE_SPEED;
            }
            else if (name.equals("moveLeft") ) {
                move.x = MOVE_SPEED;
            }
            if (name.equals("moveUp") ) {
                move.y = MOVE_SPEED;
            }
            else if (name.equals("moveDown") ) {
                move.y = -MOVE_SPEED;
            }
            if (name.equals("lmb")) {

            } else if (name.equals("rmb")) {

            }

            /*
            TODO: resolve jump, move direction weirdness (look at Unity character controllers)
             */
            float velY = move.y;
            move.y = 0;
            Quaternion camro = headNode.getLocalRotation().clone(); // cam.getRotation();
            turnInputToCamera(camro.mult(move.clone()));
            inputVelocity.y = velY;
        }
    };
    private void turnInputToCamera(Vector3f camV) {
        inputVelocity.x = camV.x;
        inputVelocity.z = camV.z;
    }
    private void resetInputVelocity() {
        inputVelocity.x = 0f;
        inputVelocity.y = 0f;
        inputVelocity.z = 0f;
    }
    private void rotatePlayerHead() {
        Vector2f mouse;
        mouse = app.getInputManager().getCursorPosition();
        if (mouse == null) return;

        Vector2f halfScreen = app.getScreenDims().multy(.5f).toVector2f();

        mouse = mouse.subtract(halfScreen);
        Quaternion roll = new Quaternion();
        roll.fromAngleAxis( 2f * FastMath.PI * -mouse.x /(halfScreen.x ) , new Vector3f(0,1,0) );
        Quaternion rollY = new Quaternion();
        rollY.fromAngleAxis( FastMath.PI * -mouse.y /(halfScreen.y * 2.0f) , new Vector3f(1,0,0) );
        roll = roll.mult(rollY);

        headNode.setLocalRotation(roll);
    }
    private void movePlayer(float tpf) {
        Vector3f pos = playerNode.getLocalTranslation().clone();
        if (AUTO_MOVE == 1) {
            inputVelocity.x = MOVE_SPEED;
        }
        Vector3f scaledV = inputVelocity.clone();
        if ((grounded && jumpVelocity < .01) || headBump) {
            jumpVelocity = 0f;
        } else {
            scaledV.y += jumpVelocity;
            jumpVelocity -= gravity * tpf;
        }
        scaledV = scaledV.mult(tpf);
        scaledV = checkCollisions(pos, scaledV); //*

        playerNode.move(scaledV);

        if(!jumping) {
            resetInputVelocity();
        }
    }

    private Vector3f checkCollisions(final Vector3f curLoc, final Vector3f proposedMove) {
        Vector3f proposedLoc = curLoc.add(proposedMove).add(playerBodyOffset);

        List<Integer> unimpededDirections = new ArrayList<Integer>(); // for use later when finding ground

        /* check side walls */
        for (int i = 0; i < Direction.DirectionXZVector3fs.length; ++i) {
            Vector3f xzunitdir = Direction.DirectionXZVector3fs[i].clone();
            Vector3f xzdir = xzunitdir.mult(halfWidthXZ);
            Vector3f xzEdgePos = proposedLoc.add(xzdir);
            Coord3 edge = Coord3.FromVector3fAdjustNegative(xzEdgePos);
            boolean found_solid = BlockType.IsSolid(terrainMap.lookupBlock(edge));
            if (!found_solid){ // check head level
                edge.y++;
                found_solid = BlockType.IsSolid(terrainMap.lookupBlock(edge));
            }
            if (!found_solid) {
                unimpededDirections.add(i);
                continue;
            }

            Vector3f corner = edge.toVector3();
            if (xzdir.x < 0 || xzdir.z < 0) {
                corner = corner.add(Direction.UNIT_XZ.clone()); //doesn't matter which dir since the other dir will be zeroed out.
            }
            proposedLoc = proposedLoc.add(corner.subtract(xzEdgePos).mult(VektorUtil.Abs(xzunitdir)));
        }
        /* check grounded */
        Coord3 pco = null;
        boolean gotGround = false;
        if (jumpVelocity < 0.001) {
            pco = Coord3.FromVector3fAdjustNegative(proposedLoc.subtract(0, .1f, 0));
            if (BlockType.IsSolid(terrainMap.lookupBlock(pco))) {
                gotGround = true;
                proposedLoc.y = (pco.y + 1f) + (jumping ? .11f : .1f);
            }

            //only look for ground in unimpeded directions b/c our 'toe' can't be on an edge that
            // we can't occupy (and only if we're not already grounded)
            if (!gotGround) for (Integer i : unimpededDirections) {
                Vector3f xzunitdir = Direction.DirectionXZVector3fs[i];
                Vector3f xzdir = xzunitdir.mult(halfWidthXZ);
                pco = Coord3.FromVector3fAdjustNegative(proposedLoc.subtract(0, .1f, 0).add(xzdir));
                if (BlockType.IsSolid(terrainMap.lookupBlock(pco))) {
                    gotGround = true;
                    proposedLoc.y = (pco.y + 1f) + (jumping ? .11f : .1f);
                    break;
                }
            }
            grounded = gotGround;
            if (!gotGround) jumping = false; // need.
        }

    // check for head bump
        boolean gotHeadBump = false;
        if (jumpVelocity > 0) {
            float headRoom = -playerBodyOffset.y + .7f;
            pco = Coord3.FromVector3fAdjustNegative(proposedLoc.add(0, headRoom, 0));
            if (BlockType.IsSolid(terrainMap.lookupBlock(pco))){
                proposedLoc.y = pco.y - headRoom - .1f;
                jumpVelocity = 0;
                gotHeadBump = true;
            }
            if (!gotHeadBump) for (Integer i : unimpededDirections) {
                Vector3f xzunitdir = Direction.DirectionXZVector3fs[i];
                Vector3f xzdir = xzunitdir.mult(halfWidthXZ);
                pco = Coord3.FromVector3fAdjustNegative(proposedLoc.add(0, headRoom, 0).add(xzdir));
                if (BlockType.IsSolid(terrainMap.lookupBlock(pco))){
                    proposedLoc.y = pco.y - headRoom - .1f;
                    jumpVelocity = 0;
                    gotHeadBump = true;
                    break;
                }
            }
        }
        headBump = gotHeadBump;
        return proposedLoc.subtract(curLoc).subtract(playerBodyOffset);
    }

    public Player(TerrainMap _terrainMap, Camera _camera, Audio _audio, VoxelLandscape _app, Node _overlayNode, Node _terrainNode)
    {
        doSettingsForFlyMode();
    	terrainMap = _terrainMap;
    	audio = _audio;

    	app = _app;

    	initBlockCursor();
        _overlayNode.attachChild(blockCursor);
    	initPlayerGeom(_camera, _terrainNode);

        if (_camera != null)
            adjustCamera(_camera);
    }

    public Node getPlayerNode() { return playerNode; }
    public Node getCamNode() { return playerControl.getNode(); }
    public Node getHeadNode() { return headNode; }
    public ActionListener getUserInputListener() { return userInputListener; }
    public AnalogListener getAnalogListener() { return analogListener; }

    private void moveBlockCursor() {
        Vector3f pos = stepThroughBlocksUntilHitSolid(false);
    	if (pos == null) pos = Vector3f.NEGATIVE_INFINITY.clone();
    	pos = VektorUtil.Floor(pos);
        blockCursor.setLocalTranslation(pos);
    }
    private void handleBreakBlock() 
    {
    	Vector3f vhit = stepThroughBlocksUntilHitSolid(false);
    	if (vhit == null) return;
    	Coord3 hitV =Coord3.FromVector3f( vhit  ); 
    	if (hitV == null) return;
        audio.playBreakCompleteSound();
        terrainMap.SetBlockAndRecompute((byte) BlockType.AIR.ordinal(), hitV);
    }
    private void handlePlaceBlock()
    {
    	Vector3f vhit = stepThroughBlocksUntilHitSolid(true);
    	if (vhit == null) return;
    	Coord3 placeCo = Coord3.FromVector3f( vhit);
    	if (placeCo == null) return;
    	audio.playBreakCompleteSound();
    	terrainMap.SetBlockAndRecompute(blockInHandType, placeCo);
    }
    private Vector3f stepThroughBlocksUntilHitSolid(boolean wantPlaceBlock) {
        return stepThroughBlocksUntilHitSolid(playerControl.getLocation(), playerControl.getDirection(), wantPlaceBlock);
    }
	/*
	 * updating JMonkey geometry bounds and checking collisions are computationally expensive...
	 * So, instead, rely completely on block look-ups for placing/breaking/colliding
	 */
    private Vector3f stepThroughBlocksUntilHitSolid(Vector3f start, Vector3f direction, boolean wantPlaceBlock) {
    	byte block;
    	start = start.add(new Vector3f(.5f,.5f,.5f)); //BECAUSE VOXELS ARE ACTUALLY CENTERED AROUND WHOLE NUMBER COORDS...
    	Coord3 hit;
    	Vector3f hitV;
        hitV = start.clone();
        MutableInteger hitFaceDirectionIn = wantPlaceBlock ? new MutableInteger() : null;
        for(int count = 0; count < 40; ++count) {
            hitV = VektorUtil.EscapePositionOnUnitGrid(hitV, direction, hitFaceDirectionIn);
            hit = Coord3.FromVector3f(hitV);
            block = terrainMap.lookupBlock(hit);
            if (wantPlaceBlock && BlockType.AcceptsPlaceBlock(block)) {
                Coord3 placeRelCoord = Direction.DirectionCoordForDirection(Direction.OppositeDirection(hitFaceDirectionIn.integer));
                return Coord3.FromVector3fAdjustNegative(hitV.add(placeRelCoord.toVector3())).toVector3();
            } else if (BlockType.IsBreakAble(block)) {
                return hitV;
            }
        }
    	return null;
    }
    protected void initBlockCursor() {
      Box box = new Box(.505f, .505f, .505f);
      blockCursor = new Geometry("block_cursor", box);
      Material mark_mat = app.wireFrameMaterialWithColor(ColorRGBA.Black); 
      blockCursor.setMaterial(mark_mat);
    }
    private Geometry makePlayerGeometry() {
        Box box = new Box(Vector3f.ZERO, new Vector3f(halfWidthXZ*2f,height,halfWidthXZ*2f));
        Geometry playerGeom = new Geometry("player_geom", box);
    	playerGeom.setMaterial(app.wireFrameMaterialWithColor(ColorRGBA.Blue));
        return playerGeom;
    }
    private Geometry makeSmallBox() {
        Box box = new Box(Vector3f.ZERO, new Vector3f(.2f,.2f,.2f));
        Geometry playerGeom = new Geometry("small_geom", box);
        playerGeom.setMaterial(app.wireFrameMaterialWithColor(ColorRGBA.Orange));
        return playerGeom;
    }
    private void debugMoveHeadNodeUp() {
        headNode.setLocalTranslation(-3, 5, -3);
    }

    private void initPlayerGeom(Camera _cam, Node _terrainNode)
    {
    	playerNode = new Node("player_node");
//    	playerNode.attachChild(makePlayerGeometry());
//        playerNode.attachChild(makeSmallBox());


        headNode = new Node("head_node");
        playerNode.attachChild(headNode);

        CameraNode camNode;
        if (_cam != null) {
            camNode = new CameraNode("cam_node", _cam);
            headNode.attachChild(camNode);

            //This mode means that camera copies the movements of the target:
            camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
            camNode.lookAt(headNode.getLocalTranslation(), Vector3f.UNIT_Y.clone());
            camNode.addControl(playerControl);
        } else {
            _terrainNode.addControl(playerControl);
        }
    	playerNode.setLocalTranslation(new Vector3f(0,50,0));
    }

    public void adjustCamera(Camera _camera) {
        Coord2 dims = app.getScreenDims();
        float aspect = dims.y/(float)dims.x;
        float near = .12f; ;
        float right = near * .75f ;
        float left = -right ;
        float top = right* aspect;
        float bottom = -top;
        float far = 1000.0f;
        _camera.setFrustum(near, far, left, right, top, bottom);
    }

    public class PlayerControl extends AbstractControl implements Cloneable, Savable
    {
    	private float timeSinceUpdate = 0;
    	private static final float TIME_PER_UPDATE = .15f;

        private Node getNode() { return (Node) getSpatial(); }
        private Vector3f getLocation() { return getSpatial().getWorldTranslation().clone(); }
        private Vector3f getDirection() { return getSpatial().getWorldRotation().clone().mult(Vector3f.UNIT_Z); }
		@Override
		protected void controlUpdate(float tpf) {
			timeSinceUpdate += tpf;
			if (timeSinceUpdate > TIME_PER_UPDATE) {
				timeSinceUpdate = 0;
				moveBlockCursor();
			}
            rotatePlayerHead();
			movePlayer(tpf);
		}
    	
		@Override
		protected void controlRender(RenderManager arg0, ViewPort arg1) {
		}
    }

//
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
//        inputManager.addListener(this.getUserInputListener(), "Break", "Place", "GoHome", "Up", "Down", "Right", "Left", "Inventory");
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
//        inputManager.addListener(this.getAnalogListener(),
//                "moveForward", "moveBackward", "moveRight", "moveLeft", "moveDown", "moveUp", "jump",
//                "lmb", "rmb");
//    }

}
