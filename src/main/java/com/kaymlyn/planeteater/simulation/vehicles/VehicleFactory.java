package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.resources.Material;

public class VehicleFactory {

//    public static Spacecraft buildSpaceCraftFromPlans() {
//
//    }

    /**
     * Create a basic cargo shuttle (no life support)
     */
    public static Spacecraft createCargoShuttle(String id) {
        Spacecraft ship = new Spacecraft(id, 2000.0, 5000.0, 10000.0, 3000.0, false, 0);

        // Construction materials
        ship.getConstruction().addMaterialAsVolume(Material.ALUMINUM, 1200.0);
        ship.getConstruction().addMaterialAsVolume(Material.TITANIUM, 500.0);
        ship.getConstruction().addMaterialAsVolume(Material.IRON, 300.0);

        return ship;
    }

    /**
     * Create a heavy hauler for large cargo loads
     */
    public static Spacecraft createHeavyHauler(String id) {
        Spacecraft ship = new Spacecraft(id, 10000.0, 20000.0, 50000.0, 4000.0, false, 0);

        // Construction materials
        ship.getConstruction().addMaterialAsVolume(Material.IRON, 5000.0);
        ship.getConstruction().addMaterialAsVolume(Material.ALUMINUM, 3000.0);
        ship.getConstruction().addMaterialAsVolume(Material.TITANIUM, 2000.0);

        return ship;
    }
    /**
     * Create a crewed mining vessel with life support
     */
    public static Spacecraft createMiningVessel(String id) {
        Spacecraft ship = new Spacecraft(id, 5000.0, 8000.0, 15000.0, 3500.0, true, 4);

        // Construction materials
        ship.getConstruction().addMaterialAsVolume(Material.ALUMINUM, 2500.0);
        ship.getConstruction().addMaterialAsVolume(Material.TITANIUM, 1500.0);
        ship.getConstruction().addMaterialAsVolume(Material.IRON, 500.0);
        ship.getConstruction().addMaterialAsVolume(Material.SILICA, 300.0); // Radiation shielding
        ship.getConstruction().addMaterialAsVolume(Material.WATER_ICE, 200.0); // Life support reserves

        return ship;
    }
}
