**Context:** These instructions should be added to Alex's persona for writing test suites.

---

## Test Framework Standards

**Framework:** JUnit 5 exclusively
- Use `@Test`, `@BeforeEach`, `@DisplayName` annotations
- Use `assertAll()` for all tests with multiple assertions
- Use `assertEquals()`, `assertTrue()`, `assertFalse()`, etc. from JUnit 5

**Structure Pattern:**
```java
@DisplayName("Human-readable description of what's being tested")
public class ClassNameTest {

  // Test constants at class level
  private static final double TOLERANCE = 1e-6;

  // Common test data
  private TypeName standardInstance;
  private double standardValue;

  @BeforeEach
  void setUp() {
    // ALL common test setup goes here
    // Initialize test fixtures used by multiple tests
  }

  @Test
  @DisplayName("Specific behavior being tested")
  void testMethodName() {
    // Arrange
    // Act  
    // Assert (use assertAll for multiple assertions)
  }
}
```

---

## Prescriptive vs Descriptive Tests

This is the most important distinction in the test suite. Every assertion must be
traceable to one of two sources:

### Prescriptive Tests (preferred)
The assertion expresses what the code MUST do, derived from a specification or
physical law. These tests are allowed to fail when the code is wrong - that is
their purpose.

Traceable to:
- A physical law: `delta_v = v_exhaust * ln(mass_initial / mass_final)`
- A state machine contract: `launch() from ORBITING must transition to TRAVELING`
- An explicit design decision: `arrival position within POSITION_TOLERANCE of destination`

```java
// Prescriptive: derived from Tsiolkovsky equation
double expectedFuelConsumed = totalMass * (1.0 - Math.exp(-deltaV / exhaustVelocity));
assertEquals(expectedFuelConsumed, actualFuelConsumed, TOLERANCE,
        "Fuel consumed must satisfy Tsiolkovsky equation");
```

### Descriptive Tests (use with caution)
The assertion expresses what the code CURRENTLY does, without verifying correctness.
These protect against unintended regressions but can silently encode bugs as
expected behavior.

When a descriptive test is unavoidable, label it explicitly:

```java
// DESCRIPTIVE: records current behavior, not a specification.
// If this fails after a physics change, verify whether the new behavior is correct
// before updating the expected value.
assertEquals(113_166_081.0, distanceToMars, 1000.0, "Current arrival distance");
```

### The test to avoid
An assertion written to match observed output without checking whether that output
is physically or logically correct. The canonical example from this project:

```java
// WRONG: accepted broken behavior as expected
assertFalse(explorer.launch(system.getTimeStep()), "Return launch should succeed");
// The comment contradicts the assertion. This encoded a bug.
```

If a comment on an assertion contradicts what the assertion checks, the assertion
is wrong.

### Traceability rule
Before writing any assertion, state in a comment what specification or law it
is derived from. If you cannot state one, the assertion is descriptive - label it
as such or reconsider whether it belongs.

---

## Test Organization Principles

### 1. Group Tests by Category
Use comment headers to organize test methods:
```java
// ==================== CONSTRUCTION TESTS ====================
// ==================== CALCULATION TESTS ====================
// ==================== EDGE CASES ====================
```

### 2. Test Naming Convention
- Method name: `testMethodName` or `testBehaviorBeingTested`
- @DisplayName: Full sentence describing what's verified
- Example:
  ```java
  @Test
  @DisplayName("Calculate delta-V magnitude for standard burn")
  void testDeltaVMagnitude() { ... }
  ```

### 3. One Concept Per Test
- Each test validates one behavior/concept
- Use `assertAll()` to group related assertions
- Example: Testing X-axis, Y-axis, Z-axis burns together is one concept

---

## Assertion Guidelines

### Always Use assertAll for Multiple Assertions
```java
assertAll("Descriptive group name",
    () -> assertEquals(expected1, actual1, "Failure message 1"),
    () -> assertTrue(condition2, "Failure message 2"),
    () -> assertNotNull(object3, "Failure message 3")
);
```

### Assertion Message Format
- Always provide failure messages
- Make them descriptive: explain what SHOULD be true
- Example: `"Fuel should increase monotonically with delta-V"`

### Tolerance for Floating-Point
```java
private static final double TOLERANCE = 1e-6;
assertEquals(expected, actual, TOLERANCE, "Message");
```

### Physics Assertions
Derive expected values from the same physical laws the code implements.
Do not copy expected values from a previous run output.

```java
// Derive from Tsiolkovsky, not from observed output
double mu = centralStar.getGravitationalParameter();
double expectedTransferTime = Math.PI * Math.sqrt(Math.pow(semiMajorAxis, 3) / mu);
assertEquals(expectedTransferTime, itinerary.getEstimatedDuration(), TIME_TOLERANCE,
        "Transfer time must match Kepler's third law");
```

---

## Code Style for Tests

### Prefer Clarity Over DRY
- Repetition is acceptable in tests for readability
- Don't extract helper methods unless used 3+ times
- Each test should be self-contained and readable

### Use Descriptive Variable Names
```java
// Good
double expectedMagnitude = Math.sqrt(100.0*100.0 + 50.0*50.0 + 25.0*25.0);
double actualMagnitude = vector.magnitude();

// Avoid
double em = ...;
double am = ...;
```

### @BeforeEach vs Inline Setup
**Use @BeforeEach for:**
- Test fixtures used by 3+ tests
- Complex object construction
- Standard test values

**Use inline setup for:**
- Test-specific edge cases
- Values unique to one test
- Demonstrating specific scenarios

---

## Test Coverage Requirements

### For Each Class, Test:
1. **Construction** - All constructors, parameter validation
2. **Core Methods** - All public methods with typical inputs
3. **Edge Cases** - Zero, negative, null, extreme values
4. **Boundary Conditions** - Min/max values, tolerance limits
5. **Error Handling** - Exception cases if applicable
6. **Integration Points** - How class interacts with dependencies

### Specific Test Types

**Mathematical Operations:**
- Standard cases with known results
- Edge cases (zero, negative, infinity)
- Commutative/associative properties where applicable
- Inverse operations (e.g., add then subtract)

**State Transitions:**
- Valid transitions
- Invalid transitions
- Boundary states
- Initial and terminal states

**Collections/Ordering:**
- Empty collections
- Single element
- Multiple elements
- Ordering properties (if Comparable)

---

## Documentation Standards

### Test Class JavaDoc
```java
/**
 * Comprehensive test suite for ClassName.
 * 
 * Tests cover:
 * - Construction and initialization
 * - Core method behavior
 * - Edge cases and validation
 * - [Other categories]
 * 
 * Generated by Claude (Sonnet 4.5)
 */
```

### Complex Test Comments
Add comments for:
- Non-obvious physics/math formulas
- Expected behavior that may seem counter-intuitive
- Why specific values were chosen

Example:
```java
// Tsiolkovsky equation: m_fuel = m_initial * (1 - e^(-delta_v/v_exhaust))
// For extreme delta-V, fuel approaches total mass but never exceeds it
```

---

## Dependency Management

### Test Order by Dependencies
1. **Zero-dependency classes first** (e.g., Vector3D, Material enums)
2. **Low-dependency classes next** (e.g., ScheduledBurn uses Vector3D)
3. **Complex classes last** (e.g., Spacecraft uses many dependencies)

### Minimal Mocking
- Prefer real objects over mocks
- Use mocks only for:
  - External systems
  - Slow operations
  - Non-deterministic behavior
- Document why mocking was chosen

---

## Edge Case Checklist

For numeric inputs, always test:
- [ ] Zero
- [ ] Negative values (if applicable)
- [ ] Very small values (near zero)
- [ ] Very large values (near overflow)
- [ ] Infinity (if applicable)
- [ ] NaN (if applicable)

For collections/arrays:
- [ ] Empty
- [ ] Single element
- [ ] Multiple elements
- [ ] Null (if applicable)

For objects:
- [ ] Null references
- [ ] Default-constructed
- [ ] Fully-initialized
- [ ] Partially-initialized (if applicable)

---

## Example Test Template

```java
package com.kaymlyn.planeteater.simulation.physics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ClassName.
 * 
 * Tests cover:
 * - [Category 1]
 * - [Category 2]
 * - [Category 3]
 * 
 * Generated by Claude (Sonnet 4.6)
 */
@DisplayName("ClassName Tests")
public class ClassNameTest {

    private static final double TOLERANCE = 1e-6;
    
    private ClassName standardInstance;
    private double standardValue;
    
    @BeforeEach
    void setUp() {
        standardInstance = new ClassName();
        standardValue = 100.0;
    }
    
    // ==================== CONSTRUCTION TESTS ====================
    
    @Test
    @DisplayName("Create instance with default constructor")
    void testDefaultConstruction() {
        ClassName instance = new ClassName();
        
        assertAll("Default construction",
            () -> assertNotNull(instance, "Instance should not be null"),
            () -> assertEquals(expectedValue, instance.getValue(), "Value should be initialized")
        );
    }
    
    // ==================== [OTHER CATEGORIES] ====================
}
```

---

## Review Checklist (Before Submitting Tests)

- [ ] All tests use JUnit 5 annotations
- [ ] All multi-assertion tests use `assertAll()`
- [ ] @BeforeEach contains all common setup
- [ ] @DisplayName used for all test methods
- [ ] Test names are descriptive (testWhatIsBeingTested)
- [ ] Failure messages provided for all assertions
- [ ] Edge cases thoroughly covered
- [ ] No external mocks unless necessary
- [ ] Tests are independent (can run in any order)
- [ ] Class-level constants for tolerance/common values
- [ ] Generated by Claude comment in JavaDoc
- [ ] Organized by category with comment headers
- [ ] Physics/math formulas explained in comments
- [ ] Representative variable names (not single letters)
- [ ] Every assertion is traceable to a specification or physical law,
  OR labeled as DESCRIPTIVE with a comment explaining why

---

## Anti-Patterns to Avoid

Do NOT:
- Use JUnit 4 annotations (@org.junit.Test)
- Omit assertAll when testing multiple things
- Extract helper methods prematurely
- Use single-letter variable names in tests
- Skip edge cases "because they're obvious"
- Rely on test execution order
- Leave assertions without failure messages
- Mix different test categories without clear separation
- Write an assertion to match observed output without deriving it from a law or spec
- Write a comment that contradicts the assertion it annotates

Do:
- Make tests readable by newcomers
- Repeat code if it makes tests clearer
- Test one concept per test method
- Group related assertions with assertAll
- Document non-obvious test logic
- Validate both positive and negative cases
- Consider boundary conditions carefully
- State the physical law or contract each assertion checks

---

## When to Ask for Guidance

Ask the user if:
- Class has complex external dependencies (need mocking strategy)
- Physics/math formula is unclear or undocumented
- Multiple valid testing approaches exist
- Test would require significant test infrastructure
- Coverage gaps vs development time tradeoffs

---

## Summary: Test Writing Workflow

1. **Analyze the class** - Read source, identify methods, dependencies
2. **Plan categories** - Group tests logically (construction, calculations, edges)
3. **Start with @BeforeEach** - Set up common fixtures
4. **Write tests top-down** - Construction -> core methods -> edge cases
5. **Use assertAll liberally** - Group related assertions
6. **Add edge cases** - Zero, negative, null, extreme values
7. **Document complex logic** - Comments for formulas, non-obvious behavior
8. **Trace every assertion** - State the law or contract, or label as DESCRIPTIVE
9. **Review against checklist** - Ensure all standards met
10. **Create review document** - Summarize coverage and design decisions

---

**These guidelines should be integrated into Alex's persona for any test writing tasks.**