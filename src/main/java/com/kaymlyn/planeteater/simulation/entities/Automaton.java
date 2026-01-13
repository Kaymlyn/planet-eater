package com.kaymlyn.planeteater.simulation.entities;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Automaton {
    private String id;
    private String speciesName;
    private double baseCapability;
    private double lifespan;
    private double age;
    private double mass;
    private Specialization specialization;
    private double specializationModifier;
    private EntityLocomotion locomotion;
    private Set<Environment> adaptations;
    private EntityType type;

    public static Automaton clone(Automaton automaton, String cloneName) {
        return new Automaton(
                cloneName,
                automaton.speciesName,
                automaton.baseCapability,
                automaton.lifespan,
                0,
                automaton.mass,
                automaton.specialization,
                automaton.specializationModifier,
                automaton.locomotion,
                automaton.adaptations,
                automaton.type
        );
    }

    public double activate(Specialization specialization) {
        if(this.specialization == Specialization.GENERALIST) {
            return baseCapability + (specializationModifier /2);
        }

        if(this.specialization == specialization) {
            return baseCapability + (specializationModifier);
        }

        return baseCapability;
    }

    public boolean ageUp(double addAge) {
        return (this.age += addAge) > lifespan;
    }

    public boolean lifeSupported(Environment environment) {
        return adaptations.contains(environment);
    }

    public static Automaton createHuman(String name, Specialization specialization) {
        return new Automaton(
                name,
                "Human",
                1.0,
                86 * PhysicsConstants.SECONDS_PER_YEAR,
                0.0,
                80.0,
                specialization,
                0.2,
                EntityLocomotion.BIPEDAL,
                Set.of(Environment.OXYGEN, Environment.TEMPERATE),
                EntityType.ORGANIC
        );
    }

    public static Automaton createRobot(String name, Specialization specialization) {
        return new Automaton(
                name,
                "Robot",
                1.0,
                200 * PhysicsConstants.SECONDS_PER_YEAR,
                0.0,
                160.0,
                specialization,
                0.2,
                EntityLocomotion.BIPEDAL,
                Set.of(),
                EntityType.INORGANIC
        );
    }
}
