package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.celestial.Dockable;
import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the player's orbital platform/base of operations
 */
@Data
public class CentralMind extends Vehicle implements Orbiter, Satellite, Dockable {

    private Gravitational parentBody;
    private OrbitalSystem system;
    private Orbit initialOrbit;
    private Map<String, Spacecraft> hanger;

    public CentralMind(String id, OrbitalSystem system) {
        super(id, 1e6, 1e4, 1e3, 2, true, 12,0);
        this.system = system;
        this.crew = new ArrayList<>();
        this.hanger = new HashMap<>();
    }

    @Override
    public Orbit calculateCurrentOrbit() {
        Orbit orbit = Orbit.calculateOrbitFor(this);
        if(initialOrbit == null) {
            initialOrbit = orbit;
        }
        return orbit;
    }

    @Override
    public double getMass() {
        return dryMass
                + getFuelMass()
                + crew.stream().mapToDouble(Automaton::getMass).reduce(0.0, Double::sum);
    }

    @Override
    public String toString() {
        return String.format("OrbitalPlatform[id=%s, position=%s, crew=%s",
                id, position, crew);
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

    @Override
    public void setParentBody(Gravitational parentBody) {
        this.parentBody = parentBody;
    }

    @Override
    public OrbitalSystem getSystem() {
        return system;
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

    /**
     * No where.
     */
    @Override
    public Gravitational getLocation() {
        return parentBody;
    }

    @Override
    public Map<String, Spacecraft> getHanger() {
        return hanger;
    }

    @Override
    public void dock(Spacecraft spacecraft) {
        hanger.put(spacecraft.getId(),spacecraft);
        spacecraft.setPosition(this.position);
        spacecraft.setVelocity(this.velocity);
        spacecraft.setOrbiting(this);
    }
}