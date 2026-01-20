package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.physics.OrbitalStateVectors;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.RocketryCalculator;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public record Orbit(double semiMajorAxis,
                    double eccentricity,
                    double inclination,
                    double ascendingNode,
                    double periapsis,
                    double trueAnomaly) {
    /**
     * Calculate orbital elements from current position and velocity
     * Useful for analyzing orbits
     *
     * @return OrbitInitializer containing a recreation of the orbit
     */
    public static Orbit calculateOrbit(Gravitational centerBody, Vector3D startPosition, Vector3D startVelocity) {

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

        // Node vector (points to ascending node)
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
                trueAnomaly
        );
    }

    public static Orbit calculateOrbitInitializerFor(Orbiter orbiter) {
        return calculateOrbit(orbiter.getParentBody(),orbiter.getPosition(),orbiter.getVelocity());
    }

    public static OrbitalStateVectors calculateOrbitalStateVectorsAfterT0(Orbiter orbiter,
                                                                          double timeElapsed,
                                                                          OrbitalSystem system
    ) {
        Orbit orbit = calculateOrbitInitializerFor(orbiter);

        //Gravitational strength of central star
        double gStar = PhysicsConstants.G * system.getCentralStar().getMass();

        // Calculate mean motion: n = sqrt(μ/a³)
        double meanMotion = Math.sqrt(Math.abs(gStar / Math.pow(orbit.semiMajorAxis(), 3)));

        // Map current True Anomaly onto a circular orbit
        double E0 = RocketryCalculator.trueAnomalyToEccentricAnomaly(orbit.trueAnomaly(), orbit.eccentricity());

        // Angle of True Anomaly mapped to a circular orbit and projected to given time discarding rotation past 2*PI
        double M = RocketryCalculator.normalizeAngle(E0 - orbit.eccentricity() * Math.sin(E0) + meanMotion * timeElapsed);

        // Solve Kepler's equation via iteration: M = E - e*sin(E) for E
        double E = RocketryCalculator.solveKeplersEquation(M, orbit.eccentricity());

        // Convert eccentric anomaly to true anomaly based taking the orbit's real eccentricity
        double nu = RocketryCalculator.eccentricAnomalyToTrueAnomaly(E, orbit.eccentricity());

        // Move nu out of the complex plane and into the real.
        double adjustedNu = Double.isNaN(nu) ? 0.0 : nu;

        // Calculate average orbital radius
        double r = orbit.semiMajorAxis() * (1 - orbit.eccentricity() * Math.cos(E));

        // Magnitude of orbital velocity at apoapsis (I think)
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

        // Rotate orbital components around the plane
        double cosO = Math.cos(orbit.ascendingNode());
        double sinO = Math.sin(orbit.ascendingNode());
        double cosi = Math.cos(orbit.inclination());
        double sini = Math.sin(orbit.inclination());
        double cosw = Math.cos(orbit.periapsis());
        double sinw = Math.sin(orbit.periapsis());

        // perform rotation of 2D orbit to 3D orbit with expected inclination.
        double x = xOrbital * (cosO * cosw - sinO * sinw * cosi) -
                yOrbital * (cosO * sinw + sinO * cosw * cosi);
        double y = xOrbital * (sinO * cosw + cosO * sinw * cosi) -
                yOrbital * (sinO * sinw - cosO * cosw * cosi);
        double z = xOrbital * (sinw * sini) + yOrbital * (cosw * sini);
        // Velocity transformation
        double vx = vxOrbital * (cosO * cosw - sinO * sinw * cosi) -
                vyOrbital * (cosO * sinw + sinO * cosw * cosi);
        double vy = vxOrbital * (sinO * cosw + cosO * sinw * cosi) -
                vyOrbital * (sinO * sinw - cosO * cosw * cosi);
        double vz = vxOrbital * (sinw * sini) + vyOrbital * (cosw * sini);

        return new OrbitalStateVectors(new Vector3D(x, y, z), new Vector3D(vx,vy,vz));
    }
}
