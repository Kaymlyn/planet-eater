package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.Asteroid;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Material;

import java.util.List;
import java.util.Random;

public class Driver {

    public static void main(String... args) {

        Random rand = new Random(0L);

        OrbitalSystem spark = new OrbitalSystem(CelestialBodyFactory.createMainSequenceStar("Sol2", 1),3600);

        List<Asteroid> coreBelt = CelestialBodyFactory.createRandomAsteroidBelt("CORE", 1000,10000, 100000, 0);
        spark.placeAllInCircularOrbits(coreBelt,2,5);

        new RenderingThread(spark).run();
        System.out.println("done");
    }


}
