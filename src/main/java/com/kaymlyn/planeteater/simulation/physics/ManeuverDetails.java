package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ManeuverDetails {
    private final Vector3D startPosition;
    private final Vector3D startingVelocity;
    private final Vector3D endingPosition;
    private final Vector3D endingVelocity;
    private final double deltaV;
    private final Orbit orbitState;
    private final double timeToExecute;
}
