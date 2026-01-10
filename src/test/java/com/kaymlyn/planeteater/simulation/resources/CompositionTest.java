package com.kaymlyn.planeteater.simulation.resources;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                () -> assertEquals(densityFeO*standardVolume + standardMass, initialTotalMass, "Total Mass should be 100m^3 of FeO plus 100kg of Fe"),
                () -> assertEquals(standardMass, initialMassFe,  "Fe Mass should be equal to standard Mass"),
                () -> assertEquals(standardMass * densityFeO, initialMassFeO,  "FeO Mass should relate to it's density"),
                () -> assertEquals(densityFeO*standardVolume + 2*standardMass, totalMassWithAddition, "Total Mass should be 100m^3 of FeO plus 200kg of Fe"),
                () -> assertEquals(standardMass*2, massWithAdditionFe,  "Fe Mass should be equal to standard Mass"),
                () -> assertEquals(initialMassFeO, massWithAdditionFeO,  "FeO Mass should remain unchanged after addition of other resources"),
                () -> assertEquals(standardMass, removedFeO, "Return of removed mass should equal mass requested to be removed if available"),
                () -> assertEquals(densityFeO*standardVolume + standardMass, totalMassWithRemoval, "Total Mass should be 100m^3-100kg of FeO plus 200kg of Fe"),
                () -> assertEquals(massWithAdditionFe, massWithRemovalFe,  "Fe Mass should remain unchanged after removal of other resources"),
                () -> assertEquals(densityFeO*standardVolume - standardMass, massWithRemovalFeO,  "FeO Mass should be 100kg less than before"),
                () -> assertFalse(presenceH, "Hydrogen should not register as present when not added"),
                () -> assertEquals(0.0, removedH, "Removal of mass that is not present results in 0.0 mass removed"),
                () -> assertTrue(presenceBeforeRemoval, "Materials should register as present when in the list."),
                () -> assertEquals(standardMass * 2, removedFE, "Removal of more mass than available results in the amount available"),
                () -> assertFalse(presenceAfterRemoval, "Material should not be present on the materials list after it is exhausted")
        );
    }

    @Test
    public void validateExtraction(){
        Composition composition = new Composition() //1 : 2 : 10
                .addMaterialAsRawMass(Material.IRON,1e4)
                .addMaterialAsRawMass(Material.IRON_OXIDE, 2e4)
                .addMaterialAsRawMass(Material.SILICA, 1e5);

        //composition: 10000 Fe, 20000 FeO, 100000 Si
        Composition smallExtracted = composition.extract(standardMass*2,1.0);
        double postSmallExtractionFe = smallExtracted.getMass(Material.IRON); // 200/13 ~ 15.38
        double postSmallExtractionFeO = smallExtracted.getMass(Material.IRON_OXIDE); // 400/13 ~ 30.76
        double postSmallExtractionSi = smallExtracted.getMass(Material.SILICA); //2000/13 ~ 153.84

        //Composition: 9984.62 Fe, 19969.24 FeO, 99846.16 Si
        Composition smallTargetedExtracted = composition.extract(standardMass*2,2.0,Material.IRON); // expected: 28.57 Fe, 1.42 FeO, 7.142 Si
        double postSmallTargetedExtractionFe = smallTargetedExtracted.getMass(Material.IRON); // 200/7 ~ 28.57
        double postSmallTargetedExtractionFeO = smallTargetedExtracted.getMass(Material.IRON_OXIDE); // 200/7 ~ 28.57 | retain 5% = 1.42
        double postSmallTargetedExtractionSi = smallTargetedExtracted.getMass(Material.SILICA); //2000/14 ~ 142.84 | retain 5% = 7.14


        //Composition: 9956.05 Fe, 19967.82 FeO, 99839.02 Si | 129762.89 : Fe 7.67%, FeO 15.39%, Si %76.94
        Composition smallMultiTargetedExtracted = composition.extract(standardMass*2, 2.0, Material.IRON, Material.IRON_OXIDE); // expected: 15.34 Fe, 30.78 FeO, 7.694 Si
        double postSmallMultiTargetedExtractionFe = smallMultiTargetedExtracted.getMass(Material.IRON); // 200*1534/12306 ~ 24.93
        double postSmallMultiTargetedExtractionFeO = smallMultiTargetedExtracted.getMass(Material.IRON_OXIDE); // 200*3078/12306 ~ 50.02
        double postSmallMultiTargetedExtractionSi = smallMultiTargetedExtracted.getMass(Material.SILICA); //200*7694/12306 ~ 125.04 | retain 5% = 6.25

        //129,762.89

        Assertions.assertAll(
                () -> assertEquals(standardMass*2, (int)smallExtracted.getTotalMass(), "Untargeted extraction should get the full desired amount"),
                () -> assertEquals(1538, (int)(postSmallExtractionFe*100), "Fe amount after first extraction should be 7.69%"),
                () -> assertEquals(3076, (int)(postSmallExtractionFeO*100),"FeO amount after first extraction should be 15.38%"),
                () -> assertEquals(15384, (int)(postSmallExtractionSi*100), "Si amount after first extraction should be 76.92%"),
                () -> assertEquals(3714, (int)(smallTargetedExtracted.getTotalMass()*100), "Targeted extraction for one resource should get efficiency multiplied value and 5% of untargeted resources"),
                () -> assertEquals(2857, (int)(postSmallTargetedExtractionFe*100), "Fe amount after targeted extraction should be 14.28%"),
                () -> assertEquals(142, (int)(postSmallTargetedExtractionFeO*100), "FeO amount after targeted extraction should be 95% of 14.28%"),
                () -> assertEquals(714, (int)(postSmallTargetedExtractionSi*100), "Si amount after targeted extraction should be 95% of 71.44%"),
                () -> assertEquals(8120, (int)(smallMultiTargetedExtracted.getTotalMass()*100), "Targeted extraction of multiple resources should get efficiency multiplied value and 5% of untargeted resources"),
                () -> assertEquals(2493, (int)(postSmallMultiTargetedExtractionFe*100), "Fe amount after multi targeted extraction should be 7.67%"),
                () -> assertEquals(5001, (int)(postSmallMultiTargetedExtractionFeO*100), "FeO amount after multi targeted extraction should be 15.39%"),
                () -> assertEquals(625, (int)(postSmallMultiTargetedExtractionSi*100), "Si amount after multi targeted extraction should be 95% of 76.94%")
        );
    }
}
