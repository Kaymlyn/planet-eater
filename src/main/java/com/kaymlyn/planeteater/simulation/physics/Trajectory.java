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
    public Orbiter destination;
    public double deltaV;           // Required delta-v (m/s)
    public double travelTime;       // Travel duration (seconds)
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
        this(startPosition,endPosition,null,deltaV,travelTime,fuelRequired,endVelocity, null);
    }

    @Override
    public String toString() {
        return String.format("Trajectory[distance=%.3e m, deltaV=%.1f m/s, " +
                        "time=%.2f days, fuel=%.1f kg]",
                startPosition.distanceTo(endPosition),
                deltaV, travelTime / PhysicsConstants.SECONDS_PER_DAY, fuelRequired);
    }
}
