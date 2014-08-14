package voxel.landscape.map.water;

import voxel.landscape.BlockType;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.map.TerrainMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 8/9/14.
 */
public class WaterFlowComputer
{
    public static final byte MIN_WATER_LEVEL = 0;
    public static final byte MAX_WATER_LEVEL = 3;

    private static ArrayList<Coord3> list = new ArrayList<Coord3>();

    public static void Scatter(TerrainMap map, ArrayList<Coord3> list) {
        WaterLevelMap liquidLevelMap = map.getLiquidLevelMap();
        List<Coord3> downAndHorizontal = new ArrayList<Coord3>(5);
        downAndHorizontal.add(Coord3.yneg.clone());
        for(Coord3 xzco : Direction.DirectionXZCoords) downAndHorizontal.add(xzco.clone());

        for(int i=0; i<list.size(); i++) {

            Coord3 pos = list.get(i);
            if(pos.y<0) continue;
//            B.bugln("Scattering: " + pos.toString());
            byte block = map.lookupBlock(pos);
            int waterLevel = liquidLevelMap.GetWaterLevel(pos) - WaterFlowComputerUtils.GetWaterStep(block);
            if(waterLevel <= MIN_WATER_LEVEL) {
//                B.bug("water level was: " + waterLevel + "continuing \n");
                continue;
            }

            for(Coord3 dir : downAndHorizontal) {
                Coord3 nextPos = pos.add(dir);
                block = map.lookupBlock(nextPos);
                byte levelToAdd = dir.y == -1 ? WaterFlowComputer.MAX_WATER_LEVEL : (byte) waterLevel;
                if( BlockType.AcceptsWater(block) && liquidLevelMap.SetWaterLevelIfPossible(levelToAdd, nextPos) ) {
//                    B.bugln("added water to pos: " + nextPos.toString());
                    list.add( nextPos );
                    if(!BlockType.IsEmpty(block)) {
                        map.setWaterRunOff(nextPos);
                        WaterFlowComputerUtils.SetWaterDirty(map, nextPos);
                    }
                    if (dir.y == -1) break; // don't add laterally if water flowed down
                }
            }
        }
    }

    public static void RecomputeWaterAtPosition(TerrainMap map, Coord3 pos) {
        //TODO: implement
//        LightMap lightmap = map.GetLightmap();
//        int oldLight = lightmap.GetLight(pos);
//        int light = lightAtWorldCoord(map, pos); // map.blockAtWorldCoord(pos).GetLight();
//
//        if(oldLight > light) {
//            RemoveLight(map, pos);
//        }
//        if(light > MIN_LIGHT) {
//            Scatter(map, pos);
//        }
    }

    //TODO: If water is placed, call this
    private static void UpdateWater(TerrainMap map, Coord3 pos) {
        list.clear();
        for(Coord3 dir : Direction.DirectionCoords) {
            list.add( pos.add(dir) );
        }
        Scatter(map, list);
    }

    // TODO: If water is remove, call this
    private static void RemoveWater(TerrainMap map, Coord3 pos) {
        list.clear();
        list.add(pos);
        RemoveWater(map, list);
    }

    private static void RemoveWater(TerrainMap map, ArrayList<Coord3> list) {
        WaterLevelMap liquidLevelMap = map.getLiquidLevelMap();
        for(Coord3 pos : list) {
            liquidLevelMap.SetWaterLevel(MAX_WATER_LEVEL, pos);
        }

        ArrayList<Coord3> reAddWaterCoords = new ArrayList<Coord3>();
        for(int i=0; i<list.size(); i++) {
            Coord3 pos = list.get(i);
            if(pos.y<0) continue;
            if(map.lookupBlock(pos.x, pos.y, pos.z) == BlockType.WATER.ordinal()) {
                reAddWaterCoords.add( pos );
                continue;
            }
            byte light = (byte) (liquidLevelMap.GetWaterLevel(pos) - WaterFlowComputerUtils.WATER_STEP);
            liquidLevelMap.SetWaterLevel(MIN_WATER_LEVEL, pos);
            map.unsetWater(pos);
            if (light <= MIN_WATER_LEVEL) continue;

            for(Coord3 dir : Direction.DirectionCoords) {
                if (dir.y == -1) continue; // don't erase water downwards (only go up and side to side)
                Coord3 nextPos = pos.add(dir);
                byte block = map.lookupBlock(nextPos);
                if(BlockType.AcceptsWater(block)) {
                    if(liquidLevelMap.GetWaterLevel(nextPos) <= light) {
                        list.add( nextPos );
                    } else {
                        reAddWaterCoords.add( nextPos );
                    }
                    if(!BlockType.IsEmpty(block)) WaterFlowComputerUtils.SetWaterDirty(map, nextPos);
                }
            }
        }

        Scatter(map, reAddWaterCoords);
    }

}
