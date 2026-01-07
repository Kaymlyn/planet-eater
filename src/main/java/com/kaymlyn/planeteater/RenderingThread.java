package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.rendering.OrbitalSystemRenderer;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;

import java.io.IOException;
import java.util.Date;

public class RenderingThread implements Runnable {

    private final OrbitalSystem spark;
    private final int cycles;
    private final int stepOver;

    public RenderingThread(OrbitalSystem spark, int cycles, int stepOver) {
        this.spark = spark;
        this.cycles = cycles;
        this.stepOver = stepOver;
    }

    @Override
    public void run() {
        try {
            renderOrbiting(cycles,stepOver);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderOrbiting(int cycles, int stepOver) throws IOException {
        System.out.printf("Simulating %d cycles with %d stepOver between frames rendered.%n",cycles,stepOver);
        OrbitalSystemRenderer renderer = new OrbitalSystemRenderer(spark);
        for(int i = 0; i < cycles; i++) {
            Date start = new Date();
            if(i%stepOver == 0) {
                System.out.printf("Rendering cycle %d as Frame-%d%n",i,i/stepOver);
                renderer.render(true, 5);
                System.out.println("Image Rendering took " + (new Date().getTime() - start.getTime()) + " milliseconds");
            }
            spark.stepVerlet();
        }
        renderer.renderVideoFromImages(.5);
    }
}
