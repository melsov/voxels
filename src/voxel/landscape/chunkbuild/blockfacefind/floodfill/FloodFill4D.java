package voxel.landscape.chunkbuild.blockfacefind.floodfill;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSlice;
import voxel.landscape.chunkbuild.blockfacefind.floodfill.chunkslice.ChunkSliceBag;
import voxel.landscape.coord.Box;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by didyouloseyourdog on 10/9/14.
 * The fourth 'D' is time
 * Class to manage flood filling as
 * the camera moves around
 */
public class FloodFill4D
{
    private Camera camera;
    public BlockingQueue<Coord3> floodFilledChunkCoords;

    private ChunkSliceBag inBoundsBag;
    private ChunkSliceBag outOfBoundsBag;
    private AtomicBoolean shouldStop;
    private TerrainMap map;
    public static final int UntouchedType = BlockType.NON_EXISTENT.ordinal();
    private static Coord3 FFBoundsHalf = TerrainMap.MAX_CHUNK_COORD.minus(TerrainMap.MIN_CHUNK_COORD).divideBy(new Coord3(2));

    public FloodFill4D(TerrainMap _map, Camera _camera, BlockingQueue<Coord3> _floodFilledChunkCoords, AtomicBoolean _shouldStop) {
        map = _map;
        camera = _camera; floodFilledChunkCoords = _floodFilledChunkCoords; shouldStop = _shouldStop;
        outOfBoundsBag = ChunkSliceBag.UnboundedChunkSliceBag();
        inBoundsBag = ChunkSliceBag.ChunkSliceBagWithBounds(boundsFromGlobal(camera.getLocation()));
    }
    private Box boundsFromGlobal(Vector3f global) {
        return boundsFromChunkCoord(Chunk.ToChunkPosition(Coord3.FromVector3f(global)));
    }
    private Box boundsFromChunkCoord(Coord3 chunkCo) {
        Coord3 start = chunkCo.minus(FFBoundsHalf);
        return new Box(start, start.add(FFBoundsHalf.multy(2)));
    }

    public void flood(boolean forever) {
        Coord3 initialSeed = Coord3.FromVector3f(camera.getLocation());

//        FloodFill floodFill = new FloodFill(map, camera, )
        ChunkSlice[] seedChunkShell = new ChunkSlice[6];

        // floodFill.flood(initialSeed, seedChunkShell)
        // add each seed chunk shell to inBoundsBag
        while(inBoundsBag.size() > 0) {
            ChunkSlice chunkSlice = inBoundsBag.removeNext();
            if (!boundsFromGlobal(camera.getLocation()).contains(chunkSlice.getChunkCoord())){
                outOfBoundsBag.add(chunkSlice);
                continue;
            }

            while(chunkSlice.size() > 0) {
                // maybe clear the chunkShell[]
                Coord3 seed =  chunkSlice.removeNext();
                Chunk chunk = map.lookupOrCreateChunkAtPosition(Chunk.ToChunkPosition(seed));
                // floodFill.flood(seed, chunkShell, etc.)
                // add each chunk shell side to either innerBag or outerBag
                try {
                    floodFilledChunkCoords.put(seed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
