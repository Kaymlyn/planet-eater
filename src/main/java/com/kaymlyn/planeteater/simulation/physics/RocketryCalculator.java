package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;

import java.util.ArrayList;
import java.util.List;

public class RocketryCalculator {

    public static ManeuverDetails calculateTakeoffToStandardOrbit(Gravitational currentLocation,
                                                Spacecraft spacecraft) {

        double rOrbit = currentLocation.getStandardOrbitalRadius();

        Vector3D orbitalVelocity = currentLocation.getStandardCircularOrbitVector();
        Vector3D orbitalPosition = currentLocation.getStandardCircularOrbitPosition( Vector3D.randomUnitVector());
        Orbit orbit = Orbit.calculateOrbit(currentLocation,orbitalPosition,orbitalVelocity);
        return new ManeuverDetails(
                "Take off to Standard Orbit " + spacecraft.getId() + " ^ " + currentLocation,
                spacecraft.getPosition(),spacecraft.getVelocity(),
                orbitalPosition,
                orbitalVelocity,
                currentLocation.getEscapeVelocityFromRadius(rOrbit) + (currentLocation.getOrbitalVelocity(rOrbit) - Math.sqrt(currentLocation.getGravitationalParameter() / rOrbit)),
                orbit,
                //distance divided by half orbital velocity
                currentLocation.getStandardOrbitalAltitude()*2/orbitalVelocity.magnitude()
        );
    }

    public static ManeuverDetails calculateLandingOnGravitational(Gravitational currentLocation,
                                                   Spacecraft spacecraft) {
        double rOrbit = currentLocation.getStandardOrbitalRadius();

        // Surface velocity (if rotating, would subtract rotation speed)
        // For simplification, assume no rotation
        double vSurface = 0.0;

        Orbit orbit = Orbit.calculateOrbit(
                currentLocation,
                currentLocation.getStandardCircularOrbitPosition(Vector3D.randomUnitVector()),
                currentLocation.getStandardCircularOrbitVector());

        return new ManeuverDetails(
                "Landing " + spacecraft.getId() + " v " + currentLocation,
                spacecraft.getPosition(),spacecraft.getVelocity(),
                currentLocation.getPosition(),
                Vector3D.ZERO,
                Math.abs(currentLocation.getOrbitalVelocity(rOrbit) - currentLocation.getOrbitalVelocity(currentLocation.getRadius())),
                orbit,
                //reentry speed ~333.3 m/s (Transonic speed) scale factor 3600/1200 = 3
                currentLocation.getStandardOrbitalAltitude() * 3
        );
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
//
//
//    private static double calculateMeanTransferTime(Orbiter origin, Orbit targetOrbit) {
//        if(origin.getParentBody() == targetOrbit.centerBody()) {
//            return Math.PI * Math.sqrt(
//                    Math.pow((origin.getPosition().magnitude() + targetOrbit.semiMajorAxis()) / 2, 3) / origin.getParentBody().getGravitationalParameter());
//        } else {
//            return Double.NaN;
//        }
//    }

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
    public static ManeuverDetails phaseOrbit(Orbit currentOrbit, double targetTrueAnomaly) {
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
                time
                );
    }

    private static double timeToAnomaly(Orbit currentOrbit, double targetTrueAnomaly) {
        return currentOrbit.orbitalPeriod()/(2 * Math.PI) * (currentOrbit.eccentricAnomalyOnOrbit(targetTrueAnomaly)
                - (currentOrbit.eccentricity()*Math.sin(currentOrbit.eccentricAnomalyOnOrbit(targetTrueAnomaly))));
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
                                                                 Orbit target) {
        double parentMass = PhysicsConstants.G * origin.centerBody().getMass();
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
        OrbitalState beginningState = origin.calculateOrbitalState();
        OrbitalState endingState = target.calculateOrbitAfterT0(transferTime);
        return new ManeuverDetails(
                "Hohmann Transfer " + origin + " > " + target,
                beginningState.position(),
                beginningState.velocity(),
                endingState.position(),
                endingState.velocity(),
                totalDeltaV,
                endingState.orbitalElements(),
                transferTime
        );
    }

    //Hohmann Transfer plotting:
    //Orbital elements
    // a = distance between start and end,
    // e = c
    // distance parent + end = b
    // distance parent to focus = distance between start and end = f = c
    // string plot says 2a + 2c = 2b
    // 2a + 2c = 2b
    // 2c = 2b - 2a
    //

    /**
     * Provides the ManeuverDetails to move from orbiting a local body to the next largest body. The orbit will be coplanar
     * to the starting orbit. This is to escape the Sphere of Influence from the smaller body.
     * This utilizes a Hohmann Transfer
     * NOTE:This will behave poorly if attempting to escape the star. Stars are not currently Orbiters so not valid
     * method arguments.
     * @param origin orbit around orbited
     * @param orbited orbiter being orbited
     * @return ManeuverDetails for use in telemetry and
     */
    public static ManeuverDetails calculateEscapeOrbit(Orbit origin,
                                                       Orbiter orbited) {
        Orbit latestOrbit = orbited.calculateCurrentOrbit();
        double sphereOfInfluenceRadius = calculateSphereOfInfluence(
                orbited.getMass(),
                orbited.getParentBody().getMass(),
                latestOrbit.semiMajorAxis());

        //TODO: validate behavior when origin orbit is not related to orbited body.
        if(origin.semiMajorAxis() > sphereOfInfluenceRadius) { //No action required. Already orbiting parent body.
            OrbitalState state = origin.calculateOrbitalState();
            return new ManeuverDetails(
                    "NoOp Orbiting " + orbited.getId(),
                    state.position(),state.velocity(),
                    state.position(),state.velocity(),
                    0.0,
                    state.orbitalElements(),0);
        } else {
            //Calculate transfer out of orbited body's sphere of influence
            ManeuverDetails details = calculateHohmannTransferBetweenOrbits(origin, new Orbit(sphereOfInfluenceRadius,
                    origin.eccentricity(),
                    origin.inclination(),
                    origin.ascendingNode(),
                    origin.periapsis(),
                    origin.trueAnomaly(),
                    origin.centerBody()));
            //Recalculate orbit in terms of new Parent Body.
            Orbit recalculateOrbit = Orbit.calculateOrbit(origin.centerBody(),details.getEndingPosition().add(orbited.getPosition()),details.getEndingVelocity().add(orbited.getVelocity()));
            //Update ManeuverDetails with new Orbital Details
            return new ManeuverDetails(
                    "Hohmann Transfer To Orbiter " + origin + " > " + orbited.getId(),
                    details.getStartingPosition(),details.getStartingVelocity(),
                    details.getEndingPosition(),details.getEndingVelocity(),
                    details.getDeltaV(),
                    recalculateOrbit,
                    details.getTimeToExecute());
        }
    }

    /**
     * Adjust the origin orbit to be coplanar with another orbit. This only makes sense if both orbits are around the
     * same central body.
     * @param origin
     * @param targetInclination
     * @return
     */
    public static List<ManeuverDetails> adjustToCoplanar(Orbit origin,
                                                         Orbit targetInclination) {
        OrbitalState originState = origin.calculateOrbitalState();
        System.out.println("Coplanar start state " + originState.position());

        Vector3D ascendingCrossOver = origin.getCoincidalAscendingNode(targetInclination);
        double waitToAscendingCrossOver = origin.calculateTimeToPoint(ascendingCrossOver);
        Vector3D descendingCrossOver = origin.getCoincidalDescendingNode(targetInclination);
        double waitToDescendingCrossOver = origin.calculateTimeToPoint(descendingCrossOver);
        Vector3D transferPoint;
        if(waitToAscendingCrossOver < waitToDescendingCrossOver) {
            transferPoint = ascendingCrossOver;
        } else {
            transferPoint = descendingCrossOver;
        }
        Vector3D targetVelocity = targetInclination.calculateVelocityVectorAtPosition(transferPoint);
        ManeuverDetails wait = new ManeuverDetails(origin, Math.min(waitToAscendingCrossOver,waitToDescendingCrossOver));

        List<ManeuverDetails> coplanarManeuvers = new ArrayList<>();
        ManeuverDetails crossOverWait = new ManeuverDetails("Wait until CrossOver",
                wait.getStartingPosition(),
                wait.getStartingVelocity(),
                wait.getEndingPosition(),
                wait.getEndingVelocity(),
                wait.getDeltaV(),
                wait.getOrbitState(),
                wait.getTimeToExecute()
                );

        coplanarManeuvers.add(crossOverWait);

        coplanarManeuvers.add(adjustToCoplanarNow(origin,targetInclination,crossOverWait,targetVelocity));

        return coplanarManeuvers;
    }

    public static ManeuverDetails adjustToCoplanarNow(Orbit origin,
                                                      Orbit targetInclination,
                                                      ManeuverDetails crossOverWait,
                                                      Vector3D targetVelocity) {
        return  new ManeuverDetails(
                "Coplanar Burn " + origin.inclination() + " > " + targetInclination.inclination(),
                crossOverWait.getEndingPosition(),
                crossOverWait.getEndingVelocity(),
                crossOverWait.getEndingPosition(),
                targetVelocity,
                Math.abs(deltaVForInclinationChange(origin, targetInclination)),
                Orbit.calculateOrbit(origin.centerBody(), crossOverWait.getEndingPosition(), targetVelocity),
                0
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
                                                                  double targetOrbitAltitude) {

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

                OrbitalState state = origin.calculateOrbitalState();
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

                // The position and velocity are non-continuous.
                // For the purposes of insertion they are reasonable add we can assume the maneuver itself makes up
                // for the slop. Making the assumption that the deltaV is expended not all at once and is used to not
                // only provide for the capture but also fine control to put it in a circular orbit matching the angle
                // of the ecliptic. Assuming a flat 4 hours for pulling off the maneuver due to slop.
                return new ManeuverDetails(
                        "Orbital Insertion " + destinationOrbiter.getId(),
                        state.position(),
                        state.velocity(),
                        finalOrbit.position(),
                        finalOrbit.velocity(),
                        deltaV,
                        finalOrbit.orbitalElements(),
                        4 * 3600
                );
            }
        }

        //Not a valid Maneuver, so do nothing.
        return new ManeuverDetails(origin);
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
            double maxTime) {

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
                bestTime
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
    public static List<ManeuverDetails> buildBiellipticTransfer(
            Vector3D startingPosition,
            Vector3D startingVelocity,
            Orbit originOrbit,
            Orbit targetOrbit) {

        Gravitational centerBody = originOrbit.centerBody();
        double mu = centerBody.getGravitationalParameter();

        // === COPLANAR ADJUSTMENT ===

        List<ManeuverDetails> route = new ArrayList<>(RocketryCalculator.adjustToCoplanar(
                Orbit.calculateOrbit(centerBody, startingPosition, startingVelocity),
                targetOrbit));

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

        Orbit intermediateOrbit = new Orbit(a_transfer1,
                (r_intermediate - r1) / (r_intermediate + r1),
                targetOrbit.inclination(), 0, 0, 0, centerBody);

        List<ManeuverDetails> intermediate = RocketryCalculator.adjustToCoplanar(
                Orbit.calculateOrbit(centerBody, startingPosition, startingVelocity),
                intermediateOrbit);

        route.add(intermediate.getFirst());
        route.add(new ManeuverDetails(
                "Bi-elliptic: Raise to intermediate",
                intermediate.getLast().getEndingPosition(),
                intermediate.getLast().getEndingVelocity(),
                intermediate.getLast().getEndingPosition().normalize().multiply(r1),
                intermediate.getLast().getEndingVelocity().normalize().multiply(v_transfer1_peri),
                dv1,
                intermediateOrbit,
                0.0));

        // COAST 1: To intermediate apoapsis
        ManeuverDetails coast1 = new ManeuverDetails(intermediateOrbit, Math.PI * Math.sqrt(Math.pow(a_transfer1, 3) / mu));
        route.add(coast1);

        // BURN 2: At intermediate apoapsis, adjust for final orbit
        double v_int = Math.sqrt(mu * (2.0/r_intermediate - 1.0/a_transfer1));
        double a_transfer2 = (r_intermediate + r2) / 2.0;
        double v_transfer2_apo = Math.sqrt(mu * (2.0/r_intermediate - 1.0/a_transfer2));
        double dv2 = Math.abs(v_transfer2_apo - v_int);

        Orbit transfer2 = new Orbit(a_transfer2,
                Math.abs(r2 - r_intermediate) / (r2 + r_intermediate),
                targetOrbit.inclination(), 0, 0, Math.PI, centerBody);

        Vector3D intermediatePos = coast1.getEndingPosition();

        route.add(new ManeuverDetails(
                "Bi-elliptic: Adjust at intermediate",
                intermediatePos,
                intermediatePos.cross(Vector3D.UNIT_Z).normalize().multiply(v_int),
                intermediatePos,
                intermediatePos.cross(Vector3D.UNIT_Z).normalize().multiply(v_transfer2_apo),
                dv2,
                transfer2,
                0.0));

        // COAST 2: To target orbit
        double coast2Time = Math.PI * Math.sqrt(Math.pow(a_transfer2, 3) / mu);
        ManeuverDetails coast2 = new ManeuverDetails(transfer2, coast2Time);
        route.add(coast2);

        RocketryCalculator.adjustToCoplanarNow(coast2.getOrbitState(),targetOrbit,coast2,
                targetOrbit.calculateVelocityVectorAtPosition(coast2.getEndingPosition()));

        // BURN 3: Circularize at target
        double v_arrival = Math.sqrt(mu * (2.0/r2 - 1.0/a_transfer2));
        double v_target = Math.sqrt(mu / r2);
        double dv3 = Math.abs(v_target - v_arrival);

        Vector3D targetPos = coast2.getEndingPosition().normalize().multiply(r2);
        route.add(new ManeuverDetails(
                "Bi-elliptic: Circularize at target",
                targetPos,
                targetPos.cross(Vector3D.UNIT_Z).normalize().multiply(v_arrival),
                targetPos,
                targetPos.cross(Vector3D.UNIT_Z).normalize().multiply(v_target),
                dv3,
                targetOrbit,
                0.0));

        return route;
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



    private static double deltaVForInclinationChange(Orbit origin, Orbit targetInclination) {

        double inclinationDelta = Math.abs(origin.inclination() - targetInclination.inclination())/2;

        double eAsConicSlice = origin.conicSlice();

        //current position in orbit with respect to periapsis
        double currentPositionPastPeriapsis = origin.periapsis() + origin.trueAnomaly();

        return 2.0 * Math.sin(inclinationDelta)
                * origin.distanceToOrbitParallelSemiMajorAxis(origin.trueAnomaly())
                * origin.meanAngularVelocity()
                * origin.semiMajorAxis()
                / (eAsConicSlice * Math.cos(currentPositionPastPeriapsis));
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
    private static double calculateSphereOfInfluence(double bodyMass,
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
    public static PiecewiseState calculateTrajectoryState(ManeuverDetails deets,
                                                          double elapsedTime) {
        double cardinalTime = elapsedTime < 0 ? 0 : Math.min(elapsedTime, deets.getTimeToExecute());
        // Clamp time to trajectory duration


        // Simplified model: instant initial burn, coast, instant final burn
        // More realistic would be finite burn times
        // Note on Claude's documentation: Burn times are generally measured in seconds and the expectation is the
        // simulation is running on a minimum of one hour time steps so burn times are negligible.

        OrbitalState state = deets.getOrbitState().calculateOrbitAfterT0(cardinalTime);

        return new PiecewiseState(deets.getId() + " at " + elapsedTime, state.position(), state.velocity(), elapsedTime,
                elapsedTime / deets.getTimeToExecute());
    }
}
