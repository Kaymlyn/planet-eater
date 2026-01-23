package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface Orbiter {
    String getId();

    Vector3D getPosition();

    Vector3D getVelocity();

    Gravitational getParentBody();

    OrbitalSystem getSystem();

    Orbit calculateCurrentOrbit();

    void setPosition(Vector3D position);

    void setVelocity(Vector3D velocity);

    double getMass();

    void update(Vector3D vector3D, double timeStep);

    void setParentBody(Gravitational parentBody);
}
