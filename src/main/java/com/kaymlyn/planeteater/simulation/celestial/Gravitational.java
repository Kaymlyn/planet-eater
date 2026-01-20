package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;

public interface Gravitational {
    double STANDARD_ORBIT_MULTIPLIER = 1.1;

    /**
     * Gets a circular orbital vector above the given Gravitational body.
     * The radius of the Standard Circular Orbit is set to an altitude equal to 10% of the Gravitational object over the
     * atmospheric radius. For most bodies this is sufficient to lower gravitational influence enough to allow for
     * orbital transfers.
     * @param parentBody the gravitational body to orbit
     * @return Vector3D representation of the velocity vector to maintain the altitude.
     */
    static Vector3D getStandardCircularOrbitVector(Gravitational parentBody) {
        return getCircularOrbitVector(parentBody, parentBody.getRadius() * (STANDARD_ORBIT_MULTIPLIER - 1));
    }

    /**
     * Gets a circular orbital vector above the given Gravitational body at a given altitude above the atmospheric
     * radius.
     * @param parentBody the gravitational body to orbit
     * @return Vector3D representation of the velocity vector to maintain the altitude.
     */
    static Vector3D getCircularOrbitVector(Gravitational parentBody, double altitude) {

        return parentBody.getVelocity().add(
                new Vector3D(
                        0,
                        Math.sqrt(PhysicsConstants.G * parentBody.getMass() * (1/ (parentBody.getRadius() + altitude)))
                )
        );
    }

    static Vector3D getStandardCircularOrbitPosition(Gravitational parentBody, Vector3D directionAboveCenter){
        //TODO: add derivation from rotational axis in the future. Assume random Axial Tilt for now. Thi will make Vector3D argument unnecessary.
        if(directionAboveCenter == Vector3D.ZERO) {
            return Vector3D.randomUnitVector().multiply(parentBody.getRadius()*STANDARD_ORBIT_MULTIPLIER);
        } else {
            return directionAboveCenter.normalize().multiply(parentBody.getRadius()*STANDARD_ORBIT_MULTIPLIER);
        }
    }

    double getMass();
    Vector3D getVelocity();
    double getRadius();
}
