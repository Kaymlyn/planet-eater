package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;

public class RocketryCalculator {

    public static ManeuverDetails calculateTakeoffToStandardOrbit(Gravitational currentLocation,
                                                Spacecraft spacecraft) {

        double rOrbit = currentLocation.getStandardOrbitalRadius();

        Vector3D orbitalVelocity = currentLocation.getStandardCircularOrbitVector();
        Vector3D orbitalPosition = currentLocation.getStandardCircularOrbitPosition( Vector3D.randomUnitVector());
        Orbit orbit = Orbit.calculateOrbit(currentLocation,orbitalPosition,orbitalVelocity);
        return new ManeuverDetails(
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
                spacecraft.getPosition(),spacecraft.getVelocity(),
                null,
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

//
//    /**
//     *
//     * @param target
//     * @param targetOrbit
//     * @return
//     */
//    public static double calculateNextLaunchWindowWaitTime(Orbiter origin,
//                                                           Orbiter target,
//                                                           Orbit targetOrbit) {
//
//        double currentPhaseAngle = origin.getPosition().angleBetween(target.getPosition());
//
//        double meanAngularVelocity = targetOrbit.meanAngularVelocity();
//
//        double meanTransferTime = calculateMeanTransferTime(origin,targetOrbit);
//        if(Double.isNaN(meanTransferTime)) meanTransferTime = 0.0;
//        return getMinimalWaitTime(
//                normalizeAngle(Math.PI - meanAngularVelocity * meanTransferTime - currentPhaseAngle),
//                meanAngularVelocity,
//                origin.getVelocity().magnitude() / origin.getPosition().magnitude());
//    }


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
//
//    public static double calculateNextLaunchWindowAbsoluteTime(Orbiter origin,
//                                                               Orbiter target,
//                                                               Orbit targetOrbit,
//                                                               OrbitalSystem system) {
//        return calculateNextLaunchWindowWaitTime(origin,target,targetOrbit) + system.getCurrentTime();
//    }

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
                state.position(),
                state.velocity(),
                futureState.position(),
                futureState.velocity(),
                2*(phasedAngularMomentum/r) - (state.angularMomentum()/r),futureState.orbitalElements(),
                time
                );
    }

    private static double timeToAnomaly(Orbit currentOrbit, double targetTrueAnomaly) {
        return currentOrbit.orbitalPeriod()/(2 * Math.PI) * (currentOrbit.eccentricAnomaly(targetTrueAnomaly)
                - (currentOrbit.eccentricity()*Math.sin(currentOrbit.eccentricAnomaly(targetTrueAnomaly))));
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
        System.out.println("delta V : " + totalDeltaV);
        return new ManeuverDetails(
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
            Orbit recalculateOrbit = Orbit.calculateOrbit(origin.centerBody(),details.getEndingPosition(),details.getEndingVelocity());
            //Update ManeuverDetails with new Orbital Details
            return new ManeuverDetails(
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
    public static ManeuverDetails adjustToCoplanar(Orbit origin,
                                                   Orbit targetInclination) {
        OrbitalState originState = origin.calculateOrbitalState();

        Vector3D rotatedVelocity = originState.velocity().rotateIn3Space(
                targetInclination.ascendingNode(),
                targetInclination.inclination(),
                targetInclination.periapsis());

        return new ManeuverDetails(
                originState.position(),
                originState.velocity(),
                originState.position(),
                rotatedVelocity,
                Math.abs(deltaVForInclinationChange(origin, targetInclination)),
                Orbit.calculateOrbit(origin.centerBody(), originState.position(),rotatedVelocity),
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

    private static double deltaVForInclinationChange(Orbit origin, Orbit targetInclination) {

        double angularVelocity = origin.meanAngularVelocity();
        
        double inclinationDelta = Math.abs(origin.inclination() - targetInclination.inclination())/2;

        //representation of e as a conic slice (K: i think?)
        double eAsConicSlice = Math.sqrt(1 - Math.pow(origin.eccentricity(),2));

        //current position in orbit with respect to periapsis
        double currentPositionPastPeriapsis = origin.periapsis() + origin.trueAnomaly();

        //1 if circular orbit
        double adjustForEccentricity = 1 + origin.eccentricity()*Math.cos(origin.trueAnomaly());

        return 2.0 * Math.sin(inclinationDelta) * (adjustForEccentricity) * angularVelocity * origin.semiMajorAxis()/
                        (eAsConicSlice * Math.cos(currentPositionPastPeriapsis));
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
        System.out.println("Elasped Time : " + elapsedTime);
        // Clamp time to trajectory duration
        if (elapsedTime < 0) elapsedTime = 0;
        if (elapsedTime > deets.getTimeToExecute()) elapsedTime = deets.getTimeToExecute();

        // Simplified model: instant initial burn, coast, instant final burn
        // More realistic would be finite burn times
        // Note on Claude's documentation: Burn times are generally measured in seconds and the expectation is the
        // simulation is running on a minimum of one hour time steps so burn times are negligible.

        // Calculate transfer orbit parameters
        double r1 = deets.getStartingPosition().magnitude();
        double r2 = deets.getEndingPosition().magnitude();
        double a = (r1 + r2) / 2.0; // Semi-major axis of transfer orbit

        // Position along transfer ellipse
        double n = Math.sqrt(deets.getOrbitState().meanAngularVelocity()); // Mean motion
        double M = n * elapsedTime; // Mean anomaly

        // Solve for eccentric anomaly
        double e = Math.abs(r2 - r1) / (r1 + r2); // Eccentricity of transfer
        double E = solveKeplersEquation(M, e);

        // True anomaly
        double nu = eccentricAnomalyToTrueAnomaly(E, e);

        // Radius at this point
        double r = a * (1 - e * Math.cos(E));

        // Direction from start to end
        Vector3D direction = deets.getEndingPosition().subtract(deets.getStartingPosition()).normalize();

        // Simplified: position along the straight line weighted by true anomaly
        // (Real trajectory would follow ellipse)
        double fraction = nu / Math.PI; // 0 to 1 over half orbit
        Vector3D currentPos = deets.getStartingPosition().add(direction.multiply(r * fraction));

        System.out.println("Piecewise Position: " + currentPos);

        // Velocity magnitude from vis-viva equation
        double v = Math.sqrt(deets.getOrbitState().centerBody().getGravitationalParameter() * (2.0/r - 1.0/a));

        // Velocity direction (perpendicular to radius, in direction of motion)
        Vector3D velocityDirection = new Vector3D(-direction.getY(), direction.getX(), 0).normalize();
        Vector3D currentVel = velocityDirection.multiply(v);

        return new PiecewiseState(currentPos, currentVel, elapsedTime,
                elapsedTime / deets.getTimeToExecute());
    }
}
