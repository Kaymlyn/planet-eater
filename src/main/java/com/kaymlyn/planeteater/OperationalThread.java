package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.physics.ManeuverDetails;
import com.kaymlyn.planeteater.simulation.physics.TransferOptimizer;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.entities.Specialization;
import com.kaymlyn.planeteater.simulation.physics.Itinerary;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
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
        List<Planet> asteroids = factory.createRandomAsteroidBelt(
                "Main-Belt",
                108,
                1e5,
                1e6,
                2,
                5,
                spark.getCentralStar(),
                23L
        );
        Planet planet_1 = factory.createPlanetFromPattern(
                null,
                spark.getCentralStar(),
                new Orbit(PhysicsConstants.AU, 0.02, .05, 0, 2, 1.5, spark.getCentralStar(), 0.0),
                PlanetPattern.EARTH,
                1.0
        );
        Planet planet_2 = factory.createPlanetFromPattern(null,
                spark.getCentralStar(),
                new Orbit(PhysicsConstants.AU*5, 0.6, .02, 0, 2, 3, spark.getCentralStar(), 0.0),
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

        CentralMind mind =  factory.createCentralMind(PhysicsConstants.AU);
        Orbit orbit = spark.placeInEllipticalOrbit(mind,
                spark.getCentralStar(),
                new Orbit(PhysicsConstants.AU*1.5, 0, 0.02,
                        Math.PI/3, 2, .2, spark.getCentralStar(), 0.0));
        System.out.println("Orbit post placement : " + orbit);
        System.out.println("Init orbit according to Mind : " + mind.getInitialOrbit());
        System.out.println("Calc orbit according to Mind : " + mind.calculateCurrentOrbit());
        mind.setSystem(spark);

        Spacecraft vehicle = VehicleFactory.createCargoShuttle("Shuttle-1",mind);
        mind.dock(vehicle);

        Itinerary route = vehicle.planRoute(planet_1, true, spark.getCurrentTime(), TransferOptimizer.OptimizationGoal.MINIMUM_DELTAV );

        System.out.println(route.getSummary());

        System.out.println(" Time: " + spark.getCurrentTime() + " " + mind.calculateCurrentOrbit().calculateOrbitalState().position() + " " + mind.getPosition());
        ManeuverDetails details = route.getInitialManeuver();
        while(details != null) {
            System.out.println(details.getStartingPosition() + " " + details.getEndingPosition() + " " + details.getId());
            details = details.getNext();
        }

        System.out.println(vehicle.fuelRequired(route.getTravelDeltaV()));
        System.out.println(vehicle.getFuelMass());
        System.out.println(vehicle.fuelRequired(route.getTravelDeltaV()) < vehicle.getFuelMass());
        vehicle.setItinerary(route);
        vehicle.programItinerary(route);
        System.out.println("Travel Time : " + route.getTotalFlightTime()/PhysicsConstants.SECONDS_PER_DAY);
        System.out.println("Total Fuel : " + vehicle.getFuelMass());
        System.out.println("Travel Fuel : " + vehicle.fuelRequired(route.getTravelDeltaV()));

    }

    @Override
    public void run() {
        new RenderingThread(spark, (int)(780*PhysicsConstants.SECONDS_PER_DAY/3600),8, 4, new Vector3D(.9,0,0).multiply(Math.PI)).run();

    }
}
