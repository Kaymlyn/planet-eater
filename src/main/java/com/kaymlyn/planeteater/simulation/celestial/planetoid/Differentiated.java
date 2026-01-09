package com.kaymlyn.planeteater.simulation.celestial.planetoid;

import com.kaymlyn.planeteater.simulation.resources.Material;

public interface Differentiated {
    boolean canMineCrust();

    boolean canHarvestAtmosphere();

    double mineCrustMaterial(Material material, double requestedMass);

    double harvestAtmosphere(Material material, double requestedMass);
}
