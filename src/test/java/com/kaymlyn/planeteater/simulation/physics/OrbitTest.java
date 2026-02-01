package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrbitTest {

    private OrbitalSystem spark;
    private CelestialBodyFactory factory;
    private Orbit configOrbit;
    private Planet planet_1;

    @BeforeEach
    public void setup() {
        factory = new CelestialBodyFactory(CelestialBodyFactory.createMainSequenceStar(null, 1),3600);
        spark = factory.getSystem();
        configOrbit =  new Orbit(PhysicsConstants.AU, 0.02, .05, 0, 2, 0, spark.getCentralStar(),0.0);

        planet_1 = factory.createPlanetFromPattern(
                null,
                spark.getCentralStar(),
                configOrbit,
                PlanetPattern.EARTH,
                1.0
        );
    }

    @Test
    public void orbitalStateConversion() {

        Orbit orbit = planet_1.getInitialOrbit();
        OrbitalState state = new OrbitalState(planet_1.getPosition(),planet_1.getVelocity(), orbit);

        Orbit statefulOrbit = state.orbitalElements();

        Assertions.assertAll(
                () -> Assertions.assertEquals(orbit,statefulOrbit)
        );
    }

    @Test
    public void orbitalDriftFromIdealized() {

        spark.stepVerlet();

        Orbit orbit = planet_1.calculateCurrentOrbit();
        OrbitalState state = orbit.calculateOrbitalState();
        Orbit statefulOrbit = state.orbitalElements();
        OrbitalState expectedState =configOrbit.calculateOrbitAfterT0(spark.getCurrentTime());
        Assertions.assertAll(
                () -> Assertions.assertNotEquals(orbit,statefulOrbit),
                () -> Assertions.assertTrue(
                        Math.abs(expectedState.orbitalElements().trueAnomaly() - state.orbitalElements().trueAnomaly()) < .001
                )
        );
    }

    @Test
    public void orbitalDriftOverLongPeriods() {

        for(int i = 0; i < 15; i++) spark.stepVerlet();

        Orbit orbit = planet_1.calculateCurrentOrbit();
        OrbitalState state = orbit.calculateOrbitalState();
        Orbit statefulOrbit = state.orbitalElements();
        OrbitalState expectedState = configOrbit.calculateOrbitAfterT0(spark.getCurrentTime());
        Assertions.assertAll(
                () -> Assertions.assertNotEquals(orbit,statefulOrbit),
                () -> Assertions.assertTrue(
                        Math.abs(expectedState.orbitalElements().trueAnomaly() - state.orbitalElements().trueAnomaly()) < .1
                )
        );
    }
}
