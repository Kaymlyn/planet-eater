package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.OrbitInitializer;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;

import java.io.FileNotFoundException;

public class Driver {

    public static void main(String... args) throws FileNotFoundException {

        CelestialBodyFactory factory = new CelestialBodyFactory(CelestialBodyFactory.createMainSequenceStar("Sol2", 1),28800);
        OrbitInitializer init = new OrbitInitializer(1,0,0,0,0,0);
        Planet planet = factory.createArbitraryPlanet("Earth2", init,
                factory.getProfiles().get("HABITABLE_CORE"),120000
                ,null,0,null,0,null,0);

        System.out.println(planet.getMass());



//        List<Asteroid> coreBelt = createRandomAsteroidBelt("CORE", 1000,10000, 100000, 0);
//        spark.placeAllInEllipticalOrbits(coreBelt,2,5, Math.PI/8,0.5);
//
//        Spacecraft spacecraft = VehicleFactory.createMiningVessel("Origin");
//        System.out.println("   " + spacecraft);
//        System.out.println("   Construction materials:");
//        for (var entry : spacecraft.getConstruction().getMaterials().entrySet()) {
//            System.out.println("      " + entry.getKey() + ": " +
//                    String.format("%.1f kg", entry.getValue()));
//        }
//
//        new OperationalThread(spark).run();
////        new RenderingThread(spark,480,3).run();
//        System.out.println("done");
    }
}
