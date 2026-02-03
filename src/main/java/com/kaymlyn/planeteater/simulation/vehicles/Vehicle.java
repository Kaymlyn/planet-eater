package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.entities.EntityType;
import com.kaymlyn.planeteater.simulation.entities.Environment;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@EqualsAndHashCode
@ToString
@Data
public abstract class Vehicle {

    protected String id;
    protected Vector3D position;
    protected Vector3D velocity;
    protected double dryMass;              // Mass of spacecraft without fuel/cargo (kg)
    protected double fuelMass;             // Current fuel mass (kg)
    protected double maxFuelCapacity;      // Maximum fuel that can be carried (kg)
    protected double cargoCapacity;        // Maximum cargo mass (kg)
    protected double exhaustVelocity;      // Effective exhaust velocity (m/s)
    protected boolean hasLifeSupport;      // Whether it can carry humans
    protected int maxCrewCapacity;         // Maximum number of entities
    protected int minCrewRequirement;
    protected Composition cargo;
    protected List<Environment> environments;// Current cargo
    protected List<Automaton> crew;

    public Vehicle(String id, double dryMass, double maxFuelCapacity, double cargoCapacity, double exhaustVelocity, boolean hasLifeSupport, int maxCrewCapacity, int minCrewRequirement) {

        this.id = id;
        this.position = Vector3D.ZERO;
        this.velocity = Vector3D.ZERO;

        this.dryMass = dryMass;
        this.fuelMass = maxFuelCapacity; // Start fully fueled
        this.maxFuelCapacity = maxFuelCapacity;
        this.cargoCapacity = cargoCapacity;
        this.exhaustVelocity = exhaustVelocity;
        this.hasLifeSupport = hasLifeSupport;
        this.maxCrewCapacity = maxCrewCapacity;
        this.minCrewRequirement = minCrewRequirement;
        this.cargo = new Composition();
        this.crew = new ArrayList<>();
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

    public boolean boardAutomaton(Automaton entity) {
        if (crew.size() >= maxCrewCapacity) return false;

        // Inorganic automatons can board anything
        if (entity.getType() == EntityType.INORGANIC) {
            crew.add(entity);
            return true;
        }

        // Organic automatons need life support
        if (!hasLifeSupport) {
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

    public double fuelRequired(double deltaVSpent){
//        if(getAvailableDeltaV() < deltaVSpent) {
//            return Double.POSITIVE_INFINITY;
//        }

        return getTotalMass() - getTotalMass()/Math.exp(deltaVSpent/exhaustVelocity);

    }

    public abstract Gravitational getLocation();
//    /**
//     * Calculate fuel required for a given delta-v
//     * m_fuel = m_dry * (e^(Δv/v_e) - 1)
//     */
//    public double getFuelRequiredForDeltaV(double deltaV, double spentFuelMass) {
//        double payloadMass = dryMass + cargo.getTotalMass() +
//            crew.stream().mapToDouble(Automaton::getMass).sum() + fuelMass;
//        System.out.println("Payload Mass : " + payloadMass);
//
//        //Should be: exhaustVelocity*ln(mass beginning / mass after)
//        return Math.exp(deltaV/exhaustVelocity)*payloadMass
//        return exhaustVelocity *
//        return payloadMass * (1 - Math.exp(- (deltaV /
//                (exhaustVelocity * getLocation().getGravitationalForce(getLocation().getPosition().distanceTo(position))))));
//    }

}
