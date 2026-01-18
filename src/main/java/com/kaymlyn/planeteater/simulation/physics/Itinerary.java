package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;


public class Itinerary {
    private final Deque<Trajectory> trajectories;
    private final List<PiecewiseState> telemetry;
    @Setter
    @Getter
    private double launchFuel;
    @Setter
    @Getter
    private double landingFuel;
    @Setter
    @Getter
    private Spacecraft.SpacecraftState finalSpacecraftState;
    @Getter
    private Orbiter finalDestination;
    @Getter
    private double startTime = 0.0;
    @Setter
    private OrbitalSystem system;

    public Itinerary(OrbitalSystem system) {
        this.trajectories = new LinkedList<>();
        this.telemetry = new ArrayList<>();
        this.system = system;
    }

    public boolean addFlightPlan(Trajectory trajectory) {
        if (!trajectories.isEmpty() && trajectories.getLast().endPosition != trajectory.startPosition) {
            System.out.println("Hit");
            return false;
        } else {
            trajectories.add(trajectory);
            finalDestination = trajectory.destination;
            System.out.println("Addint Trajectory" + trajectories);
            return true;
        }
    }

    public List<PiecewiseState> generateTelemetry(double timeStep) {
        if (telemetry.isEmpty()) {
            for (Trajectory trajectory : trajectories) {
                for (int i = 0; i < (trajectory.travelTime / timeStep); i++) {
                    System.out.println("Generating Telemetry : " + TravelCalculator.calculateTrajectoryState(
                            trajectory,
                            i * timeStep,
                            system.getCentralStar().getMass()
                    ));
                    System.out.println("Travel Time : " + trajectory.travelTime);
                    System.out.println("Time Step : " + timeStep);
                    telemetry.add(TravelCalculator.calculateTrajectoryState(
                            trajectory,
                            i * timeStep,
                            system.getCentralStar().getMass()
                            )
                    );
                }
            }
        }
        return telemetry;
    }

    public void setStartTime(double startTime) {
        if(this.startTime != 0.0) {
            this.startTime = startTime;
        }
    }

    public double getTotalFuelRequirement() {
        double fuelTotal = launchFuel + landingFuel;
        for(Trajectory trajectory : trajectories) {
            fuelTotal += trajectory.fuelRequired;
        }
        return fuelTotal;
    }

    public double getTotalFlightTime() {
        double time = 0.0;
        for (Trajectory trajectory : trajectories) {
            time += trajectory.travelTime;
        }
        return time;
    }

}
