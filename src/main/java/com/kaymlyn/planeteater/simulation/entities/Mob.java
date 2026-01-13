package com.kaymlyn.planeteater.simulation.entities;

import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import jdk.dynalink.beans.StaticClass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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




}
