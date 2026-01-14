package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface Gravitational {
    double getMass();
    Vector3D getVelocity();
    double getRadius();
}
