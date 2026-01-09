package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

import com.kaymlyn.planeteater.simulation.celestial.BodyType;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Zone;

import java.util.List;


public record LayerProfile(BodyType bodyType, Zone zone, List<Materials> materials){
    public double ratioDivisor() {
        return materials.stream().mapToDouble(Materials::ratio).reduce(0.0,Double::sum);
    }
}
