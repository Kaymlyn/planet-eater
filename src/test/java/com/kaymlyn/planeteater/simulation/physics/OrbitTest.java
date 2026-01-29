package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import org.junit.jupiter.api.Test;

public class OrbitTest {

    @Test
    public void test () {
        CelestialBodyFactory factory = new CelestialBodyFactory(CelestialBodyFactory.createMainSequenceStar(null, 1),3600);
        OrbitalSystem spark = factory.getSystem();
        Orbit configOrbit =  new Orbit(PhysicsConstants.AU, 0.02, .05, 0, 2, 0, spark.getCentralStar());

        Planet planet_1 = factory.createPlanetFromPattern(
                null,
                spark.getCentralStar(),
                configOrbit,
                PlanetPattern.EARTH,
                1.0
        );

        Orbit orbit = planet_1.getInitialOrbit();
        OrbitalState state = new OrbitalState(planet_1.getPosition(),planet_1.getVelocity(), orbit);

        Orbit statefulOrbit = state.orbitalElements();

        System.out.println(configOrbit);
        System.out.println(orbit);
        System.out.println(statefulOrbit);
    }
}
