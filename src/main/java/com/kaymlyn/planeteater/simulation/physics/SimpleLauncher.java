package com.kaymlyn.planeteater.simulation.physics;

import com.kaymlyn.planeteater.simulation.celestial.planetoid.Planet;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;

public class SimpleLauncher {

    public Itinerary launch(double departureTime, Spacecraft spacecraft){

        double currentTime = spacecraft.getSystem().getCurrentTime();
        Itinerary itinerary = new Itinerary(currentTime);

        if(spacecraft.getState() != Spacecraft.SpacecraftState.DOCKED) {
            return itinerary;
        }

        Vector3D velocityAtLaunch = spacecraft.getDockingLocation().snapshotOrbit().stateAt(departureTime,currentTime).velocity();

        Vector3D launchDeltaV;
        if(spacecraft.getDockingLocation() instanceof Planet planet) {
            launchDeltaV = planet.getStandardCircularOrbitVector().subtract(velocityAtLaunch);
        } else {
            launchDeltaV = Vector3D.ZERO;
        }

        // Departure burn: accelerate from current velocity to Lambert departure velocity

        itinerary.addBurn(new ScheduledBurn(
                "simple-launch",
                departureTime,
                launchDeltaV,
                "Simple launch from surface."
        ));

        return itinerary;
    }
}
