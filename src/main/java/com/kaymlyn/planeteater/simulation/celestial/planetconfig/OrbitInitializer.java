package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

public record OrbitInitializer(double semiMajorAxis,
                               double eccentricity,
                               double inclination,
                               double ascendingNode,
                               double periapsis,
                               double trueAnomaly) {
}
