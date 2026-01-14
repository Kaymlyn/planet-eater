package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface CelestialBody extends Gravitational {
    String getId();
    Vector3D getPosition();

    double getDensity();
    double getVolume();
    double getAggregatedMass();
    double getCircularOrbitVelocity(double radius);

}
