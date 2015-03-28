package voxel.landscape.map.structure;

import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.map.TerrainMap;
import voxel.landscape.map.structure.structures.AbstractStructure;
import voxel.landscape.noise.TerrainDataProvider;

import java.util.HashSet;

/**
 * Created by didyouloseyourdog on 3/28/15.
 */
public class StructureBuilder {

    private SurfaceStructureDataProvider surfaceStructureDataProvider = new SurfaceStructureDataProvider();

    public void addStructures(Coord2 chunkColumn, TerrainMap map, TerrainDataProvider dataProvider, HashSet<Coord3> touchedChunkCoords) {
        int x1 = chunkColumn.getX()* Chunk.CHUNKDIMS.x;
        int z1 = chunkColumn.getZ()*Chunk.CHUNKDIMS.z;

        int x2 = x1+Chunk.CHUNKDIMS.x;
        int z2 = z1+Chunk.CHUNKDIMS.z;
        int surfaceY = 0;
        for(int z=z1; z<z2; z++) {
            for(int x=x1; x<x2; x++) {
                surfaceY = map.getSurfaceHeight(x, z);
                Coord3 global = new Coord3(x, surfaceY, z);
                AbstractStructure structure = surfaceStructureDataProvider.structureAt(global);
                if (structure == null || !structure.viablePlot(global, map)) continue;
                for (Coord3 structureLocal : structure.outerBlocks.keySet()) {
                    BlockType blockType = structure.outerBlocks.get(structureLocal);
                    Coord3 structureGlobal = global.add(structureLocal);
                    map.setBlockAtWorldCoord((byte) blockType.ordinal(), structureGlobal);
                    touchedChunkCoords.add(Chunk.ToChunkPosition(structureGlobal));
                }
            }
        }
    }
}
