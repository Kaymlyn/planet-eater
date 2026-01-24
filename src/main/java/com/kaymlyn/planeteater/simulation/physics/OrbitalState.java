package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;

public record OrbitalState(
    Vector3D position,
    Vector3D velocity,
    Orbit orbitalElements
) {
    public double angularMomentum() {
        return position.cross(velocity).magnitude();
    }
}
