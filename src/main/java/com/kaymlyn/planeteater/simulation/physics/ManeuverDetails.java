package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
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
        OrbitalState state = orbit.calculateOrbitalState();
        startingPosition = state.position();
        endingPosition = state.position();
        startingVelocity = state.velocity();
        endingVelocity = state.velocity();
        deltaV = 0.0;
        orbitState = orbit;
        timeToExecute = 0.0;
    }

    //Holding Orbit
    public ManeuverDetails(Orbit orbit, double waitTime) {
        OrbitalState state = orbit.calculateOrbitalState();
        OrbitalState wait = orbit.calculateOrbitAfterT0(waitTime);
        startingPosition = state.position();
        endingPosition = wait.position();
        startingVelocity = state.velocity();
        endingVelocity = wait.velocity();
        deltaV = 0.0;
        orbitState = wait.orbitalElements();
        timeToExecute = waitTime;

    }
}
