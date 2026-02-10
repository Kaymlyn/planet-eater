# Planet Eater - Test Inventory

**Last Updated:** 2026-02-10  
**Purpose:** Comprehensive catalogue of all test suites to reduce context requirements

---

## Test Coverage Summary

| Test Suite | Status | Test Count | Coverage Areas | Notes |
|------------|--------|------------|----------------|-------|
| Vector3DTest | ✅ Complete | 61 | Core vector operations | Foundation for all physics |
| ScheduledBurnTest | ✅ Complete | 29 | Burn scheduling, fuel calc | Tsiolkovsky equation validated |
| VehicleTest | ✅ Complete | 45 | Base vehicle class | **Volume-based cargo** |
| CompositionTest | ✅ Complete | 14 | Resource management | Mass + volume tracking |
| OrbitTest | ✅ Complete | ~30 | Orbital mechanics | Round-trip validation |
| PlanetTest | ✅ Complete | ~15 | Planet creation, mining | Realistic scenarios |
| StarTest | ✅ Complete | ~5 | Stellar properties | Main sequence validation |
| ItineraryTest | ⚠️ Partial | 2 | Transfer validation | **Needs expansion** |
| SpacecraftTest | ✅ Complete | 45 | Launch, travel, docking | **ALL TESTS PASSING** |
| TransferPlannerTest | ❌ Missing | 0 | N/A | **High priority** |

---

## Detailed Test Documentation

### ✅ Vector3DTest.java (61 tests)

**Coverage:** Comprehensive foundation for all vector operations  
**Test Categories:**
- Construction (3D, 2D, negative components, static constants)
- Addition (basic, commutative, identity)
- Subtraction (basic, self, identity)
- Scalar multiplication (positive, zero, one, negative)
- Scalar division (basic, identity, zero throws exception, negative)
- Magnitude (standard, zero vector, unit vectors, non-negative, squared)
- Normalization (unit length, maintains direction, zero vector, unit vector)
- Dot product (basic, commutative, zero vector, orthogonal, parallel)
- Cross product (basic, anti-commutative, zero vector, self, parallel, perpendicular, right-hand rule)
- Distance (basic, self, symmetric, non-negative)
- Rotation (Z-axis 90°, identity, full circle)
- Equality and hash code (same components, self, different components, hash consistency)
- ToString (contains components)
- Random unit vector (magnitude 1)
- Edge cases (very small values, very large values)

**Key Validations:**
- Pythagorean theorem for magnitude
- Vector algebra properties (commutativity, associativity where applicable)
- Orthogonality and parallelism checks
- Numerical stability with extreme values

**Dependencies:** None (foundation class)

---

### ✅ ScheduledBurnTest.java (29 tests)

**Coverage:** Burn scheduling, fuel requirements, timing, ordering  
**Test Categories:**
- Construction (all parameters, zero delta-V, negative time)
- Delta-V magnitude (standard, single-axis, negative components)
- Fuel calculation (standard, increases with delta-V, zero delta-V, high delta-V, different exhaust velocities, different masses, very small/large masses)
- Execution timing (exact match, within tolerance, outside tolerance, custom tolerance, tight tolerance)
- Comparable/ordering (natural ordering, same time, negative times)
- Edge cases (toString, independence)

**Key Validations:**
- Tsiolkovsky rocket equation: `m_fuel = m_initial * (1 - e^(-Δv/v_e))`
- Fuel requirements scale correctly with mass and efficiency
- Execution timing logic with tolerance windows
- Chronological ordering for burn sequences
- Extreme delta-V approaches but never exceeds spacecraft mass

**Physics Accuracy:**
- Standard exhaust velocity: 3000 m/s (chemical rocket)
- Validates fuel never exceeds total spacecraft mass
- Higher exhaust velocity (ion drive) uses less fuel

**Dependencies:** Vector3D (for delta-V vectors)

---

### ✅ VehicleTest.java (45 tests)

**CRITICAL DESIGN CHANGE:** Cargo capacity is **volume-based (m³)**, not mass-based (kg)

**Coverage:** Base vehicle class (abstract) - fuel, cargo, crew, delta-V  
**Test Categories:**
- Construction (all parameters, initial cargo empty, initial crew empty)
- Mass calculation (empty, with cargo, complete with fuel+cargo+crew)
- Fuel management (add fuel, respect capacity, consume fuel)
- **Cargo management BY VOLUME** (load by volume, capacity enforced, dense vs light materials, available space, multiple materials, unload by volume, respect available)
- Crew management (board human with life support, board robot without life support, reject human without life support, respect capacity, disembark)
- Delta-V calculation (available, decreases with fuel consumption, zero fuel = zero delta-V, fuel required)
- Integration test (realistic cargo scenario)

**Key Validations:**
- **Volume constraint:** `cargoCapacity` is m³, not kg
- Dense materials (iron: 7874 kg/m³) pack more mass per volume
- Light materials (water ice: 917 kg/m³) pack less mass per volume
- Life support logic: organics require it, inorganics don't
- Tsiolkovsky equation for delta-V: `Δv = v_e * ln(m_initial / m_final)`
- Crew mass contributes to total mass calculations

**Design Rationale:**
- Cargo holds constrained by physical space, not weight
- Same volume of iron weighs ~8.6× more than water ice
- Realistic cargo manifests account for volume, not just mass

**Test Implementation:**
- Uses `TestVehicle` concrete implementation (abstract base tested)
- Tolerance: 1e-6 for floating-point comparisons

**Dependencies:** Automaton (for crew), Material (for cargo density), Specialization, EntityType, Environment

---

### ✅ CompositionTest.java (14 tests)

**CRITICAL DISTINCTION:** Composition stores **mass (kg)** internally, provides **volume (m³)** accessors

**Coverage:** Resource management with mass and volume tracking  
**Test Categories:**
- **Original tests (4):** Addition/removal by mass and volume, error checking, metrics, extraction with targeting
- **Volume tracking tests (10):** Get volume, volume non-existent, total volume, add by volume, volume consistency, removal updates volume, dense vs light materials, cargo volume constraints, same volume different mass, realistic manifest

**Key Validations:**
- Internal storage is mass (kg) for physics calculations
- Volume calculated from mass using material density
- `addMaterialAsVolume(material, volume_m³)` converts to mass internally
- `getVolume(material)` converts stored mass back to volume
- Extraction with targeting efficiency modifies ratios
- Auto-dumping for non-targeted materials (95% dumped)

**Physics/Chemistry:**
- Bulk density calculation for mixtures
- Material-specific densities from Material enum
- Volume-to-mass conversions: `mass = volume × density`

**Test Scenarios:**
- Pure iron composition: density = iron density
- Mixed iron/iron-oxide: density = average for equal volumes
- Extraction targeting: efficiency coefficient shifts ratios

**Dependencies:** Material (for density values)

---

### ✅ OrbitTest.java (~30 tests)

**Coverage:** Comprehensive orbital mechanics validation  
**Test Categories:**
- Orbit creation (circular from state, elliptical from state, inclined orbit)
- Round-trip consistency (circular, elliptical at periapsis/apoapsis, various angles, 3D elliptical)
- Orbital geometry (periapsis/apoapsis distances, semi-minor axis, orbital period)
- Anomaly conversions (true to eccentric, mean anomaly)
- Time propagation (forward propagation, full orbit period)
- Coordinate systems (orbital nodes, periapsis/apoapsis positions)
- Energy conservation (specific orbital energy across orbit)
- Edge cases (nearly circular e≈0, highly eccentric e≈0.99)

**Key Validations:**
- Kepler's equation solving
- Position/velocity ↔ orbital elements conversions
- Energy conservation: `E = v²/2 - μ/r` remains constant
- Orbital period: `T = 2π√(a³/μ)`
- 1 AU circular orbit ≈ 365 days

**Physics Accuracy:**
- Uses Sun's gravitational parameter μ = GM☉
- Validates against known solutions (Hohmann delta-V formulas)
- Round-trip tolerance: position ≤ 1 km, velocity ≤ 10 m/s

**Dependencies:** Vector3D (position/velocity), Star (gravitational parameter), CelestialBodyFactory

---

### ✅ PlanetTest.java (~15 tests)

**Coverage:** Planet creation, resource extraction, satellite mechanics  
**Test Categories:**
- Vector expectations (initial position, motion after time step)
- Basic planet creation (from arbitrary layers)
- Complicated creation (from patterns like EARTH)
- Mineability (crust, atmosphere presence)
- Satellites (Hill sphere, tidal locking, sub-satellites)
- Resource consumption (mining, atmosphere harvesting, mass/gravity changes, over-mining, exhaustion)

**Key Validations:**
- Layer composition (core, mantle, crust, atmosphere)
- Mining removes mass from correct layer
- Mass changes affect gravity: `g = GM/r²`
- Cannot mine more than available (returns actual amount)
- Exhausted materials removed from composition list
- Hill sphere: `r_Hill = a(m/3M)^(1/3)`

**Scenarios Tested:**
- Default planet (Earth-like with all layers)
- Weird planet (only core, no crust/atmosphere)
- Mining 1e21 kg iron oxide from crust
- Harvesting 2e5 kg nitrogen from atmosphere
- Over-mining attempts (returns available, not requested)

**Dependencies:** CelestialBodyFactory, Material, Composition, PlanetPattern, Orbit

---

### ✅ StarTest.java (~5 tests)

**Coverage:** Stellar properties for main sequence stars  
**Test Categories:**
- Default star creation (mass, composition, density, radius, luminosity, spectral class, mass loss rate)

**Key Validations:**
- Mass-radius relationship: R ∝ M^0.57 (M ≥ 1 M☉), R ∝ M^0.8 (M < 1 M☉)
- Luminosity: L ∝ M^3.5
- Composition: 73% H, 25% He, 2% metals
- Solar parameters: M☉ = 1.989e30 kg, R☉ = 6.96e8 m
- Spectral class from temperature

**Physics:**
- Stefan-Boltzmann law for temperature
- Mass loss rate ∝ M²
- Gravitational compression vs fusion pressure balance

**Dependencies:** CelestialBodyFactory, Material, Composition, PhysicsConstants

---

### ⚠️ ItineraryTest.java (2 tests - NEEDS EXPANSION)

**Current Coverage:** Basic transfer validation  
**Existing Tests:**
1. `motionIsTrackedProperly()` - Spacecraft follows Earth, then departs to orbit
2. `testEarthToVenusTransfer()` - Hohmann transfer Earth→Venus

**Key Validations:**
- Spacecraft position tracks with docking platform until launch
- After launch, spacecraft enters standard orbit before departure burn
- Transfer completes within tolerance of destination

**NEEDS EXPANSION:**
- Burn scheduling logic
- Completion detection edge cases
- Validation against spacecraft (fuel, delta-V)
- Multiple burn sequences
- Infeasibility detection
- Edge cases: empty burns, overlapping burns, negative times

**Dependencies:** CelestialBodyFactory, OrbitalSystem, Planet, Spacecraft, TransferPlanner, VehicleFactory

---

### ✅ SpacecraftTest.java (45 tests - ALL PASSING)

**Coverage:** Complete spacecraft lifecycle from construction to recycling  
**Test Categories:**
- Construction (4): all parameters, initial position/cargo/crew
- State management (2): initial DOCKED, launch transition
- Itinerary programming (4): program itinerary, plan route (from docked/orbit), no valid route
- Launch validation (6): success, failures (no itinerary, infeasible, not docked, insufficient fuel, already traveling)
- Fuel consumption (2): burn matches Tsiolkovsky, multiple burns sequential
- Position tracking (3): tracks platform when docked, velocity change after burn, separation after burn
- Docking (3): manual docking, hanger presence, launch removes from hanger
- Travel completion (2): ORBITING state, DOCKED at dockable destination
- Stranded state (1): insufficient fuel mid-mission
- Location tracking (2): returns orbiting body, updates after completion
- Recycling (2): returns construction materials, unregisters from system
- Cargo/crew integration (2): cargo affects mass, crew affects mass
- ToString (1): contains key information
- Edge cases (8): null orbiting fallback, future position query, current position query, system tracking
- System registration (2): tracks after launch, stops after completion

**Key Validations:**
- Launch sequence with 5 distinct failure modes
- State transitions: DOCKED → TRAVELING → ORBITING/DOCKED → STRANDED
- Fuel consumption matches Tsiolkovsky equation predictions
- Position updates from n-body simulation after burns
- Docking/undocking with hanger tracking
- System registration only during transit
- Route planning from different orbital states

**Bugs Fixed During Testing:**
1. Time advancement: 7 infinite loops with `getCurrentTime() < getCurrentTime() + delta`
2. Lombok conflicts: getPosition()/getVelocity()/getLocation() overridden by @Getter
3. Manual docking: Test expectations vs actual API design
4. Recycling: Mass vs volume conversion in Composition
5. Route planning: Defensive handling of null returns

**Physics Accuracy:**
- Tsiolkovsky fuel calculations
- Velocity changes from delta-V burns
- Position separation from velocity differences
- Mass updates from cargo/crew/fuel changes

**Dependencies:** CelestialBodyFactory, OrbitalSystem, Planet, Star, Itinerary, ScheduledBurn, TransferPlanner, Composition, Material, Automaton, Dockable

---

### ❌ TransferPlannerTest.java (MISSING - HIGH PRIORITY)

**Target Coverage:**
- Launch sequence (validation, state transitions, fuel checks)
- State transitions (DOCKED → TRAVELING → ORBITING → DOCKED)
- Fuel consumption during burns (matches Tsiolkovsky predictions)
- Position tracking during itinerary execution
- Stranded state (insufficient fuel, failed burns)
- Edge cases (null itinerary, launch while traveling, negative fuel)

**Critical Behaviors to Test:**
- Launch validation (feasibility, fuel, state)
- Burn execution and fuel consumption
- Position updates from n-body simulation
- Completion logic (docking vs orbiting)
- Recycling materials

**Dependencies:** Itinerary, ScheduledBurn, TransferPlanner, OrbitalSystem, Dockable

---

### ❌ TransferPlannerTest.java (MISSING - HIGH PRIORITY)

**Target Coverage:**
- Hohmann transfer (delta-V calculations, transfer time)
- Bi-elliptic transfer (efficiency vs Hohmann for various radius ratios)
- Lambert solver (accuracy for different time-of-flight values)
- Transfer option generation (multiple strategies)
- Selection logic (minimum time, minimum delta-V, balanced)
- Feasibility validation (fuel requirements, spacecraft constraints)

**Critical Validations:**
- Hohmann delta-V formula: `Δv = √(μ/r₁)[√(2r₂/(r₁+r₂)) - 1] + √(μ/r₂)[1 - √(2r₁/(r₁+r₂))]`
- Bi-elliptic more efficient when `r₂/r₁ > 11.94`
- Lambert solver converges for various geometries
- Option ranking by efficiency score

**Dependencies:** Spacecraft, Orbit, Itinerary, ScheduledBurn

---

## Test Development Guidelines Reference

**Framework:** JUnit 5 exclusively  
**Structure:** Use `@BeforeEach` for common setup, `assertAll()` for multiple assertions  
**Naming:** `testBehaviorBeingTested()` with `@DisplayName("Human readable")`  
**Organization:** Group by category with comment headers (`// ==================== CATEGORY ====================`)

**For detailed guidelines, see:** `/src/test/ai/alex/TEST_WRITING_GUIDELINES.md`

---

## Missing Coverage Summary

**Unit Tests Needed:**
1. ❌ TransferPlannerTest.java - Hohmann, Bi-elliptic, Lambert validation
2. ⚠️ ItineraryTest.java - Expand beyond 2 basic tests

**Integration Tests Needed:**
1. End-to-end mission (create→plan→execute→arrive)
2. Multi-stage transfer (multiple gravity assists)
3. Resource extraction mission (travel→extract→return)

**Validation Tests Needed:**
1. Orbital mechanics accuracy (compare to analytical solutions)
2. Energy conservation in n-body simulation
3. Numerical drift over long simulations

---

## Test Writing Workflow

1. **Analyze class** - Read source, identify methods, dependencies
2. **Plan categories** - Group tests logically
3. **Write @BeforeEach** - Set up common fixtures
4. **Write top-down** - Construction → core methods → edge cases
5. **Add edge cases** - Zero, negative, null, extreme values
6. **Document complex logic** - Physics formulas, non-obvious behavior
7. **Review checklist** - Standards compliance

---

## Notes on Test Design

**Volume vs Mass:**
- Composition stores MASS internally
- Vehicle cargo capacity is VOLUME
- Tests must account for density conversions

**Physics Validation:**
- Tsiolkovsky equation for fuel: `m_fuel = m_initial * (1 - e^(-Δv/v_e))`
- Orbital energy: `E = v²/2 - μ/r`
- Hill sphere: `r_Hill = a(m/3M)^(1/3)`

**Tolerance Guidelines:**
- Standard: 1e-6 for exact calculations
- Physics: 1e-5 for accumulated floating-point errors
- Position: 1 km for orbital mechanics
- Velocity: 10 m/s for orbital mechanics

---

**This document should be updated whenever:**
- New test suites are created
- Existing test suites are significantly expanded
- Test coverage gaps are identified
- Design changes affect test expectations (like cargo volume fix)