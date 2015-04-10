package voxel.landscape.map.structure;

import com.sudoplay.joise.module.Module;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.structure.structures.AbstractStructure;
import voxel.landscape.map.structure.structures.Pyramid;

/**
 * Created by didyouloseyourdog on 3/28/15.
 */
public class SurfaceStructureDataProvider {
    Module module;

    public AbstractStructure structureAt(int x, int y, int z) {
        Coord3 local = Chunk.ToChunkLocalCoord(x,y,z);
        if (local.x == 4 && local.z == 4) {
            return new Pyramid(123);
        }
        return null;
    }
    public AbstractStructure structureAt(Coord3 global) {
        return structureAt(global.x, global.y, global.z);
    }

}
