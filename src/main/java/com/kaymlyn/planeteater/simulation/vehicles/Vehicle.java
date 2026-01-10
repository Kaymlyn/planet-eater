package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.entities.Environment;
import com.kaymlyn.planeteater.simulation.entities.MiningEntity;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Data
public class Vehicle { // Entities aboard
    protected Vector3D position;
    protected Vector3D velocity;
    protected double dryMass;              // Mass of spacecraft without fuel/cargo (kg)
    protected double fuelMass;             // Current fuel mass (kg)
    protected double maxFuelCapacity;      // Maximum fuel that can be carried (kg)
    protected double cargoCapacity;        // Maximum cargo mass (kg)
    protected double exhaustVelocity;      // Effective exhaust velocity (m/s)
    protected boolean hasLifeSupport;      // Whether it can carry humans
    protected int maxCrewCapacity;         // Maximum number of entities
    protected Composition cargo;
    protected List<Environment> environments;// Current cargo
    protected List<Automaton> crew;

    public Vehicle(double dryMass, double maxFuelCapacity, double cargoCapacity, double exhaustVelocity, boolean hasLifeSupport, int maxCrewCapacity) {
        this.dryMass = dryMass;
        this.fuelMass = maxFuelCapacity; // Start fully fueled
        this.maxFuelCapacity = maxFuelCapacity;
        this.cargoCapacity = cargoCapacity;
        this.exhaustVelocity = exhaustVelocity;
        this.hasLifeSupport = hasLifeSupport;
        this.maxCrewCapacity = maxCrewCapacity;
        this.cargo = new Composition();
        this.crew = new ArrayList<>();
        this.position = Vector3D.ZERO;
        this.velocity = Vector3D.ZERO;
    }

    /**
     * Calculate total current mass (dry + fuel + cargo + crew)
     */
    public double getTotalMass() {
        double crewMass = crew.stream().mapToDouble(Automaton::getMass).sum();
        return dryMass + fuelMass + cargo.getTotalMass() + crewMass;
    }

    /**
     * Calculate available cargo space
     */
    public double getAvailableCargoSpace() {
        return cargoCapacity - cargo.getTotalMass();
    }

    /**
     * Calculate available crew slots
     */
    public int getAvailableCrewSlots() {
        return maxCrewCapacity - crew.size();
    }

    /**
     * Add fuel to the spacecraft
     * Returns actual amount added
     */
    public double addFuel(double amount) {
        double space = maxFuelCapacity - fuelMass;
        double toAdd = Math.min(amount, space);
        fuelMass += toAdd;
        return toAdd;
    }

    /**
     * Consume fuel (for propulsion)
     * Returns actual amount consumed
     */
    public double consumeFuel(double amount) {
        double toConsume = Math.min(amount, fuelMass);
        fuelMass -= toConsume;
        return toConsume;
    }

    /**
     * Load cargo onto the spacecraft
     * Returns actual amount loaded
     */
    public double loadCargo(Material material, double mass) {
        double available = getAvailableCargoSpace();
        double toLoad = Math.min(mass, available);
        if (toLoad > 0) {
            cargo.addMaterialAsVolume(material, toLoad);
        }
        return toLoad;
    }

    /**
     * Unload cargo from the spacecraft
     * Returns actual amount unloaded
     */
    public double unloadCargo(Material material, double mass) {
        return cargo.removeMaterial(material, mass);
    }

    /**
     * Board a mining entity
     */
    public boolean boardAutomaton(Automaton entity) {
        if (crew.size() >= maxCrewCapacity) {
            return false;
        }
        if (new HashSet<>(environments).containsAll(entity.getAdaptations())) {
            return false;
        }
        crew.add(entity);
        return true;
    }

    /**
     * Disembark a mining entity
     */
    public Automaton disembarkCrew(Automaton entity) {
        if (crew.remove(entity)) {
            return entity;
        }
        return null;
    }

    /**
     * Calculate delta-v available with current fuel
     * Uses Tsiolkovsky rocket equation: Δv = v_e * ln(m_initial / m_final)
     */
    public double getAvailableDeltaV() {
        double initialMass = getTotalMass();
        double finalMass = initialMass - fuelMass;

        if (finalMass <= 0 || initialMass <= 0) {
            return 0.0;
        }

        return exhaustVelocity * Math.log(initialMass / finalMass);
    }

    /**
     * Calculate fuel required for a given delta-v
     * m_fuel = m_dry * (e^(Δv/v_e) - 1)
     */
    public double getFuelRequiredForDeltaV(double deltaV) {
        double payloadMass = dryMass + cargo.getTotalMass() +
            crew.stream().mapToDouble(Automaton::getMass).sum();
        return payloadMass * (Math.exp(deltaV / exhaustVelocity) - 1.0);
    }

}
