package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface Gravitational extends Body {
    double STANDARD_ORBIT_MULTIPLIER = 1.1;

    double getMass();
    double getRadius();
    OrbitalSystem getSystem();

    /**
     * Gets a circular orbital vector above the given Gravitational body.
     * The radius of the Standard Circular Orbit is set to an altitude equal to 10% of the Gravitational object over the
     * atmospheric radius. For most bodies this is sufficient to lower gravitational influence enough to allow for
     * orbital transfers.
     * @return Vector3D representation of the velocity vector to maintain the altitude.
     */
    default Vector3D getStandardCircularOrbitVector() {
        return getCircularOrbitVector(getRadius() * (STANDARD_ORBIT_MULTIPLIER - 1));
    }

    /**
     * Gets a circular orbital vector above the given Gravitational body at a given altitude above the atmospheric
     * radius.
     * @return Vector3D representation of the velocity vector to maintain the altitude.
     */
    default Vector3D getCircularOrbitVector(double altitude) {
        double orbitalRadius = getRadius() + altitude;
        double orbitalSpeed = Math.sqrt(getGravitationalParameter() / orbitalRadius);

        // Determine orbital direction (perpendicular to radial)
        Vector3D radial = getPosition().subtract(
                getSystem() != null && getSystem().getCentralStar() != null
                        ? getSystem().getCentralStar().getPosition()
                        : Vector3D.ZERO
        );

        if(radial.magnitude() < 1e-10) {
            radial = Vector3D.UNIT_X;
        }

        // Orbital velocity is perpendicular to radial direction
        // Use cross product with Z-axis to get tangential direction
        Vector3D tangential = Vector3D.UNIT_Z.cross(radial).normalize();

        if(tangential.magnitude() < 1e-10) {
            // Edge case: radial is along Z-axis
            tangential = Vector3D.UNIT_X;
        }

        // Return body's velocity PLUS orbital velocity around body
        return getVelocity().add(tangential.multiply(orbitalSpeed));
    }

    /**
     * Gets a deterministic standard circular orbit position.
     * Places the orbit along the direction from the system's center to this body,
     * ensuring launches are "outward" from the system center.
     *
     * @param directionAboveCenter Optional direction vector. If ZERO, uses position relative to parent.
     * @return Position vector at standard orbital altitude
     */
    default Vector3D getStandardCircularOrbitPosition(Vector3D directionAboveCenter){
        if(directionAboveCenter == Vector3D.ZERO) {
            // For bodies orbiting a star: orbital position points away from star
            Vector3D toThisBody = getPosition().subtract(
                    getSystem().getCentralStar().getPosition()
            );

            if(toThisBody.magnitude() < 1e-10) {
                // At star center - use +X direction
                directionAboveCenter = Vector3D.UNIT_X;
            } else {
                // Point away from star (radial direction)
                directionAboveCenter = toThisBody.normalize();
            }
        }

        // Position is body-relative: radius * 1.1 in specified direction
        // Add getPosition() to get absolute coordinates
        return directionAboveCenter.normalize()
                .multiply(getRadius() * STANDARD_ORBIT_MULTIPLIER)
                .add(getPosition());
    }

    /**
     * Calculate sphere of influence (SOI) radius for a body
     * Region where the body's gravity dominates over the primary
     *
     * @param semiMajorAxis Distance between the bodies (m)
     * @return SOI radius (m)
     */
    default double calculateSphereOfInfluence(Orbiter orbiter,
                                              double semiMajorAxis) {
        // SOI radius: r_SOI = a * (m/M)^(2/5)
        return semiMajorAxis * Math.pow(orbiter.getMass() / getMass(), 0.4);
    }

    default double getGravitationalParameter() {
        return PhysicsConstants.G * getMass();
    }

    default double getOrbitalVelocity(double rOrbit) {
        return Math.sqrt(getGravitationalParameter() / rOrbit);
    }

    default double getEscapeVelocityFromRadius(double radius) {

        // sqrt(2GM(1.0/r - 1.0/2d). when distance = radius sqrt(2GM(2/2r - 1/2r) or sqrt(2GM(1/2r)) or  sqrt(
        return Math.sqrt(2 * getGravitationalParameter() / radius);
    }

    default double getSurfaceEscapeVelocity() {
        return getEscapeVelocityFromRadius(getRadius());
    }

    default double getStandardOrbitalRadius() {
        return getRadius() * STANDARD_ORBIT_MULTIPLIER;
    }

    default double getStandardOrbitalAltitude() {
        return getRadius() * (STANDARD_ORBIT_MULTIPLIER - 1);
    }

    default double getGravitationalForce(double distanceToCenter) {
        return PhysicsConstants.G * Math.pow(getRadius()/distanceToCenter,2);
    }

}