package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.physics.Orbit;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetPattern;
import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.physics.OrbitalState;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Materials;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.LayerProfile;
import com.kaymlyn.planeteater.simulation.vehicles.CentralMind;
import lombok.Getter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class CelestialBodyFactory {

    @Getter
    private final OrbitalSystem system;
    private final HashMap<String,LayerProfile> profiles;
    private final static ObjectMapper MAPPER = JsonMapper.builder().build();

    private final static List<String> bodyNames = initBodyNames();
    private final static Random random = new Random(0L);

    public CelestialBodyFactory(Star star, double timeStep) {

        profiles = new HashMap<>();
        system = new OrbitalSystem(star,timeStep);
        try {
            List<LayerProfile> profileList = MAPPER.readValue(
                    new File("src/main/resources/bodyprofiles.json"),
                    new TypeReference<>() {
                    }
            );
            profileList.forEach((layerProfile -> profiles.put(layerProfile.bodyType() + "_" + layerProfile.zone(),layerProfile)));

        } catch (JacksonException exception) {
            exception.printStackTrace();
        }
    }

    public Star getCentralStar() {
        return system.getCentralStar();
    }

    public Map<String,LayerProfile> getProfiles() {
        return profiles;
    }

    public CentralMind createCentralMind(double orbitalRadius) {
        CentralMind centralMind = new CentralMind("KHI Central Mind", system);
        Orbit orbit = system.placeInCircularOrbit(centralMind,orbitalRadius,0);
        centralMind.setInitialOrbit(orbit);
        OrbitalState state = orbit.calculateOrbitalState();
        centralMind.setPosition(state.position());
        centralMind.setVelocity(state.velocity());
        return centralMind;
    }
    public Planet createPlanetFromPattern(String id,
                                          CelestialBody parent,
                                          Orbit init,
                                          PlanetPattern pattern,
                                          double scale) {
        return createArbitraryPlanet(id, parent, init,
                profiles.get(pattern.coreType),pattern.coreRadius*scale,
                profiles.get(pattern.mantleType),pattern.mantleRadius*scale,
                profiles.get(pattern.crustType),pattern.crustRadius*scale,
                profiles.get(pattern.atmosphereType), pattern.atmosphereRadius*scale);

    }

    public Planet createArbitraryPlanet(String id,
                                        Orbit init,
                                        LayerProfile core, double coreRadius,
                                        LayerProfile mantle, double mantleRadius,
                                        LayerProfile crust, double crustRadius,
                                        LayerProfile atmosphere, double atmosphereRadius) {
        return createArbitraryPlanet(id,getCentralStar(),init,core,coreRadius,mantle,mantleRadius,crust,crustRadius,atmosphere,atmosphereRadius);
    }

    public Planet createArbitraryPlanet(String id,
                                        CelestialBody parentBody,
                                        Orbit init,
                                        LayerProfile core, double coreRadius,
                                        LayerProfile mantle, double mantleRadius,
                                        LayerProfile crust, double crustRadius,
                                        LayerProfile atmosphere, double atmosphereRadius) {


        Planet planet = new Planet(id == null || id.isEmpty() ? bodyNames.get(random.nextInt(bodyNames.size())) : id, parentBody, Vector3D.ZERO, Vector3D.ZERO, BodyType.ABERRANT);

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


        planet.initialOrbit = system.placeInEllipticalOrbit(planet,
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

        return ((4.0 / 3.0) * Math.PI * Math.pow(externalRadius, 3)) - ((4.0 / 3.0) * Math.PI * Math.pow(internalRadius, 3));
    }

    /**
     * Create a star with specific mass (in solar masses)
     */
    public static Star createMainSequenceStar(String id, double solarMasses) {

        String name = id != null && !id.isEmpty() ? id : bodyNames.get(random.nextInt(bodyNames.size()));
        Star star = new Star(name,Vector3D.ZERO, Vector3D.ZERO);

        double totalMass = solarMasses * PhysicsConstants.SOLAR_MASS;

        star.getTotalComposition().addMaterialAsRawMass(Material.HYDROGEN, totalMass * 0.73);
        star.getTotalComposition().addMaterialAsRawMass(Material.HELIUM, totalMass * 0.25);
        star.getTotalComposition().addMaterialAsRawMass(Material.OXYGEN_GAS, totalMass * 0.01);
        star.getTotalComposition().addMaterialAsRawMass(Material.CARBON, totalMass * 0.003);
        star.getTotalComposition().addMaterialAsRawMass(Material.IRON, totalMass * 0.001);

        star.initialize();

        return star;
    }

    /**
     * Convenience Method to create asteroid belts
     * @param beltID        identifier for the asteroid belt
     * @param population    number of asteroids to put in the belt
     * @param minimumRadius minimum radius of an asteroid
     * @param maximumRadius maximum radius of an asteroid
     * @return list of Asteroids of random compositions and sizes
     */
    public List<Planet> createRandomAsteroidBelt(
            String beltID,
            int population,
            double minimumRadius,
            double maximumRadius,
            double minimumOrbitalRadius,
            double maximumOrbitalRadius,
            Gravitational centerBody,
            long seed) {
        List<Planet> asteroidBelt = new ArrayList<>();

        Random asteroids = new Random(seed);
        for(int i=0; i < population; i++) {
            Orbit init = OrbitalSystem.generateRandomOrbit(
                    minimumOrbitalRadius,
                    maximumOrbitalRadius,
                    0.01,
                    Math.PI/4,
                    true,
                    asteroids,centerBody);
            asteroidBelt.add(
                    createArbitraryPlanet(beltID + "-BeltAsteroid-"+i,
                            init,
                            null, 0,
                            null, 0,
                            getRandomAsteroidType(asteroids), asteroids.nextDouble(minimumRadius,maximumRadius),
                            null, 0)
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

    private static List<String> initBodyNames() {

        List<String> bodyNames = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File("src/main/resources/bodynames.txt"));
            while (scanner.hasNextLine()) bodyNames.add(scanner.nextLine());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        if(bodyNames.isEmpty()) {
            bodyNames.add("Planet");
        }
        return Collections.unmodifiableList(bodyNames);
    }

}
