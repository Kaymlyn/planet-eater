package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.celestial.Dockable;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;

public class VehicleFactory {


    /**
     * Create a basic cargo shuttle (no life support)
     */
    public static Spacecraft createCargoShuttle(String id, Dockable shipyard) {
        Composition construction = new Composition();
        construction.addMaterialAsVolume(Material.ALUMINUM, 2.0);
        construction.addMaterialAsVolume(Material.TITANIUM, 3.0);
        construction.addMaterialAsVolume(Material.IRON, 1.0);
        System.out.println("Total Mass : " + construction.getTotalMass());
        Spacecraft ship = new Spacecraft(id, construction.getTotalMass(), 2.0e7, 120.0, 8000.0, false, 0,0, shipyard);

        //Place in shipyard
        shipyard.dock(ship);

        // Construction materials
        ship.getConstruction().addBulkMaterial(construction);
        return ship;
    }

    /**
     * Create a heavy hauler for large cargo loads
     */
    public static Spacecraft createHeavyHauler(String id, Orbiter shipyard) {
        Spacecraft ship = new Spacecraft(id, 10000.0, 20000.0, 50000.0, 4000.0, false, 0,0, shipyard);

        // Construction materials
        ship.getConstruction().addMaterialAsVolume(Material.IRON, 5000.0);
        ship.getConstruction().addMaterialAsVolume(Material.ALUMINUM, 3000.0);
        ship.getConstruction().addMaterialAsVolume(Material.TITANIUM, 2000.0);

        return ship;
    }
    /**
     * Create a crewed mining vessel with life support
     */
    public static Spacecraft createMiningVessel(String id, Orbiter shipyard) {
        Spacecraft ship = new Spacecraft(id, 5000.0, 8000.0, 15000.0, 3500.0, true, 4,1, shipyard);

        // Construction materials
        ship.getConstruction().addMaterialAsVolume(Material.ALUMINUM, 2500.0);
        ship.getConstruction().addMaterialAsVolume(Material.TITANIUM, 1500.0);
        ship.getConstruction().addMaterialAsVolume(Material.IRON, 500.0);
        ship.getConstruction().addMaterialAsVolume(Material.SILICA, 300.0); // Radiation shielding
        ship.getConstruction().addMaterialAsVolume(Material.WATER_ICE, 200.0); // Life support reserves

        return ship;
    }
}
