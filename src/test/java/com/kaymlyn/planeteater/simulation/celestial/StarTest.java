package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.resources.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StarTest {
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
        Assertions.assertEquals(81226632, Math.round(sun.circularOrbitVelocity(20000)));
        Assertions.assertEquals(57436,Math.round(sun.circularOrbitVelocity(4.0e+10)));
        Assertions.assertEquals(366, Math.round(sun.orbitalPeriod(PhysicsConstants.AU)/PhysicsConstants.SECONDS_PER_DAY));
    }
}
