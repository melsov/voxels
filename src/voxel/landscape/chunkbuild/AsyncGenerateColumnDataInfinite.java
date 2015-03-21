package voxel.landscape.chunkbuild;

import voxel.landscape.collection.ColumnMap;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;
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
        Thread.currentThread().setName("Async Gen Column Data Thread");
        while(keepGoing.get()) {
            try {
                Coord2 colCoord = columnsToBeBuilt.take(); //thread will block while nothing is available...maybe forever...
                x = colCoord.x; z = colCoord.y;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (columnMap.SetIsBuildingOrReturnFalseIfStartedAlready(x,z)) {
                touchedChunkCoords.clear();
                terrainMap.generateSurface(x, z, dataProvider, touchedChunkCoords);
                //WE MAY HAVE CRAWLED BACK OVER AN ALREADY PROCESSED COLUMN
                //WE COULD BE ITERATING OVER ITS ENTIRE X,Z SURFACE WHEN (SAY) ONLY
                //ONE X,Z HEIGHT COORD WAS CHANGED. THIS SEEMS WASTEFUL BUT, TO GET AROUND
                //IT, WE'D NEED TO MAKE A PER-CHUNK 'DIRTY' HEIGHT COORD2 LIST: UGH?
                for (Coord2 col2 : columnsFromChunkCoords(touchedChunkCoords)) {
                    terrainMap.populateFloodFillSeedsUpdateFaceMapsInChunkColumn(col2.getX(), col2.getZ(), dataProvider, touchedChunkCoords);
                }
//                terrainMap.populateFloodFillSeedsUpdateFaceMapsInChunkColumn(x, z, dataProvider, touchedChunkCoords); // ORDER OF THIS LINE AND THE SUN/WATER COMPUTER LINES MATTERS! TODO: FIX

                columnMap.SetBuiltSurface(x, z);
//                ChunkSunLightComputer.ComputeRays(terrainMap, x, z); // no need. terrain map does this while generating
                if (!keepGoing.get()) break; //PREVENT FREEZE AT END OF RUN??

//                ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z); //WANT
//                ChunkWaterLevelComputer.Scatter(terrainMap, columnMap, x, z); //WANT
//                columnMap.SetBuilt(x, z); // MOVED

                try { sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
                /*
                 * AWKWARD BUT: IF THERE WERE NO TOUCHED-CHUNK-COORDS AT ALL,
                 * ADD A 'DUMMY' CHUNK COORD, SO THAT FLOOD FILL 4D WILL BE PROMPTED
                 * TO PUT ANY CHUNK SLICES IN THE CHUNK COLUMN IN QUESTION INTO ITS 'IN BOUNDS BAG'
                 */
                terrainMap.updateChunksToBeFlooded(touchedChunkCoords);
            }
        }
    }

    private HashSet<Coord2> columnsFromChunkCoords(HashSet<Coord3> chunkCoords) {
        HashSet<Coord2> columns = new HashSet<>(6);
        for (Coord3 co : chunkCoords) {
            columns.add(new Coord2(co.x, co.z));
        }
        return columns;
    }

    public int getX() { return x; }
    public int getZ() { return z; }

}
