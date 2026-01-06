package com.kaymlyn.planeteater.simulation.celestial.planetconfig;

import com.kaymlyn.planeteater.simulation.celestial.BodyType;

public record PlanetInitializer(String id,
                                BodyType type,
                                double orbitalRadius,
                                double inclination,
                                double eccentricity,
                                double apogeeAngle) {
}
