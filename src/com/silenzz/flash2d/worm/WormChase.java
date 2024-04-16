package com.silenzz.flash2d.worm;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import com.silenzz.flash2d.GameFrame;

public class WormChase extends GameFrame {
    
    private static final int FPS = 60;
    
    private Worm fred;
    private Obstacles obs;
    private int boxesUsed;
    
    // used at game termination
    private volatile boolean gameOver;
    private int score = 0;
    private Font font;
    private FontMetrics metrics;
    private boolean finishedOff;

    // used by quit 'button'
    private volatile boolean isOverQuitButton;
    private Rectangle quitArea;

    // used by the pause 'button'
    private volatile boolean isOverPauseButton;
    private Rectangle pauseArea;
    
    public WormChase() {
        super("WormChase", FPS);
    }
    
    @Override
    public void start() {
        // create game components
        obs = new Obstacles(this);
        fred = new Worm(width, height, obs);
        
        // set up message font
        font = new Font("SansSerif", Font.BOLD, 24);
        metrics = this.getFontMetrics(font);

        // specify screen areas for the buttons
        pauseArea = new Rectangle(width - 100, height - 45, 70, 15);
        quitArea = new Rectangle(width - 100, height - 20, 70, 15);
        
        setListeners();
    }
    
    private void setListeners() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                testPress(e.getX(), e.getY());
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                testMove(e.getX(), e.getY());
            }
            public void mouseDragged(MouseEvent e) {
                if (!gameLoop.isPaused() && !gameOver) {
                    obs.add(e.getX(), e.getY());
                }
            }
        });
        readyForTermination();
    }
    
    private void readyForTermination() {
        addKeyListener(new KeyAdapter() {
            // listen for esc, q, end, ctrl-c on the canvas to
            // allow a convenient exit from the full screen configuration
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) || (keyCode == KeyEvent.VK_END)
                        || ((keyCode == KeyEvent.VK_C) && e.isControlDown())) {
                    gameLoop.stop();
                }
            }
        });

        // for shutdown tasks
        // a shutdown may not only come from the program
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                gameLoop.stop();
                //finishOff();
            }
        });
    }
    
    /**
     * called from Obstacles object
     */
    public void setBoxNumber(int boxesUsed) {
        this.boxesUsed = boxesUsed;
    }
    
    @Override
    public void updateGame() {
        if (!gameLoop.isPaused() && !gameOver) {
            fred.move();
        }
    }
    
    @Override
    public void renderGame(Graphics gScr) {
        // clear the background
        gScr.setColor(Color.WHITE);
        gScr.fillRect(0, 0, width, height);

        gScr.setColor(Color.BLUE);
        gScr.setFont(font);

        // report frame count & average FPS and UPS at top left
        gScr.drawString(gameLoop.getStats().getFrameCountLabel(), 10, 25);
        gScr.drawString(gameLoop.getStats().getAverageLabel(), 250, 25);

        // report time used and boxes used at bottom left
        gScr.drawString(gameLoop.getStats().getTimeSpentLabel(), 10, height - 15);
        //gScr.drawString("Boxes used: " + boxesUsed, 260, pHeight - 15);

        // draw the pause and quit 'buttons'
        drawButtons(gScr);

        gScr.setColor(Color.BLACK);
        
        obs.draw(gScr);
        fred.draw(gScr);

        if (gameOver) {
          gameOverMessage(gScr);
        }
    }
    
    private void drawButtons(Graphics g) {
        g.setColor(Color.BLACK);

        // draw the pause 'button'
        if (isOverPauseButton) {
            g.setColor(Color.GREEN);
        }

        g.drawOval(pauseArea.x, pauseArea.y, pauseArea.width, pauseArea.height);
        if (gameLoop.isPaused()) {
            g.drawString("Paused", pauseArea.x, pauseArea.y + 10);
        } else {
            g.drawString("Pause", pauseArea.x + 5, pauseArea.y + 10);
        }

        if (isOverPauseButton) {
            g.setColor(Color.BLACK);
        }

        // draw the quit 'button'
        if (isOverQuitButton) {
            g.setColor(Color.GREEN);
        }

        g.drawOval(quitArea.x, quitArea.y, quitArea.width, quitArea.height);
        g.drawString("Quit", quitArea.x + 15, quitArea.y + 10);

        if (isOverQuitButton) {
            g.setColor(Color.BLACK);
        }
    }
    
    private void gameOverMessage(Graphics g) {
        String msg = "Game Over. Your Score: " + score;
        int x = (width - metrics.stringWidth(msg)) / 2;
        int y = (height - metrics.getHeight()) / 2;
        g.setColor(Color.RED);
        g.setFont(font);
        g.drawString(msg, x, y);
    }
    
    /**
     * Deal with pause and quit buttons. Also, is (x,y) near the head, or should an
     * obstacle be added?
     */
    private void testPress(int x, int y) {
        if (isOverPauseButton) {
            gameLoop.togglePausing();
        } else if (isOverQuitButton) {
            gameLoop.stop();
        } else {
            if (!gameLoop.isPaused() && !gameOver) {
                if (fred.nearHead(x, y)) { // was mouse pressed near the head?
                    gameOver = true;
                    score = (40 - gameLoop.getStats().getTimeSpentInGame()) + (40 - boxesUsed);
                    // hack together a score
                } else { // add an obstacle if possible
                    if (!fred.touchedAt(x, y)) { // was the worm's body untouched?
                        obs.add(x, y);
                    }
                }
            }
        }
    }
    
    private void testMove(int x, int y) {
        if (gameLoop.isRunning()) { // stops problems with a rapid move after pressing 'quit'
            isOverPauseButton = pauseArea.contains(x, y) ? true : false;
            isOverQuitButton = quitArea.contains(x, y) ? true : false;
        }
    }

}
