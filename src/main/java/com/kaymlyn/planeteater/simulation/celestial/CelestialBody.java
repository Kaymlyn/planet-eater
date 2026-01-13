package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface CelestialBody {
    String getId();
    Vector3D getPosition();

//    Vector3D gravitationalForceOn(Orbiter other);

    double getRadius();
    double getDensity();
    double getVolume();
    double getMass();
    double getAggregatedMass();
    double getCircularOrbitVelocity(double radius);

}
