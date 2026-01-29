package com.kaymlyn.planeteater.simulation.physics;

import lombok.AllArgsConstructor;

/**
 * Represents the state of a spacecraft at a point in its trajectory
 *
 * @param timeElapsed      Time since trajectory start (s)
 * @param fractionComplete 0.0 to 1.0
 */
public record PiecewiseState(String id, Vector3D position, Vector3D velocity, double timeElapsed, double fractionComplete) {

    @Override
    public String toString() {
        return String.format("State[pos=%s, vel=%s, t=%.1f s, progress=%.1f%%]",
                position, velocity, timeElapsed, fractionComplete * 100);
    }
}
