package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.rendering.OrbitalSystemRenderer;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;

import java.io.IOException;

public class RenderingThread implements Runnable {

    private final OrbitalSystem spark;

    public RenderingThread(OrbitalSystem spark) {
        this.spark = spark;
    }

    @Override
    public void run() {
        try {
            orbitingWithFollowedEntity(240 + 1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void orbitingWithFollowedEntity(int cycles) throws IOException {
        OrbitalSystemRenderer renderer = new OrbitalSystemRenderer(spark);
        for(int i = 0; i < cycles; i++) {
            renderer.render(true, 5);
            spark.stepVerlet();
        }
        renderer.renderVideoFromImages();
    }
}
