package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public record OrbitInitializer(double semiMajorAxis,
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
    public static OrbitInitializer calculateOrbitalInitializer(Gravitational centerBody, Vector3D startPosition, Vector3D startVelocity) {

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

        return new OrbitInitializer(
                semiMajorAxis,
                eccentricity.magnitude(),
                inclination,
                ascendingNode,
                argumentOfPeriapsis,
                trueAnomaly
        );
    }
}
