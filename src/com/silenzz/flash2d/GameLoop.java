package com.silenzz.flash2d;

public class GameLoop implements Runnable {
    
    /** 
     * Number of frames with a delay of 0 ms before the 
     * animation thread yields to other running threads
     */ 
    private static final int NO_DELAYS_PER_YIELD = 16;
    
    /** 
     * Number of frames that can be skipped in game loop
     * where the game state is updated but not rendered
     */
    private static final int MAX_FRAME_SKIP = 5;
    
    public static final long MILLISECONDS = 1000;
    public static final long MICROSECONDS = MILLISECONDS * 1000;
    public static final long NANOSECONDS = MICROSECONDS * 1000;
    
    private Thread animator;
    private volatile boolean running;
    private volatile boolean pause;
    private int fps;
    private long period;
    private GameStateListener gameState;
    private GameStats stats;
    
    private boolean finishedOff;
    
    public GameLoop(int fps, GameStateListener gameState) {
        this.fps = fps;
        this.gameState = gameState;
        this.period = NANOSECONDS / fps;
        stats = new GameStats(this);
    }
    
    @Override
    public void run() {
        long beforeTime = 0;
        long afterTime = 0;
        long timeDiff = 0;
        long sleepTime = 0;
        long overSleepTime = 0;
        int noDelays = 0;
        long excess = 0;
        
        beforeTime = System.nanoTime();
        running = true;
        gameState.start();
        
        while (running) {
            gameState.update();
            gameState.render();
            
            afterTime = System.nanoTime();
            timeDiff = afterTime - beforeTime;
            sleepTime = (period - timeDiff) - overSleepTime;
            
            if (sleepTime > 0) {
                sleep(sleepTime / 1_000_000); // nano -> ms
                overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
            } else {
                excess -= sleepTime; // store excess time value
                overSleepTime = 0;
                if (++noDelays >= NO_DELAYS_PER_YIELD) {
                    Thread.yield(); // Give another thread chance to run
                    noDelays = 0;
                }
            }
            
            beforeTime = System.nanoTime();
            
            int skips = 0;
            while((excess > period) && (skips < MAX_FRAME_SKIP)) {
                excess -= period;
                gameState.update();
                skips++;
            }
            
            stats.addFramesSkipped(skips);
            stats.store();
        }
        
        finishOff();
        gameState.finish();
    }
    
    public GameStats getStats() {
        return stats;
    }
    
    /**
     * Tasks to do before terminating. Called at end of run() and via the shutdown
     * hook in readyForTermination().
     * 
     * The call at the end of run() is not really necessary, but included for
     * safety. The flag stops the code being called twice.
     */
    private void finishOff() {
        System.out.println("finishOff");
        if (!finishedOff) {
            finishedOff = true;
            stats.print();
        }
    }
    
    public void start() {
        if (animator != null || running) {
            return;
        }
        animator = new Thread(this);
        animator.start();
    }
    
    public void stop() {
        running = false;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void pause() {
        pause = true;
    }
    
    public void resume() {
        pause = false;
    }
    
    public boolean isPaused() {
        return pause;
    }
    
    public void togglePausing() {
        pause = !pause;
    }
    
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {}
    }
    
    /**
     * Regresa el tiempo en nano segundos
     * utilizando {@code System.nanoTime()}
     * @return long nano segundos
     */
    public long getTime() {
        return System.nanoTime();
    }
    
    public long secondsToMillis(long seconds) {
        return seconds * MILLISECONDS;
    }
    
    public long millisToSeconds(long millis) {
        return millis / MILLISECONDS;
    }
    
    public long millisToNano(long millis) {
        return millis * MICROSECONDS;
    }
    
    public long nanoToMillis(long nano) {
        return nano / MICROSECONDS;
    }

    public int getFps() {
        return fps;
    }

    public long getPeriod() {
        return period;
    }

}
