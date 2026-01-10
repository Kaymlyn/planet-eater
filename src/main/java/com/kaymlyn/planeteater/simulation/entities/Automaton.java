package com.kaymlyn.planeteater.simulation.entities;

import lombok.Data;

import java.util.Set;

@Data
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

    public double activate(Specialization specialization) {
        if(this.specialization == Specialization.GENERALIST) {
            return baseCapability + (specializationModifier /2);
        }

        if(this.specialization == specialization) {
            return baseCapability + (specializationModifier);
        }

        return baseCapability;
    }

    public boolean lifeSupported(Environment environment) {
        return adaptations.contains(environment);
    }
}
