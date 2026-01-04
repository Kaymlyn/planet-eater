package com.kaymlyn.planeteater.primordials;

import com.kaymlyn.planeteater.physics.OrbitalSystem;
import com.kaymlyn.planeteater.celestial.Star;
import com.kaymlyn.planeteater.resources.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OrbitalSystemTest {
    @Test
    public void verifyDefaultStar() {
        Star sun = Star.createSunLike("Sol");

        Assertions.assertEquals(1.977066e+30, sun.getMass());
        Assertions.assertEquals(1.45197e+30, sun.getComposition().getMass(Material.HYDROGEN));
        Assertions.assertEquals(4.9725e+29, sun.getComposition().getMass(Material.HELIUM));
        Assertions.assertEquals(80.67560353951733, sun.getDensity());
        Assertions.assertEquals(697926560,Math.round(sun.getRadius()));
    }

    @Test
    public void verifyOrbitalSystemInitialization() {
        Star sun = Star.createSunLike("Sol");
        double timeStep = 3600.0; // 1 hour time steps
        OrbitalSystem system = new OrbitalSystem(sun, timeStep);
        Assertions.assertEquals(81226632, Math.round(system.circularOrbitVelocity(20000)));
        Assertions.assertEquals(57436,Math.round(system.circularOrbitVelocity(4.0e+10)));
        Assertions.assertEquals(365, Math.round(system.orbitalPeriod(1.50e+8/2)));
    }
}
