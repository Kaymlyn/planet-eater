package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.OrbitInitializer;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.entities.Specialization;
import com.kaymlyn.planeteater.simulation.physics.Itinerary;
import com.kaymlyn.planeteater.simulation.physics.Trajectory;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import com.kaymlyn.planeteater.simulation.vehicles.CentralMind;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import com.kaymlyn.planeteater.simulation.vehicles.VehicleFactory;

import java.util.List;

public class OperationalThread implements Runnable {

    private final OrbitalSystem spark;

    public OperationalThread() {
        CelestialBodyFactory factory = new CelestialBodyFactory(CelestialBodyFactory.createMainSequenceStar(null, 1),3600);
        this.spark = factory.getSystem();
        factory.createRandomAsteroidBelt(
                "Main-Belt",
                108,
                1e5,
                1e6,
                2,
                5,23L
        );
        Planet planet_1 = factory.createPlanetFromPattern(
                null,
                spark.getCentralStar(),
                new OrbitInitializer(PhysicsConstants.AU, 0.02, .05, 0, 2, 3),
                PlanetPattern.EARTH,
                1.0
        );
        Planet planet_2 = factory.createPlanetFromPattern(null,
                spark.getCentralStar(),
                new OrbitInitializer(PhysicsConstants.AU*5, 0.6, 1.3, 0, 2, 3),
                PlanetPattern.VENUS,
                1.0
        );
        List<Automaton> crew = List.of(
                Automaton.createHuman("Alice", Specialization.EXTRACTION),
                Automaton.createHuman("Bob", Specialization.OPERATION),
                Automaton.createHuman("Carol", Specialization.RESEARCH),
                Automaton.createHuman("David",Specialization.GENERALIST),
                Automaton.createRobot("Alex", Specialization.EXTRACTION),
                Automaton.createHuman("Brett", Specialization.OPERATION),
                Automaton.createHuman("Chris", Specialization.RESEARCH),
                Automaton.createHuman("Danny",Specialization.GENERALIST)
        );
        Composition composition = new Composition();
        composition.addMaterialAsRawMass(Material.IRON, 1000)
                .addMaterialAsRawMass(Material.OXYGEN_GAS, 200)
                .addMaterialAsRawMass(Material.NITROGEN_GAS, 300)
                .addMaterialAsRawMass(Material.TRACE_ELEMENTS, 100)
                .addMaterialAsRawMass(Material.HYDROGEN,400)
                .addMaterialAsRawMass(Material.ALUMINUM, 200)
                .addMaterialAsRawMass(Material.CARBON, 100)
                .addMaterialAsRawMass(Material.SILICA, 1000)
                .addMaterialAsRawMass(Material.TITANIUM_OXIDE, 200);

        CentralMind mind =  new CentralMind("KHI Central Mind",crew,composition);
        spark.placeInEllipticalOrbit(mind,
                spark.getCentralStar(),
                new OrbitInitializer(PhysicsConstants.AU, 0, 0.03,
                        Math.PI/3, 2, 3));

        Spacecraft vehicle = VehicleFactory.createCargoShuttle("Shuttle-1",mind);
        vehicle.setLocation(mind);
        Itinerary route = vehicle.planRoute(planet_1, true);
//        System.out.println(route);
//        System.out.println(route.getTotalFuelRequirement());
//        System.out.println(vehicle.getFuelMass());
//        System.out.println(route.getTotalFuelRequirement() < vehicle.getFuelMass());
        vehicle.setSystem(spark);
        vehicle.setItinerary(route);
        vehicle.launch(route);
        System.out.println("Travel Time : " + route.getTotalFlightTime()/PhysicsConstants.SECONDS_PER_DAY);
        System.out.println("Total Fuel : " + vehicle.getFuelMass());
        System.out.println("Travel Fuel : " + route.getTotalFuelRequirement());

    }

    @Override
    public void run() {
//        try {
//            new OrbitalSystemRenderer(spark).renderVideoFromImages(1);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        new RenderingThread(spark, 2400, 10, 1).run();

    }
}
