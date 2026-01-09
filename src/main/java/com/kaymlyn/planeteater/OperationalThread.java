package com.kaymlyn.planeteater;

import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;

public class OperationalThread implements Runnable {

    private final OrbitalSystem spark;

    public OperationalThread(OrbitalSystem spark) {
        this.spark = spark;
    }

    @Override
    public void run() {
//        OrbitalSystem
    }
}
