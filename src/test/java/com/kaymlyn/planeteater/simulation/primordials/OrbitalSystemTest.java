package com.kaymlyn.planeteater.simulation.primordials;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Star;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.resources.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OrbitalSystemTest {
    @Test
    public void verifyDefaultStar() {
        Star sun = CelestialBodyFactory.createMainSequenceStar("Sol",1);

        Assertions.assertEquals(1.977066e+30, sun.getMass());
        Assertions.assertEquals(1.45197e+30, sun.getTotalComposition().getMass(Material.HYDROGEN));
        Assertions.assertEquals(4.9725e+29, sun.getTotalComposition().getMass(Material.HELIUM));
        Assertions.assertEquals(1420, Math.round(sun.getDensity()));
        Assertions.assertEquals(692657191,Math.round(sun.getRadius()));
    }

    @Test
    public void verifyOrbitalSystemInitialization() {
        Star sun = CelestialBodyFactory.createMainSequenceStar("Sol", 1);
        double timeStep = 3600.0; // 1 hour time steps
        OrbitalSystem system = new OrbitalSystem(sun, timeStep);
        Assertions.assertEquals(81226632, Math.round(system.circularOrbitVelocity(20000)));
        Assertions.assertEquals(57436,Math.round(system.circularOrbitVelocity(4.0e+10)));
        Assertions.assertEquals(366, Math.round(system.orbitalPeriod(PhysicsConstants.AU)/PhysicsConstants.SECONDS_PER_DAY));
    }
}
