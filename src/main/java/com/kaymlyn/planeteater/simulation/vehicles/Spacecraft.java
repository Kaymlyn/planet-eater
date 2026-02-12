package com.kaymlyn.planeteater.simulation.vehicles;

import com.kaymlyn.planeteater.simulation.physics.ScheduledBurn;
import com.kaymlyn.planeteater.simulation.physics.TransferPlanner;
import com.kaymlyn.planeteater.simulation.celestial.Dockable;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.entities.Automaton;
import com.kaymlyn.planeteater.simulation.physics.Itinerary;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@EqualsAndHashCode(callSuper = false)
@Data
public class Spacecraft extends Vehicle {

    public enum SpacecraftState {
        DOCKED,
        TRAVELING,
        ORBITING,
        STRANDED
    }

    private Composition construction;
    private Composition cargo;
    private List<Automaton> crew;

    private SpacecraftState state;
    private Orbiter orbiting;
    private Itinerary itinerary;

    private double transitTime;

    private OrbitalSystem system;

    public Spacecraft(String id,
                      double dryMass,
                      double maxFuelCapacity,
                      double cargoCapacity,
                      double exhaustVelocity,
                      boolean hasLifeSupport,
                      int maxCrewCapacity,
                      int minCrewRequirement,
                      Orbiter shipyard) {
        super(id, dryMass, maxFuelCapacity, cargoCapacity, exhaustVelocity,
                hasLifeSupport, maxCrewCapacity, minCrewRequirement);

        this.construction = new Composition();
        this.state = SpacecraftState.DOCKED;
        this.itinerary = null;
        this.orbiting = shipyard;
        this.system = shipyard.getParentBody().getSystem();
        this.position = shipyard.getPosition();
        this.cargo = new Composition();
        this.crew = new ArrayList<>();
    }
    /**
     * Get the current position of the spacecraft.
     * When DOCKED, returns the position of the body being orbited.
     * When TRAVELING, ORBITING, or STRANDED, returns the spacecraft's own position.
     *
     * @return current position vector
     */
    @Override
    public Vector3D getPosition() {
        if (state == SpacecraftState.DOCKED && orbiting != null) {
            return orbiting.getPosition();
        }
        return position;
    }

    /**
     * Get the current velocity of the spacecraft.
     * When DOCKED, returns the velocity of the body being orbited.
     * When TRAVELING, ORBITING, or STRANDED, returns the spacecraft's own velocity.
     *
     * @return current velocity vector
     */
    @Override
    public Vector3D getVelocity() {
        if (state == SpacecraftState.DOCKED && orbiting != null) {
            return orbiting.getVelocity();
        }
        return velocity;
    }

    public Gravitational getLocation() {
        if (orbiting == null || !(orbiting instanceof Gravitational)) {
            return system.getCentralStar();
        } else {
            return (Gravitational) orbiting;
        }
    }

    public void programItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public Itinerary planRoute(
            @NonNull Orbiter destination,
            boolean land,
            double departureTime,
            TransferPlanner.OptimizationGoal priority) {

        List<TransferPlanner.TransferOption> options =
                TransferPlanner.generateTransferOptions(
                        this,
                        this.orbiting,
                        destination,
                        land,
                        departureTime
                );

        TransferPlanner.TransferOption bestOption =
                TransferPlanner.selectBestTransfer(
                        options,
                        Objects.requireNonNullElse(priority,
                                TransferPlanner.OptimizationGoal.MINIMUM_DELTAV)
                );

        return bestOption != null ? bestOption.getItinerary() : null;
    }

    public void simulateTravel() {
        if (itinerary == null) {
            return;
        }

        double currentTime = system.getCurrentTime();

        // Execute all burns that are due at this time step.
        // Loop is necessary: multiple burns may have been passed in a single step.
        ScheduledBurn nextBurn = itinerary.getNextBurn(currentTime);
        while (nextBurn != null && nextBurn.shouldExecute(currentTime)) {

            // Capture pre-burn velocity for logging
            Vector3D preBurnVelocity = velocity;

            // Apply delta-V
            velocity = velocity.add(nextBurn.deltaVelocity());

            // Calculate fuel cost using pre-burn total mass (correct Tsiolkovsky application)
            double fuelNeeded = nextBurn.fuelRequired(getTotalMass(), exhaustVelocity);
            double fuelConsumed = consumeFuel(fuelNeeded);

            if (fuelConsumed < fuelNeeded * 0.99) {
                System.err.println("WARNING: Insufficient fuel for burn " +
                        nextBurn.id() + ". Stranded.");
                state = SpacecraftState.STRANDED;
                itinerary = null;
                return;
            }

            itinerary.removeBurn(nextBurn);

            System.out.printf(
                    "[T=%.1f] Executed %s: dv=%.1f m/s, fuel=%.1f kg, v: %s -> %s%n",
                    currentTime,
                    nextBurn.description(),
                    nextBurn.deltaVelocity().magnitude(),
                    fuelConsumed,
                    preBurnVelocity,
                    velocity
            );

            if (state == SpacecraftState.DOCKED) {
                state = SpacecraftState.TRAVELING;
            }

            // Check next burn - may also be due this step
            nextBurn = itinerary.getNextBurn(currentTime);
        }

        if (itinerary.isComplete(currentTime)) {
            completeTravel();
        }
    }


    /**
     * Complete travel process.
     */
    private void completeTravel() {
        if (itinerary == null) {
            return;
        }

        System.out.printf(
                "[T=%.1f] %s completed itinerary to %s%n",
                system.getCurrentTime(),
                id,
                itinerary.getDestination() != null ?
                        itinerary.getDestination().getId() : "destination"
        );

        Orbiter destination = itinerary.getDestination();
        SpacecraftState targetState = itinerary.getFinalState();

        if (targetState == SpacecraftState.DOCKED) {
            if (destination instanceof Dockable dockable) {
                dockable.dock(this);
                setState(SpacecraftState.DOCKED);
                orbiting = destination;
                system.unregister(this);
            } else if (destination instanceof Gravitational) {
                // Orbiting a planet without docking
                setState(SpacecraftState.ORBITING);
                orbiting = destination;
            } else {
                setState(SpacecraftState.STRANDED);
            }
        } else if (targetState == SpacecraftState.ORBITING) {
            if (destination instanceof Gravitational) {
                setState(SpacecraftState.ORBITING);
                orbiting = destination;
            } else {
                setState(SpacecraftState.STRANDED);
            }
        } else {
            setState(SpacecraftState.STRANDED);
        }

        itinerary = null;
    }

    public boolean launch(double timeStep) {
        if (itinerary == null) {
            System.err.println("Cannot launch: no itinerary programmed");
            return false;
        }

        if (!itinerary.isFeasible()) {
            System.err.println("Cannot launch: " + itinerary.getInfeasibilityReason());
            return false;
        }

        if (state != SpacecraftState.DOCKED) {
            System.err.println("Cannot launch: not docked");
            return false;
        }
        double totalDeltaV = itinerary.getTotalDeltaV();
        double burnsRemoved = itinerary.consolidate(timeStep);
        double deltaDeltaV = totalDeltaV - itinerary.getTotalDeltaV();
        System.out.println(burnsRemoved + " burns Removed for " + deltaDeltaV + " less deltaV.");
        // Validate we have enough fuel for entire mission
        double requiredFuel = 0.0;
        double currentMass = getTotalMass();

        for (ScheduledBurn burn : itinerary.getBurns()) {
            double fuelNeeded = burn.fuelRequired(currentMass, exhaustVelocity);
            requiredFuel += fuelNeeded;
            currentMass -= fuelNeeded;
        }

        if (requiredFuel > fuelMass) {
            System.err.println(String.format(
                    "Cannot launch: insufficient fuel (need %.1f kg, have %.1f kg)",
                    requiredFuel, fuelMass));
            return false;
        }

        // Launch: undock and register with system
        if (orbiting instanceof Dockable dockable) {
            dockable.getHanger().remove(this.id);
        }

        setState(SpacecraftState.TRAVELING);
        system.register(this);

        System.out.printf(
                "[T=%.1f] %s launched from %s with %d scheduled burns%n",
                system.getCurrentTime(),
                id,
                orbiting.getId(),
                itinerary.getBurns().size()
        );

        return true;
    }

    /**
     * Get current position for rendering/queries.
     * No telemetry lookup - just return actual position from n-body sim.
     *
     * Phase 3 Implementation: Simplify position queries
     *
     * Generated by Claude (Sonnet 4.5)
     */
    public Vector3D getPositionAtTime(double absoluteTime) {
        // In simplified architecture, we don't predict positions
        // Just return current position - let caller run simulation if needed
        if (Math.abs(absoluteTime - system.getCurrentTime()) < 1e-6) {
            return position;
        }

        // For future/past times, caller must run simulation
        throw new UnsupportedOperationException(
                "Position prediction not supported in simplified architecture. " +
                        "Run simulation to desired time instead.");
    }

    public Composition recycle() {
        Composition materials = new Composition();

        for (Map.Entry<Material, Double> entry : construction.getMaterials().entrySet()) {
            materials.addMaterialAsRawMass(entry.getKey(), entry.getValue());
        }

        system.unregister(this);
        return materials;
    }

    @Override
    public String toString() {
        return String.format("Spacecraft[id=%s, state=%s, mass=%.1f kg, fuel=%.1f/%.1f kg, " +
                        "cargo=%.1f/%.1f kg, crew=%d/%d, deltaV=%.1f m/s]",
                id, state, getTotalMass(), fuelMass, maxFuelCapacity,
                cargo.getTotalMass(), cargoCapacity, crew.size(), maxCrewCapacity,
                getAvailableDeltaV());
    }
}