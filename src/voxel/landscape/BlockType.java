package voxel.landscape;

import voxel.landscape.map.light.SunLightComputer;
import voxel.landscape.map.water.WaterFlowComputer;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum BlockType {
	NON_EXISTENT (0), 
	AIR (1), DIRT(2), 
	GRASS(3), SAND(4), 
	STONE(5), CAVESTONE(6),
    LANTERN(7),BEDROCK(8), WATER(9), WATER_RUNOFF (10);
	
	BlockType(int i) {
		integer = i;
	}
	private static final Map lookup = new HashMap();
	// Populate the lookup table on loading time
	static {
		for (BlockType bt : EnumSet.allOf(BlockType.class))
			lookup.put(bt.integer, bt);
	}

	public static BlockType get(int i) {
		return (BlockType) lookup.get(i);
	}

	public static Color debugColor(int i) {
		switch(BlockType.get(i)) {
		case  NON_EXISTENT:
			return Color.BLACK;
		case AIR:
			return new Color(.4f,.4f, 1f);
		case DIRT:
			return new Color(.4f, .2f, .3f);
		case GRASS:
			return Color.GREEN;
		case SAND:
			return new Color(.9f, .9f, .6f);
		case STONE:
			return new Color(.4f, .4f, .4f);
		case CAVESTONE:
			return new Color(.3f, .5f, .3f);
        case LANTERN:
            return new Color(.9f, 1f, .3f);
        case WATER:
            return new Color(.3f, 1f, .3f);
        case WATER_RUNOFF:
            return new Color(.3f, 1f, .3f);
		case BEDROCK:
			return new Color(.2f,.2f,.2f);
		default:
			return Color.BLACK;
		}
	}

    public static float LiquidFlowTimeStepSeconds(int i) {
        switch(BlockType.get(i)) {
            case WATER:
            case WATER_RUNOFF:
                return 2f;
            default:
                return Float.MAX_VALUE;
        }
    }

	public boolean equals(int i) {
		return this.ordinal() == i;
	}
	
	public static boolean IsTranslucent(int i) {
		return i == AIR.ordinal() || i == NON_EXISTENT.ordinal() || IsWaterType(i);
	}
    public static boolean IsWaterSurface(int i) {
        return !IsWaterType(i);
    }

	public static boolean IsAirOrNonExistent(int i) {
		return i == AIR.ordinal() || i == NON_EXISTENT.ordinal();
	}
    public static boolean AcceptsWater(int i) {
        return i == AIR.ordinal() || IsWaterType(i); // || i == NON_EXISTENT.ordinal();
    }

	public static boolean IsSolid(int i) {
		return !IsAirOrNonExistent(i) && !IsWaterType(i);
	}

    public static boolean IsBreakAble(int i) {
        return !IsAirOrNonExistent(i) && i != WATER_RUNOFF.ordinal();
    }
    public static boolean AcceptsPlaceBlock(int i) {
        return IsSolid(i);
    }

    public static boolean IsPlaceable(int i) {
        return IsSolid(i) || BlockType.WATER.ordinal() == i;
    }
	
	public static boolean IsEmpty(int i) {
		return NON_EXISTENT.ordinal() == i;
	}
	
	public float getFloat() { return (float) this.ordinal(); }
	
	public static int LightLevelForType(int type) {
        if (type == LANTERN.ordinal()) return SunLightComputer.MAX_LIGHT;
        return 0;
	}
    public static int WaterLevelForType(int type) {
        if (type == WATER.ordinal()) return WaterFlowComputer.MAX_WATER_LEVEL;
        return 0; //NOTE: WATER_RUNOFF should have zero level (by type at least) right? (Can't be placed)
    }

    public static boolean IsWaterType(int i) { return i >= WATER.ordinal(); }

    public static byte NextPlaceableBlockFrom(int block) {
        BlockType[] blockTypes = BlockType.values();
        byte result;
        BlockType blockType;
        do {
            blockType = blockTypes[(++block) % blockTypes.length];
            result = (byte) blockType.ordinal();
        } while (!IsPlaceable(result));
        return result;
    }

	private int integer;
	
}