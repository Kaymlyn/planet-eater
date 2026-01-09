package com.kaymlyn.planeteater.simulation.celestial.planetoid;

import com.kaymlyn.planeteater.simulation.celestial.BodyType;
import com.kaymlyn.planeteater.simulation.celestial.CelestialBodyFactory;
import com.kaymlyn.planeteater.simulation.celestial.Star;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.LayerProfile;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.Materials;
import com.kaymlyn.planeteater.simulation.celestial.planetconfig.OrbitInitializer;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.physics.Vector3D;
import com.kaymlyn.planeteater.simulation.resources.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class PlanetTest {

    final static String STAR_NAME = "TestStar";
    final static int SOLAR_MASS = 1;
    final static int ONE_HOUR = 3600;
    final static double SMALL_RADIUS = 790;

    private final OrbitInitializer oneAUCircularOrbit = new OrbitInitializer(PhysicsConstants.AU,0,0,0,0,0);
    private final OrbitInitializer smallOrbit = new OrbitInitializer(1e7,0,0,0,0,0);
    private final Materials solidIron = new Materials(Material.IRON,100);
    private final Materials solidNickel = new Materials(Material.NICKEL,50);
    private final LayerProfile core = new LayerProfile(BodyType.ABERRANT,Zone.CORE,List.of(solidIron,solidNickel));

    private CelestialBodyFactory factory;
    private Planet defaultPlanet;
    private Planet weirdPlanet;


    @BeforeEach
    public void setup() {
        Star star = CelestialBodyFactory.createMainSequenceStar(STAR_NAME,SOLAR_MASS);

        factory = new CelestialBodyFactory(star, ONE_HOUR);

        Map<String, LayerProfile> profiles = factory.getProfiles();

        double coreRadius = 3.476e6;
        double mantleThickness = 2.885e6;
        double crustThickness = 6.5e4;
        double atmosphereThickness = 1.0e4;
        defaultPlanet = factory.createArbitraryPlanet("TestPlanet", oneAUCircularOrbit,
                profiles.get("HABITABLE_CORE"), coreRadius,
                profiles.get("HABITABLE_MANTLE"), coreRadius + mantleThickness,
                profiles.get("HABITABLE_CRUST"),  coreRadius + mantleThickness + crustThickness,
                profiles.get("HABITABLE_ATMOSPHERE"), coreRadius + mantleThickness + crustThickness + atmosphereThickness);

        weirdPlanet = factory.createArbitraryPlanet("WeirdPlanet", defaultPlanet, smallOrbit,
                core, 790,
                null, 0,
                null, 0,
                null, 0);

    }

    @Test
    public void validateVectorExpectations() {
        Vector3D initialPosition = defaultPlanet.getPosition();

        defaultPlanet.getParentBody().getSystem().advance(3600);

        Assertions.assertAll(
                () -> Assertions.assertEquals( new Vector3D(1.496e11,0.0,0.0), initialPosition),
                () -> Assertions.assertNotEquals( initialPosition, defaultPlanet.getPosition())
        );
    }

    @Test
    public void validateBasicPlanetCreation() {

        Assertions.assertAll(
                () -> Assertions.assertEquals(factory.getCentralStar(), weirdPlanet.getParentBody()),
                () -> Assertions.assertEquals(2.147483647e9, (int)weirdPlanet.getMass()),
                () -> Assertions.assertEquals(2.065236933e9, (int)weirdPlanet.getVolume()),
                () -> Assertions.assertEquals(2.147483647e9, (int)weirdPlanet.getDensity()),
                () -> Assertions.assertEquals(SMALL_RADIUS, Math.round(weirdPlanet.getRadius())),
                () -> Assertions.assertTrue(weirdPlanet.getCoreComposition().contains(Material.IRON)),
                () -> Assertions.assertTrue(weirdPlanet.getCoreComposition().contains(Material.NICKEL)),
                () -> Assertions.assertFalse(weirdPlanet.getCoreComposition().contains(Material.OLIVINE)),
                () -> Assertions.assertEquals(0.0, weirdPlanet.getCrustComposition().getTotalMass()),
                () -> Assertions.assertFalse(weirdPlanet.canMineCrust()),
                () -> Assertions.assertFalse(weirdPlanet.canHarvestAtmosphere()),
                () -> Assertions.assertEquals(BodyType.ABERRANT, weirdPlanet.getType())
        );
    }

    @Test
    public void validateComplicatedCreation() {
        //Truncating to make output easier to check
        Assertions.assertAll(
                () -> Assertions.assertEquals(98056, (int)(defaultPlanet.getMass()/1e20)),
                () -> Assertions.assertEquals(74023, (int)(defaultPlanet.getSurfaceGravity()*1e4))
        );
    }

    @Test
    public void validateMineability() {
        Assertions.assertAll(
                () -> Assertions.assertFalse(weirdPlanet.canHarvestAtmosphere()),
                () -> Assertions.assertFalse(weirdPlanet.canMineCrust()),
                () -> Assertions.assertTrue(defaultPlanet.canHarvestAtmosphere()),
                () -> Assertions.assertTrue(defaultPlanet.canMineCrust())
        );
    }

    @Test
    public void validateSatellites() {
        Assertions.assertAll(
                () -> Assertions.assertEquals(1.768927265e9, Math.round(defaultPlanet.calculateHillSphereRadius())),
                () -> Assertions.assertEquals(14.0, Math.round(weirdPlanet.calculateHillSphereRadius())),
                () -> Assertions.assertFalse(weirdPlanet.canHaveSubSatellites(), "Weird Planet is to close to the sun to have satellites"),
                () -> Assertions.assertTrue(defaultPlanet.canHaveSubSatellites(), "Default Planet is far enough away from the sun to have satellites"),
                () -> Assertions.assertFalse(defaultPlanet.isTidallyLocked(), "Default Planet is not tidally locked"),
                () -> Assertions.assertTrue(weirdPlanet.isTidallyLocked(), "Weird Planet is tidally locked")
        );
    }

    @Test
    public void validateResourceConsumption() {

        // Note values affected by floating arithmetic issues are so small as to be chalked up to
        // "uncertainty in the resource estimations" accuracy will only when we get within 5 orders of magnitude in change

        double startingMass = defaultPlanet.getMass();
        double startingGravity = defaultPlanet.getSurfaceGravity();

        double qtyFeO = defaultPlanet.getCrustComposition().getMass(Material.IRON_OXIDE);
        double qtySiO2 = defaultPlanet.getCrustComposition().getFraction(Material.SILICA);
        double minedFeO = defaultPlanet.mineCrustMaterial(Material.IRON_OXIDE, 1e21);
        double postMinedFeO = defaultPlanet.getCrustComposition().getMass(Material.IRON_OXIDE);
        double postMinedSiO2 = defaultPlanet.getCrustComposition().getFraction(Material.SILICA);

        double postMinedMass = defaultPlanet.getMass();
        double postMinedGravity = defaultPlanet.getSurfaceGravity();

        double qtyN2 = defaultPlanet.getAtmosphereComposition().getMass(Material.NITROGEN_GAS);
        double qtyO2 = defaultPlanet.getAtmosphereComposition().getMass(Material.OXYGEN_GAS);
        double minedN2 = defaultPlanet.harvestAtmosphere(Material.NITROGEN_GAS, 2e5);
        double postMinedN2 = defaultPlanet.getAtmosphereComposition().getMass(Material.NITROGEN_GAS);
        double postMinedO2 = defaultPlanet.getAtmosphereComposition().getMass(Material.OXYGEN_GAS);
        double postHarvestMass = defaultPlanet.getMass();

        double remainFe0 = defaultPlanet.getCrustComposition().getMass(Material.IRON_OXIDE);
        double overMinedFeO = defaultPlanet.mineCrustMaterial(Material.IRON_OXIDE, 1e25);

        double exhaustedFe0 = defaultPlanet.mineCrustMaterial(Material.IRON_OXIDE,1000);

        double coreMine = weirdPlanet.mineCrustMaterial(Material.IRON, 10000);
        double atmosphereMine = weirdPlanet.harvestAtmosphere(Material.HYDROGEN, 10000);

        //Rounding for clarity and to get rid of float point errors
        Assertions.assertAll(
                () -> Assertions.assertTrue(defaultPlanet.canMineCrust(), "Crust is Minable"),
                () -> Assertions.assertTrue(defaultPlanet.canHarvestAtmosphere(), "Atmosphere is Harvestable"),
                () -> Assertions.assertEquals(98056, (int)(startingMass/1e20), "Starting Mass is as expected based on composition"),
                () -> Assertions.assertEquals(74023, (int)(startingGravity*1e4), "Starting Gravity is as expected based on composition"),
                () -> Assertions.assertEquals((int)(qtySiO2*1e22), (int)(postMinedSiO2*1e22), "Silica is not mined"),
                () -> Assertions.assertEquals(98046, (int)(postMinedMass/1e20), "Mass has decreased by 1e20kg"),
                () -> Assertions.assertEquals(74018, (int)(postMinedGravity*1e4), "Gravity has decreased by .0005 m*s-2"),
                () -> Assertions.assertEquals(qtyFeO - minedFeO, postMinedFeO, "Mined material was removed from crust composition"),
                () -> Assertions.assertEquals(qtyO2,postMinedO2, "Oxygen was not mined"),
                () -> Assertions.assertEquals(qtyN2 - minedN2, postMinedN2, "Harvested material was removed from atmosphere composition"),
                () -> Assertions.assertEquals(postMinedMass, postHarvestMass, "Mass has not changed significantly"),
                () -> Assertions.assertNotEquals(1e25, overMinedFeO, "More mass than available can't be mined"),
                () -> Assertions.assertEquals(remainFe0, overMinedFeO, "Mass requested will match the remaining if there is not enough"),
                () -> Assertions.assertFalse(defaultPlanet.getCrustComposition().contains(Material.IRON_OXIDE), "Used up material won't remain in Composition list"),
                () -> Assertions.assertEquals(0.0, defaultPlanet.getCrustComposition().getMass(Material.IRON_OXIDE), "Mass of missing Material is 0.0"),
                () -> Assertions.assertEquals(0.0, exhaustedFe0, "Material must be present when attempting to mine"),
                () -> Assertions.assertEquals(0.0, coreMine, "Cannot mine from a planet without crust"),
                () -> Assertions.assertEquals(0.0, atmosphereMine, "Cannot harvest from a planet without atmosphere")
        );
    }
}
