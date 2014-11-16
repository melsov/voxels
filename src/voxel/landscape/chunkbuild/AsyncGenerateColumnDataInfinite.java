package voxel.landscape.chunkbuild;

import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.map.water.ChunkWaterLevelComputer;
import voxel.landscape.noise.TerrainDataProvider;

import java.util.HashSet;
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

    private HashSet<Coord3> touchedChunkCoords;

    public AsyncGenerateColumnDataInfinite(final TerrainMap _terrainMap, final ColumnMap _columnMap, final BlockingQueue<Coord2> _columnsToBeBuilt, AtomicBoolean _keepGoing) {
        columnsToBeBuilt = _columnsToBeBuilt;
        columnMap = _columnMap;
        terrainMap = _terrainMap;
        keepGoing = _keepGoing;
        touchedChunkCoords = new HashSet<>(terrainMap.getMaxChunkCoordY() - terrainMap.getMaxChunkCoordY());
    }
    @Override
    public void run() {
        while(keepGoing.get()) {
            try {
                Coord2 colCoord = columnsToBeBuilt.take(); //thread will block while nothing is available...maybe forever...
                x = colCoord.x; z = colCoord.y;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (columnMap.SetIsBuildingOrReturnFalseIfStartedAlready(x,z)) {
                touchedChunkCoords.clear();
                terrainMap.generateNoiseForChunkColumn(x, z, dataProvider, touchedChunkCoords);
                terrainMap.populateFloodFillSeedsUpdateFaceMapsInChunkColumn(x, z, dataProvider, touchedChunkCoords); // ORDER OF THIS LINE AND THE SUN/WATER COMPUTER LINES MATTERS! TODO: FIX

//                ChunkSunLightComputer.ComputeRays(terrainMap, x, z); // no need. terrain map does this while generating
                if (!keepGoing.get()) break; //PREVENT FREEZE AT END OF RUN??

                ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z); //WANT
                ChunkWaterLevelComputer.Scatter(terrainMap, columnMap, x, z);
                columnMap.SetBuilt(x, z);

                try { sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }

                terrainMap.updateChunksToBeFlooded(touchedChunkCoords);
            }
        }
    }

    public int getX() { return x; }
    public int getZ() { return z; }

}
