package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.rendering.OrbitalSystemRenderer;
import com.kaymlyn.planeteater.rendering.OrbitalSystemRendererOptimized;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

import java.io.IOException;
import java.util.Date;

public class RenderingThread implements Runnable {

    private final OrbitalSystem spark;
    private final int cycles;
    private final int stepOver;
    private final double visibleAU;
    private final Vector3D rotate;

    public RenderingThread(OrbitalSystem spark, int cycles, int stepOver, double visibleAU, Vector3D rotate) {
        this.spark = spark;
        this.cycles = cycles;
        this.stepOver = stepOver;
        this.visibleAU = visibleAU;
        this.rotate = rotate.multiply(Math.PI);
    }

    @Override
    public void run() {
        try {
            renderOrbiting(cycles,stepOver,visibleAU, rotate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderOrbiting(int cycles, int stepOver, double visibleAU, Vector3D rotate) throws IOException {
        int skip = Math.max(1, stepOver);
        System.out.printf("Simulating %d cycles with %d stepOver between frames rendered.%n",cycles,skip);
        OrbitalSystemRendererOptimized renderer = new OrbitalSystemRendererOptimized(spark,visibleAU,true);
        renderer.initializeVideoEncoder("orbits/output.mp4");
//        OrbitalSystemRenderer renderer = new OrbitalSystemRenderer(spark,visibleAU);
        for(int i = 0; i < cycles; i++) {
            if(i%skip == 0) {
                renderer.render(visibleAU, rotate, i);
            }
            spark.stepVerlet();
        }
        renderer.finalizeVideo();
    }
}
