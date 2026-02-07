package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface Body {

    default Vector3D getPosition(double tPlus) {
        return snapshotOrbit().stateAt(getSystem().getCurrentTime(),tPlus).position();
    }

    String getId();

    Vector3D getVelocity();

    Vector3D getPosition();

    Orbit snapshotOrbit();

    OrbitalSystem getSystem();
}
