package com.sudoplay.joise.examples;

import com.sudoplay.joise.module.Module;
import com.sudoplay.joise.module.ModuleGradient;
import voxel.landscape.BlockType;
import voxel.landscape.noise.TerrainNoiseSettings;
import voxel.landscape.player.B;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

/**
 * Created by didyouloseyourdog on 9/26/14.
 */
public class JoiseMultiPanelExample extends JPanel {
    private static int panelWidth = 200, panelHeight = 250, panelMarginRight = 100;
    private static int panelsPerRow = 3;
    private static int panelsPerColumn = 2;
    private static long seed = 1236;

//    private java.util.List<JoiseDemoPanel> demoPanels = new ArrayList<JoiseDemoPanel>(20);
    private HashMap<String, JoiseDemoPanel> demoPanels = new HashMap<String, JoiseDemoPanel> (20);

    public JoiseMultiPanelExample() {
        loadModules();

        for (JoiseDemoPanel demoPanel : demoPanels.values()) {
            demoPanel.updateImage();
        }
    }
    private void addDemoPanelWithModule(Module mod, String message, TerrainNoiseSettings terrainNoiseSettings) {
        JoiseDemoPanel demoPanel = demoPanels.get(message);
        if (demoPanel == null) {
            demoPanel = new JoiseDemoPanel(this, mod, panelWidth, panelHeight, panelMarginRight, message, terrainNoiseSettings);
            demoPanels.put(message, demoPanel);
        } else {
            demoPanel.setModule(mod);
        }
    }
    private TerrainNoiseSettings getDemoPanelSettings(String key) {
        JoiseDemoPanel demoPanel = demoPanels.get(key);
        if (demoPanel == null) return null;
        return demoPanel.getTerrainNoiseSettings();
    }

    public void reloadModules() {
        loadModules();
        B.bugln("reloading");
        for (JoiseDemoPanel demoPanel : demoPanels.values()) {
            demoPanel.updateImage();
        }
    }

    private void loadModules() {
        /*
         * ground_gradient
         */
        // ground_gradient
        ModuleGradient groundGradient =  new ModuleGradient();
        groundGradient.setGradient(0, 0, 0, 1, 0, 0);
        String gradientKey = "Ground Gradient";
        addDemoPanelWithModule(groundGradient, gradientKey, null);
        demoPanels.get(gradientKey).removeGUI();

        String mountainsKey = "mountains";
        TerrainNoiseSettings mountainSettings = getDemoPanelSettings(mountainsKey);
        if (mountainSettings == null) {
            mountainSettings = TerrainNoiseSettings.MountainTerrainNoiseSettings(seed);
        }
        Module mountainTerrain = mountainSettings.makeTerrainModule(groundGradient);
        addDemoPanelWithModule(mountainTerrain, mountainsKey, mountainSettings);

        /*
        String highlandKey = "highlands";
        TerrainNoiseSettings highlandSettings = getDemoPanelSettings(highlandKey);
        if (highlandSettings == null) {
            highlandSettings = TerrainNoiseSettings.HighLandTerrainNoiseSettings(seed);
        }
        Module highlandTerrain = TerrainDataProvider.MakeTerrainNoise(groundGradient, highlandSettings );
        addDemoPanelWithModule(highlandTerrain, highlandKey, highlandSettings);
        */
        /*
         * select air or solid with mountains
         */
        String terrainSelectKey = "terrain Select";
        TerrainNoiseSettings terrSelectSettings = getDemoPanelSettings(terrainSelectKey);
        if (terrSelectSettings == null) {
            terrSelectSettings = TerrainNoiseSettings.TerrainSettingsWithZeroOneModuleSelect(seed, mountainTerrain);
        } else {
            terrSelectSettings.moduleSelectSettings.controlSource = mountainTerrain;
        }
        Module terrSelectModule = terrSelectSettings.moduleSelectSettings.makeSelectModule();
        addDemoPanelWithModule(terrSelectModule, terrainSelectKey, terrSelectSettings);

        /*
         * noise to determine which kind of solid block
         */
        String typeSelectSettingKey = "terrain type select";
        TerrainNoiseSettings typeSelectSettings = getDemoPanelSettings(typeSelectSettingKey);
        if (typeSelectSettings == null) {
            typeSelectSettings = TerrainNoiseSettings.TerrainTypeSelectModuleNoiseSettings(seed);
        }
        Module selectTypeTerr = typeSelectSettings.makeTerrainModule(null);
        addDemoPanelWithModule(selectTypeTerr, typeSelectSettingKey, typeSelectSettings);

        String dirtOrStoneSelectSettingsKey = "dirt of stone";
        TerrainNoiseSettings dirtOrStoneSelectSettings = getDemoPanelSettings(dirtOrStoneSelectSettingsKey);
        if (dirtOrStoneSelectSettings == null) {
            dirtOrStoneSelectSettings = TerrainNoiseSettings.BlockTypeSelectModuleSettings(seed, selectTypeTerr, BlockType.DIRT, BlockType.STONE);
        } else {
            dirtOrStoneSelectSettings.moduleSelectSettings.controlSource = selectTypeTerr;
        }
        Module dirtOrStoneModule = dirtOrStoneSelectSettings.moduleSelectSettings.makeSelectModule();
        addDemoPanelWithModule(dirtOrStoneModule, dirtOrStoneSelectSettingsKey, dirtOrStoneSelectSettings);



    }

    public static void main(String[] args) {
        int N = 4;
        int width = (panelWidth + panelMarginRight) * panelsPerRow;
        int height = panelHeight * panelsPerColumn;

        JFrame frame = new JFrame("Joise Example-Multi Panel");
        frame.setPreferredSize(new Dimension(width, height));
        frame.setLayout(new GridLayout(panelsPerColumn,panelsPerRow,N,N));

        final JoiseMultiPanelExample multiPanelExample = new JoiseMultiPanelExample();
        for( JoiseDemoPanel demoPanel : multiPanelExample.demoPanels.values()) {
            frame.add(demoPanel);
        }
//        frame.add(multiPanelExample);

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent arg0) {
                //
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
                //
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                //
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                //
            }

            @Override
            public void mouseClicked(MouseEvent arg0) {
//                updateImage(canvas);
            }
        });

//        updateImage(canvas);

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

}
