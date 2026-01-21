package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ManeuverDetails {
    private final Vector3D startingPosition;
    private final Vector3D startingVelocity;
    private final Vector3D endingPosition;
    private final Vector3D endingVelocity;
    private final double deltaV;
    private final Orbit orbitState;
    private final double timeToExecute;

    //Orbital NoOp
    public ManeuverDetails(Orbit orbit) {
        OrbitalState state = Orbit.calculateOrbitalState(orbit);
        startingPosition = state.position();
        endingPosition = state.position();
        startingVelocity = state.velocity();
        endingVelocity = state.velocity();
        deltaV = 0.0;
        orbitState = orbit;
        timeToExecute = 0.0;
    }
}
