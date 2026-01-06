package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

import com.kaymlyn.planeteater.simulation.celestial.BodyType;
import com.kaymlyn.planeteater.simulation.celestial.Zone;

import java.util.List;


public record LayerProfile(BodyType bodyType, Zone zone, List<Materials> materials, double externalRadius){
    public int ratioDivisor() {
        return materials.stream().map(Materials::ratio).reduce(0,Integer::sum);
    }
}
