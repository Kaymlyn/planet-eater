package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface Body {


    String getId();

    Vector3D getVelocity();

    Vector3D getPosition();
}
