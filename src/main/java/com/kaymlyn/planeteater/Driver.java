package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.Asteroid;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

import java.util.Random;

public class Driver {

    public static void main(String... args) {

        Random rand = new Random(123456L);

        OrbitalSystem spark = new OrbitalSystem(CelestialBodyFactory.createMainSequenceStar("Sol2", 1),3600);
        for(int i = 0; i< 100; i++) {
            Asteroid asteroid = CelestialBodyFactory.createCType("cType" + i, Vector3D.ZERO,Vector3D.ZERO, 80 + rand.nextDouble()*50);
            spark.placeInCircularOrbit(asteroid,(.8+(rand.nextDouble())*.8)* PhysicsConstants.AU, rand.nextDouble()*2*Math.PI);
        }
        new GameLoopThread(spark).run();
        System.out.println("done");
    }


}
