package com.kaymlyn.planeteater.simulation.entities;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class Mob {

    private Random random;
    private Automaton prototype;
    private long population;
    private double fertility;
    private double mortality;

    public static Mob from(Automaton automaton) {
        Mob mob = new Mob();
        mob.prototype = Automaton.clone(automaton,automaton.getSpeciesName() + "-prototype");
        mob.random = new Random(0L);
        return mob;
    }

    public long addPopulation(long births) {
        return population += births;
    }

    public long decreasePopulation(long deaths) {
        return population -= deaths;
    }

    public long grow(double time) {

        long births = (long)(random.nextGaussian(1,.2)*fertility*time/PhysicsConstants.SECONDS_PER_YEAR) * population;
        long deaths = (long)((random.nextGaussian(-1,.2)*mortality*time/PhysicsConstants.SECONDS_PER_YEAR) * population);

        addPopulation(births);
        return decreasePopulation(deaths);
    }

    public Mob mergePopulation(Mob otherMob) {
        Mob newMob = null;
        if(prototype.getSpeciesName().contentEquals(otherMob.prototype.getSpeciesName())) {
            double averageCapacity =
                    (prototype.getBaseCapability() * population
                            + otherMob.prototype.getBaseCapability() * otherMob.population)/(population + otherMob.population);

            double averageLifespan =
                    (prototype.getLifespan() * population
                            + otherMob.prototype.getLifespan() * otherMob.population)/(population + otherMob.population);

            double averageAge =
                    (prototype.getAge() * population
                            + otherMob.prototype.getAge() * otherMob.population)/(population + otherMob.population);
            double averageMass =
                    (prototype.getMass() * population
                            + otherMob.prototype.getMass() * otherMob.population)/(population + otherMob.population);
            double averageSpecializationModifier =
                    (prototype.getSpecializationModifier() * population
                            + otherMob.prototype.getSpecializationModifier() * otherMob.population)/(population + otherMob.population);

            Automaton newPrototype = new Automaton(
                    prototype.getId().replaceAll("-prototype", ""),
                    prototype.getSpeciesName(),
                    averageCapacity,
                    averageLifespan,
                    averageAge,
                    averageMass,
                    prototype.getSpecialization(),
                    averageSpecializationModifier,
                    prototype.getLocomotion(),
                    prototype.getAdaptations(),
                    prototype.getType()
            );

            newMob = from(newPrototype);
            newMob.addPopulation(population + otherMob.population);
        }
        return newMob;
    }




}
