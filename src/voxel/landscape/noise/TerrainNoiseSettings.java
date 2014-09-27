package voxel.landscape.noise;

import com.sudoplay.joise.module.*;
import voxel.landscape.BlockType;

public class TerrainNoiseSettings {

    public class FractalSettings {
        public FractalSettings (long _seed) { seed = _seed; }
        public ModuleFractal.FractalType fractalType = ModuleFractal.FractalType.BILLOW;
        public int octaves = 2;
        public double frequency = 0.25f;
        public double lacunarity = ModuleFractal.DEFAULT_LACUNARITY;
        public ModuleBasisFunction.BasisType basisType = ModuleBasisFunction.BasisType.GRADIENT;
        public ModuleBasisFunction.InterpolationType interpolationType = ModuleBasisFunction.InterpolationType.QUINTIC;
        public long seed;

        public Object[] getEnums() {
            return new Object[] {
              fractalType, basisType, interpolationType
            };
        }
        public <E extends Enum<E>> void setEnum(Class<E> enumClass, E enumValue){
            for (Object e : getEnums()) {
                if (e.getClass().equals(enumClass)) {
                    E theEnum = (E) e;
                    theEnum = enumValue;
                }
            }
        }
    }


    public FractalSettings fractalSettings;
    public ModuleSelectSettings moduleSelectSettings;
    public float autoCorrectLow = 0f;
    public float autoCorrectHigh = 1f;
    public float xzscale = 1f;
    public float offset = 0f;
    public float yScale = 1f;
    public boolean renderWithBlockColors = false;

    public void setUpModuleSelectSettings(Module _controlSource) {
        moduleSelectSettings = new ModuleSelectSettings(_controlSource);
    }
    public TerrainNoiseSettings(long _seed) {
       fractalSettings = new FractalSettings(_seed);
    }

    public static TerrainNoiseSettings LowLandTerrainNoiseSettings(long _seed) {
        TerrainNoiseSettings lowlandSettings = new TerrainNoiseSettings(_seed);
        lowlandSettings.fractalSettings.fractalType = ModuleFractal.FractalType.BILLOW;
        lowlandSettings.fractalSettings.octaves = 2;
        lowlandSettings.fractalSettings.frequency = 0.25f;
        lowlandSettings.xzscale = 0.435f;
        lowlandSettings.offset = -0.45f;
        lowlandSettings.yScale = 0f;
        return lowlandSettings;
    }
    public static TerrainNoiseSettings HighLandTerrainNoiseSettings(long _seed) {
        TerrainNoiseSettings highLandSettings = new TerrainNoiseSettings(_seed);
        highLandSettings.fractalSettings.fractalType = ModuleFractal.FractalType.FBM;
        highLandSettings.fractalSettings.octaves = 4;
        highLandSettings.fractalSettings.frequency = 2f;
        highLandSettings.xzscale = .45f;
        highLandSettings.offset = 0f;
        return highLandSettings;
    }
    public static TerrainNoiseSettings MountainTerrainNoiseSettings(long _seed) {
        TerrainNoiseSettings mountainSettings = new TerrainNoiseSettings(_seed);
        mountainSettings.fractalSettings.fractalType = ModuleFractal.FractalType.RIDGEMULTI;
        mountainSettings.fractalSettings.octaves = 8;
        mountainSettings.fractalSettings.frequency = 1f;
        mountainSettings.autoCorrectLow = -1;
        mountainSettings.xzscale = .45f;
        mountainSettings.offset = .15f;
        mountainSettings.yScale = .1f;
        return mountainSettings;
    }
    public static TerrainNoiseSettings TerrainSettingsWithZeroOneModuleSelect(long _seed, Module _controlSource) {
        TerrainNoiseSettings selectorSettings = new TerrainNoiseSettings(_seed);
        selectorSettings.setUpModuleSelectSettings(_controlSource);
        return selectorSettings;
    }

    public static TerrainNoiseSettings TerrainTypeSelectModuleNoiseSettings(long _seed) {
        TerrainNoiseSettings typeSelectSettings = new TerrainNoiseSettings(_seed);
        typeSelectSettings.fractalSettings.fractalType = ModuleFractal.FractalType.FBM;
        typeSelectSettings.fractalSettings.octaves = 4;
        typeSelectSettings.fractalSettings.frequency = 1f;
        typeSelectSettings.yScale = 1f;
        return typeSelectSettings;
    }
    public static TerrainNoiseSettings BlockTypeSelectModuleSettings(long _seed, Module _controlSource, BlockType a, BlockType b) {
        TerrainNoiseSettings blockSelect = new TerrainNoiseSettings(_seed);
        blockSelect.moduleSelectSettings = ModuleSelectSettings.BlockTypeSelectSettings(_controlSource, a, b);
        blockSelect.renderWithBlockColors = true;
        return blockSelect;
    }

    public Module makeTerrainModule(Module groundGradient) {
        // land_shape_fractal
        Module landShape = makeFractalModule(fractalSettings);
        // land_autocorrect
        ModuleAutoCorrect landShapeAutoCorrect =
                new ModuleAutoCorrect(autoCorrectLow, autoCorrectHigh);
        landShapeAutoCorrect.setSource(landShape);
        landShapeAutoCorrect.calculate();

        // land_scale
        ModuleScaleOffset landScale = new ModuleScaleOffset();
        landScale.setScale(xzscale);
        landScale.setOffset(offset);
        landScale.setSource(landShapeAutoCorrect);

        // land_y_scale
        ModuleScaleDomain landYScale = new ModuleScaleDomain();
        landYScale.setScaleY(yScale);
        landYScale.setSource(landScale);

        if (groundGradient == null) {
            return  landYScale;
        }
        // terrain
        ModuleTranslateDomain landTerrain = new ModuleTranslateDomain();
        landTerrain.setAxisYSource(landYScale);
        landTerrain.setSource(groundGradient);

        return landTerrain;
    }
    public Module makeFractalModule(TerrainNoiseSettings.FractalSettings fractalSettings) {
        ModuleFractal landShape = new ModuleFractal(fractalSettings.fractalType,
                fractalSettings.basisType, fractalSettings.interpolationType);
        landShape.setNumOctaves(fractalSettings.octaves);
        landShape.setFrequency(fractalSettings.frequency);
        landShape.setSeed(fractalSettings.seed);
        return landShape;
    }

}
