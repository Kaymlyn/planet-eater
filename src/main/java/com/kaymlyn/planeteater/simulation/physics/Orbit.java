package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.Gravitational;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.celestial.Orbiter;

public record Orbit(double semiMajorAxis,
                    double eccentricity,
                    double inclination,
                    double ascendingNode,
                    double periapsis,
                    double trueAnomaly,
                    Gravitational centerBody,
                    double epoch) {

    //Orbital Creation
    /**
     * Calculate orbital elements from current position and velocity useful for analyzing orbits.
     * This assumes the orbiter is in a 2 body system as 3+body systems are inherently unstable.
     * To calculate a full n-body system use iterative gravitational calculations instead.
     *
     * @return Orbit according to the initial state of the orbiter and the gravitational body.
     * @see OrbitalSystem::stepVerlet()
     */
    /**
     * Calculate orbital elements from current position and velocity.
     *
     * CORRECTED VERSION ensures that:
     *   Orbit orbit = calculateOrbit(body, pos, vel);
     *   OrbitalState state = orbit.calculateOrbitalState();
     *   state.position() ≈ pos (within numerical precision)
     *
     * @param centerBody The gravitational body being orbited
     * @param startPosition Position vector in 3D space (meters)
     * @param startVelocity Velocity vector in 3D space (m/s)
     * @return Orbit with elements that reproduce the input state
     */
    public static Orbit calculateOrbit(Gravitational centerBody,
                                       Vector3D startPosition,
                                       Vector3D startVelocity) {

        System.out.println(startPosition + " " + startVelocity);

        // Specific orbital energy
        double energy = startVelocity.magnitudeSquared() / 2.0 -
                centerBody.getGravitationalParameter() / startPosition.magnitude();

        // Semi-major axis
        double semiMajorAxis = Math.abs(-centerBody.getGravitationalParameter() / (2.0 * energy));

        // Angular momentum vector (NOT velocity)
        Vector3D angularMomentum = startPosition.cross(startVelocity);

        // Eccentricity vector
        Vector3D eccentricity = startVelocity
                .cross(angularMomentum)
                .divide(centerBody.getGravitationalParameter())
                .subtract(startPosition.normalize());

        // Node vector (points to ascending node)
        Vector3D node = Vector3D.UNIT_Z.cross(angularMomentum);

        // === CALCULATE ORBITAL ANGLES ===

        // Inclination (angle from reference plane)
        double inclination = Math.acos(angularMomentum.getZ() / angularMomentum.magnitude());

        // Longitude of ascending node
        double ascendingNode;
        if (node.magnitude() > 1e-10) {
            double omega = Math.acos(node.getX() / node.magnitude());
            ascendingNode = node.getY() < 0 ? 2 * Math.PI - omega : omega;
        } else {
            ascendingNode = 0.0; // Undefined for non-inclined orbits
        }

        // Argument of periapsis
        double argumentOfPeriapsis;
        if (node.magnitude() > 1e-10 && eccentricity.magnitude() > 1e-10) {
            double aop = Math.acos(node.dot(eccentricity) /
                    (node.magnitude() * eccentricity.magnitude()));
            argumentOfPeriapsis = eccentricity.getZ() < 0 ? 2 * Math.PI - aop : aop;
        } else {
            argumentOfPeriapsis = 0.0; // Undefined for circular or non-inclined orbits
        }

        // === CORRECTED TRUE ANOMALY CALCULATION ===
        // Project 3D position into 2D orbital plane BEFORE calculating angle
        Vector3D positionIn2D = startPosition.rotateInto2spaceFrom3space(
                ascendingNode,
                inclination,
                argumentOfPeriapsis
        );

        // Calculate true anomaly in the orbital plane
        // This is the angle from periapsis to the current position, measured in the orbital plane
        double trueAnomaly = Math.atan2(positionIn2D.getY(), positionIn2D.getX());

        // Normalize to [0, 2π]
        trueAnomaly = wrapAngle(trueAnomaly);

        if (eccentricity.magnitude() < 0) {
            throw new IllegalArgumentException("Eccentricity cannot be negative: " + eccentricity);
        }
//        if (eccentricity.magnitude() >= 1.0) {
//            throw new UnsupportedOperationException(
//                    "Hyperbolic/parabolic orbits not yet supported. Eccentricity: " + eccentricity);
//        }

        // Validate semi-major axis
        if (semiMajorAxis <= 0) {
            throw new IllegalArgumentException("Semi-major axis must be positive: " + semiMajorAxis);
        }

        // Validate inclination
        if (inclination < 0 || inclination > Math.PI) {
            throw new IllegalArgumentException(
                    "Inclination must be [0, π]: " + inclination);
        }

        return new Orbit(
                semiMajorAxis,
                eccentricity.magnitude(),
                inclination,
                ascendingNode,
                argumentOfPeriapsis,
                trueAnomaly,
                centerBody,
                centerBody.getSystem().getCurrentTime()  // epoch = now
        );
    }
    /**
     * Convenience method for Orbiter objects
     */
    public static Orbit calculateOrbitFor(Orbiter orbiter) {
        return calculateOrbit(orbiter.getParentBody(),
                orbiter.getPosition(),
                orbiter.getVelocity());
    }

    private static double singularityAdjustment(double trueAnomaly) {
        return Double.isNaN(trueAnomaly) ? 0.0 : wrapAngle(trueAnomaly);
    }

    public static OrbitalState createCircularOrbit(double radius, Gravitational centerBody) {
        return new Orbit(radius, 0.0, 0.0, 0.0, 0.0, 0.0, centerBody,0.0)
                .calculateOrbitalState();
    }

    //Current and Future State Retrieval
    public OrbitalState calculateOrbitalState() {
        double elapsedSinceEpoch = centerBody.getSystem().getCurrentTime() - epoch;
        return calculateOrbitAfterT0(elapsedSinceEpoch);
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
        double eccentricAnomaly = wrapAngle(meanAnomaly() + meanAngularVelocity() * timeElapsed);
        //^Tells us where the orbiter is

        // Solve Kepler's equation via iteration: M = E - e*sin(E) for E
        // The angle past periapsis around the ellipse represented by orbit's eccentricity
        double E = solveKeplersEquation(eccentricAnomaly);

        // Convert eccentric anomaly to true anomaly taking into account the orbit's real eccentricity
        double nu = eccentricToTrueAnomaly(E);

        // Move nu out of the complex plane and into the real.
        double adjustedNu = singularityAdjustment(nu);

        double r = calculateRadiusAtTrueAnomaly(adjustedNu);;

        // Magnitude of orbital velocity at periapsis (K: I think)
        double vMagnitude = Math.sqrt(centerBody.getGravitationalParameter() * (2.0 / r - 1.0 / semiMajorAxis));

        // Velocity direction (perpendicular to radius vector)
        // Flight path angle: tan(φ) = e*sin(ν) / (1 + e*cos(ν))
        double flightPathAngle = Math.atan2(
                getEccentricAnomalySineComponent(trueAnomaly),
                1.0 + eccentricity * Math.cos(trueAnomaly)
        );

        // Velocity components in the 2D orbital plane

        // Position in the 2D orbital plane

        Vector3D position = new Vector3D(r * Math.cos(adjustedNu), r * Math.sin(adjustedNu))
                .rotateInto3spaceFrom2space(
                    ascendingNode,
                    inclination,
                    periapsis
                );
// Velocity components using orbital mechanics formulas
        double radialVelocity = getRadialVelocity(trueAnomaly);
        double tangentialVelocity = tangentialVelocity(trueAnomaly);

        Vector3D velocity = new Vector3D(
                radialVelocity * Math.cos(trueAnomaly) - tangentialVelocity * Math.sin(trueAnomaly),
                radialVelocity * Math.sin(trueAnomaly) + tangentialVelocity * Math.cos(trueAnomaly)
        ).rotateInto3spaceFrom2space(
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
                        adjustedNu,
                        centerBody,
                        timeElapsed
                )
        );
    }

    public double meanAngularVelocity() {
        return Math.sqrt(centerBody.getGravitationalParameter() / Math.pow(semiMajorAxis, 3));
    }

    public double orbitalPeriod() {
        return 2 * Math.PI * Math.sqrt(Math.pow(semiMajorAxis, 3) / centerBody().getGravitationalParameter());
    }

    /**
     * Mean anomaly (period of time since the passing of periapsis) for the orbit based on current True Anomaly
     * @return a
     */
    public double meanAnomaly() {

        // Map current True Anomaly mapped onto an ellipse
        double eccentricAnomaly = trueToEccentricAnomaly(centerBody().getGravitationalParameter());
        return meanAnomaly(eccentricAnomaly);
    }

    /**
     * Mean anomaly (period of time since the passing of periapsis) for the given Eccentric Anomaly
     * @return a
     */
    private double meanAnomaly(double eccentricAnomaly) {
        return eccentricAnomaly - getEccentricAnomalySineComponent(eccentricAnomaly);
    }

    public double semiMinorAxis() {
        return semiMajorAxis * getSemiMinorToSemiMajorAxisRatio();
    }

    private double getRadiusAtEccentricAnomaly(double eccentricAnomaly) {
        return semiMajorAxis * getRadiusRatioAtEccentricAnomaly(eccentricAnomaly);
    }

    public double calculateRadiusAtTrueAnomaly(double trueAnomaly) {
        // Polar equation: r = a(1-e²)/(1+e*cos(ν))
        // where a(1-e²) is the semi-latus rectum
        double semiLatusRectum = semiMajorAxis * (1.0 - eccentricity * eccentricity);
        return semiLatusRectum / (1.0 + eccentricity * Math.cos(trueAnomaly));
    }

    public double getRadiusRatioAtTrueAnomaly(double trueAnomaly) {
        return 1 + eccentricity * Math.cos(trueAnomaly);
    }

    private double getRadiusRatioAtEccentricAnomaly(double eccentricAnomaly) {
        return 1 - eccentricity * Math.cos(eccentricAnomaly);
    }

    /**
     * Eccentric Anomaly is the angle from the center of the ellipse to a point mapped onto a circle with a radius equal
     * to the Semi-Major Axis of the current angle made by the True Anomaly
     * @return the Eccentric Anomaly mapping of the current True Anomaly
     */
    public double eccentricAnomaly() {
        return trueToEccentricAnomaly(trueAnomaly);
    }

    /**
     * The x position of the point on the ellipse indicated by the given True Anomaly in 2 space (no inclination)
     * @param trueAnomaly the True Anomaly to map to 2 space
     * @return the x component of the orbit in 2 space at the True Anomaly
     */
    private double xOrbital(double trueAnomaly) {
        return calculateRadiusAtTrueAnomaly(trueAnomaly) * Math.cos(trueAnomaly);
    }

    /**
     * The y position of the point on the ellipse indicated by the given True Anomaly in 2 space (no inclination)
     * @param trueAnomaly the True Anomaly to map to 2 space
     * @return the y component of the orbit in 2 space at the True Anomaly
     */
    private double yOrbital(double trueAnomaly) {
        return calculateRadiusAtTrueAnomaly(trueAnomaly) * Math.sin(trueAnomaly);
    }

    //Derived Vector Values

    /**
     * The point closest to the foci of the orbital ellipse where the parent mass resides. This is where the orbiter is
     * moving the fastest.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D periapsisPoint() {
        return calculatePositionAtTrueAnomaly(0.0);
    }

    /**
     * The point farthest from the foci of the orbital ellipse where the parent mass resides. This is where the orbiter
     * is moving the slowest.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D apoapsisPoint() {
        return calculatePositionAtTrueAnomaly(Math.PI);
    }

    /**
     * The point on the orbital ellipse equidistant from both foci of the ellipse after passing Periapsis.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D semiMinorPositive() {
        return calculatePositionAtTrueAnomaly(Math.PI / 2.0);
    }

    /**
     * The point on the orbital ellipse equidistant from both foci of the ellipse after passing Apoapsis.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D semiMinorNegative() {
        return calculatePositionAtTrueAnomaly(3.0 * Math.PI / 2.0);
    }

    /**
     * The point in 3 space where the orbital ellipse crosses the reference plane after passing Periapsis.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D getDescendingNode() {
        if(Math.abs(inclination) > 1e-6) {
            return new Vector3D(
                    Math.cos(ascendingNode + Math.PI),
                    Math.sin(ascendingNode + Math.PI),
                    0.0
            ).multiply(calculateRadiusAtTrueAnomaly(Math.PI - periapsis));
        } else {
            return Vector3D.ZERO;
        }
    }

    /**
     * The point in 3 space where the orbital ellipse crosses the reference plane after passing Apoapsis.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D getAscendingNode() {
        if(Math.abs(inclination) > 1e-6) {
            return new Vector3D(
                    Math.cos(ascendingNode),
                    Math.sin(ascendingNode),
                    0.0
            ).multiply(calculateRadiusAtTrueAnomaly(-periapsis));

        } else {
            return Vector3D.ZERO;
        }
    }

    //Point at Time Projection methods
    /**
     * Calculate position vector at a given true anomaly.
     * @return Vector representing a Position in 3 space
     */
    public Vector3D calculatePositionAtTrueAnomaly(double trueAnomaly) {
        return new Vector3D( //Vector in  3 space for the true anomaly
                xOrbital(trueAnomaly),
                yOrbital(trueAnomaly)
        ).rotateInto3spaceFrom2space(ascendingNode,inclination,periapsis); //rotate the ellipse into 3 space
    }

    /**
     * Calculate the velocity vector for a given orbit at a specific position.
     * Uses orbital elements to determine velocity magnitude and direction
     * Note: Makes the assumption that the position vector is on an orbit with the ascending node, inclination, and
     * periapsis of the host orbit. Therefore,the eccentricity, semi-major axis, and true anomaly of the resultant
     * velocity will be inferred. A point not on the host orbit will essentially create a new valid orbit that is
     * co-planar and co-apsidal to the host orbit
     *
     * @param position Position vector
     * @return Velocity vector at this position
     */
    public Vector3D calculateVelocityVectorAtPosition(Vector3D position) {

        // Find true anomaly at this position
        double nu = calculateTrueAnomalyAtPosition(position);

        // Convert to orbital plane cartesian coordinates
        return new Vector3D(
                getRadialVelocity(nu) * Math.cos(nu) - tangentialVelocity(nu) * Math.sin(nu),
                getRadialVelocity(nu) * Math.sin(nu) + tangentialVelocity(nu) * Math.cos(nu)
        ).rotateInto3spaceFrom2space(ascendingNode, inclination, periapsis);
    }

    /**
     * Magnitude of the Velocity Vector at any given True Anomaly on the orbital Ellipse tangent to the orbit.
     * @param nu the given True Anomaly
     * @return the speed of the tangent velocity at the given True Anomaly
     */
    private double tangentialVelocity(double nu) {
        return centerBody.getGravitationalParameter() / conservedAngularMomentum() * (getRadiusRatioAtTrueAnomaly(nu));
    }

    /**
     * Magnitude of the Velocity Vector at any given True Anomaly on the orbital ellipse orthogonal to the Tangent
     * Velocity oriented towards the center of the orbital ellipse.
     * NOTE: double check this description is accurate. this should be the centripetal force. otherwise it's the centrifugal force.
     * @param trueAnomaly the given True Anomaly
     * @return the speed of the radial velocity at the given True Anomaly
     */
    private double getRadialVelocity(double trueAnomaly) {
        return centerBody.getGravitationalParameter() / conservedAngularMomentum() * getEccentricAnomalySineComponent(trueAnomaly);
    }

    private double conservedAngularMomentum() {
        return Math.sqrt(centerBody.getGravitationalParameter() * semiMinorAxis());
    }

    //Time at Point Projection methods
    /**
     * Calculate time to reach a specific point from current position
     *
     * @param targetPoint Target position vector
     * @return Time in seconds to reach target point
     */
    public double calculateTimeToPoint(Vector3D targetPoint) {

        // Convert to eccentric anomalies
        double E_current = eccentricAnomaly();
        double E_target = trueToEccentricAnomaly(calculateTrueAnomalyAtPosition(targetPoint));

        // Convert to mean anomalies
        double M_current = meanAnomaly(E_current);
        double M_target = meanAnomaly(E_target);

        // Time = ΔM / n
        return wrapAngle(M_target - M_current) / meanAngularVelocity();
    }

    /**
     * Angle to the eccentricAnomaly
     * @param eccentricAnomaly Eccentric Anomaly
     * @return the angle on the orbital ellipse to the associated eccentric anomaly
     */
    private double getEccentricAnomalySineComponent(double eccentricAnomaly) {
        return eccentricity * Math.sin(eccentricAnomaly);
    }

    /**
     * Find true anomaly of a given position on the orbit
     * Note: Makes the assumption that the position vector is on an orbit with the ascending node, inclination, and
     * periapsis of the host orbit. Therefore,the eccentricity, semi-major axis, and true anomaly of the resultant
     * velocity will be inferred. A point not on the host orbit will essentially create a new valid orbit that is
     * co-planar and co-apsidal to the host orbit.
     * @param position position in 3 space
     * @return trueAnomaly translation of the given position
     */
    private double calculateTrueAnomalyAtPosition(Vector3D position) {

        Vector3D flattenedOrbit = position.rotateInto2spaceFrom3space(ascendingNode,inclination,periapsis);
        return Math.atan2(flattenedOrbit.getY(), flattenedOrbit.getX());
    }

    /**
     * TODO: Review this is even something that makes sense. It is going to make a bunch of inferences off the host orbit.
     * @param position
     * @return
     */
    private double calculateEccentricAnomalyAtPosition(Vector3D position) {
        Vector3D orbitIn2D = position.rotateInto2spaceFrom3space(ascendingNode,inclination,periapsis);
        double trueAnomaly = Math.atan2(orbitIn2D.getY(), orbitIn2D.getX());
        double anomaly = Math.sqrt(semiMinorAxis()/semiMajorAxis) * Math.sin(trueAnomaly) / getRadiusRatioAtTrueAnomaly(trueAnomaly);
        return Math.atan2(singularityAdjustment(anomaly),
                (eccentricity + Math.cos(trueAnomaly)) / getRadiusRatioAtTrueAnomaly(trueAnomaly));
    }

    //Comparative Analysis
    public Vector3D getCoincidentalAscendingNode(Orbit orbit) {
        OrbitalState state = orbit.calculateOrbitalState();
        Orbit rotatedDown = Orbit.calculateOrbit(this.centerBody,
                state.position().rotateInto2spaceFrom3space(ascendingNode,inclination,periapsis),
                state.velocity().rotateInto2spaceFrom3space(ascendingNode,inclination,periapsis));
        return rotatedDown.getAscendingNode().rotateInto3spaceFrom2space(ascendingNode,inclination,periapsis);
    }
    //Comparative Analysis
    public Vector3D getCoincidentalDescendingNode(Orbit orbit) {
        OrbitalState state = orbit.calculateOrbitalState();
        Orbit rotatedDown = Orbit.calculateOrbit(this.centerBody,
                state.position().rotateInto2spaceFrom3space(ascendingNode,inclination,periapsis),
                state.velocity().rotateInto2spaceFrom3space(ascendingNode,inclination,periapsis));
        return rotatedDown.getDescendingNode().rotateInto3spaceFrom2space(ascendingNode,inclination,periapsis);
    }


    //Utility Methods
    /**
     * Takes a phase angle, positive or negative, and moves it to a value between 0 and 2*PI.
     * effectively: A = (PhaseAngle modulo (2PI))
     * NOTE: should be possible with the statement var A = PhaseAngle % (2 * Math.PI),
     * but I haven't verified the accuracy of floating point modulo and I know this works well enough.
     *
     * @param currentPhaseAngle angle in radians to normalize between 0 and 2*PI
     * @return normalized angle in radians
     */
    public static double wrapAngle(double currentPhaseAngle) {
        double normalized = currentPhaseAngle % (2*Math.PI);
        if(normalized < 0) {
            return normalized + (2*Math.PI);
        } else {
            return normalized;
        }
    }

    /**
     * Convert eccentric anomaly to true anomaly
     */
    public double eccentricToTrueAnomaly(double eccentricAnomaly) {
        return Math.atan2(
                getSemiMinorToSemiMajorAxisRatio() * Math.sin(eccentricAnomaly) / getRadiusRatioAtEccentricAnomaly(eccentricAnomaly),
                (Math.cos(eccentricAnomaly) - eccentricity) / getRadiusRatioAtEccentricAnomaly(eccentricAnomaly)
        );
    }

    /**
     * Convert true anomaly to eccentric anomaly
     */
    public double trueToEccentricAnomaly(double trueAnomaly) {
        double anomaly = getSemiMinorToSemiMajorAxisRatio() * Math.sin(trueAnomaly) / (getRadiusRatioAtTrueAnomaly(trueAnomaly));
        return Math.atan2(
                singularityAdjustment(anomaly),
                (eccentricity + Math.cos(trueAnomaly)) / (getRadiusRatioAtTrueAnomaly(trueAnomaly))
        );
    }

    /**
     * The scale factor that represents what needs to be multiplied to the Semi-Major Axis to get the Semi-Minor Axis
     * @return Axis scale factor
     */
    public double getSemiMinorToSemiMajorAxisRatio() {
        return Math.sqrt(1 - eccentricity * eccentricity);
    }

    public Vector3D rotationVector(){
        return new Vector3D(ascendingNode,inclination,periapsis);
    }

    /**
     * Solve Kepler's equation using Newton-Raphson iteration.
     * M = E - e*sin(E)
     *
     * @param eccentricAnomaly the initial eccentric anomaly
     * @return angle representing the position of a point around an ellipse
     */
    private double solveKeplersEquation(double eccentricAnomaly) {
        double tempEccentricAnomaly = eccentricAnomaly;// Initial guess
        double tolerance = 1e-8;
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            double f = meanAnomaly(tempEccentricAnomaly) - eccentricAnomaly;
            double deltaE = f / getRadiusRatioAtEccentricAnomaly(tempEccentricAnomaly);
            tempEccentricAnomaly -= deltaE;

            if (Math.abs(deltaE) < tolerance) {
                break;
            }
        }

        return tempEccentricAnomaly;
    }

}
