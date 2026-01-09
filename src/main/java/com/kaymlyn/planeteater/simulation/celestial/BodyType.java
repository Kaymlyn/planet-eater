package com.kaymlyn.planeteater.simulation.celestial;

/**
 * Some definitions of extant planetary and asteroid types
 */
public enum BodyType {
    //Asteroids - Non-Differentiated Non-spheres
    C_TYPE,  // Carbonaceous - rich in water, carbon
    S_TYPE,  // Silicaceous - silicates, some metals
    M_TYPE,   // Metallic - iron, nickel rich

    //Planets - differentiated spheres
    BARREN,         //Rocky planets with no atmospheres
    HABITABLE,      //Planets like Earth
    GREENHOUSE,     //Rocky planets with thick atmospheres of greenhouse gases
    ICE_BALLS,      //Icy Planets with ocean mantles and ice crusts

    ICE_GIANT,      //Neptune, Uranus
    GAS_GIANT,      //Jupiter, Saturn
    ABERRANT        //Planets that Make no sense
}
