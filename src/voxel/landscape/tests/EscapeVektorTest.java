package voxel.landscape.tests;

import com.jme3.math.Vector3f;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.VektorUtil;
import voxel.landscape.player.B;

/**
 * Created by didyouloseyourdog on 11/16/14.
 */
public class EscapeVektorTest extends TestCase {

    @Test
    public void testEscapeVektor() {
        Vector3f direction = new Vector3f(.2f, .8f, .2f).normalize();
        Vector3f pos = new Vector3f(10.1f, -300.2f, 10.1f);
        Vector3f lastPos;
        Coord3 last, current;
        for(int i=0; i < 200; ++i) {
            lastPos = pos.clone();
            last = Coord3.FromVector3f(lastPos);
            pos = VektorUtil.EscapePositionOnUnitGrid(pos, direction, null);
            current = Coord3.FromVector3f(pos);
            if (last.equal(current)) {
                B.bugln("lst: " + lastPos.toString());
                B.bugln("pos: " + pos.toString());
                B.bugln("dir: " + direction.toString());
            }
            assertTrue("we escaped the last coord", !last.equal(current));
        }
    }
}
