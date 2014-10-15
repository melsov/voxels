package voxel.landscape.chunkbuild;

import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.map.water.ChunkWaterLevelComputer;
import voxel.landscape.noise.TerrainDataProvider;
import voxel.landscape.player.B;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

/**
 * Created by didyouloseyourdog on 8/4/14.
 */
public class AsyncGenerateColumnDataChunkWise implements Runnable // extends ResponsiveRunnable
{

    private int x,z;
    private TerrainMap terrainMap;
    private ColumnMap columnMap;
    private TerrainDataProvider dataProvider = new TerrainDataProvider();
    BlockingQueue<Coord3> chunksCoordsThatNeedColumnData;
    BlockingQueue<Coord3> chunksCoordsThatHaveColumnData;
    AtomicBoolean keepGoing;
    public AsyncGenerateColumnDataChunkWise(final TerrainMap _terrainMap,
                                            final ColumnMap _columnMap,
                                            final BlockingQueue<Coord3> _chunksCoordsThatNeedColumnData,
                                            final BlockingQueue<Coord3> _chunkCoordsThatHaveColumnData,
                                            AtomicBoolean _keepGoing) {
        chunksCoordsThatNeedColumnData = _chunksCoordsThatNeedColumnData;
        chunksCoordsThatHaveColumnData = _chunkCoordsThatHaveColumnData;
        columnMap = _columnMap;
        terrainMap = _terrainMap;
        keepGoing = _keepGoing;
    }
    @Override
    public void run() {
        while(keepGoing.get()) {
            Coord3 chunkCoord = null;
            try {
                chunkCoord = chunksCoordsThatNeedColumnData.take(); //thread will block while nothing is available...maybe forever...
                x = chunkCoord.x; z = chunkCoord.z;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (columnMap.SetIsBuildingOrReturnFalseIfStartedAlready(x,z)) {
                terrainMap.generateNoiseForChunkColumn(x, z, dataProvider);
                terrainMap.populateFloodFillSeedsUpdateFaceMapsInChunkColumn(x,z, dataProvider); // ORDER OF THIS LINE AND THE COMPUTERS MATTER! TODO: FIX
                B.bug("got a chunk coord");
                ChunkSunLightComputer.ComputeRays(terrainMap, x, z);

                if (!keepGoing.get()) break; // SEEMS TO PREVENT HANG WHEN QUITTING

                ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z);
                ChunkWaterLevelComputer.Scatter(terrainMap, columnMap, x, z);

                /* flood fill and column sides */

                columnMap.SetBuilt(x, z);
            }
            try {
                chunksCoordsThatHaveColumnData.put(chunkCoord);
                sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getX() { return x; }
    public int getZ() { return z; }

}
