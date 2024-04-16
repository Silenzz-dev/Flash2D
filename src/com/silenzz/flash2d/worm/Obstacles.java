package com.silenzz.flash2d.worm;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Obstacles {
    
    private static final int BOX_LENGTH = 12;
    
    private List<Rectangle> boxes;
    private WormChase wormChase;
    
    public Obstacles(WormChase wormChase) {
        this.wormChase = wormChase;
        boxes = new ArrayList<>();
    }
    
    public synchronized void add(int x, int y) {
        boxes.add(new Rectangle(x, y, BOX_LENGTH, BOX_LENGTH));
        wormChase.setBoxNumber(boxes.size()); // report new number of boxes
    }

    public synchronized boolean hits(Point p, int size) {
        Rectangle r = new Rectangle(p.x, p.y, size, size);
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).intersects(r)) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized void draw(Graphics g) {
        g.setColor(Color.BLUE);
        Rectangle box;
        for (int i = 0; i < boxes.size(); i++) {
            box = boxes.get(i);
            g.fillRect(box.x, box.y, box.width, box.height);
        }
    }

    public synchronized int getNumObstacles() {
        return boxes.size();
    }

}
