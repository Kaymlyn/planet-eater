package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.resources.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StarTest {
    @Test
    public void verifyDefaultStarCreation() {
        Star sun = CelestialBodyFactory.createMainSequenceStar("Sol",1.0);

        Assertions.assertAll(
                () -> Assertions.assertEquals(2.147483647e9, (int)sun.getMass()),
                () -> Assertions.assertEquals(1.451969e6, (int)(sun.getTotalComposition().getMass(Material.HYDROGEN)/1e24)),
                () -> Assertions.assertEquals(4.9725e8, (int)Math.round(sun.getTotalComposition().getMass(Material.HELIUM)/1e21)),
                () -> Assertions.assertEquals(1420, (int)(sun.getDensity())),
                () -> Assertions.assertEquals(6.92657191e8,Math.round(sun.getRadius())),
                () -> Assertions.assertEquals(979, (int)(sun.getLuminosity()*1000.0)),
                () -> Assertions.assertEquals("G", sun.getSpectralClass()),
                () -> Assertions.assertEquals(1.976071e6, (int)(sun.calculateMassLossRate()/1e3)),
                () -> Assertions.assertEquals(8.1226632e7, Math.round(sun.circularOrbitVelocity(20000))),
                () -> Assertions.assertEquals(5.7436e4,Math.round(sun.circularOrbitVelocity(4.0e10))),
                () -> Assertions.assertEquals(366, Math.round(sun.orbitalPeriod(PhysicsConstants.AU)/PhysicsConstants.SECONDS_PER_DAY))
        );

    }
}
