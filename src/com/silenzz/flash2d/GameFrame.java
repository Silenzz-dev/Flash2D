package com.silenzz.flash2d;

import java.awt.BufferCapabilities;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;

public class GameFrame extends JFrame implements GameStateListener {

    private static final int NUM_BUFFERS = 2;

    // used for full-screen exclusive mode
    private GraphicsDevice gd;
    private Graphics gScr;
    private BufferStrategy bufferStrategy;

    protected GameLoop gameLoop;
    protected int width;
    protected int height;

    public GameFrame(String title, int fps) {
        super(title);
        initFullScreen();
        gameLoop = new GameLoop(fps, this);
        gameLoop.start();
    }
    
    @Override
    public void start() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        updateGame();
    }

    @Override
    public void render() {
        screenUpdate();
    }

    @Override
    public void finish() {
        restoreScreen();
        System.exit(0);
    }
    
    public void updateGame() {
        // TODO Abstracto
    }
    
    public void renderGame(Graphics gScr) {
        // TODO Abstracto
    }

    private void initFullScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gd = ge.getDefaultScreenDevice();

        setUndecorated(true); // no menu bar, borders, etc. or Swing components
        setIgnoreRepaint(true); // turn off all paint events since doing active rendering
        setResizable(false);

        if (!gd.isFullScreenSupported()) {
            System.out.println("Full-screen exclusive mode not supported");
            System.exit(0);
        }
        gd.setFullScreenWindow(this); // switch on full-screen exclusive mode

        // we can now adjust the display modes, if we wish
        showCurrentMode();

        // setDisplayMode(800, 600, 8); // or try 8 bits
        // setDisplayMode(1280, 1024, 32);

        reportCapabilities();

        width = getBounds().width;
        height = getBounds().height;

        setBufferStrategy();
    }
    
    private void reportCapabilities() {
        GraphicsConfiguration gc = gd.getDefaultConfiguration();

        // Image Capabilities
        ImageCapabilities imageCaps = gc.getImageCapabilities();
        System.out.println("Image Caps. isAccelerated: " + imageCaps.isAccelerated());
        System.out.println("Image Caps. isTrueVolatile: " + imageCaps.isTrueVolatile());

        // Buffer Capabilities
        BufferCapabilities bufferCaps = gc.getBufferCapabilities();
        System.out.println("Buffer Caps. isPageFlipping: " + bufferCaps.isPageFlipping());
        System.out.println("Buffer Caps. Flip Contents: " + getFlipText(bufferCaps.getFlipContents()));
        System.out.println("Buffer Caps. Full-screen Required: " + bufferCaps.isFullScreenRequired());
        System.out.println("Buffer Caps. MultiBuffers: " + bufferCaps.isMultiBufferAvailable());
    }
    
    private String getFlipText(BufferCapabilities.FlipContents flip) {
        if (flip == null) {
            return "false";
        } else if (flip == BufferCapabilities.FlipContents.UNDEFINED) {
            return "Undefined";
        } else if (flip == BufferCapabilities.FlipContents.BACKGROUND) {
            return "Background";
        } else if (flip == BufferCapabilities.FlipContents.PRIOR) {
            return "Prior";
        } else { // if (flip == BufferCapabilities.FlipContents.COPIED)
            return "Copied";
        }
    }

    /**
     * Switch on page flipping: NUM_BUFFERS == 2 so there will be a 'primary
     * surface' and one 'back buffer'.
     * 
     * The use of invokeAndWait() is to avoid a possible deadlock with the event
     * dispatcher thread. Should be fixed in J2SE 1.5
     * 
     * createBufferStrategy) is an asynchronous operation, so sleep a bit so that
     * the getBufferStrategy() call will get the correct details.
     */
    private void setBufferStrategy() {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    createBufferStrategy(NUM_BUFFERS);
                }
            });
        } catch (Exception e) {
            System.out.println("Error while creating buffer strategy");
            System.exit(0);
        }

        try { // sleep to give time for the buffer strategy to be carried out
            Thread.sleep(500); // 0.5 sec
        } catch (InterruptedException ex) {
        }

        bufferStrategy = getBufferStrategy();
    }
    
    /**
     * use active rendering
     */
    private void screenUpdate() {
        try {
            gScr = bufferStrategy.getDrawGraphics();
            renderGame(gScr);
            gScr.dispose();
            if (!bufferStrategy.contentsLost()) {
                bufferStrategy.show();
            } else {
                System.out.println("Contents Lost");
            }
            // Sync the display on some systems.
            // (on Linux, this fixes event queue problems)
            Toolkit.getDefaultToolkit().sync();
        } catch (Exception e) {
            e.printStackTrace();
            gameLoop.stop();
        }
    }

    /**
     * Switch off full screen mode. This also resets the display mode if it's been
     * changed.
     */
    private void restoreScreen() {
        Window w = gd.getFullScreenWindow();
        if (w != null) {
            w.dispose();
        }
        gd.setFullScreenWindow(null);
    }
    
    /**
     * attempt to set the display mode to the given width, height, and bit depth
     */
    private void setDisplayMode(int width, int height, int bitDepth) {
        if (!gd.isDisplayChangeSupported()) {
            System.out.println("Display mode changing not supported");
            return;
        }

        if (!isDisplayModeAvailable(width, height, bitDepth)) {
            System.out.println("Display mode (" + width + "," + height + "," + bitDepth + ") not available");
            return;
        }

        DisplayMode dm = new DisplayMode(width, height, bitDepth, DisplayMode.REFRESH_RATE_UNKNOWN); // any refresh rate
        try {
            gd.setDisplayMode(dm);
            System.out.println("Display mode set to: (" + width + "," + height + "," + bitDepth + ")");
        } catch (IllegalArgumentException e) {
            System.out.println("Error setting Display mode (" + width + "," + height + "," + bitDepth + ")");
        }

        try { // sleep to give time for the display to be changed
            Thread.sleep(1000); // 1 sec
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Check that a displayMode with this width, height, bit depth is available. We
     * don't care about the refresh rate, which is probably REFRESH_RATE_UNKNOWN
     * anyway.
     */
    private boolean isDisplayModeAvailable(int width, int height, int bitDepth) {
        DisplayMode[] modes = gd.getDisplayModes();
        showModes(modes);

        for (int i = 0; i < modes.length; i++) {
            if (width == modes[i].getWidth() && height == modes[i].getHeight() && bitDepth == modes[i].getBitDepth()) {
                return true;
            }
        }
        return false;
    }

    /**
     * pretty print the display mode information in modes
     */
    private void showModes(DisplayMode[] modes) {
        System.out.println("Modes");
        for (int i = 0; i < modes.length; i++) {
            System.out.print("(" + modes[i].getWidth() + "," + modes[i].getHeight() + "," + modes[i].getBitDepth() + ","
                    + modes[i].getRefreshRate() + ")  ");
            if ((i + 1) % 4 == 0) {
                System.out.println();
            }
        }
        System.out.println();
    }

    /**
     * print the display mode details for the graphics device
     */
    private void showCurrentMode() {
        DisplayMode dm = gd.getDisplayMode();
        System.out.println("Current Display Mode: (" + dm.getWidth() + "," + dm.getHeight() + "," + dm.getBitDepth()
                + "," + dm.getRefreshRate() + ")  ");
    }

}
