package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;

public interface Satellite {
    public Gravitational getParentBody();
}
