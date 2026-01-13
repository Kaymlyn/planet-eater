package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBody;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the player's orbital platform/base of operations
 */
@Data
public class CentralMind extends Vehicle implements Orbiter {

    private CelestialBody parentBody; // Orbital velocity

    public CentralMind(String id) {
        super(id, 1e6, 1e4, 1e3, 2, true, 12,0);
        this.crew = new ArrayList<>();
    }

    public CentralMind(String id, List<Automaton> initialCrew, Composition initialInventory) {
        super(id, 1e6, 1e4, 1e3, 2, true, 12,0);
        this.crew = new ArrayList<>(initialCrew);
    }

    @Override
    public String toString() {
        return String.format("OrbitalPlatform[id=%s, crew=%s",
                id, crew);
    }

    @Override
    public double getMass() {
        return getTotalMass();
    }

    /**
     * Update position and velocity based on acceleration over time step
     * Uses simple Euler integration for now
     */
    public void update(Vector3D acceleration, double dt) {
        // v = v + a * dt
        velocity = velocity.add(acceleration.multiply(dt));
        // p = p + v * dt
        position = position.add(velocity.multiply(dt));
    }

    public boolean equals(final Object o) {
        return o == this
                || o instanceof CentralMind other
                && other.canEqual(this)
                && Objects.equals(this.getParentBody(), other.getParentBody());
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CentralMind;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + (this.getParentBody() == null ? 43 : this.getParentBody().hashCode());
        result = result * 59 + (this.getId() == null ? 43 : this.getId().hashCode());
        return result;
    }
}