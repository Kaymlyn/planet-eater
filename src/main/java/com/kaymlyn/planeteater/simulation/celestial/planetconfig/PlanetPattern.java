package com.kaymlyn.planeteater.simulation.celestial.planetconfig;


public enum PlanetPattern {
    EARTH("IRON_NICKEL_CORE", 3.476e6,
            "MG_FE_SILICATE_MANTLE", 2.885e6,
            "ALUMINA_SILICA_CRUST", 6.5e4,
            "NITROGEN_OXYGEN_ATMOSPHERE", 1.0e4),

    VENUS("IRON_NICKEL_CORE", 3.0e6,
            "MG_FE_SILICATE_MANTLE", 2.84e6,
            "ALUMINA_SILICA_CRUST", 2.5e4,
            "GREENHOUSE_ATMOSPHERE", 2.50e5);

    final public String coreType;
    final public double coreRadius;
    final public String mantleType;
    final public double mantleRadius;
    final public String crustType;
    final public double crustRadius;
    final public String atmosphereType;
    final public double atmosphereRadius;

    PlanetPattern(String coreType, double coreRadius,
                  String mantleType, double mantleRadius,
                  String crustType, double crustRadius,
                  String atmosphereType, double atmosphereRadius) {
        this.coreType = coreType;
        this.coreRadius = coreRadius;
        this.mantleType = mantleType;
        this.mantleRadius = mantleRadius + this.coreRadius;
        this.crustType = crustType;
        this.crustRadius = crustRadius + this.mantleRadius;
        this.atmosphereType = atmosphereType;
        this.atmosphereRadius = atmosphereRadius + this.crustRadius;
    }
}
