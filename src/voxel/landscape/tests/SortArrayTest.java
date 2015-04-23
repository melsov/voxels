package voxel.landscape.tests;

import com.sun.tools.hat.internal.util.ArraySorter;
import com.sun.tools.hat.internal.util.Comparer;
import junit.framework.TestCase;
import org.junit.Test;
import voxel.landscape.player.B;

/**
 * Created by didyouloseyourdog on 4/18/15.
 */
public class SortArrayTest extends TestCase {

    @Test
    public void testSorter() {
        Integer[] integers = new Integer[] { 2, 3, 5, 1 };
        ArraySorter.sort(integers, new Comparer() {
            @Override
            public int compare(Object o, Object o1) {
                if ((Integer) o > (Integer) o1) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        for(Integer i : integers) {
            B.bug(i);
        }
    }

}
