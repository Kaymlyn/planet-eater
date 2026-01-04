package com.kaymlyn.planeteater.simulation.resources;

import com.kaymlyn.planeteater.simulation.celestial.BodyType;
import com.kaymlyn.planeteater.simulation.celestial.Zone;
import lombok.Data;

import java.util.List;

@Data
public class Profiles {

    private BodyType bodyType;
    private Zone zone;
    private List<Materials> materials;

    public record Materials(Material material, int ratio) {}
}
