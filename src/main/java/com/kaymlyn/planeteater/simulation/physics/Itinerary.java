package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class Itinerary {
    private final List<ManeuverDetails> maneuvers;
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
    private double startTime = 0.0;
    @Getter
    @Setter
    private Orbiter finalDestination;
    @Getter
    @Setter
    private double totalFuelCost;

    public Itinerary() {
        this.telemetry = new ArrayList<>();
        this.maneuvers = new ArrayList<>();
    }

    public void addFlightPlan(ManeuverDetails maneuver) {
        maneuvers.add(maneuver);
    }

    public List<PiecewiseState> generateTelemetry(double timeStep) {
        if (telemetry.isEmpty()) {
            int j = 0;
            for (ManeuverDetails maneuver : maneuvers) {
                for (int i = 0; i < ((maneuver.getTimeToExecute()) / timeStep); i++) {
                    telemetry.add(TravelCalculator.calculateTrajectoryState(
                            maneuver,
                            j * timeStep
                    ));
                    j++;
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

    public double getTotalFlightTime() {
        return maneuvers.stream().mapToDouble(ManeuverDetails::getTimeToExecute).sum();
    }

    public ManeuverDetails getFinalManeuver() {
        return maneuvers.getLast();
    }
}
