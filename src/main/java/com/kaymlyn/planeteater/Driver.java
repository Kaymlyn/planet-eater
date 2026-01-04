package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.Asteroid;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

import java.util.Random;

public class Driver {

    public static void main(String... args) {

        //locking seed for repeatability
        Random rand = new Random();

        OrbitalSystem spark = new OrbitalSystem(CelestialBodyFactory.createMainSequenceStar("Sol2", 1),3600);
        //Create 100 asteroids with sizes between 80 and 130 meters and place them in orbits between .8 and 1.6 AUs at random angles.
        for(int i = 0; i< 100; i++) {
            Asteroid asteroid = CelestialBodyFactory.createCType("cType" + i, Vector3D.ZERO,Vector3D.ZERO, 80 + rand.nextDouble()*50);
            double location = (.8+(rand.nextDouble())*.8) ;
            System.out.println("Distance in AUs " + location);
            System.out.println("Orbital Period in Days " + spark.orbitalPeriod(location*PhysicsConstants.AU)/PhysicsConstants.SECONDS_PER_DAY);
            spark.placeInCircularOrbit(asteroid,location * PhysicsConstants.AU, rand.nextDouble()*2*Math.PI);
        }
        new GameLoopThread(spark).run();
        System.out.println("done");
    }


}
