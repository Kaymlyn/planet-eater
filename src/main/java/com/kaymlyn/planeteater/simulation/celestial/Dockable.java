package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;

import java.util.Map;

public interface Dockable extends Orbiter {
    Map<String,Spacecraft> getHanger();

    default void dock(Spacecraft spacecraft) {
        getHanger().put(spacecraft.getId(), spacecraft);
        spacecraft.setState(Spacecraft.SpacecraftState.DOCKED);
        spacecraft.setDockingLocation(this);
        spacecraft.setOrbiting(null);
    }

    default void undock(Spacecraft spacecraft) {
        if(this instanceof Gravitational gravitational) {
            spacecraft.setOrbiting(gravitational);
        } else {
            spacecraft.setOrbiting(this.getParentBody());
        }
        getHanger().remove(spacecraft.getId());
        spacecraft.setDockingLocation(null);

    }
}
