package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a planned trajectory between two positions
 */
@Getter
@AllArgsConstructor
public class Trajectory {
    public Vector3D startPosition;
    public Vector3D endPosition;
    public Vector3D launchPosition;
    public Orbiter destination;
    public double deltaV;           // Required delta-v (m/s)
    public double travelTime;       // Travel duration (seconds)
    public double waitTime;
    public double fuelRequired;     // Fuel needed (kg)
    public double endVelocity;
    public Spacecraft.SpacecraftState finalState;

    public Trajectory(
            Vector3D startPosition,
            Vector3D endPosition,
            double deltaV,
            double travelTime,
            double fuelRequired,
            double endVelocity) {
        this(startPosition,endPosition, startPosition,null,deltaV,  travelTime,0.0, fuelRequired,endVelocity, null);
    }
    public Trajectory(
            Vector3D startPosition,
            Vector3D endPosition,
            Vector3D launchPosition,
            double deltaV,
            double travelTime,
            double waitTime,
            double fuelRequired,
            double endVelocity) {
        this(startPosition,endPosition, launchPosition,null,deltaV,travelTime, waitTime, fuelRequired,endVelocity, null);
    }

    @Override
    public String toString() {
        return String.format("Trajectory[distance=%.3e m, deltaV=%.1f m/s, " +
                        "startPos=%s, endPos=%s, lauPos=%s" +
                        "time=%.2f days, fuel=%.1f kg]",
                startPosition.distanceTo(endPosition),deltaV,startPosition,endPosition,launchPosition,
                 travelTime / PhysicsConstants.SECONDS_PER_DAY, fuelRequired);
    }
}
