package com.silenzz.flash2d;

import java.text.DecimalFormat;

public class GameStats {
    
    private GameLoop gameLoop;
    
    private long statsInterval; // in ns
    private long prevStatsTime;
    private long totalElapsedTime;
    private long gameStartTime;
    private int timeSpentInGame; // in seconds

    private long frameCount;
    private double[] fpsStore;
    private long statsCount;
    private double averageFPS;

    private long framesSkipped;
    private long totalFramesSkipped;
    private double[] upsStore;
    private double averageUPS;

    private DecimalFormat df = new DecimalFormat("#,#00"); // 2 dp
    private DecimalFormat timedf = new DecimalFormat("0.####"); // 4 dp
    
    public GameStats(GameLoop gameLoop) {
        this.gameLoop = gameLoop;
        fpsStore = new double[gameLoop.getFps()];
        upsStore = new double[gameLoop.getFps()];
        for (int i = 0; i < gameLoop.getFps(); i++) {
            fpsStore[i] = 0.0;
            upsStore[i] = 0.0;
        }
        gameStartTime = gameLoop.getTime();
    }
    
    /**
     * The statistics: - the summed periods for all the iterations in this interval
     * (period is the amount of time a single frame iteration should take), the
     * actual elapsed time in this interval, the error between these two numbers;
     * 
     * - the total frame count, which is the total number of calls to run();
     * 
     * - the frames skipped in this interval, the total number of frames skipped. A
     * frame skip is a game update without a corresponding render;
     * 
     * - the FPS (frames/sec) and UPS (updates/sec) for this interval, the average
     * FPS & UPS over the last NUM_FPSs intervals.
     * 
     * The data is collected every MAX_STATS_INTERVAL (1 sec).
     */
    public void store() {
        frameCount++;
        statsInterval += gameLoop.getPeriod();

        if (statsInterval >= GameLoop.NANOSECONDS) { // record stats every MAX_STATS_INTERVAL
            long timeNow = gameLoop.getTime();
            timeSpentInGame = (int) ((timeNow - gameStartTime) / 1000000000L); // ns --> secs

            long realElapsedTime = timeNow - prevStatsTime; // time since last stats collection
            totalElapsedTime += realElapsedTime;

            double timingError = ((double) (realElapsedTime - statsInterval) / statsInterval) * 100.0;

            totalFramesSkipped += framesSkipped;

            double actualFPS = 0; // calculate the latest FPS and UPS
            double actualUPS = 0;
            if (totalElapsedTime > 0) {
                double elapsedTimeNano = totalElapsedTime - gameStartTime;
                
                actualFPS = (((double) frameCount / elapsedTimeNano) * 1000000000L);
                actualUPS = (((double) (frameCount + totalFramesSkipped) / elapsedTimeNano) * 1000000000L);
            }

            // store the latest FPS and UPS
            fpsStore[(int) statsCount % gameLoop.getFps()] = actualFPS;
            upsStore[(int) statsCount % gameLoop.getFps()] = actualUPS;
            statsCount = statsCount + 1;
            
            double countFps = statsCount < gameLoop.getFps() ? 
                    statsCount : gameLoop.getFps();

            double totalFPS = 0.0; // total the stored FPSs and UPSs
            double totalUPS = 0.0;
            
            for (int i = 0; i < countFps; i++) {
                totalFPS += fpsStore[i];
                totalUPS += upsStore[i];
            }

            averageFPS = totalFPS / countFps;
            averageUPS = totalUPS / countFps;
            
            /*
             * System.out.println(timedf.format( (double) statsInterval/1000000000L) + " " +
             * timedf.format((double) realElapsedTime/1000000000L) + "s " +
             * df.format(timingError) + "% " + frameCount + "c " + framesSkipped + "/" +
             * totalFramesSkipped + " skip; " + df.format(actualFPS) + " " +
             * df.format(averageFPS) + " afps; " + df.format(actualUPS) + " " +
             * df.format(averageUPS) + " aups" );
             */
            
            framesSkipped = 0;
            prevStatsTime = timeNow;
            statsInterval = 0L; // reset
        }
    }
    
    public void print() {
        System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
        System.out.println("Average FPS: " + df.format(averageFPS));
        System.out.println("Average UPS: " + df.format(averageUPS));
        System.out.println("Time Spent: " + timeSpentInGame + " secs");
    }
    
    public void addFramesSkipped(int skips) {
        framesSkipped += skips;
    }
    
    public String getFrameCountLabel() {
        return "Frame Count " + frameCount;
    }
    
    public String getAverageLabel() {
        return "Average FPS/UPS: " + df.format(averageFPS) + " / " + df.format(averageUPS);
    }
    
    public String getTimeSpentLabel() {
        return "Time Spent: " + timeSpentInGame + " secs";
    }

    public int getTimeSpentInGame() {
        return timeSpentInGame;
    }

}
