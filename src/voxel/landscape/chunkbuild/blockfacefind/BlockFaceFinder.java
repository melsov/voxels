package voxel.landscape.chunkbuild.blockfacefind;

import com.jme3.renderer.Camera;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class BlockFaceFinder {

    private final FloodFill floodFill;
    public BlockFaceFinder(TerrainMap _map, Camera _cam) {
        floodFill = new FloodFill(_map, _cam);
    }

    public class FloodFill
    {
        private final TerrainMap map;
        private final Camera camera;
        private final List<Coord3> seeds = new ArrayList<Coord3>(30);
        private int steps;
        private static final int MAX_STEPS = 500;

        public FloodFill(TerrainMap _map, Camera _cam) {
            map = _map; camera = _cam;
        }

        public void flood() {
            // TODO: explore around...
            // find exposed faces...
            // count steps for each legit move (to a non-existent block from an air block)
            // when steps == MAX_STEPS add the current coord to seeds
        }

    }

}
