package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.physics.OrbitalState;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public record Orbit(double semiMajorAxis,
                    double eccentricity,
                    double inclination,
                    double ascendingNode,
                    double periapsis,
                    double trueAnomaly,
                    Gravitational centerBody) {
    /**
     * Calculate orbital elements from current position and velocity useful for analyzing orbits.
     * This assumes the orbiter is in a 2 body system as 3+body systems are inherently unstable.
     * To calculate a full n-body system use iterative gravitational calculations instead.
     *
     * @see OrbitalSystem::stepVerlet()
     *
     * @return Orbit according to the initial state of the orbiter and the gravitational body.
     */
    public static Orbit calculateOrbit(Gravitational centerBody, Vector3D startPosition, Vector3D startVelocity) {

        // gravitational force of dominant center mass.
        double mu = PhysicsConstants.G * centerBody.getMass();

        // Specific orbital energy
        double energy = startVelocity.magnitudeSquared() / 2.0 - mu / startPosition.magnitude();

        // Semi-major axis
        double semiMajorAxis = -mu / (2.0 * energy);

        // Angular momentum vector
        Vector3D angularMomentum = startPosition.cross(startVelocity);
        double hMag = angularMomentum.magnitude();

        // Eccentricity vector
        Vector3D eccentricity = startVelocity.cross(angularMomentum).divide(mu).subtract(startPosition.normalize());

        // Inclination
        double inclination = Math.acos((angularMomentum.getZ()+0.00000001) / hMag);

        // Node vector tangent to the ecliptic (points to ascending node)
        Vector3D node = Vector3D.UNIT_Z.cross(angularMomentum);

        // Longitude of ascending node
        double ascendingNode;
        if (node.magnitude() > 1e-10) {
            double w = Math.acos(node.getX() / node.magnitude());
            ascendingNode = node.getY() < 0 ? 2 * Math.PI - w : w;
        } else {
            ascendingNode = 0.0; // Undefined for non-inclined orbits
        }

        // Argument of periapsis
        double argumentOfPeriapsis;
        if (node.magnitude() > 1e-10 && eccentricity.magnitude() > 1e-10) {
            double aop = Math.acos(node.dot(eccentricity) / (node.magnitude() * eccentricity.magnitude()));
            argumentOfPeriapsis = eccentricity.getZ() < 0 ? 2 * Math.PI - aop : aop;
        } else {
            argumentOfPeriapsis = 0.0; // Undefined for circular or non-inclined orbits
        }

        // True anomaly
        double trueAnomaly;
        if (eccentricity.magnitude() > 1e-10) {
            double ta = Math.acos(eccentricity.dot(startPosition) / (eccentricity.magnitude() * startPosition.magnitude()));
            trueAnomaly = startPosition.dot(startVelocity) < 0 ? 2 * Math.PI - ta : ta;
        } else {
            // For circular orbits, measure from ascending node
            if (node.magnitude() > 1e-10) {
                double ta = Math.acos(node.dot(startPosition) / (node.magnitude() * startPosition.magnitude()));
                trueAnomaly = startPosition.getZ() < 0 ? 2 * Math.PI - ta : ta;
            } else {
                trueAnomaly = Math.atan2(startPosition.getY(), startPosition.getX());
            }
        }
        if(Double.isNaN(trueAnomaly)) {
            trueAnomaly = 0.0;
        }

        return new Orbit(
                semiMajorAxis,
                eccentricity.magnitude(),
                inclination,
                ascendingNode,
                argumentOfPeriapsis,
                trueAnomaly,
                centerBody
        );
    }

    public static OrbitalState calculateOrbitalState(Orbit orbit) {
        return calculateOrbitAfterT0(orbit,0.0);
    }

    public static Orbit calculateOrbitFor(Orbiter orbiter) {
        return calculateOrbit(orbiter.getParentBody(),orbiter.getPosition(),orbiter.getVelocity());
    }

    public static OrbitalState createCircularOrbit(double radius, Gravitational centerBody) {
        return calculateOrbitalState(new Orbit(radius,0.0,0.0,0.0,0.0,0.0, centerBody));
    }

    /**
     * Calculates the Orbital State Vectors and the corresponding Orbital Elements. This information must be
     * adjusted by the absolute position and velocity of the parent body relative to its parent body if not the central
     * star.
     * @param orbit
     * @param timeElapsed
     * @return
     */
    public static OrbitalState calculateOrbitAfterT0(Orbit orbit,
                                                     double timeElapsed) {

        //Gravitational strength of orbited body
        double gStar = PhysicsConstants.G * orbit.centerBody.getMass();

        // Calculate mean motion: n = sqrt(μ/a³)
        double meanMotion = Math.sqrt(Math.abs(gStar / Math.pow(orbit.semiMajorAxis(), 3)));

        // Map current True Anomaly mapped onto an ellipse
        double E0 = trueAnomalyToEccentricAnomaly(orbit.trueAnomaly(), orbit.eccentricity());

        // Angle of Eccentric Anomaly projected to given time discarding rotation past 2*PI
        double M = normalizeAngle(E0 - orbit.eccentricity() * Math.sin(E0) + meanMotion * timeElapsed);

        // Solve Kepler's equation via iteration: M = E - e*sin(E) for E
        // The angle past periapsis around the ellipse represented by orbit's eccentricity
        double E = solveKeplersEquation(M, orbit.eccentricity());

        // Convert eccentric anomaly to true anomaly taking into account the orbit's real eccentricity
        double nu = eccentricAnomalyToTrueAnomaly(E, orbit.eccentricity());

        // Move nu out of the complex plane and into the real.
        double adjustedNu = Double.isNaN(nu) ? 0.0 : nu;

        // Calculate average orbital radius
        double r = orbit.semiMajorAxis() * (1 - orbit.eccentricity() * Math.cos(E));

        // Magnitude of orbital velocity at periapsis (K: I think)
        double vMagnitude = Math.sqrt(gStar * (2.0 / r - 1.0 / orbit.semiMajorAxis()));

        // Velocity direction (perpendicular to radius vector)
        // Flight path angle: tan(φ) = e*sin(ν) / (1 + e*cos(ν))
        double flightPathAngle = Math.atan2(orbit.eccentricity() * Math.sin(orbit.trueAnomaly()),
                1.0 + orbit.eccentricity() * Math.cos(orbit.trueAnomaly()));

        // Velocity components in the 2D orbital plane
        double vxOrbital = vMagnitude * Math.cos(orbit.trueAnomaly() + Math.PI/2 + flightPathAngle);
        double vyOrbital = vMagnitude * Math.sin(orbit.trueAnomaly() + Math.PI/2 + flightPathAngle);

        // Position in the 2D orbital plane
        double xOrbital = r * Math.cos(adjustedNu);
        double yOrbital = r * Math.sin(adjustedNu);

        Vector3D position = new Vector3D(xOrbital,yOrbital).rotateIn3Space(
                orbit.ascendingNode(),
                orbit.inclination(),
                orbit.periapsis()
        );

        Vector3D velocity = new Vector3D(vxOrbital,vyOrbital).rotateIn3Space(
                orbit.ascendingNode(),
                orbit.inclination(),
                orbit.periapsis()
        );

        return new OrbitalState(position,  //Position at indicated time
                velocity,                 //Velocity at indicated time
                new Orbit(orbit.semiMajorAxis,          //Orbit with updated True Anomaly at indicated time
                        orbit.eccentricity,
                        orbit.inclination,
                        orbit.ascendingNode,
                        orbit.periapsis,
                        nu,
                        orbit.centerBody
                )
        );
    }

    /**
     * Projects the orbital state into the future by a given amount of time for a Star Orbiting body.
     * @param orbiter orbiter to predict
     * @param timeElapsed time from current time
     * @return OrbitalState of the orbiter after the given time has elapsed
     */
    public static OrbitalState calculateOrbitAfterT0(Orbiter orbiter,
                                                     double timeElapsed
    ) {
        return calculateOrbitAfterT0(calculateOrbitFor(orbiter),timeElapsed);
    }

    /**
     * Takes a phase angle, positive or negative, and moves it to a value between 0 and 2*PI.
     * effectively: A = (PhaseAngle modulo (2PI))
     * NOTE: should be possible with the statement var A = PhaseAngle % (2 * Math.PI),
     * but I haven't verified the accuracy of floating point modulo and I know this works well enough.
     * @param currentPhaseAngle angle in radians to normalize between 0 and 2*PI
     * @return normalized angle in radians
     */
    public static double normalizeAngle (double currentPhaseAngle) {
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
     * Solve Kepler's equation using Newton-Raphson iteration.
     * M = E - e*sin(E)
     * @param M
     * @param e
     * @return angle representing the position of a point around an ellipse
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

    public static double meanAngularVelocity(Orbit origin) {
        return Math.sqrt(origin.centerBody().getMass()*PhysicsConstants.G / Math.pow(origin.semiMajorAxis(), 3));
    }
}
