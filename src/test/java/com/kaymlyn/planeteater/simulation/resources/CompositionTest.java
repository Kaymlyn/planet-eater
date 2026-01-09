package com.kaymlyn.planeteater.simulation.resources;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompositionTest {

    double standardMass = 100;
    double standardVolume = 100;


    @Test
    public void validateSimpleCompositionAdditionAndRemoval() {
        Composition composition = new Composition()
                .addMaterialAsRawMass(Material.IRON, standardMass)
                .addMaterialAsVolume(Material.IRON_OXIDE, standardMass);

        double initialTotalMass = composition.getTotalMass();
        double initialMassFe = composition.getMass(Material.IRON);
        double initialMassFeO = composition.getMass(Material.IRON_OXIDE);
        double densityFeO = Material.IRON_OXIDE.getDensity();

        composition.addMaterialAsRawMass(Material.IRON, standardMass);
        double totalMassWithAddition = composition.getTotalMass();
        double massWithAdditionFe = composition.getMass(Material.IRON);
        double massWithAdditionFeO = composition.getMass(Material.IRON_OXIDE);

        double removedFeO = composition.removeMaterial(Material.IRON_OXIDE, standardMass);
        double totalMassWithRemoval = composition.getTotalMass();
        double massWithRemovalFe = composition.getMass(Material.IRON);
        double massWithRemovalFeO = composition.getMass(Material.IRON_OXIDE);

        boolean presenceH = composition.contains(Material.HYDROGEN);

        double removedH = composition.removeMaterial(Material.HYDROGEN, standardMass);

        boolean presenceBeforeRemoval = composition.contains(Material.IRON);
        double removedFE = composition.removeMaterial(Material.IRON, standardMass * 3);
        boolean presenceAfterRemoval = composition.contains(Material.IRON);

        Assertions.assertAll(
                () -> Assertions.assertEquals(densityFeO*standardVolume + standardMass, initialTotalMass, "Total Mass should be 100m^3 of FeO plus 100kg of Fe"),
                () -> Assertions.assertEquals(standardMass, initialMassFe,  "Fe Mass should be equal to standard Mass"),
                () -> Assertions.assertEquals(standardMass * densityFeO, initialMassFeO,  "FeO Mass should relate to it's density"),
                () -> Assertions.assertEquals(densityFeO*standardVolume + 2*standardMass, totalMassWithAddition, "Total Mass should be 100m^3 of FeO plus 200kg of Fe"),
                () -> Assertions.assertEquals(standardMass*2, massWithAdditionFe,  "Fe Mass should be equal to standard Mass"),
                () -> Assertions.assertEquals(initialMassFeO, massWithAdditionFeO,  "FeO Mass should remain unchanged after addition of other resources"),
                () -> Assertions.assertEquals(standardMass, removedFeO, "Return of removed mass should equal mass requested to be removed if available"),
                () -> Assertions.assertEquals(densityFeO*standardVolume + standardMass, totalMassWithRemoval, "Total Mass should be 100m^3-100kg of FeO plus 200kg of Fe"),
                () -> Assertions.assertEquals(massWithAdditionFe, massWithRemovalFe,  "Fe Mass should remain unchanged after removal of other resources"),
                () -> Assertions.assertEquals(densityFeO*standardVolume - standardMass, massWithRemovalFeO,  "FeO Mass should be 100kg less than before"),
                () -> Assertions.assertFalse(presenceH, "Hydrogen should not register as present when not added"),
                () -> Assertions.assertEquals(0.0, removedH, "Removal of mass that is not present results in 0.0 mass removed"),
                () -> Assertions.assertTrue(presenceBeforeRemoval, "Materials should register as present when in the list."),
                () -> Assertions.assertEquals(standardMass * 2, removedFE, "Removal of more mass than available results in the amount available"),
                () -> Assertions.assertFalse(presenceAfterRemoval, "Material should not be present on the materials list after it is exhausted")
        );
    }

    public void validateExtraction(){
        Composition composition = new Composition() //1 : 2 : 10
                .addMaterialAsRawMass(Material.IRON,1e4)
                .addMaterialAsRawMass(Material.IRON_OXIDE, 2e4)
                .addMaterialAsRawMass(Material.SILICA, 1e5);

        //composition: 10000 Fe, 20000 FeO, 100000 Si
        Composition smallExtracted = composition.extract(200,1.0);
        double postSmallExtractionFe = composition.getMass(Material.IRON); // 200/13 ~ 15.38
        double postSmallExtractionFeO = composition.getMass(Material.IRON_OXIDE); // 400/13 ~ 30.76
        double postSmallExtractionSi = composition.getMass(Material.SILICA); //2000/13 ~ 153.84

        //Composition: 9984.62 Fe, 19969.24 FeO, 99846.16 Si
        Composition smallTargetedExtracted = composition.extract(200,2.0,Material.IRON); // expected: 28.57 Fe, 1.42 FeO, 7.142 Si
        double postSmallTargetedExtractionFe = composition.getMass(Material.IRON); // 200/7 ~ 28.57
        double postSmallTargetedExtractionFeO = composition.getMass(Material.IRON_OXIDE); // 200/7 ~ 28.57 | retain 5% = 1.42
        double postSmallTargetedExtractionSi = composition.getMass(Material.SILICA); //2000/14 ~ 142.84 | retain 5% = 7.142


        //Composition: 9956.05 Fe, 19967.82 FeO, 99839.02 Si | 129762.89 : Fe 7.67%, FeO 15.39%, Si %76.94
        Composition smallMultiTargetExtracted = composition.extract(200, 2.0, Material.IRON, Material.IRON_OXIDE);
        double postSmallMultiTargetedExtractionFe = composition.getMass(Material.IRON); // 200*767/10000 ~ 15.34
        double postSmallMultiTargetedExtractionFeO = composition.getMass(Material.IRON_OXIDE); // 200*1539/10000 ~ 30.78
        double postSmallMultiTargetedExtractionSi = composition.getMass(Material.SILICA); //200*7694/10000 ~ 153.88 | retain 5% = 7.694


    }
}
