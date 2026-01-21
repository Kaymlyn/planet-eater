package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Orbit;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;

import java.util.List;

public class RocketryCalculator {

    public static ManeuverDetails calculateTakeoffToStandardOrbit(Gravitational currentLocation,
                                                Spacecraft spacecraft) {

        double mu = PhysicsConstants.G * currentLocation.getMass();
        double rOrbit = currentLocation.getRadius()*Gravitational.STANDARD_ORBIT_MULTIPLIER;

        // Surface Velocity from the ground
        // Sqrt(2 * G * M (1/R - 1/2r))
        double vSurface = Math.sqrt(2 * mu * (1.0/currentLocation.getRadius() - 1.0/(2*rOrbit)));

        // Orbital velocity at target altitude
        double vOrbit = Math.sqrt(mu / rOrbit);

        Vector3D orbitalVelocity = Gravitational.getStandardCircularOrbitVector(currentLocation);
        Vector3D orbitalPosition = Gravitational.getStandardCircularOrbitPosition(currentLocation, Vector3D.randomUnitVector());
        Orbit orbit = Orbit.calculateOrbit(currentLocation,orbitalPosition,orbitalVelocity);
        return new ManeuverDetails(
                spacecraft.getPosition(),spacecraft.getVelocity(),
                orbitalPosition,
                orbitalVelocity,
                vSurface + (vOrbit - Math.sqrt(mu / rOrbit)),
                orbit,
                //distance divided by half orbital velocity
                currentLocation.getRadius()*(Gravitational.STANDARD_ORBIT_MULTIPLIER - 1)*2/orbitalVelocity.magnitude()

        );
    }

    public static ManeuverDetails calculateLandingOnGravitational(Gravitational currentLocation,
                                                   Spacecraft spacecraft) {
        double mu = PhysicsConstants.G * currentLocation.getMass();
        double rOrbit = currentLocation.getRadius() * Gravitational.STANDARD_ORBIT_MULTIPLIER;

        // Orbital velocity
        double vOrbit = Math.sqrt(mu / rOrbit);

        // Surface velocity (if rotating, would subtract rotation speed)
        // For simplification, assume no rotation
        double vSurface = 0.0;

        // Velocity needed to deorbit and descend (Hohmann-like descent)
        // This is a simplification - real landing is more complex
        double vLanding = Math.sqrt(mu / currentLocation.getRadius());

        Vector3D orbitalVelocity = Gravitational.getStandardCircularOrbitVector(currentLocation);
        Vector3D orbitalPosition = Gravitational.getStandardCircularOrbitPosition(currentLocation, Vector3D.randomUnitVector());
        Orbit orbit = Orbit.calculateOrbit(currentLocation,orbitalPosition,orbitalVelocity);

        return new ManeuverDetails(
                spacecraft.getPosition(),spacecraft.getVelocity(),
                null,
                Vector3D.ZERO,
                vOrbit + vLanding - vSurface,
                orbit,
                //reentry speed ~333.3 m/s (Transonic speed) scale factor 3600/1200 = 3
                currentLocation.getRadius()*(Gravitational.STANDARD_ORBIT_MULTIPLIER - 1) * 3
        );
    }

    /**
     *
     * @param target
     * @param targetOrbit
     * @param system
     * @return
     */
    public static double calculateNextLaunchWindowWaitTime(Orbiter origin,
                                                           Orbiter target,
                                                           Orbit targetOrbit,
                                                           OrbitalSystem system) {
        double mu = PhysicsConstants.G * system.getCentralStar().getMass();

        double targetAngle = Math.atan2(target.getPosition().getY(), target.getPosition().getX());
        double currentPhaseAngle = normalizeAngle(targetAngle - Math.atan2(origin.getPosition().getY(), origin.getPosition().getX()));

        double transferTime = Math.PI * Math.sqrt(Math.pow((origin.getPosition().magnitude() + targetOrbit.semiMajorAxis()) / 2, 3) / mu);
        double targetAngularVelocity = Math.sqrt(mu / Math.pow(targetOrbit.semiMajorAxis(), 3));
        double targetTravelAngle = targetAngularVelocity * transferTime;
        double optimalPhaseAngle = normalizeAngle(Math.PI - targetTravelAngle);

        double phaseAngleDifference = normalizeAngle(optimalPhaseAngle - currentPhaseAngle);

        double spacecraftAngularVelocity = origin.getVelocity().magnitude() / origin.getPosition().magnitude();
        double waitTime = phaseAngleDifference / Math.abs(targetAngularVelocity - spacecraftAngularVelocity);

        double synodicPeriod = 2 * Math.PI / Math.abs(targetAngularVelocity - spacecraftAngularVelocity);
        if (waitTime > synodicPeriod / 2) {
            waitTime = synodicPeriod - waitTime;
        }

        return waitTime;
    }

    public static double calculateNextLaunchWindowAbsoluteTime(Orbiter origin,
                                                               Orbiter target,
                                                               Orbit targetOrbit,
                                                               OrbitalSystem system) {
        return calculateNextLaunchWindowWaitTime(origin,target,targetOrbit,system) + system.getCurrentTime();
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
        OrbitalState beginningState = Orbit.calculateOrbitalState(origin);
        OrbitalState endingState = Orbit.calculateOrbitAfterT0(target,transferTime);
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

    public List<PiecewiseState> generateTelemetry(ManeuverDetails details) {
        double fociSeparation = details.getStartingPosition().distanceTo(details.getEndingPosition())/2;
        double majorAxis = Vector3D.ZERO.distanceTo(details.getEndingPosition());
        double eccentricity = fociSeparation/majorAxis;
        
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
            OrbitalState state = Orbit.calculateOrbitAfterT0(origin,0);
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
        OrbitalState originState = Orbit.calculateOrbitalState(origin);

        Vector3D rotatedVelocity = originState.velocity().rotateIn3Space(
                targetInclination.ascendingNode(),
                targetInclination.inclination(),
                targetInclination.periapsis());

        return new ManeuverDetails(
                originState.position(),
                originState.velocity(),
                originState.position(),
                rotatedVelocity,
                deltaVForInclinationChange(origin, targetInclination),
                Orbit.calculateOrbit(origin.centerBody(), originState.position(),rotatedVelocity),
                0
        );
    }

    /**
     * Calculate delta-v required to enter orbit around a body from an approach trajectory
     *
     * @param approachVelocity Velocity relative to the body when entering SOI (m/s)
     * @param bodyMass Mass of the body to orbit (kg)
     * @param bodyRadius Radius of the body (m)
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

                OrbitalState state = Orbit.calculateOrbitalState(origin);
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

        double angularVelocity = Orbit.meanAngularVelocity(origin);
        
        double inclinationDelta = (origin.inclination() - targetInclination.inclination())/2;

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


}
