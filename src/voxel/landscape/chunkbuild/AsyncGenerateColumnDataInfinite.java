package voxel.landscape.chunkbuild;

import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Coord2;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.map.water.ChunkWaterLevelComputer;
import voxel.landscape.noise.TerrainDataProvider;
import voxel.landscape.util.Asserter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

/**
 * Created by didyouloseyourdog on 8/4/14.
 */
public class AsyncGenerateColumnDataInfinite implements Runnable // extends ResponsiveRunnable
{

    private int x,z;
    private TerrainMap terrainMap;
    private ColumnMap columnMap;
    private TerrainDataProvider dataProvider = new TerrainDataProvider();
    BlockingQueue<Coord2> columnsToBeBuilt;
    AtomicBoolean keepGoing;
    public AsyncGenerateColumnDataInfinite(final TerrainMap _terrainMap, final ColumnMap _columnMap, final BlockingQueue<Coord2> _columnsToBeBuilt, AtomicBoolean _keepGoing) {
        columnsToBeBuilt = _columnsToBeBuilt;
        columnMap = _columnMap;
        terrainMap = _terrainMap;
        keepGoing = _keepGoing;
    }
    @Override
    public void run() {
        while(keepGoing.get()) {
            try {
                Coord2 colCoord = columnsToBeBuilt.take(); //thread will block while nothing is available...maybe forever...
                Asserter.assertTrue(colCoord != null, "async got null col coord?? (not supposed to happen)");
                x = colCoord.x; z = colCoord.y;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            columnMap.SetBuildingData(x,z);
            terrainMap.generateNoiseForChunkColumn(x, z, dataProvider);
            ChunkSunLightComputer.ComputeRays(terrainMap, x, z);
            if (keepGoing.get()) { //PREVENT FREEZE AT END OF RUN??
                ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z);
                ChunkWaterLevelComputer.Scatter(terrainMap, columnMap, x, z);
                columnMap.SetBuilt(x, z);
                try { sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

    public int getX() { return x; }
    public int getZ() { return z; }

}
