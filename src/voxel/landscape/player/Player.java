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
import voxel.landscape.*;
import voxel.landscape.coord.VektorUtil;
import voxel.landscape.map.TerrainMap;

import java.util.ArrayList;
import java.util.List;

public class Player 
{
    private TerrainMap terrainMap;
    private Camera cam;
    private Node terrainNode;
    private Audio audio;
    private VoxelLandscape app;
    private PlayerControl playerControl = new PlayerControl();
    private Geometry blockCursor;
    //    private Geometry[] triMarkerSpheres = new Geometry[3];
    private Node playerNode;
    private Node headNode;
    private Node overlayNode;
    public static final int BREAK_BLOCK_RADIUS = 50;
    private CameraNode camNode;

    private static float height = 1.9f;
    private static float halfWidthXZ = .35f;
    private static Vector3f playerBodyOffset = new Vector3f(halfWidthXZ,-height*.75f,halfWidthXZ);

	private static float MOVE_SPEED = 8f;
    private Vector3f inputVelocity = Vector3f.ZERO;
    private float jumpVelocity = 0f;
    private static final float JUMP_VELOCITY= 6f;
    private static final float gravity = 3.7f;
    private Vector3f velocity = Vector3f.ZERO;
    private boolean grounded = false;
    private boolean jumping = false;
    private boolean headBump = false;

    private float adjustNear = .001f;
    private float adjustLeftRight = .001f;
    private float adjustTopBottom = .001f;
    private float flip = 1f;

    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;
    private Vector2f startMousePosition = new Vector2f();

    private boolean debugPlaceCoord = false;


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
            }
            else if (name.equals("Right") && !keyPressed) {
            }
            else if (name.equals("Left") && !keyPressed) {
            }
            else if (name.equals("Inventory") && !keyPressed) {
            }

    	}
    };
    private void teleportHome() {
        Coord3 home = new Coord3(1,0,1);
        playerNode.setLocalTranslation(new Vector3f(home.x,terrainMap.GetMaxY(home.x,home.z) + 4,home.z));
    }
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float keyPressed, float tpf) {

            if (name.equals("jump") && grounded && !jumping && jumpVelocity < .01 ) {
                jumping = true;
                jumpVelocity = JUMP_VELOCITY;
            }

            Vector3f move = Vector3f.ZERO;
            leftMouseDown = rightMouseDown = false;
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
                leftMouseDown = true;
            } else if (name.equals("rmb")) {
                rightMouseDown = true;
            }
            /*
            TODO: resolve jump, move direction weirdness (look at Unity character controllers)
             */
            Quaternion camro = cam.getRotation();
            turnInputToCamera(camro.mult(move));
            inputVelocity.y = move.y;
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
            Vector3f xzunitdir = Direction.DirectionXZVector3fs[i];
            Vector3f xzdir = xzunitdir.mult(halfWidthXZ);
            Vector3f xzEdgePos = proposedLoc.add(xzdir);
            Coord3 edge = Coord3.FromVector3f(xzEdgePos);
            boolean found_solid = BlockType.IsSolid(terrainMap.lookupBlock(edge));
            if (!found_solid){ // look at head
                edge.y++;
                found_solid = BlockType.IsSolid(terrainMap.lookupBlock(edge));
            }
            if (!found_solid) {
                unimpededDirections.add(i);
                continue;
            }

            Vector3f corner = edge.toVector3();
            if (xzdir.x < 0 || xzdir.z < 0) {
                corner = corner.add(Direction.UNIT_XZ); //doesn't matter which dir since the other dir will be zeroed out.
            }
            proposedLoc = proposedLoc.add(corner.subtract(xzEdgePos).mult(VektorUtil.Abs(xzunitdir)));
        }

        /* check grounded */
        Coord3 pco = null;
        boolean gotGround = false;
        if (jumpVelocity < 0.001) {
            pco = Coord3.FromVector3f(proposedLoc.subtract(0, .1f, 0));
            if (BlockType.IsSolid(terrainMap.lookupBlock(pco))) {
                gotGround = true;
                proposedLoc.y = (pco.y + 1) + (jumping ? .11f : .1f);
            }

            /* only look for ground in unimpeded directions (and only if we're not already grounded) */
            if (!gotGround) for (Integer i : unimpededDirections) {
                Vector3f xzunitdir = Direction.DirectionXZVector3fs[i];
                Vector3f xzdir = xzunitdir.mult(halfWidthXZ);
                pco = Coord3.FromVector3f(proposedLoc.subtract(0, .1f, 0).add(xzdir));
                if (BlockType.IsSolid(terrainMap.lookupBlock(pco))) {
                    gotGround = true;
                    proposedLoc.y = (pco.y + 1) + (jumping ? .11f : .1f);
                    break;
                }
            }
            grounded = gotGround;
            if (!gotGround) jumping = false; // need.
        }

        /* check for head bump */
        boolean gotHeadBump = false;
        if (jumpVelocity > 0) {
            float headRoom = -playerBodyOffset.y + .7f;
            pco = Coord3.FromVector3f(proposedLoc.add(0,headRoom,0));
            if (BlockType.IsSolid(terrainMap.lookupBlock(pco))){
                proposedLoc.y = pco.y - headRoom - .1f;
                jumpVelocity = 0;
                gotHeadBump = true;
            }
            if (!gotHeadBump) for (Integer i : unimpededDirections) {
                Vector3f xzunitdir = Direction.DirectionXZVector3fs[i];
                Vector3f xzdir = xzunitdir.mult(halfWidthXZ);
                pco = Coord3.FromVector3f(proposedLoc.add(0,headRoom,0).add(xzdir));
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


    public Player(TerrainMap _terrainMap, Camera _camera, Node _worldNode, Audio _audio, VoxelLandscape _app, Node _overlayNode)
    {
    	terrainMap = _terrainMap; cam = _camera; terrainNode = _worldNode;
    	audio = _audio;
    	terrainNode.addControl(playerControl);
    	app = _app;
    	overlayNode = _overlayNode;
    	initBlockCursor();
    	overlayNode.attachChild(blockCursor);
    	initPlayerGeom();
        adjustCamera();
    }

    public Node getPlayerNode() { return playerNode; }
    public ActionListener getUserInputListener() { return userInputListener; }
    public AnalogListener getAnalogListener() { return analogListener; }

    private void moveBlockCursor() {
    	Vector3f pos = stepThroughBlocksUntilHitSolid(cam.getLocation(), cam.getDirection(), false);
    	if (pos == null) pos = Vector3f.NEGATIVE_INFINITY;
    	pos = VektorUtil.Floor(pos);
        blockCursor.setLocalTranslation(pos);
    }
    private void handleBreakBlock() 
    {
    	Vector3f vhit = stepThroughBlocksUntilHitSolid(cam.getLocation(), cam.getDirection(), false);
    	if (vhit == null) return;
    	Coord3 hitV =Coord3.FromVector3f( vhit  ); 
    	if (hitV == null) return;
        audio.playBreakCompleteSound();
        terrainMap.SetBlockAndRecompute((byte) BlockType.AIR.ordinal(), hitV);
    }
    private void handlePlaceBlock()
    {
    	Vector3f vhit = stepThroughBlocksUntilHitSolid(cam.getLocation(), cam.getDirection(), true);
    	if (vhit == null) return;
    	Coord3 placeCo = Coord3.FromVector3f( vhit);
    	if (placeCo == null) return;
    	audio.playBreakCompleteSound();
    	terrainMap.SetBlockAndRecompute((byte) BlockType.GRASS.ordinal(), placeCo);
    }
	/*
	 * updating JMonkey geometry bounds and checking collisions are computationally expensive...
	 * So, instead, rely completely on block look-ups for placing/breaking/colliding
	 */
    private Vector3f stepThroughBlocksUntilHitSolid(Vector3f start, Vector3f direction, boolean wantPlaceBlock) {
    	byte block = (byte) BlockType.NON_EXISTENT.ordinal();
    	start = start.add(new Vector3f(.5f,.5f,.5f));
    	
    	Coord3 hit = null;
    	int scale = 0;
    	Vector3f hitV = null;
    	Vector3f cheatFracDir = direction.mult(.25f);
    	while (BlockType.IsAirOrNonExistent(block)) {
    		hitV = start.add( cheatFracDir.mult(scale));
    		hit = Coord3.FromVector3f( hitV );
    		block = terrainMap.lookupBlock(hit);
    		scale++;
    		if (scale > BREAK_BLOCK_RADIUS) return null;
    	}

    	if (wantPlaceBlock) {
    		Vector3f oppdir = direction.mult(-1f);
            Vector3f corner = DistToCorner(hitV, direction);
            Vector3f relEscape = RelativeEscapeVector(corner, oppdir);
//    		float escapeLength = .dot(oppdir);
    		Vector3f escapeTheBlock = hitV.add(relEscape); // oppdir.mult(escapeLength));
    		Vector3f escapeNudge = VektorUtil.Sign(oppdir).mult(VektorUtil.MaskClosestToWholeNumber(escapeTheBlock).mult(.5f));
    		return escapeTheBlock.add(escapeNudge);
    	}
    	
    	return hitV;
    }
	 /* CONSIDER: implement to replace 'cheat Frac Dir' */
    private static Vector3f[] ComputeSteps(Vector3f pos, Vector3f dir) {
    	Vector3f _pos = pos.clone();
    	
    	Vector3f blockCorner = VektorUtil.OneIfPos(dir).add(_pos);
    	blockCorner = VektorUtil.Floor(blockCorner);
    	return null; 
    }
    private static Vector3f DistToCorner(Vector3f pos, Vector3f dir) {
    	Vector3f corner = EntryCorner(pos, dir);
    	return corner.subtract(pos);
    }
    private static Vector3f RelativeEscapeVector(Vector3f cornerDistance, Vector3f dir) {
        Vector3f lengths = VektorUtil.Abs(cornerDistance.divide(dir));
        float length = lengths.x < lengths.y ? lengths.x : lengths.y;
        length = length < lengths.z ? length : lengths.z;
        return dir.mult(length);
    }
    private static Vector3f EntryCorner(Vector3f pos, Vector3f dir) {
    	Vector3f corner = VektorUtil.OneIfNeg(dir).add(pos);
    	return VektorUtil.Floor(corner);
    }

    protected void initBlockCursor() {
      Box box = new Box(.505f, .505f, .505f);
      blockCursor = new Geometry("block_cursor", box);
      Material mark_mat = app.wireFrameMaterialWithColor(ColorRGBA.Black); 
      blockCursor.setMaterial(mark_mat);
    }
    private void initPlayerGeom()
    {

//        Box box = new Box(Vector3f.ZERO, new Vector3f(halfWidthXZ*2f,height,halfWidthXZ*2f));
//        Geometry playerGeom = new Geometry("player_geom", box);
//    	playerGeom.setMaterial(app.wireFrameMaterialWithColor(ColorRGBA.BlackNoAlpha));
    	playerNode = new Node("player_node");
//    	playerNode.attachChild(playerGeom);

//        Box debugCamBox = new Box(Vector3f.ZERO, Vector3f.UNIT_XYZ.mult(.1f));
//        Geometry debugCamGeom = new Geometry("debugCam", debugCamBox);
//        debugCamGeom.setMaterial(app.wireFrameMaterialWithColor(ColorRGBA.Pink));
//        playerNode.attachChild(debugCamGeom);

//        playerGeom.setLocalTranslation(new Vector3f(-halfWidthXZ, -height, -halfWidthXZ));

        headNode = new Node("head_node");
        playerNode.attachChild(headNode);

        camNode = new CameraNode("cam_node", cam);
        //This mode means that camera copies the movements of the target:
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        headNode.attachChild(camNode);
        headNode.setLocalTranslation(0, 0, 0);
        camNode.lookAt(headNode.getLocalTranslation(), Vector3f.UNIT_Y);

    	terrainNode.attachChild(playerNode);
    	playerNode.setLocalTranslation(new Vector3f(0,50,40));
    }

    public void adjustCamera()
    {

        Coord2 dims = app.getScreenDims();
        float aspect = dims.y/(float)dims.x;
        float near = .12f; ;
        float right = near * .75f ;
        float left = -right ;
        float top = right* aspect;
        float bottom = -top;
        float far = 1000.0f;
//        left = cam.getFrustumLeft() - adjustLeftRight ;
//        right = cam.getFrustumRight() + adjustLeftRight;
//        top = cam.getFrustumTop() + adjustTopBottom;
//        bottom = cam.getFrustumBottom() - adjustTopBottom;
//        near = cam.getFrustumNear() + adjustNear;
//        far = cam.getFrustumFar();
//        B.bug(String.format(" left: %g right: %g \n top: %g bottom: %g \n near: %g far: %g \n flip: %g\n", left, right, top, bottom, near, far, flip));
        cam.setFrustum(near, far, left, right, top, bottom);
    }

    public class PlayerControl extends AbstractControl implements Cloneable, Savable
    {
    	private float timeSinceUpdate = 0;
    	private static final float TIMEPERUPDATE = .25f;
		@Override
		protected void controlUpdate(float tpf) {
			timeSinceUpdate += tpf;
			if (timeSinceUpdate > TIMEPERUPDATE) {
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
}
