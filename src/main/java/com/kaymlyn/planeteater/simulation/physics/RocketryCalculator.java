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
                //reentry speed ~333.3 m/s (Transonic speeds) scale factor 3600/1200 = 3
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

    public static List<ManeuverDetails> calculateGeneralTransferManeuvers(

    ) {

    }


    /**
     * Hohmann Transfer between orbits. This treats origin and destination as point-like masses for
     * simplicity. This only makes sense in the absolute sense of transferring between orbits, not between
     * orbiters. Everything is orbiting the sun in this case. This method is mostly here for reference at this time
     * TODO: create version of this method to handle this accurately
     * NOTE: Only accurate for COPLANAR, CIRCULAR orbits
     * For inclined or elliptical orbits, use calculateGeneralTransfer()
     * @param origin
     * @param target
     * @param system
     * @return
     */
    public static ManeuverDetails calculateHohmannTransferBetweenOrbiters(Orbiter origin,
                                                                          Orbiter target,
                                                                          OrbitalSystem system) {
        Orbit originOrbit = Orbit.calculateOrbitInitializerFor(origin);
        Orbit targetOrbit = Orbit.calculateOrbitInitializerFor(target);
        double starMass = PhysicsConstants.G * system.getCentralStar().getMass();
        // Semi-major axis of transfer ellipse
        double semiMajorAxisTransfer = (originOrbit.semiMajorAxis() + targetOrbit.semiMajorAxis()) / 2.0;

        // Orbital velocities
        double v1 = Math.sqrt(starMass / originOrbit.semiMajorAxis());
        double v2 = Math.sqrt(starMass / targetOrbit.semiMajorAxis());

        // Velocities at periapsis and apoapsis of transfer orbit
        double vp = Math.sqrt(starMass * (2.0/originOrbit.semiMajorAxis() - 1.0/semiMajorAxisTransfer));
        double va = Math.sqrt(starMass * (2.0/targetOrbit.semiMajorAxis() - 1.0/semiMajorAxisTransfer));

        // Delta-v requirements
        double deltaV1 = Math.abs(vp - v1);  // Injection burn
        double deltaV2 = Math.abs(v2 - va);  // Circularization burn
        double totalDeltaV = deltaV1 + deltaV2;

        // Transfer time (half orbital period of transfer ellipse)
        double transferTime = Math.PI * Math.sqrt(Math.pow(semiMajorAxisTransfer, 3) /
                (starMass));

        return new ManeuverDetails(origin.getPosition(),origin.getVelocity(),
                calculateOrbitalPositionAfterT0(target,transferTime,system),
                calculateOrbitalVelocityAfterT0(target,transferTime,system),

                );
    }

    public static ManeuverDetails calculateOneTangentBurnBetweenOrbiters(Orbiter origin,
                                                                          Orbiter target,
                                                                          OrbitalSystem system

    ) {


        return new ManeuverDetails();
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
     * Convert true anomaly to eccentric anomaly
     */
    private static double trueAnomalyToEccentricAnomaly(double nu, double e) {
        double anomaly = Math.sqrt(1 - e*e) * Math.sin(nu) / (1 + e * Math.cos(nu));
        return Math.atan2(
                Double.isNaN(anomaly) ? 0.0 : anomaly,
                (e + Math.cos(nu)) / (1 + e * Math.cos(nu))
        );
    }

    /**
     * Convert eccentric anomaly to true anomaly
     */
    private static double eccentricAnomalyToTrueAnomaly(double E, double e) {
        return Math.atan2(
                Math.sqrt(1 - e*e) * Math.sin(E) / (1 - e * Math.cos(E)),
                (Math.cos(E) - e) / (1 - e * Math.cos(E))
        );
    }


    /**
     * Solve Kepler's equation using Newton-Raphson iteration
     * M = E - e*sin(E)
     */
    private static double solveKeplersEquation(double M, double e) {
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
     * Calculate sphere of influence (SOI) radius for a body
     * Region where the body's gravity dominates over the primary
     *
     * @param bodyMass Mass of the orbiting body (kg)
     * @param primaryMass Mass of the primary body (kg)
     * @param semiMajorAxis Distance between the bodies (m)
     * @return SOI radius (m)
     */
    public static double calculateSphereOfInfluence(double bodyMass,
                                                    double primaryMass,
                                                    double semiMajorAxis) {
        // SOI radius: r_SOI = a * (m/M)^(2/5)
        return semiMajorAxis * Math.pow(bodyMass / primaryMass, 0.4);
    }


}
