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
     * @return Orbit according to the initial state of the orbiter and the gravitational body.
     * @see OrbitalSystem::stepVerlet()
     */
    public static Orbit calculateOrbit(Gravitational centerBody, Vector3D startPosition, Vector3D startVelocity) {

        // gravitational force of dominant center mass.

        // Specific orbital energy

        double energy = startVelocity.magnitudeSquared() / 2.0 - centerBody.getGravitationalParameter() / startPosition.magnitude();

        // Semi-major axis
        double semiMajorAxis = -centerBody.getGravitationalParameter() / (2.0 * energy);

        // Angular MOMENTUM vector not velocity
        Vector3D angularMomentum = startPosition.cross(startVelocity);

        // Eccentricity vector
        Vector3D eccentricity = startVelocity
                .cross(angularMomentum).divide(centerBody.getGravitationalParameter())
                .subtract(startPosition.normalize());


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

        return new Orbit(
                semiMajorAxis,
                eccentricity.magnitude(),
                Math.acos((angularMomentum.getZ() + 0.00000001) / angularMomentum.magnitude()),
                ascendingNode,
                argumentOfPeriapsis,
                Double.isNaN(trueAnomaly) ? 0.0 : trueAnomaly,
                centerBody
        );
    }

    public OrbitalState calculateOrbitalState() {
        return calculateOrbitAfterT0(0.0);
    }

    public static Orbit calculateOrbitFor(Orbiter orbiter) {
        return calculateOrbit(orbiter.getParentBody(), orbiter.getPosition(), orbiter.getVelocity());
    }

    public static OrbitalState createCircularOrbit(double radius, Gravitational centerBody) {
        return new Orbit(radius, 0.0, 0.0, 0.0, 0.0, 0.0, centerBody)
                .calculateOrbitalState();
    }

    /**
     * Calculates the Orbital State Vectors and the corresponding Orbital Elements. This information must be
     * adjusted by the absolute position and velocity of the parent body relative to its parent body if not the central
     * star.
     *
     * @param timeElapsed
     * @return
     */
    public OrbitalState calculateOrbitAfterT0(double timeElapsed) {

        //Gravitational strength of orbited body

        // Angle of Eccentric Anomaly projected to given time discarding rotation past 2*PI
        double M = normalizeAngle0and2PI(meanAnomaly() + meanAngularVelocity() * timeElapsed);
        //^Tells us where the orbiter is

        // Solve Kepler's equation via iteration: M = E - e*sin(E) for E
        // The angle past periapsis around the ellipse represented by orbit's eccentricity
        double E = solveKeplersEquation(M, eccentricity);

        // Convert eccentric anomaly to true anomaly taking into account the orbit's real eccentricity
        double nu = eccentricAnomalyToTrueAnomaly(E, eccentricity);

        // Move nu out of the complex plane and into the real.
        double adjustedNu = Double.isNaN(nu) ? 0.0 : nu;

        double r = distanceToBarycenter(E);

        // Magnitude of orbital velocity at periapsis (K: I think)
        double vMagnitude = Math.sqrt(this.centerBody().getGravitationalParameter() * (2.0 / r - 1.0 / semiMajorAxis));

        // Velocity direction (perpendicular to radius vector)
        // Flight path angle: tan(φ) = e*sin(ν) / (1 + e*cos(ν))
        double flightPathAngle = Math.atan2(eccentricity * Math.sin(trueAnomaly),
                1.0 + eccentricity * Math.cos(trueAnomaly));

        // Velocity components in the 2D orbital plane
        double vxOrbital = vMagnitude * Math.cos(trueAnomaly + Math.PI / 2 + flightPathAngle);
        double vyOrbital = vMagnitude * Math.sin(trueAnomaly + Math.PI / 2 + flightPathAngle);

        // Position in the 2D orbital plane
        double xOrbital = r * Math.cos(adjustedNu);
        double yOrbital = r * Math.sin(adjustedNu);

        Vector3D position = new Vector3D(xOrbital, yOrbital).rotateIn3Space(
                ascendingNode,
                inclination,
                periapsis
        );

        Vector3D velocity = new Vector3D(vxOrbital, vyOrbital).rotateIn3Space(
                ascendingNode,
                inclination,
                periapsis
        );

        return new OrbitalState(position,  //Position at indicated time
                velocity,                 //Velocity at indicated time
                new Orbit(semiMajorAxis,          //Orbit with updated True Anomaly at indicated time
                        eccentricity,
                        inclination,
                        ascendingNode,
                        periapsis,
                        nu,
                        centerBody
                )
        );
    }

    public double meanAngularVelocity() {
        return Math.sqrt(centerBody.getGravitationalParameter() / Math.pow(semiMajorAxis, 3));
    }

    public double orbitalPeriod() {
        return 2 * Math.PI * Math.sqrt(Math.pow(semiMajorAxis, 3) / centerBody().getGravitationalParameter());
    }

    public double distancePerSecond() {
        return 2 * Math.PI / orbitalPeriod();
    }

    public double meanAnomaly() {

        // Map current True Anomaly mapped onto an ellipse
        double eccentricAnomaly = trueAnomalyToEccentricAnomaly(centerBody().getGravitationalParameter(), eccentricity);
        return eccentricAnomaly - (eccentricity * Math.sin(eccentricAnomaly));
    }

    public double semiMinorAxis() {
        return semiMajorAxis * Math.sqrt(1 - Math.pow(eccentricity, 2));
    }

    private double distanceToBarycenter(double E) {
        return semiMajorAxis * (1 - eccentricity * Math.cos(E));
    }

    /**
     * Takes a phase angle, positive or negative, and moves it to a value between 0 and 2*PI.
     * effectively: A = (PhaseAngle modulo (2PI))
     * NOTE: should be possible with the statement var A = PhaseAngle % (2 * Math.PI),
     * but I haven't verified the accuracy of floating point modulo and I know this works well enough.
     *
     * @param currentPhaseAngle angle in radians to normalize between 0 and 2*PI
     * @return normalized angle in radians
     */
    public static double normalizeAngle0and2PI(double currentPhaseAngle) {
        if (currentPhaseAngle < 0) {
            return normalizeAngle0and2PI(currentPhaseAngle + 2 * Math.PI);
        } else if (currentPhaseAngle >= 2 * Math.PI) {
            return normalizeAngle0and2PI(currentPhaseAngle - 2 * Math.PI);
        } else {
            return currentPhaseAngle;
        }
    }

    public double eccentricAnomaly() {
        return trueAnomalyToEccentricAnomaly(trueAnomaly, eccentricity);
    }

    public double eccentricAnomaly(double newTrueAnomaly) {
        return trueAnomalyToEccentricAnomaly(newTrueAnomaly, eccentricity);
    }

    /**
     * Convert true anomaly to eccentric anomaly
     */
    private static double trueAnomalyToEccentricAnomaly(double nu, double e) {
        double anomaly = Math.sqrt(1 - e * e) * Math.sin(nu) / (1 + e * Math.cos(nu));
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
                Math.sqrt(1 - e * e) * Math.sin(E) / (1 - e * Math.cos(E)),
                (Math.cos(E) - e) / (1 - e * Math.cos(E))
        );
    }

    /**
     * Solve Kepler's equation using Newton-Raphson iteration.
     * M = E - e*sin(E)
     *
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


}
