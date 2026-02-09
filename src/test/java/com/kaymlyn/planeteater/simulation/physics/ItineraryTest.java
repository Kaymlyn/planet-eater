package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import com.kaymlyn.planeteater.simulation.vehicles.VehicleFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ItineraryTest {

    CelestialBodyFactory factory;

    OrbitalSystem spark;

    private Planet earth;
    private Planet venus;
    private Spacecraft shuttle;

    @BeforeEach
    public void setup() {
        factory = new CelestialBodyFactory(
                CelestialBodyFactory.createMainSequenceStar("Sol", 1),3600
        );
        spark = factory.getSystem();
        earth = factory.createPlanetFromPattern("Earf",factory.getCentralStar(),new Orbit(PhysicsConstants.AU,0.0,0.0, 0.0, 0.0,0.0, factory.getCentralStar()), PlanetPattern.EARTH, 1);
        venus = factory.createPlanetFromPattern("Venas", factory.getCentralStar(), new Orbit(PhysicsConstants.AU*.723,0.0,0.0, 0.0, 0.0,0.0, factory.getCentralStar()),PlanetPattern.VENUS, 1);

        shuttle = VehicleFactory.createCargoShuttle("Test-1", earth);
    }

    @Test
    public void motionIsTrackedProperly() {
        spark.stepVerlet();
        shuttle.programItinerary(
                shuttle.planRoute(
                        venus,
                        false,
                        spark.getTimeStep()*4,
                        TransferPlanner.OptimizationGoal.MINIMUM_DELTAV)
        );

        Vector3D earthPositionStart = earth.getPosition();
        Vector3D shuttlePositionStart = shuttle.getPosition();
        for(int i = 0; i < 4; i++) spark.stepVerlet();

        Vector3D earthPositionPlus4 = earth.getPosition();
        Vector3D shuttlePositionPlus4 = shuttle.getPosition();

        spark.stepVerlet();

        Vector3D earthPositionPlus5 = earth.getPosition();
        Vector3D shuttlePositionPlus5 = shuttle.getPosition();
        System.out.println("Earf at: " + earthPositionPlus5);
        System.out.println("Shuttle at : " + shuttlePositionPlus5);

        Assertions.assertAll(
                () -> assertEquals(Vector3D.ZERO,earthPositionStart.subtract(shuttlePositionStart), "Expect Shuttle to start on Earf"),
                () -> assertEquals(Vector3D.ZERO,earthPositionPlus4.subtract(shuttlePositionPlus4), "Expect Shuttle to travel with Earf"),
                () -> assertNotEquals(Vector3D.ZERO,earthPositionPlus5.subtract(shuttlePositionPlus5), "Expect Shuttle to no longer be on Earf"),
                () -> assertEquals(earth.getStandardOrbitalRadius(), shuttlePositionPlus5.distanceTo(earthPositionPlus5), "")
        );

    }

    @Test
    public void testEarthToVenusTransfer() {
        // Setup
        OrbitalSystem system = factory.getCentralStar().getSystem();

        // Plan transfer
        Orbit earthOrbit = earth.snapshotOrbit();
        Orbit venusOrbit = venus.snapshotOrbit();

        double startTime = system.getCurrentTime() + 3600; // Launch in 1 hour
        Itinerary itinerary = TransferPlanner.buildHohmannTransfer(
                shuttle, earthOrbit, venusOrbit, earth, venus, false, startTime
        );

        shuttle.programItinerary(itinerary);

        // Simulate
        while (!itinerary.isComplete(system.getCurrentTime())) {
            system.stepVerlet();
        }

        // Verify arrival
        double distanceToVenus = shuttle.getPosition()
                .distanceTo(venus.getPosition());

        assertTrue(distanceToVenus < venus.getStandardOrbitalRadius() * 1.2, distanceToVenus + "m is not less than " + venus.getRadius()*1.2);
    }

}
