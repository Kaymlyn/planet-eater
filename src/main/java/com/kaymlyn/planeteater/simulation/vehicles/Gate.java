package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import lombok.Data;

@Data
public class Gate implements Orbiter {

    private Vector3D position;
    private Vector3D velocity;
    private OrbitalSystem system;
    private Orbit initialOrbit;
    private Gravitational parentBody;

    public Gate() {

    }

    @Override
    public String getId() {
        return "Subspace_Gate";
    }

    @Override
    public Vector3D getVelocity() {
        return velocity;
    }

    @Override
    public Gravitational getParentBody() {
        return system.getCentralStar();
    }

    @Override
    public Orbit calculateCurrentOrbit() {
        return Orbit.calculateOrbitFor(this);
    }

    @Override
    public double getMass() {
        return 0;
    }

    @Override
    public void update(Vector3D vector3D, double timeStep) {

    }

    @Override
    public OrbitalSystem getSystem() {
        return null;
    }

    public void setPosition(Vector3D position) {
        if(position != null) {
            this.position = position;
        }
    }

    @Override
    public void setVelocity(Vector3D velocity) {
        this.velocity = velocity;
    }
}
