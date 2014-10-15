package voxel.landscape.noise;

import com.sudoplay.joise.module.*;
import com.sudoplay.joise.module.ModuleBasisFunction.BasisType;
import com.sudoplay.joise.module.ModuleBasisFunction.InterpolationType;
import com.sudoplay.joise.module.ModuleCombiner.CombinerType;
import com.sudoplay.joise.module.ModuleFractal.FractalType;
import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.coord.Coord2;
import voxel.landscape.map.TerrainMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

//import com.sudoplay.joise.examples.Canvas;

/*
 * This class's job is to tell
 * whoever wants to know
 * what terrain height and block type exists
 * at a given x and z
 * 
 * it has two ways of doing this:
 * "NoiseModule" : do some math to generate 'smooth' random numbers
 * "ImageMode" : read the pixels in an image and base the terrain data off of them.
 */


//public class TerrainDataProvider implements IBlockDataProvider, IBlockTypeDataProvider
public class TerrainDataProvider {
    private static long seed;

    public enum Mode {
        NoiseModule, ImageMode
    }

    private Mode mode = Mode.NoiseModule;
    private BufferedImage buffIm;
    Coord2 buffiDims;

    Module noiseModule;
    public static final double WORLD_TO_VERTICAL_NOISE_SCALE = TerrainMap.GetWorldHeightInBlocks(); // * 2.2;
    public static final double WORLD_TO_HORIZONTAL_NOISE_SCALE = Chunk.XLENGTH * 16d;

    private static final int ARGB_POS_MAX = (256 * 256 * 256);
    private static final int ARGB_POS_ONE_CHANNEL_MAX = 256;

    public TerrainDataProvider() {
        this(Mode.NoiseModule);
    }

    public TerrainDataProvider(Mode _mode) {
        mode = _mode;
        if (mode == Mode.ImageMode) {
            String heightMapSrc = "inputTexturesMP/heightMapTex.png";
            buffIm = getBufferedImage(heightMapSrc);
            // TODO: sep ims for heightmap and btype map
            buffiDims = new Coord2(buffIm.getWidth(), buffIm.getHeight());
        } else {
            seed = -21234;
            setupModule();
        }
    }

    public int getBlockDataAtPosition(int xin, int yin, int zin) {
        return testNoise(xin, yin, zin);

//        double r = noiseModule.get(
//                xin / WORLD_TO_HORIZONTAL_NOISE_SCALE,
//                (WORLD_TO_VERTICAL_NOISE_SCALE - yin) / WORLD_TO_VERTICAL_NOISE_SCALE,
//                zin / WORLD_TO_HORIZONTAL_NOISE_SCALE);
//        return r < 0.001 ? BlockType.AIR.ordinal() : (int) r;
    }
    private int testNoise(int x, int y, int z) {
//        if  ( (x & 15 + x & 7) + (z & 15 + z & 7) + y < 54) return 3;
//        int sumxz = (x & 15) + (z & 15);
//        if ( y < 17 || (y != 20 && y != 19 && ((sumxz + 8) & 31) < 16 && y < 22)) return 3;
        int across = z;
        if ((y == 14) && ((across & 3) == 1)) {
            if ((z & 1) == 1) return 8;
            else return 5;
        }
        if (y == 10 && (across & 15) == 5) return 7;
        if (y == 12 && (across & 3) == 2) return 4;
//        if (y < 8) return 3;
        return 1;
    }

    private BufferedImage getBufferedImage(String src) {
        File imFile = new File(src);

        BufferedImage buffi = null;
        try {
            buffi = ImageIO.read(imFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffi;
    }

    private double readImageAt(double xin, double yin) {
        int rgb = getRGB(xin, yin);

        //result is neg for some reason when reading pngs...oh well
        int alpha = (rgb >> 24) & 0xFF;

        return (double) (alpha / ((double) ARGB_POS_ONE_CHANNEL_MAX * 3) * 2 - 1);
    }

    private int getRGB(double xin, double yin) {
        int x = (int) Math.abs(xin), y = (int) Math.abs(yin);
        return buffIm.getRGB(x, y) + ARGB_POS_MAX;
    }

    /*
    * parameterized land method
    */
//    public static Module MakeFractalModule(TerrainNoiseSettings.FractalSettings fractalSettings) {
//        ModuleFractal landShape = new ModuleFractal(fractalSettings.fractalType,
//                fractalSettings.basisType, fractalSettings.interpolationType);
//        landShape.setNumOctaves(fractalSettings.octaves);
//        landShape.setFrequency(fractalSettings.frequency);
//        landShape.setSeed(fractalSettings.seed);
//        return landShape;
//    }
    /*
    * parameterized land method
    */
//    public static Module MakeTerrainNoise(Module groundGradient, TerrainNoiseSettings terrainNoiseSettings) {
//        // land_shape_fractal
//        Module landShape = MakeFractalModule(terrainNoiseSettings.fractalSettings);
//        // land_autocorrect
//        ModuleAutoCorrect landShapeAutoCorrect =
//                new ModuleAutoCorrect(terrainNoiseSettings.autoCorrectLow, terrainNoiseSettings.autoCorrectHigh);
//        landShapeAutoCorrect.setSource(landShape);
//        landShapeAutoCorrect.calculate();
//
//        // land_scale
//        ModuleScaleOffset landScale = new ModuleScaleOffset();
//        landScale.setScale(terrainNoiseSettings.xzscale);
//        landScale.setOffset(terrainNoiseSettings.offset);
//        landScale.setSource(landShapeAutoCorrect);
//
//        // land_y_scale
//        ModuleScaleDomain landYScale = new ModuleScaleDomain();
//        landYScale.setScaleY(terrainNoiseSettings.yScale);
//        landYScale.setSource(landScale);
//
//        // terrain
//        ModuleTranslateDomain landTerrain = new ModuleTranslateDomain();
//        landTerrain.setAxisYSource(landYScale);
//        landTerrain.setSource(groundGradient);
//
//        return landTerrain;
//    }

    private void setupModule() {
        /*
         * ground_gradient
	     */
        ModuleGradient groundGradient = new ModuleGradient();
        groundGradient.setGradient(0, 0, 0, 1);

//        Module lowlands = TerrainNoiseSettings.LowLandTerrainNoiseSettings(seed).makeTerrainModule(groundGradient); // MakeTerrainNoise(groundGradient, TerrainNoiseSettings.LowLandTerrainNoiseSettings(seed));
//        Module highlands = TerrainNoiseSettings.HighLandTerrainNoiseSettings(seed).makeTerrainModule(groundGradient); // MakeTerrainNoise(groundGradient, TerrainNoiseSettings.HighLandTerrainNoiseSettings(seed));
        Module mountains = TerrainNoiseSettings.MountainTerrainNoiseSettings(seed, false).makeTerrainModule(groundGradient); // MakeTerrainNoise(groundGradient, TerrainNoiseSettings.MountainTerrainNoiseSettings(seed));


        ModuleSelectSettings dirtAirSelect = ModuleSelectSettings.BlockTypeSelectSettingsManualThreshold(mountains, BlockType.AIR, BlockType.DIRT, .5d);

        noiseModule = dirtAirSelect.makeSelectModule();
    }

    //Simplified version (seems faster also...)
    private void setupModuleBROKEN() {
        // ========================================================================
        // = Based on Joise module chain Example 2
        // ========================================================================

        /*
         * ground_gradient
         */
        ModuleGradient groundGradient = new ModuleGradient();
        groundGradient.setGradient(0, 0, 0, 1);


        /*
         * mountain
         */
        // mountain_shape_fractal
        ModuleFractal mountainShapeFractal = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        mountainShapeFractal.setNumOctaves(8);
        mountainShapeFractal.setFrequency(1);
        mountainShapeFractal.setSeed(seed);
        /*
         * MMP: cache for bedrock
         */
        ModuleCache mp_mountainCache = new ModuleCache();
        mp_mountainCache.setSource(mountainShapeFractal);

        // mountain_autocorrect
        ModuleAutoCorrect mountainAutoCorrect = new ModuleAutoCorrect(-1, 1);
        mountainAutoCorrect.setSource(mountainShapeFractal);
        mountainAutoCorrect.setSource(mp_mountainCache);
        mountainAutoCorrect.calculate();

        // mountain_scale
        ModuleScaleOffset mountainScale = new ModuleScaleOffset();
        mountainScale.setScale(0.45);
        mountainScale.setOffset(0.15);
        mountainScale.setSource(mountainAutoCorrect);

        // mountain_y_scale
        ModuleScaleDomain mountainYScale = new ModuleScaleDomain();
        mountainYScale.setScaleY(0.1);
        mountainYScale.setSource(mountainScale);

        // mountain_terrain
        ModuleTranslateDomain mountainTerrain = new ModuleTranslateDomain();
        mountainTerrain.setAxisYSource(mountainYScale);
        mountainTerrain.setSource(groundGradient);

        // ground_select
        ModuleSelect groundSelect = new ModuleSelect();
        groundSelect.setLowSource(0);
        groundSelect.setHighSource(1);
        groundSelect.setThreshold(0.5);
        groundSelect.setControlSource(mountainTerrain);

	    /*
	     * cave
	     */
        /*
        ModuleFractal caveShape = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        caveShape.setNumOctaves(1);
        caveShape.setFrequency(4);
        caveShape.setSeed(seed);

        ModuleFractal caveShape2 = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        caveShape2.setNumOctaves(1);
        caveShape2.setFrequency(4);
        caveShape2.setSeed(seed + 1000);

        ModuleCombiner caveShape22 = new ModuleCombiner(CombinerType.MULT);
        caveShape22.setSource(0, caveShape2);
        caveShape22.setSource(1, caveShape2);

        // combined, 'pre-perturbed' cave shape
        ModuleCombiner caveShapeA = new ModuleCombiner(CombinerType.MULT);
        caveShapeA.setSource(0, caveShape);
        caveShapeA.setSource(1, caveShape22);

        ModuleCache caveShapeCache = new ModuleCache();
        caveShapeCache.setSource(caveShapeA); // use for terrain types as well

        Module mp_caveModule = caveModuleCreate(caveShapeCache, seed);
*/
	    /*
	     * Block Type
	     */
        BlockType blockMonoType = BlockType.STONE;
        Float monoTypeFloat = blockMonoType.getFloat();
        ModuleFractal terrainTypeHelperModule = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        terrainTypeHelperModule.setNumOctaves(1);
        terrainTypeHelperModule.setFrequency(2); //high for testing
        terrainTypeHelperModule.setSeed(seed);

        ModuleAutoCorrect terrAutoCorrect = new ModuleAutoCorrect(0, 1);
        terrAutoCorrect.setSource(terrainTypeHelperModule);
        terrAutoCorrect.calculate();

        // lowland_scale
        ModuleScaleOffset terrScaleOffset = new ModuleScaleOffset();
        terrScaleOffset.setScale(.5);
        terrScaleOffset.setOffset(-0.1);
        terrScaleOffset.setSource(terrAutoCorrect);

//        ModuleScaleOffset caveTampDown = new ModuleScaleOffset();
//        caveTampDown.setScale(.2);
//        caveTampDown.setSource(caveShapeCache);

//        ModuleScaleOffset terrOffByCaveFrac = new ModuleScaleOffset();
//        terrOffByCaveFrac.setOffset(caveTampDown);
//        terrOffByCaveFrac.setSource(terrScaleOffset);

        // lowland_y_scale
//        ModuleScaleDomain terrScaleYDomain = new ModuleScaleDomain();
//        terrScaleYDomain.setScaleY(.2);
//        terrScaleYDomain.setSource(terrOffByCaveFrac);

        // sand or grass ?
        ModuleScaleOffset scaleTerrMountain = new ModuleScaleOffset();
        scaleTerrMountain.setScale(6.5);
        scaleTerrMountain.setSource(mp_mountainCache);

        ModuleSelect terrainSelect = new ModuleSelect();
        terrainSelect.setLowSource(BlockType.AIR.getFloat());
        terrainSelect.setHighSource(monoTypeFloat);
        terrainSelect.setControlSource(scaleTerrMountain);
        terrainSelect.setThreshold(.4);
        terrainSelect.setFalloff(0);

//        ModuleTranslateDomain strataGradientPerturb = new ModuleTranslateDomain();
//        strataGradientPerturb.setAxisYSource(terrScaleYDomain);
//        strataGradientPerturb.setSource(groundGradient);

        //SELECT SAND/GRASS or STONE
//        ModuleSelect stoneSandGrassSelect = new ModuleSelect();
//        stoneSandGrassSelect.setLowSource(terrainSelect); //stone value
//        stoneSandGrassSelect.setHighSource(monoTypeFloat);
//        stoneSandGrassSelect.setControlSource(strataGradientPerturb);
//        stoneSandGrassSelect.setThreshold(.94);
//        stoneSandGrassSelect.setFalloff(0);

//        //ADD AREAS NEAR CAVES AS CAVE-ISH STONE
//        ModuleSelect stoneSandGrassCaveSelect = new ModuleSelect();
//        stoneSandGrassCaveSelect.setLowSource(stoneSandGrassSelect); //stone value
//        stoneSandGrassCaveSelect.setHighSource(monoTypeFloat);
//        stoneSandGrassCaveSelect.setControlSource(caveShapeCache);
//        stoneSandGrassCaveSelect.setThreshold(.75);
//        stoneSandGrassCaveSelect.setFalloff(0);

//        ModuleCache terrSelectCache = new ModuleCache();
//        terrSelectCache.setSource(terrainSelect);

	    /*
	     * final-almost
	     */
//        ModuleCombiner groundCaveMultiply = new ModuleCombiner(CombinerType.MULT);
//        groundCaveMultiply.setSource(0, mp_caveModule);
//        groundCaveMultiply.setSource(1, groundSelect);
//        groundCaveMultiply.setSource(2, stoneSandGrassCaveSelect);

	    /*
	     * Bedrock
	     */

        ModuleGradient bedrockGradient = new ModuleGradient();
        bedrockGradient.setGradient(0, 0, .95, 1);

        ModuleScaleOffset bedrockScaleOffset = new ModuleScaleOffset();
        bedrockScaleOffset.setScale(.05);
        bedrockScaleOffset.setOffset(0.0);
        bedrockScaleOffset.setSource(mp_mountainCache);

        ModuleScaleDomain bedrockYScale = new ModuleScaleDomain();
        bedrockYScale.setScaleY(0);
        bedrockYScale.setSource(bedrockScaleOffset);

        ModuleTranslateDomain bedrockTerrain = new ModuleTranslateDomain();
        bedrockTerrain.setAxisYSource(bedrockYScale);
        bedrockTerrain.setSource(bedrockGradient);

        ModuleSelect bedrockSelect = new ModuleSelect();
        bedrockSelect.setLowSource(terrainSelect);
        bedrockSelect.setHighSource(monoTypeFloat); //BEDROCK VALUE
        bedrockSelect.setControlSource(bedrockTerrain);
        bedrockSelect.setThreshold(0.9);
        bedrockSelect.setFalloff(0);


	    /*
	     * Draw it
	     */

        noiseModule =  bedrockSelect;

    }

    private static Module caveModuleCreateM(Module caveShapeA, long seed) {
        int moduleSelect = 0;

        // cave_perturb_fractal
        ModuleFractal cavePerturbFractal = new ModuleFractal(FractalType.FBM, BasisType.GRADIENT, InterpolationType.QUINTIC);
        cavePerturbFractal.setNumOctaves(6);
        cavePerturbFractal.setFrequency(3);
        cavePerturbFractal.setSeed(seed); //CONSIDER: adjust seed??

        // cave_perturb_scale
        ModuleScaleOffset cavePerturbScale = new ModuleScaleOffset();
        cavePerturbScale.setScale(0.25);
        cavePerturbScale.setOffset(0);
        cavePerturbScale.setSource(cavePerturbFractal);

        // cave_perturb
        ModuleTranslateDomain cavePerturb = new ModuleTranslateDomain();
        cavePerturb.setAxisXSource(cavePerturbScale);
//        cavePerturb.setAxisZSource(cavePerturbScale);
//        cavePerturb.setAxisYSource(cavePerturbScale);
        cavePerturb.setSource(caveShapeA);

        /*
         * reduce caves at lower Ys with gradient
         */
        ModuleGradient caveDepthGradient = new ModuleGradient();
        caveDepthGradient.setGradient(0, 0, .85, 1);
        ModuleBias caveGradientBias = new ModuleBias();
        caveGradientBias.setSource(caveDepthGradient);
        caveGradientBias.setBias(.75);
        ModuleScaleOffset flipCaveDepthGradient = new ModuleScaleOffset();
        flipCaveDepthGradient.setScale(-3.5);
        flipCaveDepthGradient.setOffset(1.5);
        flipCaveDepthGradient.setSource(caveDepthGradient);

        ModuleCombiner minCombiner = new ModuleCombiner(CombinerType.MIN);
        minCombiner.setSource(0, 1);
        minCombiner.setSource(1, flipCaveDepthGradient);

        ModuleCombiner caveDepthCombiner = new ModuleCombiner(CombinerType.MULT);
        caveDepthCombiner.setSource(0, cavePerturb);
        caveDepthCombiner.setSource(1, minCombiner);

        // cave_select
        ModuleSelect caveSelect = new ModuleSelect();
        caveSelect.setLowSource(1);
        caveSelect.setHighSource(2);
        caveSelect.setControlSource(caveDepthCombiner);
        caveSelect.setThreshold(0.8);
        caveSelect.setFalloff(0);

        return caveSelect;
//		    canvas.updateImage(caveSelect);
//		    canvas.updateImage(caveDepthCombiner);
//		    canvas.updateImage(caveGradientBias);
//		    canvas.updateImage(minCombiner);
//		    canvas.updateImage(flipCaveDepthGradient);
//		    canvas.updateImage(caveShapeAttenuate);
//		    canvas.updateImage(caveAttenuateBias);
    }
    private void setupModuleBIG() {
        // ========================================================================
        // = Based on Joise module chain Example 2
        // ========================================================================
	    
	    /*
	     * ground_gradient
	     */
        ModuleGradient groundGradient = new ModuleGradient();
        groundGradient.setGradient(0, 0, 0, 1);

	    /*
	     * lowland
	     */
        // lowland_shape_fractal
        ModuleFractal lowlandShapeFractal = new ModuleFractal(FractalType.BILLOW, BasisType.GRADIENT, InterpolationType.QUINTIC);
        lowlandShapeFractal.setNumOctaves(2);
        lowlandShapeFractal.setFrequency(0.25);
        lowlandShapeFractal.setSeed(seed);

        // lowland_autocorrect
        ModuleAutoCorrect lowlandAutoCorrect = new ModuleAutoCorrect(0, 1);
        lowlandAutoCorrect.setSource(lowlandShapeFractal);
        lowlandAutoCorrect.calculate();

        // lowland_scale
        ModuleScaleOffset lowlandScale = new ModuleScaleOffset();
        lowlandScale.setScale(0.05);
        lowlandScale.setOffset(-0.45);
        lowlandScale.setSource(lowlandAutoCorrect);

        // lowland_y_scale
        ModuleScaleDomain lowlandYScale = new ModuleScaleDomain();
        lowlandYScale.setScaleY(0);
        lowlandYScale.setSource(lowlandScale);

        // lowland_terrain
        ModuleTranslateDomain lowlandTerrain = new ModuleTranslateDomain();
        lowlandTerrain.setAxisYSource(lowlandYScale);
        lowlandTerrain.setSource(groundGradient);

	    /*
	     * highland
	     */

        // highland_shape_fractal
        ModuleFractal highlandShapeFractal = new ModuleFractal(FractalType.FBM, BasisType.GRADIENT, InterpolationType.QUINTIC);
        highlandShapeFractal.setNumOctaves(4);
        highlandShapeFractal.setFrequency(2);
        highlandShapeFractal.setSeed(seed);

        // highland_autocorrect
        ModuleAutoCorrect highlandAutoCorrect = new ModuleAutoCorrect(-1, 1);
        highlandAutoCorrect.setSource(highlandShapeFractal);
        highlandAutoCorrect.calculate();

        // highland_scale
        ModuleScaleOffset highlandScale = new ModuleScaleOffset();
        highlandScale.setScale(0.25);
        highlandScale.setOffset(0);
        highlandScale.setSource(highlandAutoCorrect);

        // highland_y_scale
        ModuleScaleDomain highlandYScale = new ModuleScaleDomain();
        highlandYScale.setScaleY(0);
        highlandYScale.setSource(highlandScale);

        // highland_terrain
        ModuleTranslateDomain highlandTerrain = new ModuleTranslateDomain();
        highlandTerrain.setAxisYSource(highlandYScale);
        highlandTerrain.setSource(groundGradient);
	    
	    /*
	     * mountain
	     */
        // mountain_shape_fractal
        ModuleFractal mountainShapeFractal = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        mountainShapeFractal.setNumOctaves(8);
        mountainShapeFractal.setFrequency(1);
        mountainShapeFractal.setSeed(seed);
	    /*
	     * MMP: cache for bedrock
	     */
        ModuleCache mp_mountainCache = new ModuleCache();
        mp_mountainCache.setSource(mountainShapeFractal);

        // mountain_autocorrect
        ModuleAutoCorrect mountainAutoCorrect = new ModuleAutoCorrect(-1, 1);
        mountainAutoCorrect.setSource(mountainShapeFractal);
        mountainAutoCorrect.setSource(mp_mountainCache);
        mountainAutoCorrect.calculate();


        // mountain_scale
        ModuleScaleOffset mountainScale = new ModuleScaleOffset();
        mountainScale.setScale(0.45);
        mountainScale.setOffset(0.15);
        mountainScale.setSource(mountainAutoCorrect);

        // mountain_y_scale
        ModuleScaleDomain mountainYScale = new ModuleScaleDomain();
        mountainYScale.setScaleY(0.1);
        mountainYScale.setSource(mountainScale);

        // mountain_terrain
        ModuleTranslateDomain mountainTerrain = new ModuleTranslateDomain();
        mountainTerrain.setAxisYSource(mountainYScale);
        mountainTerrain.setSource(groundGradient);

	    /*
	     * terrain
	     */
        // terrain_type_fractal
        ModuleFractal terrainTypeFractal = new ModuleFractal(FractalType.FBM, BasisType.GRADIENT, InterpolationType.QUINTIC);
        terrainTypeFractal.setNumOctaves(3);
        terrainTypeFractal.setFrequency(0.125);
        terrainTypeFractal.setSeed(seed);

        // terrain_autocorrect
        ModuleAutoCorrect terrainAutoCorrect = new ModuleAutoCorrect(0, 1);
        terrainAutoCorrect.setSource(terrainTypeFractal);
        terrainAutoCorrect.calculate();

        // terrain_type_y_scale
        ModuleScaleDomain terrainTypeYScale = new ModuleScaleDomain();
        terrainTypeYScale.setScaleY(0);
        terrainTypeYScale.setSource(terrainAutoCorrect);

        // terrain_type_cache
        ModuleCache terrainTypeCache = new ModuleCache();
        terrainTypeCache.setSource(terrainTypeYScale);

        // highland_mountain_select
        ModuleSelect highlandMountainSelect = new ModuleSelect();
        highlandMountainSelect.setLowSource(highlandTerrain);
        highlandMountainSelect.setHighSource(mountainTerrain);
        highlandMountainSelect.setControlSource(terrainTypeCache);
        highlandMountainSelect.setThreshold(0.65);
        highlandMountainSelect.setFalloff(0.2);

        // highland-mountains_lowland_select
        ModuleSelect highlandLowlandSelect = new ModuleSelect();
        highlandLowlandSelect.setLowSource(lowlandTerrain);
        highlandLowlandSelect.setHighSource(highlandMountainSelect);
        highlandLowlandSelect.setControlSource(terrainTypeCache);
        highlandLowlandSelect.setThreshold(0.25);
        highlandLowlandSelect.setFalloff(0.15);

        // highland_lowland_select_cache
        ModuleCache highlandLowlandSelectCache = new ModuleCache();
        highlandLowlandSelectCache.setSource(highlandLowlandSelect);

        // ground_select
        ModuleSelect groundSelect = new ModuleSelect();
        groundSelect.setLowSource(0);
        groundSelect.setHighSource(1);
        groundSelect.setThreshold(0.5);
        groundSelect.setControlSource(highlandLowlandSelectCache);

	    /*
	     * cave
	     */
        ModuleFractal caveShape = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        caveShape.setNumOctaves(1);
        caveShape.setFrequency(4);
        caveShape.setSeed(seed);

        ModuleFractal caveShape2 = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        caveShape2.setNumOctaves(1);
        caveShape2.setFrequency(4);
        caveShape2.setSeed(seed + 1000);

        ModuleCombiner caveShape22 = new ModuleCombiner(CombinerType.MULT);
        caveShape22.setSource(0, caveShape2);
        caveShape22.setSource(1, caveShape2);

        // combined, 'pre-perturbed' cave shape
        ModuleCombiner caveShapeA = new ModuleCombiner(CombinerType.MULT);
        caveShapeA.setSource(0, caveShape);
        caveShapeA.setSource(1, caveShape22);

        ModuleCache caveShapeCache = new ModuleCache();
        caveShapeCache.setSource(caveShapeA); // use for terrain types as well

        Module mp_caveModule = caveModuleCreate(caveShapeCache, seed);

	    /*
	     * Block Type
	     */
        ModuleFractal terrainTypeHelperModule = new ModuleFractal(FractalType.RIDGEMULTI, BasisType.GRADIENT, InterpolationType.QUINTIC);
        terrainTypeHelperModule.setNumOctaves(1);
        terrainTypeHelperModule.setFrequency(2); //high for testing
        terrainTypeHelperModule.setSeed(seed);

        ModuleAutoCorrect terrAutoCorrect = new ModuleAutoCorrect(0, 1);
        terrAutoCorrect.setSource(terrainTypeHelperModule);
        terrAutoCorrect.calculate();

        // lowland_scale
        ModuleScaleOffset terrScaleOffset = new ModuleScaleOffset();
        terrScaleOffset.setScale(.5);
        terrScaleOffset.setOffset(-0.1);
        terrScaleOffset.setSource(terrAutoCorrect);

        ModuleScaleOffset caveTampDown = new ModuleScaleOffset();
        caveTampDown.setScale(.2);
        caveTampDown.setSource(caveShapeCache);

        ModuleScaleOffset terrOffByCaveFrac = new ModuleScaleOffset();
        terrOffByCaveFrac.setOffset(caveTampDown);
        terrOffByCaveFrac.setSource(terrScaleOffset);

        // lowland_y_scale
        ModuleScaleDomain terrScaleYDomain = new ModuleScaleDomain();
        terrScaleYDomain.setScaleY(.2);
        terrScaleYDomain.setSource(terrOffByCaveFrac);

        // sand or grass ?
        ModuleCombiner terrTypePlusMountainsNoise = new ModuleCombiner(CombinerType.ADD);
        terrTypePlusMountainsNoise.setSource(0, terrainTypeCache);
        terrTypePlusMountainsNoise.setSource(1, mp_mountainCache); //ever useful

        ModuleScaleOffset scaleTerrMountain = new ModuleScaleOffset();
        scaleTerrMountain.setScale(6.5);
        scaleTerrMountain.setSource(terrTypePlusMountainsNoise);

        ModuleSelect terrainSelect = new ModuleSelect();
        terrainSelect.setLowSource(BlockType.SAND.getFloat());
        terrainSelect.setHighSource(BlockType.DIRT.getFloat());
        terrainSelect.setControlSource(scaleTerrMountain);
        terrainSelect.setThreshold(.9);
        terrainSelect.setFalloff(0);

        ModuleTranslateDomain strataGradientPerturb = new ModuleTranslateDomain();
        strataGradientPerturb.setAxisYSource(terrScaleYDomain);
        strataGradientPerturb.setSource(groundGradient);

        //SELECT SAND/GRASS or STONE
        ModuleSelect stoneSandGrassSelect = new ModuleSelect();
        stoneSandGrassSelect.setLowSource(terrainSelect); //stone value
        stoneSandGrassSelect.setHighSource(BlockType.STONE.getFloat());
        stoneSandGrassSelect.setControlSource(strataGradientPerturb);
        stoneSandGrassSelect.setThreshold(.94);
        stoneSandGrassSelect.setFalloff(0);

        //ADD AREAS NEAR CAVES AS CAVE-ISH STONE
        ModuleSelect stoneSandGrassCaveSelect = new ModuleSelect();
        stoneSandGrassCaveSelect.setLowSource(stoneSandGrassSelect); //stone value
        stoneSandGrassCaveSelect.setHighSource(BlockType.CAVESTONE.getFloat());
        stoneSandGrassCaveSelect.setControlSource(caveShapeCache);
        stoneSandGrassCaveSelect.setThreshold(.75);
        stoneSandGrassCaveSelect.setFalloff(0);

        ModuleCache terrSelectCache = new ModuleCache();
        terrSelectCache.setSource(terrainSelect);

	    /*
	     * final-almost
	     */
        ModuleCombiner groundCaveMultiply = new ModuleCombiner(CombinerType.MULT);
        groundCaveMultiply.setSource(0, mp_caveModule);
        groundCaveMultiply.setSource(1, groundSelect);
        groundCaveMultiply.setSource(2, stoneSandGrassCaveSelect);
	    
	    /*
	     * Bedrock
	     */
        ModuleGradient bedrockGradient = new ModuleGradient();
        bedrockGradient.setGradient(0, 0, .95, 1);

        ModuleScaleOffset bedrockScaleOffset = new ModuleScaleOffset();
        bedrockScaleOffset.setScale(.05);
        bedrockScaleOffset.setOffset(0.0);
        bedrockScaleOffset.setSource(mp_mountainCache);

        ModuleScaleDomain bedrockYScale = new ModuleScaleDomain();
        bedrockYScale.setScaleY(0);
        bedrockYScale.setSource(bedrockScaleOffset);

        ModuleTranslateDomain bedrockTerrain = new ModuleTranslateDomain();
        bedrockTerrain.setAxisYSource(bedrockYScale);
        bedrockTerrain.setSource(bedrockGradient);

        ModuleSelect bedrockSelect = new ModuleSelect();
        bedrockSelect.setLowSource(groundCaveMultiply);
        bedrockSelect.setHighSource(BlockType.BEDROCK.getFloat()); //BEDROCK VALUE
        bedrockSelect.setControlSource(bedrockTerrain);
        bedrockSelect.setThreshold(0.9);
        bedrockSelect.setFalloff(0);

	    /*
	     * Draw it
	     */

        noiseModule = bedrockSelect;

    }

    private static Module riverModuleCreate(long seed) {
        return null; //TODO: implement
    }

    private static Module caveModuleCreate(Module caveShapeA, long seed) {
        int moduleSelect = 0;

        // cave_perturb_fractal
        ModuleFractal cavePerturbFractal = new ModuleFractal(FractalType.FBM, BasisType.GRADIENT, InterpolationType.QUINTIC);
        cavePerturbFractal.setNumOctaves(6);
        cavePerturbFractal.setFrequency(3);
        cavePerturbFractal.setSeed(seed); //CONSIDER: adjust seed??

        // cave_perturb_scale
        ModuleScaleOffset cavePerturbScale = new ModuleScaleOffset();
        cavePerturbScale.setScale(0.25);
        cavePerturbScale.setOffset(0);
        cavePerturbScale.setSource(cavePerturbFractal);

        // cave_perturb
        ModuleTranslateDomain cavePerturb = new ModuleTranslateDomain();
        cavePerturb.setAxisXSource(cavePerturbScale);
//        cavePerturb.setAxisZSource(cavePerturbScale);
//        cavePerturb.setAxisYSource(cavePerturbScale);
        cavePerturb.setSource(caveShapeA);
		    
        /*
         * reduce caves at lower Ys with gradient
         */
        ModuleGradient caveDepthGradient = new ModuleGradient();
        caveDepthGradient.setGradient(0, 0, .85, 1);
        ModuleBias caveGradientBias = new ModuleBias();
        caveGradientBias.setSource(caveDepthGradient);
        caveGradientBias.setBias(.75);
        ModuleScaleOffset flipCaveDepthGradient = new ModuleScaleOffset();
        flipCaveDepthGradient.setScale(-3.5);
        flipCaveDepthGradient.setOffset(1.5);
        flipCaveDepthGradient.setSource(caveDepthGradient);

        ModuleCombiner minCombiner = new ModuleCombiner(CombinerType.MIN);
        minCombiner.setSource(0, 1);
        minCombiner.setSource(1, flipCaveDepthGradient);

        ModuleCombiner caveDepthCombiner = new ModuleCombiner(CombinerType.MULT);
        caveDepthCombiner.setSource(0, cavePerturb);
        caveDepthCombiner.setSource(1, minCombiner);

        // cave_select
        ModuleSelect caveSelect = new ModuleSelect();
        caveSelect.setLowSource(1);
        caveSelect.setHighSource(2);
        caveSelect.setControlSource(caveDepthCombiner);
        caveSelect.setThreshold(0.8);
        caveSelect.setFalloff(0);

        return caveSelect;
//		    canvas.updateImage(caveSelect);
//		    canvas.updateImage(caveDepthCombiner);
//		    canvas.updateImage(caveGradientBias);
//		    canvas.updateImage(minCombiner);
//		    canvas.updateImage(flipCaveDepthGradient);
//		    canvas.updateImage(caveShapeAttenuate);
//		    canvas.updateImage(caveAttenuateBias);
    }

}
