package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


public class Itinerary {
    private Trajectory trajectory;
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
        this.trajectory = null;
        this.telemetry = new ArrayList<>();
        this.system = system;
    }

    public void addFlightPlan(Trajectory trajectory) {
        if (trajectory != null && this.trajectory == null) {
            this.trajectory = trajectory;
            this.finalDestination = trajectory.destination;
        }
    }

    public List<PiecewiseState> generateTelemetry(double timeStep, Spacecraft spacecraft) {
        if (telemetry.isEmpty()) {
            Orbit orbit = Orbit.calculateOrbit(system.getCentralStar(),
                    spacecraft.getPosition(), spacecraft.getVelocity());

            for (Trajectory trajectory : trajectories) {
                for (int i = 0; i < ((trajectory.travelTime + trajectory.waitTime) / timeStep); i++) {
                    if(i * timeStep < trajectory.waitTime) {

                        telemetry.add(new PiecewiseState(
                                TravelCalculator.predictOrbitalPosition(orbit,i*timeStep, system),
                                Vector3D.ZERO,
                                0.0,
                                0.0
                                ));
                    } else {
                        telemetry.add(
                                TravelCalculator.calculateTrajectoryState(trajectory,
                                        i*timeStep,
                                        system.getCentralStar().getMass()));
                    }
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
