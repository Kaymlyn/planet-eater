package com.kaymlyn.planeteater.simulation.physics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the state of a spacecraft at a point in its trajectory
 */
@Getter
@AllArgsConstructor
public class PiecewiseState {
    public Vector3D position;
    public Vector3D velocity;
    public double timeElapsed;      // Time since trajectory start (s)
    public double fractionComplete; // 0.0 to 1.0

    @Override
    public String toString() {
        return String.format("State[pos=%s, vel=%s, t=%.1f s, progress=%.1f%%]",
                position, velocity, timeElapsed, fractionComplete * 100);
    }
}
