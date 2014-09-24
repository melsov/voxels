package com.sudoplay.joise.examples;

import com.sudoplay.joise.module.Module;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@SuppressWarnings("serial")
public class Canvas extends JPanel {

  private static final float SCALE = 1.0f;
  private BufferedImage image;
  
  private static double GRASS_VALUE = 4.0;
  private static double SAND_VALUE = 3.0;
  private static double STONE_VALUE = 2.0;
  private static double CAVE_STONE_VALUE = 2.4;
  private static double BEDROCK_VALUE = 1.0;

  Canvas(int width, int height) {
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
  }

  void updateImage(Module mod) {
    int width = image.getWidth();
    int height = image.getHeight();
    float px, py, r;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        px = x / (float) width * SCALE;
        py = y / (float) height * SCALE;

        /*
         * Sample the module chain like this...
         */
        r = (float) mod.get(px, py);

//        r = Math.max(0, Math.min(1, r));
        Color c;
        if (r >= GRASS_VALUE) {
        	c = Color.GREEN;
        } else if (r >= SAND_VALUE) {
        	c = new Color (0.9f ,0.9f, 0.3f);
        } else if (r >= STONE_VALUE) {
        	if (r > CAVE_STONE_VALUE) {
        		c = new Color(.4f, .45f, .4f);
        	} else {
	        	c = new Color(.3f, .4f, .3f);
        	}
        }
        else {
        	r = Math.max(0, Math.min(1, r));
        	c = new Color(r,r,r);
        } 
//        else {
//        	c = new Color(1f, 1 - r, 1 - r);
//        }
        image.setRGB(x, y, c.getRGB());
      }
    }
    repaint();
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.drawImage(image, null, null);
    g2.dispose();
  }

}
