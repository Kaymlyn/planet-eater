package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface CelestialBody extends Gravitational {
    String getId();

    double getDensity();
    double getVolume();
    double getAggregatedMass();

}
