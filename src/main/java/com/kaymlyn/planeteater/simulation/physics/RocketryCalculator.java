package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;


public class RocketryCalculator {

    public static ManeuverDetails calculateLandingOnGravitational(Gravitational currentLocation,
                                                                  ManeuverDetails previousManeuver) {

        return previousManeuver.finallyLand(currentLocation);
    }

    public static double calculateNextLaunchWindowWaitTime(Orbit origin,
                                                           Orbit destination) {
        OrbitalState originState = origin.calculateOrbitalState();
        OrbitalState destinationState = destination.calculateOrbitalState();
        double currentPhaseAngle = originState.position().angleBetween(destinationState.position());

        double meanAngularVelocity = destination.meanAngularVelocity();

        return getMinimalWaitTime(
                normalizeAngle(Math.PI - meanAngularVelocity * calculateMeanTransferTime(origin, destination) - currentPhaseAngle),
                meanAngularVelocity,
                originState.velocity().magnitude() / originState.position().magnitude());
    }

    private static double calculateMeanTransferTime(Orbit origin, Orbit targetOrbit) {
        if(origin.centerBody() == targetOrbit.centerBody()) {
            OrbitalState originState = origin.calculateOrbitalState();
            return Math.PI * Math.sqrt(
                    Math.pow((originState.position().magnitude() + targetOrbit.semiMajorAxis()) / 2, 3) / origin.centerBody().getGravitationalParameter());
        } else {
            return Double.NaN;
        }
    }

    private static double getMinimalWaitTime(double phaseAngleDifference, double targetAngularVelocity, double spacecraftAngularVelocity) {
        double waitTime = getWaitTime(phaseAngleDifference, targetAngularVelocity, spacecraftAngularVelocity);
        double synodicPeriod = getSynodicPeriod(targetAngularVelocity, spacecraftAngularVelocity);
        return waitTime > synodicPeriod / 2 ? synodicPeriod - waitTime : waitTime;
    }

    private static double getWaitTime(double phaseAngleDifference, double targetAngularVelocity, double spacecraftAngularVelocity) {
        return phaseAngleDifference / Math.abs(targetAngularVelocity - spacecraftAngularVelocity);
    }

    private static double getSynodicPeriod(double targetAngularVelocity, double spacecraftAngularVelocity) {
        return 2 * Math.PI / Math.abs(targetAngularVelocity - spacecraftAngularVelocity);
    }

    /**
     * Calculate the relative velocity needed to match orbits with a target
     */
    public static double calculateMatchVelocity(Vector3D currentVel, Vector3D targetVel) {
        return currentVel.subtract(targetVel).magnitude();
    }

    /**
     * Total deltaV to phase orbit
     * @param currentOrbit
     * @param targetTrueAnomaly
     * @return
     */
    public static ManeuverDetails phaseOrbit(Orbit currentOrbit, double targetTrueAnomaly, double maneuverStart) {
        OrbitalState state = currentOrbit.calculateOrbitalState();
        Gravitational centerBody = currentOrbit.centerBody();
        double time = timeToAnomaly(currentOrbit,targetTrueAnomaly);
        double r = centerBody.getPosition().distanceTo(state.position());
        double phasedApoapsis = getPhasedApoapsis(currentOrbit, targetTrueAnomaly);
        double phasedAngularMomentum = Math.sqrt(2*currentOrbit.centerBody().getGravitationalParameter()) *
                Math.sqrt((phasedApoapsis * currentOrbit.periapsis())/(phasedApoapsis + currentOrbit.periapsis()));
        OrbitalState futureState = currentOrbit.calculateOrbitAfterT0(time);
        return new ManeuverDetails(
                "Phase Orbit by " + time,
                state.position(),
                state.velocity(),
                futureState.position(),
                futureState.velocity(),
                2*(phasedAngularMomentum/r) - (state.angularMomentum()/r),futureState.orbitalElements(),
                time,
                maneuverStart
                );
    }

    private static double timeToAnomaly(Orbit currentOrbit, double targetTrueAnomaly) {
        return currentOrbit.orbitalPeriod()/(2 * Math.PI) * (currentOrbit.trueToEccentricAnomaly(targetTrueAnomaly)
                - (currentOrbit.eccentricity()*Math.sin(currentOrbit.trueToEccentricAnomaly(targetTrueAnomaly))));
    }

    private static double getPhasedApoapsis(Orbit currentOrbit, double time) {
        double phasedPeriod = currentOrbit.orbitalPeriod() - time;
        double phasedSemiMajorAxis = Math.pow(Math.sqrt(currentOrbit.centerBody().getGravitationalParameter() * phasedPeriod)/(2*Math.PI), 2.0/3.0);
        return 2 * phasedSemiMajorAxis - currentOrbit.periapsis();
    }

    /**
     * Hohmann Transfer between orbits.
     * NOTE: Only accurate for COPLANAR, CIRCULAR orbits. Circular Orbits should not be a problem as all spaceship
     * orbits should be circular by default unless in a transfer orbit.
     * @param origin
     * @param target
     * @return
     */
    public static ManeuverDetails calculateHohmannTransferBetweenOrbits(Orbit origin,
                                                                        Orbit target,
                                                                        ManeuverDetails previousManeuver) {
        double parentMass = origin.centerBody().getGravitationalParameter();
        // Semi-major axis of transfer ellipse
        double semiMajorAxisTransfer = (origin.semiMajorAxis() + target.semiMajorAxis()) / 2.0;

        // Orbital velocities near circular orbits mean semiMajorAxis ≈ radius
        double v1 = Math.sqrt(parentMass / origin.semiMajorAxis());
        double v2 = Math.sqrt(parentMass / target.semiMajorAxis());

        // Velocities at periapsis and apoapsis of transfer orbit
        double vp = Math.sqrt(parentMass * (2.0/origin.semiMajorAxis() - 1.0/semiMajorAxisTransfer));
        double va = Math.sqrt(parentMass * (2.0/target.semiMajorAxis() - 1.0/semiMajorAxisTransfer));

        // Delta-v requirements
        double deltaV1 = Math.abs(vp - v1);  // Injection burn
        double deltaV2 = Math.abs(v2 - va);  // Circularization burn
        double totalDeltaV = deltaV1 + deltaV2;

        // Transfer time (half orbital period of transfer ellipse)
        double transferTime = Math.PI * Math.sqrt(Math.pow(semiMajorAxisTransfer, 3) /
                (parentMass));

        OrbitalState endingState = target.calculateOrbitAfterT0(
                previousManeuver.getEndTime() + transferTime - target.epoch());

        return previousManeuver
                .thenManeuver(endingState.position(),endingState.velocity(),totalDeltaV,transferTime,"Hohmann Transfer " + origin + " > " + target);
    }

    /**
     * Adjust the origin orbit to be coplanar with another orbit. This only makes sense if both orbits are around the
     * same central body.
     * @param origin
     * @param targetInclination
     * @return
     */
    public static ManeuverDetails adjustToCoplanar(Orbit origin,
                                                         Orbit targetInclination,
                                                         ManeuverDetails previousManeuver) {
        OrbitalState cardinalOrbit = origin.calculateOrbitalState();
        Vector3D ascendingCrossOver = cardinalOrbit.orbitalElements().getCoincidentalAscendingNode(targetInclination);
        double waitToAscendingCrossOver = cardinalOrbit.orbitalElements().calculateTimeToPoint(ascendingCrossOver);
        Vector3D descendingCrossOver = cardinalOrbit.orbitalElements().getCoincidentalDescendingNode(targetInclination);
        double waitToDescendingCrossOver = cardinalOrbit.orbitalElements().calculateTimeToPoint(descendingCrossOver);
        Vector3D transferPoint;
        if(waitToAscendingCrossOver < waitToDescendingCrossOver) {
            transferPoint = ascendingCrossOver;
        } else {
            transferPoint = descendingCrossOver;
        }

        Vector3D targetVelocity = targetInclination.calculateVelocityVectorAtPosition(transferPoint);

        ManeuverDetails crossOverWait = previousManeuver
                .thenCoast(Math.min(waitToAscendingCrossOver,waitToDescendingCrossOver));

        double coplanarDeltaV = crossOverWait.getEndingVelocity()
                .subtract(targetVelocity)
                .magnitude();

        return crossOverWait.thenManeuver(
                crossOverWait.getEndingPosition(),
                targetVelocity,
                coplanarDeltaV,
                0.0,
                "Coplanar Burn " + origin.inclination() + " > " + targetInclination.inclination()
        );
    }

    /**
     * Calculate delta-v required to enter orbit around a body from an approach trajectory
     *
     * @param targetOrbitAltitude Desired altitude above surface (m)
     * @return Delta-v required for orbital insertion (m/s)
     */
    public static ManeuverDetails calculateOrbitalInsertionDeltaV(Orbit origin,
                                                                  Gravitational destinationGrav,
                                                                  Orbiter destinationOrbiter,
                                                                  double targetOrbitAltitude,
                                                                  ManeuverDetails previousManeuver) {

        //destination needs to be gravitational and be orbiting, so verify they are the same object
        //if not then docking is required instead. Orbital Insertion only makes sense if going from
        //a parent body to a child body, thus this is not usable for the Star type anyway.
        if(destinationGrav == destinationOrbiter) {
            double soiRadius = calculateSphereOfInfluence(
                    destinationGrav.getMass(),
                    origin.centerBody().getMass(),
                    destinationOrbiter.calculateCurrentOrbit().semiMajorAxis()
                    );
            if(targetOrbitAltitude < soiRadius && targetOrbitAltitude > destinationGrav.getRadius()) {

                double timeFromEpoch = previousManeuver.getEndTime() - origin.epoch();
                OrbitalState state = origin.calculateOrbitAfterT0(timeFromEpoch);
                double mu = PhysicsConstants.G * destinationGrav.getMass();
                double r = destinationGrav.getRadius() + targetOrbitAltitude; // Orbital radius

                // Velocity needed for circular orbit at target altitude
                double orbitalVelocity = Math.sqrt(mu / r);

                // Periapsis velocity when approaching (conservation of energy)
                // v_periapsis^2 = v_infinity^2 + v_circular^2
                // where v_infinity is the approach velocity at infinity (SOI edge)
                double vPeriapsis = Math.sqrt(state.velocity().magnitudeSquared() +
                        2 * mu / r);//claude code doesn't match above equation

                OrbitalState finalOrbit = Orbit.createCircularOrbit(r,destinationGrav);

                // Delta-v is the difference between periapsis velocity and orbital velocity
                double deltaV = Math.abs(vPeriapsis - orbitalVelocity);

                return previousManeuver.thenManeuver(finalOrbit.position(),finalOrbit.velocity(),deltaV,4 * 3600, "Orbital Insertion " + destinationOrbiter.getId());
            }
        }

        //Not a valid Maneuver, so do nothing.
        return previousManeuver;
    }

    /**
     * Find Lambert transfer that minimizes delta-V for a given flight time
     *
     * Lambert's problem: Given two positions and transfer time, find required velocity.
     * This method finds the transfer time that minimizes total delta-V.
     *
     * @param initialPos Starting position vector
     * @param targetPos Ending position vector
     * @param centerBody Gravitational body being orbited
     * @param minTime Minimum transfer time to consider (seconds)
     * @param maxTime Maximum transfer time to consider (seconds)
     * @return ManeuverDetails for optimal transfer, or null if no solution
     */
    public static ManeuverDetails minimalLambert(
            Vector3D initialPos,
            Vector3D targetPos,
            Gravitational centerBody,
            double minTime,
            double maxTime,
            double maneuverStart) {

        // Sample transfer times and find minimum delta-V
        int samples = 20;
        double timeStep = (maxTime - minTime) / samples;

        double bestTime = minTime;
        double bestDeltaV = Double.POSITIVE_INFINITY;
        Vector3D bestVelocity = null;

        for (int i = 0; i <= samples; i++) {
            double transferTime = minTime + i * timeStep;

            // Calculate required initial velocity for this transfer time
            Vector3D departureVel = calculateLambertVelocity(
                    initialPos, targetPos, transferTime);

            // Calculate current orbital velocity at initial position
            // Assumes circular orbit for simplification
            double r = initialPos.magnitude();
            double orbitalSpeed = Math.sqrt(centerBody.getGravitationalParameter() / r);
            Vector3D currentVel = initialPos.cross(Vector3D.UNIT_Z).normalize()
                    .multiply(orbitalSpeed);

            // Delta-V required for departure burn
            double departureDeltaV = departureVel.subtract(currentVel).magnitude();

            // Calculate arrival velocity
            Vector3D arrivalVel = calculateLambertArrivalVelocity(
                    initialPos, targetPos, transferTime);

            // Calculate orbital velocity at target
            double rTarget = targetPos.magnitude();
            double targetOrbitalSpeed = Math.sqrt(
                    centerBody.getGravitationalParameter() / rTarget);
            Vector3D targetVel = targetPos.cross(Vector3D.UNIT_Z).normalize()
                    .multiply(targetOrbitalSpeed);

            // Delta-V required for arrival burn
            double arrivalDeltaV = arrivalVel.subtract(targetVel).magnitude();

            // Total delta-V
            double totalDeltaV = departureDeltaV + arrivalDeltaV;

            // Track best option
            if (totalDeltaV < bestDeltaV) {
                bestDeltaV = totalDeltaV;
                bestTime = transferTime;
                bestVelocity = departureVel;
            }
        }

        if (bestVelocity == null) {
            return null;
        }

        // Create orbit for the transfer
        Orbit transferOrbit = Orbit.calculateOrbit(
                centerBody, initialPos, bestVelocity);

        return new ManeuverDetails(
                "Lambert Transfer",
                initialPos,
                bestVelocity,
                targetPos,
                calculateLambertArrivalVelocity(initialPos, targetPos, bestTime),
                bestDeltaV,
                transferOrbit,
                bestTime,
                maneuverStart
        );
    }

    /**
     * Build a bi-elliptic transfer itinerary
     *
     * Bi-elliptic transfers can be more efficient than Hohmann for large radius changes.
     * Uses an intermediate apoapsis beyond the target orbit.
     *
     * Theory: For large radius ratios (>11.94), going "the long way" via a very high
     * intermediate orbit requires less total delta-V than a direct Hohmann transfer.
     */
    public static ManeuverDetails buildBiellipticTransfer(
            Orbit originOrbit,
            Orbit targetOrbit,
            ManeuverDetails previousManeuver) {

        Gravitational centerBody = originOrbit.centerBody();
        double mu = centerBody.getGravitationalParameter();

        // === COPLANAR ADJUSTMENT ===


        // === BI-ELLIPTIC TRANSFER ===
        double r1 = originOrbit.semiMajorAxis();
        double r2 = targetOrbit.semiMajorAxis();

        // Choose intermediate radius (typically 1.5x the larger orbit)
        double r_intermediate = Math.max(r1, r2) * 1.5;

        // BURN 1: Raise apoapsis to intermediate orbit
        double v1 = Math.sqrt(mu / r1);
        double a_transfer1 = (r1 + r_intermediate) / 2.0;
        double v_transfer1_peri = Math.sqrt(mu * (2.0/r1 - 1.0/a_transfer1));
        double dv1 = Math.abs(v_transfer1_peri - v1);


        ManeuverDetails coplanar = RocketryCalculator.adjustToCoplanar(
                Orbit.calculateOrbit(centerBody, previousManeuver.getEndingPosition(), previousManeuver.getEndingVelocity()),
                targetOrbit,
                previousManeuver);

        Orbit intermediateOrbit = new Orbit(a_transfer1,
                (r_intermediate - r1) / (r_intermediate + r1),
                targetOrbit.inclination(), 0, 0, 0, centerBody,
                coplanar.getEndTime());

        ManeuverDetails coplanar2 = RocketryCalculator.adjustToCoplanar(
                Orbit.calculateOrbit(centerBody, previousManeuver.getEndingPosition(), previousManeuver.getEndingVelocity()),
                intermediateOrbit, coplanar);

        ManeuverDetails raise = coplanar2.thenManeuver(
                coplanar2.getEndingPosition().normalize().multiply(r1),
                coplanar2.getEndingVelocity().normalize().multiply(v_transfer1_peri),
                dv1,
                0.0,
                "Bi-elliptic: Raise to intermediate")
                .thenHalfOrbitCoast();


        // BURN 2: At intermediate apoapsis, adjust for final orbit
        double v_int = Math.sqrt(mu * (2.0/r_intermediate - 1.0/a_transfer1));
        double a_transfer2 = (r_intermediate + r2) / 2.0;
        double v_transfer2_apo = Math.sqrt(mu * (2.0/r_intermediate - 1.0/a_transfer2));
        double dv2 = Math.abs(v_transfer2_apo - v_int);


        ManeuverDetails adjust = raise.thenManeuver(
                raise.getEndingPosition(),
                raise.getEndingVelocity().cross(Vector3D.UNIT_Z).normalize().multiply(v_transfer2_apo),
                dv2,
                0.0,
                "Bi-elliptic: Adjust at intermediate")
                .thenHalfOrbitCoast();

        ManeuverDetails match = RocketryCalculator.adjustToCoplanar(adjust.getOrbitState(),targetOrbit,adjust);


        // BURN 3: Circularize at target
        double v_arrival = Math.sqrt(mu * (2.0/r2 - 1.0/a_transfer2));
        double v_target = Math.sqrt(mu / r2);
        double dv3 = Math.abs(v_target - v_arrival);

        return match.thenManeuver(
                match.getEndingPosition(),
                match.getEndingVelocity().cross(Vector3D.UNIT_Z).normalize().multiply(v_target),
                dv3,
                0.0,
                "Bi-elliptic: Circularize at target"
        );
    }

    /**
     * Calculate arrival velocity for Lambert transfer
     * This is the velocity at the target position
     */
    private static Vector3D calculateLambertArrivalVelocity(
            Vector3D initialPos,
            Vector3D targetPos,
            double transferTime) {

        // Use Lagrange coefficients to propagate velocity
        // This is a simplified approach

        double r0 = initialPos.magnitude();
        double r1 = targetPos.magnitude();
        double c = initialPos.subtract(targetPos).magnitude();

        // Lagrange coefficient g
        double g = 1 - (r1 / (2 * (r0 + r1 + c)));

        // Arrival velocity
        Vector3D v0 = calculateLambertVelocity(initialPos, targetPos, transferTime);
        return targetPos.subtract(initialPos.multiply(g)).divide(transferTime);
    }

    /**
     * Improved Lambert velocity calculation using universal variable formulation
     * This is more accurate than the simplified Lagrange approach
     *
     * @param r1 Initial position vector
     * @param r2 Final position vector
     * @param tof Time of flight (seconds)
     * @return Initial velocity vector required for transfer
     */
    public static Vector3D calculateLambertVelocityUniversal(
            Vector3D r1,
            Vector3D r2,
            double tof,
            Gravitational centerBody) {

        double mu = centerBody.getGravitationalParameter();
        double r1Mag = r1.magnitude();
        double r2Mag = r2.magnitude();
        double cosDeltaNu = r1.dot(r2) / (r1Mag * r2Mag);

        // Transfer angle
        double A = Math.sqrt(r1Mag * r2Mag * (1 + cosDeltaNu));

        if (A == 0) {
            // 180-degree transfer - use different approach
            return calculateLambertVelocity(r1, r2, tof);
        }

        // Initial guess for universal variable
        double psi = 0;
        double c2 = 0.5;
        double c3 = 1.0 / 6.0;

        // Iterate to find psi
        for (int iter = 0; iter < 50; iter++) {
            double y = r1Mag + r2Mag + A * (psi * c3 - 1) / Math.sqrt(c2);

            if (y <= 0) {
                // Adjust psi
                psi = 0.8 * (1.0 / c3) * (1 - r1Mag * r2Mag * Math.sqrt(c2) / A);
            }

            double chi = Math.sqrt(y / c2);
            double tofCalculated = Math.pow(chi, 3) * c3 + A * Math.sqrt(y);
            tofCalculated /= Math.sqrt(mu);

            // Check convergence
            if (Math.abs(tofCalculated - tof) < 1e-6) {
                break;
            }

            // Newton-Raphson update
            double dt = tofCalculated - tof;
            double dtdpsi = Math.pow(chi, 3) * (c3 - 3 * c3 * psi / (2 * c2)) / (2 * c2);
            dtdpsi += A / 8.0 * (3 * c3 * Math.sqrt(y) / c2 + A / chi);
            dtdpsi /= Math.sqrt(mu);

            psi -= dt / dtdpsi;

            // Update Stumpff functions
            if (psi > 1e-6) {
                double sqrtPsi = Math.sqrt(psi);
                c2 = (1 - Math.cos(sqrtPsi)) / psi;
                c3 = (sqrtPsi - Math.sin(sqrtPsi)) / Math.sqrt(psi * psi * psi);
            } else if (psi < -1e-6) {
                double sqrtMinusPsi = Math.sqrt(-psi);
                c2 = (1 - Math.cosh(sqrtMinusPsi)) / psi;
                c3 = (Math.sinh(sqrtMinusPsi) - sqrtMinusPsi) /
                        Math.sqrt(-psi * psi * psi);
            } else {
                c2 = 0.5;
                c3 = 1.0 / 6.0;
            }
        }

        // Calculate Lagrange coefficients
        double y = r1Mag + r2Mag + A * (psi * c3 - 1) / Math.sqrt(c2);
        double f = 1 - y / r1Mag;
        double g = A * Math.sqrt(y / mu);

        // Initial velocity
        return r2.subtract(r1.multiply(f)).divide(g);
    }

    /**
     * Calculate velocity for Lambert's problem (simplified)
     * Lambert's Problem is assuming two points are on the same orbit and are separated by some time t what is the
     * composition of the resulting orbit.
     * This method calculates the velocity vector from the initial point required to make that traversal in the given time.
     */
    public static Vector3D calculateLambertVelocity(Vector3D orbitalPointInitial, Vector3D orbitalPointFinal,
                                                     double time) {

        double finalMag = orbitalPointFinal.magnitude();
        double initialMag = orbitalPointInitial.magnitude();
        double diffMag = orbitalPointInitial.subtract(orbitalPointFinal).magnitude();

        // Calculate velocities using Lagrange coefficients (simplified)
        double f = 2 - (finalMag / (2 * (initialMag + finalMag + diffMag))); // Simplified

        return orbitalPointFinal.subtract(orbitalPointInitial.multiply(f)).divide(time);
    }

    /**
     * Takes a phase angle, positive or negative, and moves it to a value between 0 and 2*PI.
     * effectively: A = (PhaseAngle modulo (2PI))
     * NOTE: should be possible with the statement var A = PhaseAngle % (2 * Math.PI),
     * but I haven't verified the accuracy.
     * @param currentPhaseAngle angle in radians to normalize between 0 and 2*PI
     * @return normalized angle in radians
     */
    private static double normalizeAngle (double currentPhaseAngle) {
        if(currentPhaseAngle < 0) {
            return normalizeAngle(currentPhaseAngle + 2 * Math.PI);
        } else if(currentPhaseAngle >= 2 * Math.PI) {
            return normalizeAngle(currentPhaseAngle -  2 * Math.PI);
        } else {
            return currentPhaseAngle;
        }
    }

    /**
     * Calculate sphere of influence (SOI) radius for a body
     * Region where the body's gravity dominates over the primary
     *
     * @param bodyMass Mass of the orbiting body (kg)
     * @param primaryMass Mass of the primary body (kg)
     * @param semiMajorAxis Distance between the bodies (m)
     * @return SOI radius (m)
     */
    static double calculateSphereOfInfluence(double bodyMass,
                                             double primaryMass,
                                             double semiMajorAxis) {
        // SOI radius: r_SOI = a * (m/M)^(2/5)
        return semiMajorAxis * Math.pow(bodyMass / primaryMass, 0.4);
    }


    /**
     * Solve Kepler's equation using Newton-Raphson iteration
     * M = E - e*sin(E)
     */
    static double solveKeplersEquation(double M, double e) {
        double E = M;// Initial guess
        double tolerance = 1e-8;
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            double f = E - e * Math.sin(E) - M;
            double fPrime = 1 - e * Math.cos(E);
            double deltaE = f / fPrime;
            E -= deltaE;

            if (Math.abs(deltaE) < tolerance) {
                break;
            }
        }

        return E;
    }

    /**
     * Convert eccentric anomaly to true anomaly
     */
    static double eccentricAnomalyToTrueAnomaly(double E, double e) {
        double cosNu = (Math.cos(E) - e) / (1 - e * Math.cos(E));
        double sinNu = Math.sqrt(1 - e*e) * Math.sin(E) / (1 - e * Math.cos(E));
        return Math.atan2(sinNu, cosNu);
    }

    /**
     * PIECEWISE TRAJECTORY: Calculate spacecraft position at any point during transfer
     * Uses a simple two-burn Hohmann-like transfer model
     *
     * @param elapsedTime Time since trajectory start (seconds)
     * @return Position and velocity at the given time
     */
    public static PiecewiseState calculateTrajectoryState(ManeuverDetails details,
                                                          double elapsedTime) {
        double cardinalTime = elapsedTime < 0 ? 0 : elapsedTime;
        // Clamp time to trajectory duration


        // Simplified model: instant initial burn, coast, instant final burn
        // More realistic would be finite burn times
        // Note on Claude's documentation: Burn times are generally measured in seconds and the expectation is the
        // simulation is running on a minimum of one hour time steps so burn times are negligible.

        OrbitalState state = details.getOrbitState().calculateOrbitAfterT0(cardinalTime);

        return new PiecewiseState(details.getId() + " at " + elapsedTime, state.position(), state.velocity(), elapsedTime,
                elapsedTime / details.getTimeToExecute());
    }
}
