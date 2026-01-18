package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.rendering.OrbitalSystemRenderer;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;

import java.io.IOException;
import java.util.Date;

public class RenderingThread implements Runnable {

    private final OrbitalSystem spark;
    private final int cycles;
    private final int stepOver;
    private final double visibleAU;

    public RenderingThread(OrbitalSystem spark, int cycles, int stepOver, double visibleAU) {
        this.spark = spark;
        this.cycles = cycles;
        this.stepOver = stepOver;
        this.visibleAU = visibleAU;
    }

    @Override
    public void run() {
        try {
            renderOrbiting(cycles,stepOver,visibleAU);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderOrbiting(int cycles, int stepOver, double visibleAU) throws IOException {
        int skip = Math.max(1, stepOver);
        System.out.printf("Simulating %d cycles with %d stepOver between frames rendered.%n",cycles,skip);
        OrbitalSystemRenderer renderer = new OrbitalSystemRenderer(spark,visibleAU);
        for(int i = 0; i < cycles; i++) {
            Date start = new Date();
            if(i%skip == 0) {
                System.out.printf("Rendering cycle %d as Frame-%d%n",i,i/skip);
                renderer.render(true, visibleAU);
                System.out.println("Image Rendering took " + (new Date().getTime() - start.getTime()) + " milliseconds");
            }
            spark.stepVerlet();
        }
        renderer.renderVideoFromImages(1);
    }
}
