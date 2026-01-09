package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.OrbitInitializer;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Materials;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.LayerProfile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CelestialBodyFactory {

    private final OrbitalSystem system;

    private final HashMap<String,LayerProfile> profiles;

    private final ObjectMapper mapper;

    public CelestialBodyFactory(Star star, double timeStep) {
        mapper = JsonMapper.builder().build();
        profiles = new HashMap<>();
        system = new OrbitalSystem(star,timeStep);
        try {
            List<LayerProfile> profileList = mapper.readValue(
                    new File("src/main/resources/bodyprofiles.json"),
                    new TypeReference<>() {
                    }
            );
            profileList.forEach((layerProfile -> profiles.put(layerProfile.bodyType() + "_" + layerProfile.zone(),layerProfile)));
        } catch (JacksonException je) {
            je.printStackTrace();
        }
    }

    public Star getCentralStar() {
        return system.getCentralStar();
    }

    public Map<String,LayerProfile> getProfiles() {
        return profiles;
    }

//    /**
//     * Create a C-type (carbonaceous) asteroid
//     * Rich in volatiles, organic compounds, and hydrated minerals
//     */
//    public Asteroid createCType(String id, OrbitalSystem system, Vector3D position, Vector3D velocity, double targetRadius) {
//
//
//        Composition comp = new Composition();
//
//        // C-type composition (approximate)
//        double totalVolume = (4.0 / 3.0) * Math.PI * Math.pow(targetRadius, 3);
//
//        comp.addMaterialAsVolume(Material.ORGANIC_COMPOUNDS, (totalVolume * 0.20));
//        comp.addMaterialAsVolume(Material.WATER_ICE, (totalVolume * 0.20));
//        comp.addMaterialAsVolume(Material.CARBON, (totalVolume * 0.10));
//        comp.addMaterialAsVolume(Material.SILICA, (totalVolume * 0.25));
//        comp.addMaterialAsVolume(Material.IRON_OXIDE, (totalVolume * 0.10));
//        comp.addMaterialAsVolume(Material.MAGNESIUM_OXIDE, (totalVolume * 0.10));
//        comp.addMaterialAsVolume(Material.IRON_SULFIDE, (totalVolume * 0.05));
//
//        return new Asteroid(id, system, position, velocity, comp, BodyType.C_TYPE);
//    }

//    /**
//     * Create an S-type (silicaceous) asteroid
//     * Silicate minerals with moderate metal content
//     */
//    public static Asteroid createSType(String id, OrbitalSystem system, Vector3D position, Vector3D velocity, double targetRadius) {
//        Composition comp = new Composition();
//
//        double totalVolume = (4.0 / 3.0) * Math.PI * Math.pow(targetRadius, 3);
//
//        comp.addMaterialAsVolume(Material.OLIVINE, (totalVolume * 0.35));
//        comp.addMaterialAsVolume(Material.PYROXENE, (totalVolume * 0.25));
//        comp.addMaterialAsVolume(Material.IRON, (totalVolume * 0.15));
//        comp.addMaterialAsVolume(Material.FELDSPAR, (totalVolume * 0.10));
//        comp.addMaterialAsVolume(Material.NICKEL, (totalVolume * 0.08));
//        comp.addMaterialAsVolume(Material.IRON_SULFIDE, (totalVolume * 0.05));
//        comp.addMaterialAsVolume(Material.CHROMIUM, (totalVolume * 0.02));
//
//        return new Asteroid(id, system, position, velocity, comp, BodyType.S_TYPE);
//    }

//    /**
//     * Create an M-type (metallic) asteroid
//     * Iron-nickel rich, potentially exposed planetary cores
//     */
//    public static Asteroid createMType(String id, OrbitalSystem system, Vector3D position, Vector3D velocity, double targetRadius) {
//        Composition comp = new Composition();
//
//        double totalVolume = (4.0 / 3.0) * Math.PI * Math.pow(targetRadius, 3);
//
//        comp.addMaterialAsVolume(Material.IRON, (totalVolume * 0.70));
//        comp.addMaterialAsVolume(Material.NICKEL, (totalVolume * 0.18));
//        comp.addMaterialAsVolume(Material.IRON_SULFIDE, (totalVolume * 0.05));
//        comp.addMaterialAsVolume(Material.CHROMIUM, (totalVolume * 0.03));
//        comp.addMaterialAsVolume(Material.PHOSPHORUS, (totalVolume * 0.02));
//        comp.addMaterialAsVolume(Material.SILICA, (totalVolume * 0.02));
//
//        return new Asteroid(id, system, position, velocity, comp, BodyType.M_TYPE);
//    }
    public Planet createArbitraryPlanet(String id,
                                        OrbitInitializer init,
                                        LayerProfile core, double coreRadius,
                                        LayerProfile mantle, double mantleRadius,
                                        LayerProfile crust, double crustRadius,
                                        LayerProfile atmosphere, double atmosphereRadius) {
        return createArbitraryPlanet(id,getCentralStar(),init,core,coreRadius,mantle,mantleRadius,crust,crustRadius,atmosphere,atmosphereRadius);
    }

    public Planet createArbitraryPlanet(String id,
                                        CelestialBody parentBody,
                                        OrbitInitializer init,
                                        LayerProfile core, double coreRadius,
                                        LayerProfile mantle, double mantleRadius,
                                        LayerProfile crust, double crustRadius,
                                        LayerProfile atmosphere, double atmosphereRadius) {

        Planet planet = new Planet(id, parentBody, system, Vector3D.ZERO, Vector3D.ZERO, BodyType.ABERRANT);

        if(core != null)
            planet.getCoreComposition().addBulkMaterial(
                    generateComposition(
                            core.materials(),
                            concaveSphericalVolume(0, coreRadius),
                            core.ratioDivisor()
                    )
            );

        if(mantle != null)
            planet.getMantleComposition().addBulkMaterial(
                    generateComposition(
                            mantle.materials(),
                            concaveSphericalVolume(coreRadius, mantleRadius),
                            mantle.ratioDivisor()
                    )
            );

        if(crust != null)
            planet.getCrustComposition().addBulkMaterial(
                    generateComposition(
                            crust.materials(),
                            concaveSphericalVolume(mantleRadius, crustRadius),
                            crust.ratioDivisor()
                    )
            );
        if(atmosphere != null)
            planet.getAtmosphereComposition().addBulkMaterial(
                    generateComposition(
                            atmosphere.materials(),
                            concaveSphericalVolume(crustRadius, atmosphereRadius),
                            atmosphere.ratioDivisor()
                    )
            );

        system.placeInEllipticalOrbit(planet,
                parentBody,
                init.semiMajorAxis(),
                init.eccentricity(),
                init.inclination(),
                init.ascendingNode(),
                init.periapsis(),
                init.trueAnomaly());


        return planet;
    }

    private static Composition generateComposition(List<Materials> materials, double totalVolume, double ratioDivisor) {
        Composition composition = new Composition();

        for(Materials material : materials) {
            composition.addMaterialAsVolume(
                    material.type(),
                    totalVolume * (material.ratio()/ratioDivisor)
            );
        }
        return composition;
    }

    private static double concaveSphericalVolume(double internalRadius, double externalRadius) {

        if(externalRadius < internalRadius) {
            throw new IllegalArgumentException("internalRadius is larger than externalRadius");
        }

        return ((4.0 / 3.0) * Math.PI * Math.pow(externalRadius, 3))
                - (internalRadius < 0.00 ? (4.0 / 3.0) * Math.PI * Math.pow(internalRadius, 3) : 0);
    }

    /**
     * Create a star with specific mass (in solar masses)
     */
    public static Star createMainSequenceStar(String id, double solarMasses) {
        Star star = new Star(id,Vector3D.ZERO, Vector3D.ZERO);

        double totalMass = solarMasses * PhysicsConstants.SOLAR_MASS;

        star.getTotalComposition().addMaterialAsRawMass(Material.HYDROGEN, totalMass * 0.73);
        star.getTotalComposition().addMaterialAsRawMass(Material.HELIUM, totalMass * 0.25);
        star.getTotalComposition().addMaterialAsRawMass(Material.OXYGEN_GAS, totalMass * 0.01);
        star.getTotalComposition().addMaterialAsRawMass(Material.CARBON, totalMass * 0.003);
        star.getTotalComposition().addMaterialAsRawMass(Material.IRON, totalMass * 0.001);

        // Set up extractable atmosphere proportional to mass loss rate
        double atmosphereMass = star.getMassLossRate() * 1e10; // ~100,000 seconds worth
        star.getAtmosphereComposition().addMaterialAsVolume(Material.HYDROGEN, atmosphereMass * 0.73);
        star.getAtmosphereComposition().addMaterialAsVolume(Material.HELIUM, atmosphereMass * 0.25);
        star.getAtmosphereComposition().addMaterialAsVolume(Material.OXYGEN_GAS, atmosphereMass * 0.01);

        return star;
    }

    /**
     * Convenience Method to create asteroid belts
     * @param beltID        identifier for the asteroid belt
     * @param population    number of asteroids to put in the belt
     * @param minimumRadius minimum radius of an asteroid
     * @param maximumRadius maximum radius of an asteroid
     * @return  list of Asteroids of random compositions and sizes
     */
    public List<Planet> createRandomAsteroidBelt(
            String beltID,
            int population,
            double minimumRadius,
            double maximumRadius,
            double minimumOrbitalRadius,
            double maximumOrbitalRadius,
            long seed) {
        List<Planet> asteroidBelt = new ArrayList<>();

        Random asteroids = new Random(seed);
        OrbitInitializer init = OrbitalSystem.generateRandomOrbitInitializer(
                minimumOrbitalRadius,
                maximumOrbitalRadius,
                0.01,
                Math.PI/4,
                true,
                seed);
        for(int i=0; i < population; i++) {
            asteroidBelt.add(
                    createArbitraryPlanet(beltID + "-BeltAsteroid-"+i,
                            init,
                            null, 0,
                            null, 0,
                            getRandomAsteroidType(asteroids), asteroids.nextDouble(minimumRadius,maximumRadius),
                            null, 0)
//                    createAsteroidByType(getRandomAsteroidType(random),
//                    beltID + "-BeltAsteroid-"+i,
//                    Vector3D.ZERO,
//                    Vector3D.ZERO, random.nextDouble(minimumRadius,maximumRadius)
//                    )
            );
        }
        return asteroidBelt;
    }

    private LayerProfile getRandomAsteroidType(Random random) {
        switch (random.nextInt(0,3)) {
            case 0 -> { return profiles.get("C_TYPE_CRUST"); }
            case 1 -> { return profiles.get("M_TYPE_CRUST"); }
            default -> { return profiles.get("S_TYPE_CRUST"); }
        }
    }

}
