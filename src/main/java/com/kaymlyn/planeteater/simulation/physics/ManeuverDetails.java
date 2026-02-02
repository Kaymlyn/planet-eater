package com.kaymlyn.planeteater.simulation.physics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@AllArgsConstructor
@ToString
public class ManeuverDetails {
    private final String id;
    private final Vector3D startingPosition;
    private final Vector3D startingVelocity;
    private final Vector3D endingPosition;
    private final Vector3D endingVelocity;
    private final double deltaV;
    private final Orbit orbitState;
    private final double timeToExecute;
    private final double epoch0;

    //Orbital NoOp
    public ManeuverDetails(Orbit orbit, double startTime) {
        id = "NoOp-" + UUID.randomUUID();
        OrbitalState state = orbit.calculateOrbitalState();
        startingPosition = state.position();
        endingPosition = state.position();
        startingVelocity = state.velocity();
        endingVelocity = state.velocity();
        deltaV = 0.0;
        orbitState = orbit;
        timeToExecute = 0.0;
        epoch0 = startTime;
    }

    //Holding Orbit
    public ManeuverDetails(Orbit orbit, double waitTime, double startTime) {
        id = "Holding-" + UUID.randomUUID();
        OrbitalState state = orbit.calculateOrbitalState();
        OrbitalState wait = orbit.calculateOrbitAfterT0(orbit.epoch() + waitTime);
        startingPosition = state.position();
        endingPosition = wait.position();
        startingVelocity = state.velocity();
        endingVelocity = wait.velocity();
        deltaV = 0.0;
        orbitState = state.orbitalElements();      // FIX: start-of-wait orbit
        timeToExecute = waitTime;
        epoch0 = startTime;
    }

    public double getTimeAtManeuverEnd() {
        return epoch0 + timeToExecute;
    }
}
