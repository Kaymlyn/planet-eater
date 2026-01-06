package com.kaymlyn.planeteater.simulation.celestial;

import com.kaymlyn.planeteater.simulation.celestial.planetconfig.PlanetInitializer;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Composition;
import com.kaymlyn.planeteater.simulation.resources.Material;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Materials;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.LayerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CelestialBodyFactory {

    public static Asteroid createAsteroidByType(BodyType type,
                                                String id,
                                                Vector3D position,
                                                Vector3D velocity,
                                                double targetRadius) {
        switch(type) {
            case C_TYPE -> {
                return createCType(id,position,velocity,targetRadius);
            }
            case M_TYPE -> {
                return createMType(id,position,velocity,targetRadius);
            }
            case S_TYPE -> {
                return createSType(id,position,velocity,targetRadius);
            }
            default -> {
                return null;
            }
        }
    }
    /**
     * Create a C-type (carbonaceous) asteroid
     * Rich in volatiles, organic compounds, and hydrated minerals
     */
    public static Asteroid createCType(String id, Vector3D position, Vector3D velocity, double targetRadius) {
        Composition comp = new Composition();

        // C-type composition (approximate)
        double totalVolume = (4.0 / 3.0) * Math.PI * Math.pow(targetRadius, 3);

        comp.addMaterialAsVolume(Material.ORGANIC_COMPOUNDS, (totalVolume * 0.20));
        comp.addMaterialAsVolume(Material.WATER_ICE, (totalVolume * 0.20));
        comp.addMaterialAsVolume(Material.CARBON, (totalVolume * 0.10));
        comp.addMaterialAsVolume(Material.SILICA, (totalVolume * 0.25));
        comp.addMaterialAsVolume(Material.IRON_OXIDE, (totalVolume * 0.10));
        comp.addMaterialAsVolume(Material.MAGNESIUM_OXIDE, (totalVolume * 0.10));
        comp.addMaterialAsVolume(Material.IRON_SULFIDE, (totalVolume * 0.05));

        return new Asteroid(id, position, velocity, comp, BodyType.C_TYPE);
    }

    /**
     * Create an S-type (silicaceous) asteroid
     * Silicate minerals with moderate metal content
     */
    public static Asteroid createSType(String id, Vector3D position, Vector3D velocity, double targetRadius) {
        Composition comp = new Composition();

        double totalVolume = (4.0 / 3.0) * Math.PI * Math.pow(targetRadius, 3);

        comp.addMaterialAsVolume(Material.OLIVINE, (totalVolume * 0.35));
        comp.addMaterialAsVolume(Material.PYROXENE, (totalVolume * 0.25));
        comp.addMaterialAsVolume(Material.IRON, (totalVolume * 0.15));
        comp.addMaterialAsVolume(Material.FELDSPAR, (totalVolume * 0.10));
        comp.addMaterialAsVolume(Material.NICKEL, (totalVolume * 0.08));
        comp.addMaterialAsVolume(Material.IRON_SULFIDE, (totalVolume * 0.05));
        comp.addMaterialAsVolume(Material.CHROMIUM, (totalVolume * 0.02));

        return new Asteroid(id, position, velocity, comp, BodyType.S_TYPE);
    }

    /**
     * Create an M-type (metallic) asteroid
     * Iron-nickel rich, potentially exposed planetary cores
     */
    public static Asteroid createMType(String id, Vector3D position, Vector3D velocity, double targetRadius) {
        Composition comp = new Composition();

        double totalVolume = (4.0 / 3.0) * Math.PI * Math.pow(targetRadius, 3);

        comp.addMaterialAsVolume(Material.IRON, (totalVolume * 0.70));
        comp.addMaterialAsVolume(Material.NICKEL, (totalVolume * 0.18));
        comp.addMaterialAsVolume(Material.IRON_SULFIDE, (totalVolume * 0.05));
        comp.addMaterialAsVolume(Material.CHROMIUM, (totalVolume * 0.03));
        comp.addMaterialAsVolume(Material.PHOSPHORUS, (totalVolume * 0.02));
        comp.addMaterialAsVolume(Material.SILICA, (totalVolume * 0.02));

        return new Asteroid(id, position, velocity, comp, BodyType.M_TYPE);
    }

    public static Planet createArbitraryPlanet(PlanetInitializer init,
                                               LayerProfile core,
                                               LayerProfile mantle,
                                               LayerProfile crust,
                                               LayerProfile atmosphere) {

        Planet planet = new Planet(init.id(), Vector3D.ZERO, Vector3D.ZERO, BodyType.ABBERANT);


        planet.getCoreComposition().addBulkMaterial(
                generateComposition(
                        core.materials(),
                        concaveSphericalVolume(0, core.externalRadius()),
                        core.ratioDivisor()
                )
        );
        planet.getMantleComposition().addBulkMaterial(
                generateComposition(
                        mantle.materials(),
                        concaveSphericalVolume(core.externalRadius(), mantle.externalRadius()),
                        mantle.ratioDivisor()
                )
        );
        planet.getCrustComposition().addBulkMaterial(
                generateComposition(
                        crust.materials(),
                        concaveSphericalVolume(mantle.externalRadius(), crust.externalRadius()),
                        crust.ratioDivisor()
                )
        );
        planet.getAtmosphereComposition().addBulkMaterial(
                generateComposition(
                        crust.materials(),
                        concaveSphericalVolume(crust.externalRadius(), atmosphere.externalRadius()),
                        atmosphere.ratioDivisor()
                )
        );
        return planet;
    }

    private static Composition generateComposition(List<Materials> materials, double totalVolume, double ratioDivisor) {
        Composition composition = new Composition();

        for(Materials material : materials) {
            composition.addMaterialAsVolume(
                    material.material(),
                    totalVolume * ((double)material.ratio()/ratioDivisor)
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
     * Create an Earth-like terrestrial planet
     * Composition by mass: ~32% Fe, ~30% O, ~15% Si, ~14% Mg, ~3% S, ~2% Ni, ~1.5% Ca, ~1.4% Al
     */
    public static Planet createEarthLike(String id, Vector3D position, Vector3D velocity) {
        Planet planet = new Planet(id, position, velocity, BodyType.HABITABLE);

        // Core: Iron-nickel with sulfur (radius ~3480 km, ~32% of Earth's mass)
        double coreVolume = (4.0 / 3.0) * Math.PI * Math.pow(3.48e6, 3);
        planet.getCoreComposition().addMaterialAsVolume(Material.IRON, (coreVolume * 0.85));
        planet.getCoreComposition().addMaterialAsVolume(Material.NICKEL, (coreVolume * 0.10));
        planet.getCoreComposition().addMaterialAsVolume(Material.IRON_SULFIDE, (coreVolume * 0.05));

        // Mantle: Silicates dominated (radius ~6370 km from core, ~68% of mass)
        double mantleVolume = (4.0 / 3.0) * Math.PI *
            (Math.pow(6.37e6, 3) - Math.pow(3.48e6, 3));
        planet.getMantleComposition().addMaterialAsVolume(Material.OLIVINE, (mantleVolume * 0.50));
        planet.getMantleComposition().addMaterialAsVolume(Material.PYROXENE, (mantleVolume * 0.25));
        planet.getMantleComposition().addMaterialAsVolume(Material.MAGNESIUM_OXIDE, (mantleVolume * 0.15));
        planet.getMantleComposition().addMaterialAsVolume(Material.IRON_OXIDE, (mantleVolume * 0.10));

        // Crust: Silicates, feldspars, various minerals (~0.4% of mass)
        double crustVolume = (4.0 / 3.0) * Math.PI *
            (Math.pow(6.371e6, 3) - Math.pow(6.37e6, 3));
        planet.getCrustComposition().addMaterialAsVolume(Material.FELDSPAR, (crustVolume * 0.40));
        planet.getCrustComposition().addMaterialAsVolume(Material.SILICA, (crustVolume * 0.30));
        planet.getCrustComposition().addMaterialAsVolume(Material.PYROXENE, (crustVolume * 0.10));
        planet.getCrustComposition().addMaterialAsVolume(Material.ALUMINUM_OXIDE, (crustVolume * 0.10));
        planet.getCrustComposition().addMaterialAsVolume(Material.CALCIUM_OXIDE, (crustVolume * 0.05));
        planet.getCrustComposition().addMaterialAsVolume(Material.IRON_OXIDE, (crustVolume * 0.05));

        // Atmosphere and hydrosphere: Water, nitrogen, oxygen (negligible mass ~0.0001%)
        planet.getAtmosphereComposition().addMaterialAsRawMass(Material.WATER_ICE, 1.4e21); // Oceans
        planet.getAtmosphereComposition().addMaterialAsRawMass(Material.NITROGEN_GAS, 4.0e18); // Atmosphere N₂
        planet.getAtmosphereComposition().addMaterialAsRawMass(Material.OXYGEN_GAS, 1.2e18); // Atmosphere O₂
        planet.getAtmosphereComposition().addMaterialAsRawMass(Material.CO2_ICE, 3.0e15); // Trace CO₂

        return planet;
    }

    /**
     * Create a Jupiter-like gas giant
     * Composition: ~71% H, ~24% He, ~5% heavier elements
     */
    public static Planet createJupiterLike(String id, Vector3D position, Vector3D velocity) {
        Planet planet = new Planet(id, position, velocity, BodyType.GAS_GIANT);

        // Core: Rock and ice (radius ~10,000 km)
        double coreVolume = (4.0 / 3.0) * Math.PI * Math.pow(1.0e7, 3);
        planet.getCoreComposition().addMaterialAsVolume(Material.SILICA, (coreVolume * 0.40));
        planet.getCoreComposition().addMaterialAsVolume(Material.IRON, (coreVolume * 0.30));
        planet.getCoreComposition().addMaterialAsVolume(Material.WATER_ICE, (coreVolume * 0.20));
        planet.getCoreComposition().addMaterialAsVolume(Material.MAGNESIUM_OXIDE, (coreVolume * 0.10));

        // Atmosphere: Hydrogen and helium dominated
        double atmosphereVolume = (4.0 / 3.0) * Math.PI *
            (Math.pow(7.0e7, 3) - Math.pow(1.0e7, 3));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.HYDROGEN, (atmosphereVolume * 0.71));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.HELIUM, (atmosphereVolume * 0.24));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.METHANE_ICE, (atmosphereVolume * 0.03));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.AMMONIA_ICE, (atmosphereVolume * 0.02));

        return planet;
    }

    /**
     * Create a Neptune-like ice giant
     * Composition: ~20% H/He, ~60-70% ices, ~10-20% rock/metal
     */
    public static Planet createNeptuneLike(String id, Vector3D position, Vector3D velocity) {
        Planet planet = new Planet(id, position, velocity, BodyType.ICE_GIANT);

        // Core: Rock and metal (radius ~8,000 km)
        double coreVolume = (4.0 / 3.0) * Math.PI * Math.pow(8.0e6, 3);
        planet.getCoreComposition().addMaterialAsVolume(Material.SILICA, (coreVolume * 0.50));
        planet.getCoreComposition().addMaterialAsVolume(Material.IRON, (coreVolume * 0.30));
        planet.getCoreComposition().addMaterialAsVolume(Material.MAGNESIUM_OXIDE, (coreVolume * 0.20));

        // Mantle: Ices (water, ammonia, methane)
        double mantleVolume = (4.0 / 3.0) * Math.PI *
            (Math.pow(2.0e7, 3) - Math.pow(8.0e6, 3));
        planet.getMantleComposition().addMaterialAsVolume(Material.WATER_ICE, (mantleVolume * 0.55));
        planet.getMantleComposition().addMaterialAsVolume(Material.AMMONIA_ICE, (mantleVolume * 0.25));
        planet.getMantleComposition().addMaterialAsVolume(Material.METHANE_ICE, (mantleVolume * 0.20));

        // Atmosphere: Hydrogen, helium, methane
        double atmosphereVolume = (4.0 / 3.0) * Math.PI *
            (Math.pow(2.46e7, 3) - Math.pow(2.0e7, 3));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.HYDROGEN, (atmosphereVolume * 0.80));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.HELIUM, (atmosphereVolume * 0.15));
        planet.getAtmosphereComposition().addMaterialAsVolume(Material.METHANE_ICE, (atmosphereVolume * 0.05));

        return planet;
    }

    /**
     * Create a star with specific mass (in solar masses)
     */
    public static Star createMainSequenceStar(String id, double solarMasses) {
        Star star = new Star(id, Vector3D.ZERO, Vector3D.ZERO);

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
    public static List<Asteroid> createRandomAsteroidBelt(
            String beltID,
            int population,
            double minimumRadius,
            double maximumRadius,
            long seed) {
        List<Asteroid> asteroidBelt = new ArrayList<>();

        Random random;
        if(seed != 0) {
            random = new Random(seed);
        } else {
            random = new Random();
        }

        for(int i=0; i < population; i++) {
            asteroidBelt.add(createAsteroidByType(getRandomAsteroidType(random),
                    beltID + "-BeltAsteroid-"+i,
                    Vector3D.ZERO,
                    Vector3D.ZERO, random.nextDouble(minimumRadius,maximumRadius))
            );
        }
        return asteroidBelt;
    }

    private static BodyType getRandomAsteroidType(Random random) {
        int choice = random.nextInt(0,3);
        switch (choice) {
            case 0 -> { return BodyType.M_TYPE; }
            case 1 -> { return BodyType.C_TYPE; }
            default -> { return BodyType.S_TYPE; }
        }
    }

}
