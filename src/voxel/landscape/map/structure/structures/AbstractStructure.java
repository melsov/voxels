package voxel.landscape.map.structure.structures;

import voxel.landscape.BlockType;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;

import java.util.HashMap;

/**
 * Created by didyouloseyourdog on 3/28/15.
 */
public class AbstractStructure {
    public HashMap<Coord3, BlockType> outerBlocks = new HashMap(64);
    public HashMap<Coord3, BlockType> innerBlocks = new HashMap(64);

    public AbstractStructure() {
        generateBlocks();
    }

    private void generateBlocks() {
        // stub
    }

    public boolean viablePlot(Coord3 global, TerrainMap map) {
        return false;
    }

}
