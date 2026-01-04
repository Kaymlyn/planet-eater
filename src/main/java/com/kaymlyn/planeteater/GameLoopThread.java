package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBody;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public class GameLoopThread implements Runnable {

    private final OrbitalSystem spark;

    public GameLoopThread() {
        this(new OrbitalSystem(CelestialBodyFactory.createMainSequenceStar("Sol2", 1),3600));
    }

    public GameLoopThread(OrbitalSystem spark) {
        this.spark = spark;
    }

    @Override
    public void run() {
        orbitingWithFollowedEntity(1000, "cType0");
    }

    private void orbitingWithFollowedEntity(int cycles, String followId) {
        spark.getAsteroids().forEach(System.out::println);
        for(int i = 0; i < cycles; i++) {
            CelestialBody following = spark.getBody(followId);
            Vector3D start = following.getPosition();
            step();
            Vector3D end = following.getPosition();
            System.out.printf("%s moved %.3e meters%n",followId,start.distanceTo(end));
            System.out.println(spark.getBody(followId));
            System.out.println(spark.getCurrentTime());
        }
    }

    private OrbitalSystem step() {
        double currentTime = spark.stepVerlet();
        System.out.println("Current time is: " + currentTime);
        return spark;
    }
}
