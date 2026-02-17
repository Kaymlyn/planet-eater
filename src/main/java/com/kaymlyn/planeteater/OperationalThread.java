package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.entities.Specialization;
import com.kaymlyn.planeteater.simulation.physics.Itinerary;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.ScheduledBurn;
import com.kaymlyn.planeteater.simulation.physics.SimpleLauncher;
import com.kaymlyn.planeteater.simulation.physics.TransferPlanner;
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
                new Orbit(PhysicsConstants.AU, 0.02, .05, 0, 2, 1.5, spark.getCentralStar()),
                PlanetPattern.EARTH,
                1.0
        );
        Planet planet_2 = factory.createPlanetFromPattern(null,
                spark.getCentralStar(),
                new Orbit(PhysicsConstants.AU*5, 0.6, .02, 0, 2, 3, spark.getCentralStar()),
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
        spark.placeInEllipticalOrbit(mind,
                spark.getCentralStar(),
                new Orbit(PhysicsConstants.AU*1.5, 0, 0.02,
                        Math.PI/3, 2, .2, spark.getCentralStar()));
        mind.setSystem(spark);

        Spacecraft vehicle = VehicleFactory.createInterplanetaryProbe("Shuttle-1",mind);
        mind.dock(vehicle);

        spark.stepVerlet();
        Itinerary route = new SimpleLauncher().launch(spark.getCurrentTime(), vehicle);
        route.addBurn(new ScheduledBurn("Custom Burn",spark.getCurrentTime() + spark.getTimeStep()* 3, new Vector3D(2000,2000,0), "test burn"));
        route.addBurn(new ScheduledBurn("Custom Burn",spark.getCurrentTime() + spark.getTimeStep()* 2000, new Vector3D(2000,2000,0), "test burn"));
        System.out.println(route.getSummary());

        System.out.println(vehicle.fuelRequired(route.getTotalDeltaV()));
        System.out.println(vehicle.getFuelMass());
        System.out.println(vehicle.fuelRequired(route.getTotalDeltaV()) < vehicle.getFuelMass());
        vehicle.programItinerary(route);
        System.out.println("Positions");
        System.out.println(mind.getPosition());
        System.out.println(mind.getPosition(0));
        System.out.println(mind.getPosition(3600));
    }

    @Override
    public void run() {
        new RenderingThread(spark, (int)(1000 *PhysicsConstants.SECONDS_PER_DAY/3600),24, 4, new Vector3D(0.0,0.0,0.0).multiply(Math.PI/16)).run();

    }
}
