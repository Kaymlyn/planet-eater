package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.Asteroid;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;

import java.util.List;

public class Driver {

    public static void main(String... args) {

        OrbitalSystem spark = new OrbitalSystem(CelestialBodyFactory.createMainSequenceStar("Sol2", 1),28800);

        List<Asteroid> coreBelt = CelestialBodyFactory.createRandomAsteroidBelt("CORE", 1000,10000, 100000, 0);
        spark.placeAllInEllipticalOrbits(coreBelt,2,5, Math.PI/8,0.5);

        new RenderingThread(spark,480,3).run();
        System.out.println("done");
    }
}
