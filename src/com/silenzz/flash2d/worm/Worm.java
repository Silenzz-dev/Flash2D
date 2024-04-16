package com.silenzz.flash2d.worm;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;

public class Worm {

    private static final int DOT_SIZE = 12;
    private static final int RADIUS = DOT_SIZE / 2;
    private static final int MAX_POINTS = 40;

    private static final int NUM_DIRS = 8;
    private static final int N = 0;
    private static final int NE = 1;
    private static final int E = 2;
    private static final int SE = 3;
    private static final int S = 4;
    private static final int SW = 5;
    private static final int W = 6;
    private static final int NW = 7;
    private static final int NUM_PROBS = 9;
    
    private int currentCompass;
    private Point2D.Double[] increments;
    private int[] probsForOffset;
    
    // Worm
    private Point[] cells;
    private int nPoints;
    private int tailPos;
    private int headPos;
    
    // Word
    private int pWidth;
    private int pHeight;
    
    private long startTime; // in ms
    private Obstacles obs;
    
    public Worm(int pWidth, int pHeight, Obstacles obs) {
        this.pWidth = pWidth;
        this.pHeight = pHeight;
        this.obs = obs;
        
        cells = new Point[MAX_POINTS];
        nPoints = 0;
        headPos = -1;
        tailPos = -1;
        
        // increments for each compass dir
        increments = new Point2D.Double[NUM_DIRS];
        increments[N] = new Point2D.Double(0.0, -1.0);
        increments[NE] = new Point2D.Double(0.7, -0.7);
        increments[E] = new Point2D.Double(1.0, 0.0);
        increments[SE] = new Point2D.Double(0.7, 0.7);
        increments[S] = new Point2D.Double(0.0, 1.0);
        increments[SW] = new Point2D.Double(-0.7, 0.7);
        increments[W] = new Point2D.Double(-1.0, 0.0);
        increments[NW] = new Point2D.Double(-0.7, -0.7);
        
        // probability info for selecting a compass dir.
        //    0 = no change, -1 means 1 step anti-clockwise,
        //    1 means 1 step clockwise, etc.
        /* The array means that usually the worm continues in
           the same direction but may bear slightly to the left
           or right. */
        probsForOffset = new int[NUM_PROBS];
        probsForOffset[0] = 0;  probsForOffset[1] = 0;
        probsForOffset[2] = 0;  probsForOffset[3] = 1;
        probsForOffset[4] = 1;  probsForOffset[5] = 2;
        probsForOffset[6] = -1;  probsForOffset[7] = -1;
        probsForOffset[8] = -2;
    }
    
    /**
     * is (x,y) near the worm's head?
     */
    public boolean nearHead(int x, int y) {
        if (nPoints == 0) {
            return false;
        }
        return Math.abs(cells[headPos].x + RADIUS - x) <= DOT_SIZE
                && Math.abs(cells[headPos].y + RADIUS - y) <= DOT_SIZE;
    }

    /**
     * is (x,y) near any part of the worm's body?
     */
    public boolean touchedAt(int x, int y) {
        int i = tailPos;
        while (i != headPos) {
            if (Math.abs(cells[i].x + RADIUS - x) <= RADIUS
                    && Math.abs(cells[i].y + RADIUS - y) <= RADIUS) {
                return true;
            }
            i = (i + 1) % MAX_POINTS;
        }
        return false;
    }
    
    /**
     * A move causes the addition of a new dot to the front of
       the worm, which becomes its new head. A dot has a position
       and compass direction/bearing, which is derived from the
       position and bearing of the old head.

       move() is complicated by having to deal with 3 cases:
         * when the worm is first created
         * when the worm is growing
         * when the worm is MAXPOINTS long (then the addition
           of a new head must be balanced by the removal of a
           tail dot)
     */
    public void move() {
        int prevPosn = headPos; // save old head posn while creating new one
        headPos = (headPos + 1) % MAX_POINTS;

        if (nPoints == 0) { // empty array at start
            tailPos = headPos;
            currentCompass = (int) (Math.random() * NUM_DIRS); // random dir.
            cells[headPos] = new Point(pWidth / 2, pHeight / 2); // center pt
            nPoints++;
        } else if (nPoints == MAX_POINTS) { // array is full
            tailPos = (tailPos + 1) % MAX_POINTS; // forget last tail
            newHead(prevPosn);
        } else { // still room in cells[]
            newHead(prevPosn);
            nPoints++;
        }
    }
    
    /**
     * Create new head position and compass direction/bearing.

       This has two main parts. Initially we try to generate
       a head by varying the old position/bearing. But if
       the new head hits an obstacle, then we shift
       to a second phase. 

       In the second phase we try a head which is 90 degrees
       clockwise, 90 degress clockwise, or 180 degrees reversed
       so that the obstacle is avoided. These bearings are 
       stored in fixedOffs[].
     */
    private void newHead(int prevPosn) {
        Point newPt;
        int newBearing;
        int fixedOffs[] = { -2, 2, -4 }; // offsets to avoid an obstacle

        newBearing = varyBearing();
        newPt = nextPoint(prevPosn, newBearing);
        // Get a new position based on a semi-random
        // variation of the current position.

        if (obs.hits(newPt, DOT_SIZE)) {
            for (int i = 0; i < fixedOffs.length; i++) {
                newBearing = calcBearing(fixedOffs[i]);
                newPt = nextPoint(prevPosn, newBearing);
                if (!obs.hits(newPt, DOT_SIZE))
                    break; // one of the fixed offsets will work
            }
        }
        cells[headPos] = newPt; // new head position
        currentCompass = newBearing; // new compass direction
    }
    
    /**
     * vary the compass bearing semi-randomly
     */
    private int varyBearing() {
        int newOffset = probsForOffset[(int) (Math.random() * NUM_PROBS)];
        return calcBearing(newOffset);
    }

    /**
     * Use the offset to calculate a new compass bearing based
     * on the current compass direction.
     */
    private int calcBearing(int offset) {
        int turn = currentCompass + offset;
        // ensure that turn is between N to NW (0 to 7)
        if (turn >= NUM_DIRS) {
            turn = turn - NUM_DIRS;
        } else if (turn < 0) { 
            turn = NUM_DIRS + turn;
        }
        return turn;
    }
    
    /**
     * Return the next coordinate based on the previous position
       and a compass bearing.

       Convert the compass bearing into predetermined increments 
       (stored in incrs[]). Add the increments multiplied by the 
       DOTSIZE to the old head position.
       Deal with wraparound.
     */
    private Point nextPoint(int prevPosn, int bearing) {
        // get the increments for the compass bearing
        Point2D.Double incr = increments[bearing];

        int newX = cells[prevPosn].x + (int) (DOT_SIZE * incr.x);
        int newY = cells[prevPosn].y + (int) (DOT_SIZE * incr.y);

        // modify newX/newY if < 0, or > pWidth/pHeight; use wraparound
        if (newX + DOT_SIZE < 0) { // is right hand edge invisible?
            newX = newX + pWidth;
        } else if (newX > pWidth) {
            newX = newX - pWidth;
        }

        if (newY + DOT_SIZE < 0) { // is bottom edge invisible?
            newY = newY + pHeight;
        } else if (newY > pHeight) {
            newY = newY - pHeight;
        }

        return new Point(newX, newY);
    }
    
    /**
     * draw a black worm with a red head
     */
    public void draw(Graphics g) {
        if (nPoints > 0) {
            g.setColor(Color.BLACK);
            int i = tailPos;
            while (i != headPos) {
                g.fillOval(cells[i].x, cells[i].y, DOT_SIZE, DOT_SIZE);
                i = (i + 1) % MAX_POINTS;
            }
            g.setColor(Color.RED);
            g.fillOval(cells[headPos].x, cells[headPos].y, DOT_SIZE, DOT_SIZE);
        }
    }

}
