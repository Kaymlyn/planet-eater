package com.kaymlyn.planeteater;

import java.io.FileNotFoundException;
import com.kaymlyn.planeteater.OperationalThread;

public class Driver {

    public static void main(String... args) throws FileNotFoundException {

        new OperationalThread().run();

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
