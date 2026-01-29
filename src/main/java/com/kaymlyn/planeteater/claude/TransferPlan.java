package com.kaymlyn.planeteater.claude;

import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete transfer plan with all burns and coast phases
 */
@Data
@AllArgsConstructor
public class TransferPlan {
    private final Orbit startOrbit;
    private final Orbit targetOrbit;
    private final List<MultiBurnTransfer.BurnManeuver> burns;
    private final List<MultiBurnTransfer.CoastPhase> coasts;
    private double totalDeltaV;
    private double totalFuelRequired;
    private double totalDuration;
    private double totalWaitTime;           // Time waiting for phasing
    private boolean feasible;
    private String infeasibilityReason;

    public TransferPlan(Orbit startOrbit, Orbit targetOrbit) {
        this.startOrbit = startOrbit;
        this.targetOrbit = targetOrbit;
        this.burns = new ArrayList<>();
        this.coasts = new ArrayList<>();
        this.totalDeltaV = 0.0;
        this.totalFuelRequired = 0.0;
        this.totalDuration = 0.0;
        this.totalWaitTime = 0.0;
        this.feasible = true;
        this.infeasibilityReason = null;
    }

    public void addBurn(MultiBurnTransfer.BurnManeuver burn) {
        burns.add(burn);
        totalDeltaV += burn.getDeltaV();
        totalFuelRequired += burn.getFuelRequired();
    }

    public void addCoast(MultiBurnTransfer.CoastPhase coast) {
        coasts.add(coast);
        totalDuration += coast.getDuration();
    }

    public void setInfeasible(String reason) {
        this.feasible = false;
        this.infeasibilityReason = reason;
    }

    public int getNumberOfBurns() {
        return burns.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TRANSFER PLAN ===\n");
        sb.append(String.format("From: a=%.3e m, e=%.3f, i=%.1f°\n",
                startOrbit.semiMajorAxis(), startOrbit.eccentricity(),
                Math.toDegrees(startOrbit.inclination())));
        sb.append(String.format("To:   a=%.3e m, e=%.3f, i=%.1f°\n",
                targetOrbit.semiMajorAxis(), targetOrbit.eccentricity(),
                Math.toDegrees(targetOrbit.inclination())));
        sb.append(String.format("\nFeasible: %s\n", feasible ? "YES" : "NO - " + infeasibilityReason));

        if (feasible) {
            sb.append(String.format("Burns: %d\n", burns.size()));
            sb.append(String.format("Total Δv: %.1f m/s\n", totalDeltaV));
            sb.append(String.format("Total Fuel: %.1f kg\n", totalFuelRequired));
            sb.append(String.format("Total Time: %.2f days\n", totalDuration / PhysicsConstants.SECONDS_PER_DAY));
            if (totalWaitTime > 0) {
                sb.append(String.format("Wait Time: %.2f days\n", totalWaitTime / PhysicsConstants.SECONDS_PER_DAY));
            }

            sb.append("\n--- Maneuver Sequence ---\n");
            int burnIndex = 0;
            int coastIndex = 0;
            double currentTime = 0;

            for (int i = 0; i < burns.size() + coasts.size(); i++) {
                if (coastIndex < coasts.size() &&
                        (burnIndex >= burns.size() || coasts.get(coastIndex).getStartTime() <= currentTime)) {
                    MultiBurnTransfer.CoastPhase coast = coasts.get(coastIndex++);
                    sb.append(String.format("%d. %s\n", i + 1, coast));
                    currentTime += coast.getDuration();
                } else if (burnIndex < burns.size()) {
                    MultiBurnTransfer.BurnManeuver burn = burns.get(burnIndex++);
                    sb.append(String.format("%d. %s\n", i + 1, burn));
                }
            }
        }

        return sb.toString();
    }
}
