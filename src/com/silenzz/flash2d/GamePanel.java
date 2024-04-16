package com.silenzz.flash2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

import javax.swing.JPanel;

public class GamePanel extends JPanel implements GameStateListener {
    
    private static final int PWIDTH = 500;
    private static final int PHEIGHT = 400;
    private static final int FPS = 120;
    
    private GameLoop gameLoop;
    private Graphics dbg;
    private Image dbImage;
    
    public GamePanel() {
        gameLoop = new GameLoop(FPS, this);
        
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(PWIDTH, PHEIGHT));
        setFocusable(true);
        requestFocus();
        
        readyForTermination();
    }
    
    private void readyForTermination() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_Q ||
                        keyCode == KeyEvent.VK_END ||
                        keyCode == KeyEvent.VK_C) && e.isControlDown() ||
                        keyCode == KeyEvent.VK_ESCAPE) {
                    gameLoop.stop();
                }
            }
        });
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        gameLoop.start();
    }
    
    @Override
    public void start() {
        System.out.println("Game start!");
    }
    
    @Override
    public void update() {
        // TODO
    }
    
    @Override
    public void render() {
        if (dbImage == null) {
            dbImage = createImage(PWIDTH, PHEIGHT);
            if (dbImage != null) {
                dbg = dbImage.getGraphics();
            } else {
                System.out.println("dbImage is null");
                return;
            }
        }
        
        dbg.setColor(Color.BLACK);
        dbg.fillRect(0, 0, PWIDTH, PHEIGHT);
        
        paintScreen();
    }
    
    @Override
    public void finish() {
        System.out.println("Game finish!");
        System.exit(0);
    }
    
    private void paintScreen() {
        Graphics g;
        try {
            g = getGraphics();
            if (g != null && dbImage != null) {
                g.drawImage(dbImage, 0, 0, null);
            }
            Toolkit.getDefaultToolkit().sync();
            g.dispose();
        } catch (Exception e) {
            System.out.println("Graphics context error: " + e);
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dbImage != null) {
            g.drawImage(dbImage, 0, 0, null);
        }
    }

}
