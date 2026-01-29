package com.kaymlyn.planeteater.simulation.physics;

public record OrbitalState(
    Vector3D position,
    Vector3D velocity,
    Orbit orbitalElements
) {
    public double angularMomentum() {
        return position.cross(velocity).magnitude();
    }
}
