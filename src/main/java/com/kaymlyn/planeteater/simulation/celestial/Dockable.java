package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;

public interface Dockable {
    void land(Spacecraft spacecraft);
    void launch(Spacecraft spacecraft);
}
