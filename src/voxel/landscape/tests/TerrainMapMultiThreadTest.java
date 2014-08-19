package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.chunkbuild.ResponsiveRunnable;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.light.ChunkSunLightComputer;
import voxel.landscape.noise.TerrainDataProvider;

/**
 * Created by didyouloseyourdog on 7/31/14.
 */
public class TerrainMapMultiThreadTest extends TestCase {

    @Test
    public void testMultiThreadedDataGen() {
        TerrainMap map = new TerrainMap(null);

    }

    public class AsyncGenerateColumnDataTester extends ResponsiveRunnable
    {

        private int x,z;
        private TerrainMap terrainMap;
        private ColumnMap columnMap;
        private TerrainDataProvider dataProvider = new TerrainDataProvider();
        public AsyncGenerateColumnDataTester(final TerrainMap _terrainMap, final ColumnMap _columnMap, int xx, int zz) {
            columnMap = _columnMap;
            x = xx; z = zz;
            terrainMap = _terrainMap;
        }
        @Override
        public void doRun() {
            terrainMap.generateNoiseForChunkColumn(x,z);
            ChunkSunLightComputer.ComputeRays(terrainMap, x, z);
            ChunkSunLightComputer.Scatter(terrainMap, columnMap, x, z); //TEST WANT
        }

        public int getX() { return x; }
        public int getZ() { return z; }

    }
}
