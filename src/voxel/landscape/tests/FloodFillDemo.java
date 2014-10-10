package voxel.landscape.tests;

import voxel.landscape.coord.Coord2;
import voxel.landscape.coord.Coord3;
import voxel.landscape.coord.Direction;
import voxel.landscape.player.B;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by didyouloseyourdog on 10/4/14.
 */
public class FloodFillDemo extends JPanel implements ActionListener {


    private static int gridUnit = 16;
    public final int width = 400, height = 400;
    public final int scale = 1;
    private BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    private final Color targetColor = Color.WHITE;
    private final Color markerColor = Color.RED;
    private final Color fillColor = new Color(0,255,0);
    private final int boundaryColor = Color.BLACK.getRGB();
    public FloodFiller floodFiller = new FloodFiller();
    public boolean paintMode = true;
    public boolean mouseDown = false;
    public Coord2 paintPoint = new Coord2(0);

    @Override
    public void actionPerformed(ActionEvent e) {
        if (paintMode) {
            image.setRGB(paintPoint.x, paintPoint.y, boundaryColor);
        }
        repaint();
    }

    public class FloodFiller {
        public Coord2 startSeed = new Coord2(width/2, height/2);
        private Rect bounds = new Rect(new Coord2(0,0), new Coord2(width, height));
        private Coord2 UP = new Coord2(0, -1);
        private Coord2 DOWN = new Coord2(0, 1);

        public void flood() {
            Color current = Color.GREEN;
//            for (int i = 0; i < 7; ++i) {
            ArrayList<Coord2> neighborSeeds = new ArrayList<Coord2>(120);
            neighborSeeds.add(startSeed);
            current = nextTargetColor(current);
            long start = System.currentTimeMillis();
            long snanos = System.nanoTime();
            int neiSeedsSizeMax = 0;
            while(neighborSeeds.size() > 0) {
                Rect bound = GridRectContaining(neighborSeeds.get(0));
                neighborSeeds.addAll(floodScanLines(neighborSeeds.remove(0), current, nextColor(), bound));
                neiSeedsSizeMax = Math.max(neiSeedsSizeMax, neighborSeeds.size());
            }
            B.bugln("max seed size: " + neiSeedsSizeMax);
            long enanos = System.nanoTime();
            long end = System.currentTimeMillis();
            long time = end - start;
            double millis = time; // (time / 1000d);
            B.bug("took: millis: " + millis);
            long tnanos = enanos - snanos;
            B.bugln(" ... and nanos: " + tnanos);
        }
        private Color nextTargetColor(Color current) {
            if (current.equals(Color.GREEN)) {
                return targetColor;
            } else {
                return Color.GREEN;
            }
        }
        private void flood(Coord2 seed, Color tarColor, Color fllColor) {
            if (!bounds.contains(seed)) return;
            int curColor = image.getRGB(seed.x, seed.y);
            if (curColor != tarColor.getRGB() && curColor != markerColor.getRGB()) {
//                image.setRGB(seed.x, seed.y, new Color(0, Math.max(new Color(curColor).getGreen() - 40, 0), 0).getRGB()); // mark visit
                return;
            }
            image.setRGB(seed.x, seed.y, fllColor.getRGB());

            for(int i=0; i< 4; ++i) {
                Coord3 dir = Direction.DirectionCoords[i];
                Coord2 d = new Coord2(dir.x, dir.y);
                flood(seed.add(d), tarColor, fllColor);
            }
        }
        private Color[] colors = new Color[] {
                Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.GRAY, Color.ORANGE, Color.YELLOW
        };
        private int colorIndex;
        private Color nextColor() {
            return colors[++colorIndex % colors.length];
        }

        private ArrayList<Coord2> floodScanLines(Coord2 initialSeed, Color tarColor, Color newColor, Rect area) {
            ArrayList<Coord2> neighborSeeds = new ArrayList<Coord2>(8);

            if (initialSeed == null) return neighborSeeds;
            if (!bounds.contains(initialSeed)) return neighborSeeds;
            int curColor = image.getRGB(initialSeed.x, initialSeed.y);
            if (curColor != tarColor.getRGB() && curColor != markerColor.getRGB()) {
                return neighborSeeds;
            }
            ArrayList<Coord2> seeds = new ArrayList<Coord2>(width * height / 2);
            seeds.add(initialSeed);
            int y1 = 0;
            boolean spanLeft = false, spanRight = false;
            boolean spanUp = false, spanDown = false;
            int targetColorRGB = tarColor.getRGB();
            int xAreaStart = area.start.x, yAreaStart = area.start.y;
            int xAreaEnd = area.extent().x, yAreaEnd = area.extent().y;

            while(seeds.size() > 0) {
                Coord2 seed = seeds.remove(0);
                y1 = seed.y;
                while (y1 >= 0 && y1 >= yAreaStart && (colorAt( seed.x, y1) == targetColorRGB || colorAt(seed.x, y1) == markerColor.getRGB())) {
                    if (y1 > 0 && y1 == yAreaStart) {
                        if (!spanUp && colorAt(seed.x, y1 - 1) == targetColorRGB) {
                            neighborSeeds.add(new Coord2(seed.x, y1 - 1));
                            spanUp = true;
                        }
//                        else if (spanUp && colorAt(seed.x, y1 - 1) == boundaryColor ) {
                        if (colorAt(seed.x, y1 - 1) == boundaryColor ) {
                            spanUp = false;
                        }
                    }
                    y1--;
                }
                y1++;
                spanLeft = spanRight = false;
                while(y1 < height && y1 < yAreaEnd && (colorAt( seed.x, y1) == targetColorRGB || colorAt(seed.x, y1) == markerColor.getRGB()))
                {
                    image.setRGB(seed.x, y1, newColor.getRGB());
                    if (!spanLeft) {
                        if (seed.x > 0) {
                         if (colorAt( seed.x - 1, y1) == targetColorRGB || colorAt(seed.x - 1, y1) == markerColor.getRGB()) {
                             if (seed.x == xAreaStart ){
                                 neighborSeeds.add(new Coord2(seed.x - 1, y1));
                             } else {
                                 seeds.add(new Coord2(seed.x - 1, y1));
                             }
                             spanLeft = true;
                         }
                        }
                    }
//                    else {
                        if (seed.x > 0 &&  colorAt(seed.x - 1, y1) == boundaryColor) {
                            spanLeft = false;
                        }
//                    }
                    if (!spanRight) {
                        if (seed.x < width - 1) {
                            if (colorAt( seed.x + 1, y1) == targetColorRGB || colorAt(seed.x + 1, y1) == markerColor.getRGB()) {
                                if (seed.x == xAreaEnd - 1 ) {
                                    neighborSeeds.add(new Coord2(seed.x + 1, y1));
                                } else {
                                    seeds.add(new Coord2(seed.x + 1, y1));
                                }
                                spanRight = true;
                            }
                        }
                    }
//                    else {
                        if (seed.x < width - 1 && colorAt(seed.x + 1, y1) == boundaryColor) {
                            spanRight = false;
                        }
//                    }
                    if (y1 < height - 1 && y1 == yAreaEnd - 1) {
                        if (!spanDown && colorAt(seed.x, y1 + 1) == targetColorRGB) {
                            neighborSeeds.add(new Coord2(seed.x, y1 + 1));
                            spanDown = true;
                        }
                        if (colorAt(seed.x, y1 + 1) == boundaryColor) {
                            spanDown = false;
                        }
                    }
                    y1++;
                }

            }
            return neighborSeeds;
        }
        private int colorAt(int x, int y) {
            return colorAt(new Coord2(x,y));
        }
        private int colorAt(Coord2 co) {
            if (bounds.contains(co))
                return image.getRGB(co.x, co.y);
            return boundaryColor;
        }
    }

    public class Rect {
        public Coord2 start;
        public Coord2 length;

        public Rect(Coord2 _start, Coord2 _length) {
            start = _start; length = _length;
        }
        public boolean contains(Coord2 point) {
            Coord2 dif = point.minus(start);
            return dif.greaterThan(new Coord2(-1, -1)) && length.greaterThan(dif);
        }
        public Coord2 extent() { return start.add(length); }

    }

    private Rect GridRectContaining(Coord2 point) {
        Coord2 start = point.divideBy(new Coord2(gridUnit)).multy(gridUnit);
        return new Rect(start, new Coord2(gridUnit));
    }

    public void floodAndRepaint() {
        floodFiller.flood();
//        repaint();
    }

    public void update(Coord2 point) {
        point = point.divideBy(new Coord2(scale, scale));
        if (!floodFiller.bounds.contains(point)) return;
        if (paintMode) {
            paintPoint = point;
        } else {
            image.setRGB(floodFiller.startSeed.x, floodFiller.startSeed.y, targetColor.getRGB());
            image.setRGB(point.x, point.y, markerColor.getRGB());
            floodFiller.startSeed = point;
            repaint();
        }
//        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        g2.drawImage(image, transform, null);
        g2.dispose();
    }

    public void setUpImage() {
//        image.
        Graphics2D g = image.createGraphics();
        g.setPaint(targetColor);
        g.fillRect(0, 0, width, height);
        randomBoundaries();
        randomBoundaries();
        paintMode = false;
        update(floodFiller.startSeed);
    }

//    private void circularBoundary(int radMultiplier) {
////        Coord2 or = new Coord2(width/2, height/2);
//        Point.Double origin = new Point.Double(width/2, height/2);
//        int radius = Math.min(width,height)/10;
//        double angle = 0d;
//        while(angle <= Math.PI * 2) {
//            Point.Double point = new Point2D.Double(origin.x + radMultiplier * Math.cos(angle), origin.y + radMultiplier * Math.sin(angle));
//        }
//        for(int x = 0; x< width; ++x) {
//            int r = random.nextInt(3) - 1;
//            y = Math.min(height - 1, y + r );
//            if (!floodFiller.bounds.contains(new Coord2(x,y)))
//                return;
//            image.setRGB(x,y, boundaryColor);
//        }
//    }

    private void randomBoundaries() {
        Random random = new Random();
        int y = random.nextInt(height);
        int hedge = 0;
        for(int x = hedge; x< width  - hedge; ++x) {
            int r = random.nextInt(3) - 1;
            y = Math.min(height - 1, y + r );
            if (!floodFiller.bounds.contains(new Coord2(x,y)))
                return;
            image.setRGB(x,y, boundaryColor);
        }
        int xx = random.nextInt(width);
        for(int yy = hedge; yy< height - hedge; ++yy) {
            int r = random.nextInt(3) - 1;
            xx = Math.min(width - 1, xx + r );
            if (!floodFiller.bounds.contains(new Coord2(xx,yy)))
                return;
            image.setRGB(xx,yy, boundaryColor);
        }
    }

    public FloodFillDemo() {
        setUpImage();

        Timer timer = new Timer(50, this);
        timer.start();

    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Flood Fill test");
        final FloodFillDemo floodFillDemo = new FloodFillDemo();

        frame.setPreferredSize(new Dimension(floodFillDemo.width * floodFillDemo.scale, floodFillDemo.height * floodFillDemo.scale));

        frame.add(floodFillDemo);

        frame.setUndecorated(true);
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        floodFillDemo.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent arg0) {
//                Coord2 newSeed = new Coord2( arg0.getX(), arg0.getY());
//
//                floodFillDemo.floodFiller.startSeed = newSeed;
//                floodFillDemo.repaint();
//                floodFillDemo.paintMode = false;
                floodFillDemo.mouseDown = false;
            }
            @Override
            public void mousePressed(MouseEvent arg0) {
                floodFillDemo.mouseDown = true;
                Coord2 point = new Coord2( arg0.getX(), arg0.getY());
                B.bug(arg0.getX());
                B.bug(arg0.getY());
                floodFillDemo.update(point);
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

            }
        });

        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_F){
                    floodFillDemo.floodAndRepaint();
                } else if (keyCode == KeyEvent.VK_M) {
                    floodFillDemo.paintMode = !floodFillDemo.paintMode;
                    if (floodFillDemo.paintMode) {
                        B.bugln("paint mode on");
                    } else {
                        B.bugln("paint mode off");
                    }
                } else if (keyCode == KeyEvent.VK_R) {
                    floodFillDemo.setUpImage();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
    }
}
