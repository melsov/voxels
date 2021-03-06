package voxel.landscape.chunkbuild;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import voxel.landscape.BlockType;
import voxel.landscape.MeshSet;
import voxel.landscape.coord.Axis;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.BuildUtils;

import java.util.Arrays;

// GLSL driven grass movement and footprints!!

public class BlockMeshUtil 
{
	public static void AddFaceMeshData(Coord3 pos, MeshSet mset, int blockType, int direction, int triIndexStart, TerrainMap terrainData)
	{
		/*
		 * make four verts
		 * and four indices
		 * four UVS. + normals, light (color floats)
		 * add to mesh Set
		 */
		BlockMeshUtil.FaceVertices(mset, pos, direction);
		BlockMeshUtil.UVsForDirection(mset, direction, blockType);
		BlockMeshUtil.FaceNormalsForDirection(mset,direction);
		BlockMeshUtil.IndicesForDirection(mset, triIndexStart);
	}
	
	public static void AddFaceMeshLightData(Coord3 pos, MeshSet mset, int direction, TerrainMap terrainData) {
		BuildFaceLight(direction, terrainData, pos, mset);  
	}

    private static Vector2f TexCoordOffsetsForBlockType(MeshSet mset, int btype, int dir)
    {
        Vector2f ret = new Vector2f(0f,0f);
        if (BlockType.DIRT.equals(btype)) {
            ret.y = .25f;
        }
        else if (BlockType.GRASS.equals(btype) && dir != Direction.YPOS) {
            if (dir != Direction.YNEG) ret.y = .75f;
            else ret.y = .25f;
        }
        else if (BlockType.SAND.equals(btype)) {
            ret.x = .25f;
            ret.y = .75f;
        }
        else if (BlockType.STONE.equals(btype)) {
            ret.x = .25f;
            ret.y = .25f;
        }
        else if (BlockType.CAVESTONE.equals(btype)) {
            ret.x = .75f;
            ret.y = .25f;
        }
        else if (BlockType.LANTERN.equals(btype)) {
            ret.x = .5f;
            ret.y = .75f;
        }
        else if (BlockType.WATER.equals(btype) || BlockType.WATER_RUNOFF.equals(btype)) {
            ret.x = .75f;
            ret.y = .75f;
        }
        else if (BlockType.BEDROCK.equals(btype)) {
            ret.x = .75f;
        }
        else if (BlockType.PLACEHOLDER_AIR.equals(btype)) {
            ret.x = .5f;
            ret.y = .5f;
        }
        return ret;
    }
	private static void FaceNormalsForDirection(MeshSet mset, int dir) {
		int axis = Direction.AxisForDirection(dir);
		Vector3f normv;
		if (axis == Axis.X) {
			normv = Vector3f.UNIT_X;
		} else if (axis == Axis.Y){
			normv = Vector3f.UNIT_Y;
		} else {
			normv = Vector3f.UNIT_Z;
		}
		if (Direction.IsNegDir(dir)) 
			normv = normv.mult(-1.0f);
		mset.normals.add(normv);
	}
	
	/*
	 * returns an array of vertices that describe one of the 6 faces of a 3D box shaped column
	 * decide which face based on @param dir (direction) 
	 */
	private static void FaceVertices(MeshSet mset, Coord3 position, int dir ) {
		for (int i = 0; i < 4; ++i) {
			mset.vertices.add(faceVertices[dir][i].add(position.toVector3())); 
		}
	}
	
	private static void IndicesForDirection(MeshSet mset, int triIndexStart) {
		for (int i : FaceIndices) {
			mset.indices.add(i + triIndexStart);
		}
	}
	private static void UVsForDirection(MeshSet mset, int dir, int blockType) // int height, int width)
	{
//		if (Direction.AxisForDirection(dir) == Axis.Y) {
//			mset.uvs.addAll(Arrays.asList(new Vector2f(0,0),new Vector2f(width,0),new Vector2f(width,height),new Vector2f(0,height)));
//			return;
//		}
		//HACK FLIP HEIGHT AND WIDTH?? //
//		mset.uvs.addAll(Arrays.asList(new Vector2f(0,0),new Vector2f(0,height),new Vector2f(width,height),new Vector2f(width,0)));
        Vector2f offsetStart = TexCoordOffsetsForBlockType(mset, blockType, dir);
        mset.uvs.addAll(Arrays.asList(
                offsetStart ,
                new Vector2f(offsetStart.x, offsetStart.y +.25f),
                new Vector2f(offsetStart.x + .25f, offsetStart.y + .25f),
				new Vector2f(offsetStart.x + .25f, offsetStart.y)
        ));
//        mset.uvs.addAll(Arrays.asList(new Vector2f(0,0),new Vector2f(0,.25f),new Vector2f(.25f,.25f),new Vector2f(.25f,0)));
	}
	private static void BuildFaceLight(int facedir, TerrainMap map, Coord3 pos, MeshSet mset) {
		for(Vector3f ver : faceVertices[facedir]) {
			float[] color = BuildUtils.GetSmoothVertexLight(map, pos, ver, facedir, mset.isLiquidMaterial && ver.y > 0);
			for (float c : color) {
				mset.colors.add(c);
			}
		}
	}

    public static Vector3f[][] faceVerticesOnTheIntegers = new Vector3f[][] {
            //Xneg
            new Vector3f[] {
                    new Vector3f(0f, 0f, 0f),
                    new Vector3f(0f,  1f, 0f),
                    new Vector3f(0f,  1f,  1f),
                    new Vector3f(0f, 0f,  1f),
            },
            //Xpos
            new Vector3f[] {
                    new Vector3f(1f, 0f,  1f),
                    new Vector3f(1f,  1f,  1f),
                    new Vector3f(1f,  1f, 0f),
                    new Vector3f(1f, 0f, 0f),
            },
            //Yneg
            new Vector3f[] {
                    new Vector3f(0f, 0f, 0f),
                    new Vector3f(0f, 0f,  1f),
                    new Vector3f( 1f, 0f,  1f),
                    new Vector3f( 1f, 0f, 0f),
            },
            //Ypos
            new Vector3f[] {
                    new Vector3f( 1f, 1f, 0f),
                    new Vector3f( 1f, 1f,  1f),
                    new Vector3f(0f, 1f,  1f),
                    new Vector3f(0f, 1f, 0f),
            },
            //Zneg
            new Vector3f[] {
                    new Vector3f( 1f, 0f, 0f),
                    new Vector3f( 1f,  1f, 0f),
                    new Vector3f(0f,  1f, 0f),
                    new Vector3f(0f, 0f, 0f),
            },
            //Zpos
            new Vector3f[] {
                    new Vector3f(0f, 0f, 1f),
                    new Vector3f(0f,  1f, 1f),
                    new Vector3f( 1f,  1f, 1f),
                    new Vector3f( 1f, 0f, 1f),
            },
    };
	public static Vector3f[][] faceVertices = new Vector3f[][] {
		//Xneg
		new Vector3f[] {
			new Vector3f(-0.5f, -0.5f, -0.5f),
			new Vector3f(-0.5f,  0.5f, -0.5f),
			new Vector3f(-0.5f,  0.5f,  0.5f),
			new Vector3f(-0.5f, -0.5f,  0.5f),	
		},
		//Xpos
		new Vector3f[] {
			new Vector3f(0.5f, -0.5f,  0.5f),
			new Vector3f(0.5f,  0.5f,  0.5f),
			new Vector3f(0.5f,  0.5f, -0.5f),
			new Vector3f(0.5f, -0.5f, -0.5f),
		},
		//Yneg
		new Vector3f[] {
			new Vector3f(-0.5f, -0.5f, -0.5f),
			new Vector3f(-0.5f, -0.5f,  0.5f),
			new Vector3f( 0.5f, -0.5f,  0.5f),
			new Vector3f( 0.5f, -0.5f, -0.5f),
		},
		//Ypos
		new Vector3f[] {
			new Vector3f( 0.5f, 0.5f, -0.5f),
			new Vector3f( 0.5f, 0.5f,  0.5f),
			new Vector3f(-0.5f, 0.5f,  0.5f),
			new Vector3f(-0.5f, 0.5f, -0.5f),
		},
		//Zneg
		new Vector3f[] {
			new Vector3f( 0.5f, -0.5f, -0.5f),
			new Vector3f( 0.5f,  0.5f, -0.5f),
			new Vector3f(-0.5f,  0.5f, -0.5f),
			new Vector3f(-0.5f, -0.5f, -0.5f),
		},
		//Zpos
		new Vector3f[] {
			new Vector3f(-0.5f, -0.5f, 0.5f),
			new Vector3f(-0.5f,  0.5f, 0.5f),
			new Vector3f( 0.5f,  0.5f, 0.5f),
			new Vector3f( 0.5f, -0.5f, 0.5f),
		},
	};
	private static final int[] FaceIndices = new int[] {0,3,2, 0,2,1};
	
}
