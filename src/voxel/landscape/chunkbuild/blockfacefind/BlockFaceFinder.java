package voxel.landscape.chunkbuild.blockfacefind;

import com.jme3.renderer.Camera;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by didyouloseyourdog on 10/2/14.
 */
public class BlockFaceFinder {

    private final FloodFill floodFill;
    public final BlockingQueue<Coord3> floodFilledChunkCoords = new ArrayBlockingQueue<Coord3>(256);

    public BlockFaceFinder(TerrainMap _map, Camera _cam) {
        floodFill = new FloodFill(_map, _cam, floodFilledChunkCoords);
    }
    public void floodFind() {
        floodFill.flood();
    }

}
