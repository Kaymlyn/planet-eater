package com.kaymlyn.planeteater.simulation.physics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RocketryCalculatorTest {

    @Test
    public void lambertTest () {
        double time = 3000;
        Vector3D x = new Vector3D(100,100);
        Vector3D y = new Vector3D(-100, -100);
        Vector3D velocity  = RocketryCalculator.calculateLambertVelocity(x,y,time);
        Vector3D velocity2 = RocketryCalculator.calculateLambertVelocity(x,y,time*.5);
        Vector3D expected = new Vector3D(-100,100);
        Assertions.assertAll(
                () -> Assertions.assertEquals(expected.magnitude(), velocity.magnitude()),
                () -> Assertions.assertEquals(expected.magnitude(), velocity2.magnitude()),
                () -> Assertions.assertEquals(velocity, velocity2),
                () -> Assertions.assertEquals(time/velocity.magnitude(), time*.5/velocity2.magnitude())
        );
    }
}
