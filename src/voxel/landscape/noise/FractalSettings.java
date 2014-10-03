package voxel.landscape.noise;

import com.sudoplay.joise.module.Module;
import com.sudoplay.joise.module.ModuleBasisFunction;
import com.sudoplay.joise.module.ModuleCache;
import com.sudoplay.joise.module.ModuleFractal;

/**
 * Created by didyouloseyourdog on 10/3/14.
 */
public class FractalSettings {
    public FractalSettings (long _seed) { seed = _seed; }
    public ModuleFractal.FractalType fractalType = ModuleFractal.FractalType.BILLOW;
    public int octaves = 2;
    public double frequency = 0.25f;
    public double lacunarity = ModuleFractal.DEFAULT_LACUNARITY;
    public ModuleBasisFunction.BasisType basisType = ModuleBasisFunction.BasisType.GRADIENT;
    public ModuleBasisFunction.InterpolationType interpolationType = ModuleBasisFunction.InterpolationType.QUINTIC;
    public long seed;

    public boolean shouldCache;
    public ModuleCache moduleCache;

    public Module makeFractalModule() {
        ModuleFractal landShape = new ModuleFractal(fractalType, basisType, interpolationType);
        if (shouldCache) {
            moduleCache = new ModuleCache();
            moduleCache.setSource(landShape);
        }
        landShape.setNumOctaves(octaves);
        landShape.setFrequency(frequency);
        landShape.setLacunarity(lacunarity);
        landShape.setSeed(seed);
        return landShape;
    }
}