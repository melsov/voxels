package voxel.landscape;

import voxel.landscape.map.light.SunLightComputer;
import voxel.landscape.map.water.WaterFlowComputer;

import java.awt.*;
import java.util.*;

public enum BlockType {
	NON_EXISTENT (0, 0d),
	AIR (1, 0d),
    DIRT(2, 4d),
	GRASS(3, 0d),
    SAND(4, 3d),
	STONE(5, 5d),
    CAVESTONE(6, 1d),
    LANTERN(7, 0d),
    BEDROCK(8, 0d),
    WATER(9, .3d),
    WATER_RUNOFF (10, 0d),
    PLACEHOLDER_AIR (11, 0d); // DECOMMISSIONED!
	
	BlockType(int i, double _prevalence) {
		integer = i;
        prevalence = _prevalence;
	}
	private static final Map lookup = new HashMap();
    private static EnumSet<BlockType> existentBlockTypes;
    private static EnumSet<BlockType> generatedSolidTypes;
    private static double TotalPrevalence;

	// Populate the lookup table on loading time
	static {
		for (BlockType bt : EnumSet.allOf(BlockType.class))
			lookup.put(bt.integer, bt);
        TotalPrevalence = totalPrevalence(getExistentBlockTypes());
	}
    public static EnumSet<BlockType> getExistentBlockTypes() {
        if (existentBlockTypes == null) {
            existentBlockTypes = EnumSet.allOf(BlockType.class);
            existentBlockTypes.remove(NON_EXISTENT);
            existentBlockTypes.remove(PLACEHOLDER_AIR);
        }
        return existentBlockTypes;
    }
    public static EnumSet<BlockType> getGeneratedSolidTypes() {
        if (generatedSolidTypes == null) {
            generatedSolidTypes = EnumSet.of(DIRT, GRASS, SAND, STONE, CAVESTONE, BEDROCK, WATER);
        }
        return generatedSolidTypes;
    }
    public static Map<BlockType, Double> prevalenceTableForGeneratedSolidTypes() {
        return prevalenceTable(getGeneratedSolidTypes(), totalPrevalence(getGeneratedSolidTypes()));
    }
    private static double totalPrevalence(Set<BlockType> blockTypeSet) {
        double result = 0d;
        Iterator<BlockType> iterator = blockTypeSet.iterator();
        while(iterator.hasNext()) {
            result += iterator.next().prevalence;
        }
        return result;
    }
    public static Map<BlockType, Double> prevalenceTable(Set<BlockType> blockTypeSet) {
        return prevalenceTable(blockTypeSet, totalPrevalence(blockTypeSet));
    }
    private static Map<BlockType, Double> prevalenceTable(Set<BlockType> blockTypeSet, double totalPrevalence) {
        Iterator<BlockType> iterator = blockTypeSet.iterator();
        Map<BlockType, Double> result = new HashMap<BlockType, Double>(blockTypeSet.size());
        while(iterator.hasNext()) {
            BlockType bt = iterator.next();
            result.put(bt, bt.prevalence / totalPrevalence);
        }
        return result;
    }

	public static BlockType get(int i) {
		return (BlockType) lookup.get(i);
	}

    public static Color terrainDemoColor(BlockType blockType) { return terrainDemoColor(blockType.ordinal()); }

	public static Color terrainDemoColor(int i) {
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
		return i == AIR.ordinal() || i == NON_EXISTENT.ordinal() || i == PLACEHOLDER_AIR.ordinal() || IsWaterType(i);
	}
    public static boolean IsWaterSurface(int i) {
        return !IsWaterType(i);
    }
    public static boolean IsNonExistentOrPlaceHolderAir(int i) { return i == NON_EXISTENT.ordinal() || i == PLACEHOLDER_AIR.ordinal(); }

	public static boolean IsAirOrNonExistent(int i) {
		return i == AIR.ordinal() || i == NON_EXISTENT.ordinal() || i == PLACEHOLDER_AIR.ordinal();
	}
    public static boolean AcceptsWater(int i) {
        return i == AIR.ordinal() || i == PLACEHOLDER_AIR.ordinal() || IsWaterType(i); // || i == NON_EXISTENT.ordinal();
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
    private double prevalence;
	
}