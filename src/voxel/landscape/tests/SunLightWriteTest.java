package voxel.landscape.tests;

import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Coord2;
import voxel.landscape.map.light.SunLightMap;

/**
 * Created by didyouloseyourdog on 4/7/15.
 */
public class SunLightWriteTest extends TestCase {

    @Test
    public void testSunWrite() {
        SunLightMap sunLightMap = new SunLightMap();

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                sunLightMap.SetSunHeight(i + j, i, j);
            }
        }

        sunLightMap.writeRaysToFile(new Coord2(0));
        assertTrue("rays at 0, 0 not write dirty. ", !sunLightMap.raysWriteDirty(new Coord2(0)));
        sunLightMap.RemoveRays(0, 0);

        sunLightMap.readRaysFromFile(new Coord2(0));

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int height = sunLightMap.GetSunHeight(i,j);
                assertEquals(height, i + j);
            }
        }


    }
}
