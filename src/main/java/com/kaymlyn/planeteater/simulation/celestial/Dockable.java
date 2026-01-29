package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;

import java.util.Map;

public interface Dockable {
    Map<String,Spacecraft> getHanger();
    void dock(Spacecraft spacecraft);
}
