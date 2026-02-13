# Repository Location

GitHub: https://github.com/Kaymlyn/planet-eater/tree/main

Source follows standard Maven layout:
src/main/java/com/kaymlyn/planeteater/
src/test/java/com/kaymlyn/planeteater/
src/main/ai/alex/   (PROJECT_INSTRUCTIONS.md, ACCOMPLISHMENTS.md)
src/test/ai/alex/   (TEST_WRITING_GUIDELINES.md, TEST_INVENTORY.md)

Key packages:
simulation.celestial   - OrbitalSystem, PhysicsBody, Orbiter, Gravitational,
CelestialBodyFactory, Star, OrbitingBody, Dockable, Satellite
simulation.physics     - Orbit, TransferPlanner, Itinerary, ScheduledBurn,
Vector3D, PhysicsConstants, OrbitalState
simulation.vehicles    - Spacecraft, CentralMind, Vehicle, VehicleFactory, Gate
simulation.entities    - Automaton
simulation.resources   - Composition, Material
simulation.operations  - MiningOperation (stub)
claude                 - Alex example classes

Type hierarchy (as of 2026-02-12):
PhysicsBody            (position, velocity, mass, update)
|-- Orbiter            (adds parentBody, snapshotOrbit)
|-- Gravitational  (massive, attracts other bodies)
|-- Satellite      (not gravitationally significant)
|-- Spacecraft         (implements PhysicsBody directly, not Orbiter)

Vehicle (abstract)     (crew, cargo, fuel, Tsiolkovsky - no position/velocity)
|-- Spacecraft         (extends Vehicle, implements PhysicsBody)
|-- CentralMind        (extends Vehicle, implements Orbiter + Satellite + Dockable)

Project knowledge contains full file contents indexed from the linked repository.
Use project_knowledge_search before fetching from GitHub.
Raw URL pattern: https://raw.githubusercontent.com/Kaymlyn/planet-eater/main/<path>