package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.rendering.OrbitalSystemRenderer;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBody;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.operations.TravelCalculator;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

import java.io.IOException;

public class GameLoopThread implements Runnable {

    private final OrbitalSystem spark;

    public GameLoopThread(OrbitalSystem spark) {
        this.spark = spark;
    }

    @Override
    public void run() {
        try {
            orbitingWithFollowedEntity(8760, "cType0");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void orbitingWithFollowedEntity(int cycles, String followId) throws IOException {
//        spark.getAsteroids().forEach(System.out::println);
        OrbitalSystemRenderer renderer = new OrbitalSystemRenderer(spark);
        double travelled = 0;
        for(int i = 0; i < cycles; i++) {
            renderer.render();
            CelestialBody following = spark.getBody(followId);
            Vector3D start = following.getPosition();
            step();
            Vector3D end = following.getPosition();
            travelled += start.distanceTo(end);
            spark.orbitalPeriod(following.getPosition().distanceTo(Vector3D.ZERO));
            System.out.println(
                    spark.orbitalPeriod(following.getPosition().distanceTo(Vector3D.ZERO))/PhysicsConstants.AU);
//            System.out.printf("%s moved %.3e meters%n",followId,start.distanceTo(end));
//            System.out.printf("%s moved %.3e AUs total%n",followId,travelled/ PhysicsConstants.AU);
//            System.out.println(spark.getBody(followId));
        }
        renderer.renderVideo();
    }

    private OrbitalSystem step() {
        double currentTime = spark.stepVerlet();
        System.out.println("Current day is: " + currentTime/(3600*24));
        return spark;
    }
}
