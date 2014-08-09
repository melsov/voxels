package playground;

import com.jme3.math.Vector3f;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.coord.MutableInteger;
import voxel.landscape.coord.VektorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Created by didyouloseyourdog on 8/8/14.
 */
public class EscapeVektorDemo extends JPanel {
    private BufferedImage image;

    static int width = 800;
    static int height = 800;
    static Vector3f midPoint = new Vector3f(width/2, 0, height/2);
    static int GridPixelsPerUnit = 50;
    private static Color color = Color.BLUE;

    public EscapeVektorDemo(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    void updateImage() {

        repaint();
    }

    private void drawGrid(Graphics2D g) {
        for(int x = width /2; x < width; x += GridPixelsPerUnit) {
            for (int y = height /2; y < height; y += GridPixelsPerUnit) {
                Line2D upLine = new Line2D.Double(x,y,x, y+GridPixelsPerUnit);
                Line2D acrossLine = new Line2D.Double(x,y,x+GridPixelsPerUnit, y);
                g.draw(upLine);
                g.draw(acrossLine);
            }
        }
        for(int x = width /2; x >= 0; x -= GridPixelsPerUnit) {
            for (int y = height /2; y >= 0 ; y -= GridPixelsPerUnit) {
                Line2D upLine = new Line2D.Double(x,y,x, y-GridPixelsPerUnit);
                Line2D acrossLine = new Line2D.Double(x,y,x-GridPixelsPerUnit, y);
                g.draw(upLine);
                g.draw(acrossLine);
            }
        }
        for(int x = width /2; x < width; x += GridPixelsPerUnit) {
            for (int y = height /2; y >= 0 ; y -= GridPixelsPerUnit) {
                Line2D upLine = new Line2D.Double(x,y,x, y-GridPixelsPerUnit);
                Line2D acrossLine = new Line2D.Double(x,y,x+GridPixelsPerUnit, y);
                g.draw(upLine);
                g.draw(acrossLine);
            }
        }
        for(int x = width /2; x >= 0; x -= GridPixelsPerUnit) {
            for (int y = height /2; y < height; y += GridPixelsPerUnit) {
                Line2D upLine = new Line2D.Double(x,y,x, y+GridPixelsPerUnit);
                Line2D acrossLine = new Line2D.Double(x,y,x-GridPixelsPerUnit, y);
                g.draw(upLine);
                g.draw(acrossLine);
            }
        }
    }

    private void drawEscapePoints(Graphics2D g) {
        g.setPaint(color);
        Vector3f dir = new Vector3f(5.5f,0,-3).normalize();
        Vector3f pos = new Vector3f(0f, 0f, .5f);
        drawEscapePoints(g, dir, pos);
    }
    private void drawEscapePoints(Graphics2D g, Vector3f dir, Vector3f pos) {
        drawGridDot(g, pos);
        Vector3f escape = null;
        int count = 0;
        MutableInteger escapeThroughFace = new MutableInteger();

        while(pos.x > -width/2f && pos.x < width/2f && count++ < 5) {
            escape = VektorUtil.EscapePositionOnUnitGrid(pos, dir, escapeThroughFace);
            drawGridDot(g, escape);
            pos = escape;
        }

        int oppDir = Direction.OppositeDirection(escapeThroughFace.integer);
        Coord3 placeRelCoord = Direction.DirectionCoordForDirection(oppDir);
        g.setPaint(Color.RED);
        if (escape != null) {
            Coord3 hit = Coord3.FromVector3fAdjustNegative(escape.add(placeRelCoord.toVector3()));
            drawGridDot(g,hit.toVector3());
            g.setPaint(Color.CYAN);
            draw2DVoxelAt(g,hit);
        }
    }
    private Point2D gridScaledPoint(Vector3f v) {
        return Vector3XZToPoint(v.mult(GridPixelsPerUnit));
    }
    private void drawGridDot(Graphics2D g, Vector3f pos) {
        Vector3f posScaled = pos.mult(GridPixelsPerUnit);
        posScaled = posScaled.add(midPoint);
        Vector3f halfWH = new Vector3f(3f,0f,3f);
        Vector3f start = posScaled.subtract(halfWH);
        Vector3f width = halfWH.mult(2f);
        Ellipse2D e = new Ellipse2D.Float(start.x, start.z, width.x, width.z);
//        g.fill(e);
        g.draw(e);
    }
    private void draw2DVoxelAt(Graphics2D g, Coord3 pos) {
        Vector3f posScaled = pos.multy(GridPixelsPerUnit).toVector3();
        posScaled = posScaled.add(midPoint);
        Vector3f halfWH = new Vector3f(3f,0f,3f);
        Vector3f start = posScaled.subtract(halfWH);
        Vector3f width = halfWH.mult(2f);
        Rectangle.Float r = new Rectangle.Float(start.x, start.z, GridPixelsPerUnit, GridPixelsPerUnit);
//        g.fill(e);
        g.setStroke(new BasicStroke(4));
        g.draw(r);
    }
    private static Point2D Vector3XZToPoint(Vector3f v) { return new Point2D.Double(v.x, v.z); }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        drawGrid(g2);
        drawEscapePoints(g2);
        g2.dispose();
    }

    public static void main(String[] args) {


        JFrame frame = new JFrame("**Escape Points Demo**");
        frame.setPreferredSize(new Dimension(width, height));

        EscapeVektorDemo canvas = new EscapeVektorDemo(width, height);
        frame.add(canvas);

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        canvas.updateImage();

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

}
