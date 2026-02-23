# Planet Eater - Documentation Writing Guidelines

**Last Updated:** 2026-02-22
**Parallel document:** TEST_WRITING_GUIDELINES.md

These guidelines define the documentation standard for all public methods in the
Planet Eater codebase. The goal is a consistent, informative baseline that serves
both current contributors and future readers unfamiliar with the implementation
history.

---

## Scope

Document every non-trivial public method. A method is non-trivial if it does
anything beyond directly returning or setting a field. When in doubt, document it.

Methods that are exempt:
- Simple getters that return a field with no transformation
- Simple setters that assign a field with no validation or side effects
- Methods where the name and signature make the contract unambiguous and there is
  no behavior worth specifying (e.g. `isEmpty()` on a standard collection wrapper)

When a getter or setter does something non-obvious (validates, converts units,
delegates to a sub-object, has a meaningful return value), it is not exempt.

---

## Required Tags

Every documented method must have all applicable tags from this list. Omitting a
tag because the content seems obvious is not acceptable.

### @param

Required for every parameter.

The description must cover:
- What the parameter represents in domain terms (not just its Java type)
- The expected range or valid values if bounded (e.g. must be positive, must be
  non-null, must be in meters)
- What happens when an out-of-range or null value is passed, if the method
  defines that behavior

Do not write: `@param mass the mass`
Do write: `@param mass extraction mass in kg; must be positive; zero or negative
values result in an empty return composition`

For parameters with non-obvious semantics, use the full space available. See
Composition.extract() targetingEfficiency as the reference: it explains what a
coefficient of 1.0, 2.0, and 0.5 each mean in practical terms, with worked
examples. When the parameter controls behavior that a caller cannot infer from
the name alone, that level of detail is required.

### @return

Required when the return type is not void.

The description must cover:
- What the return value represents in domain terms
- The range of possible values (e.g. always non-negative, may be null if X,
  returns zero when Y)
- Units if the value is a physical quantity (kg, m, m/s, seconds, dimensionless)
- Whether the returned object is a new instance, a defensive copy, or a live view

Do not write: `@return the mass`
Do write: `@return total mass in kg, always non-negative; returns zero for an
empty composition`

### @throws

Required for every checked exception. Required for unchecked exceptions when the
method explicitly throws them or when a common caller mistake will trigger one.

The description must cover:
- The condition that causes the exception
- Whether the object state is modified before the throw

Do not document RuntimeExceptions that could theoretically propagate from any
Java code (NullPointerException from a null receiver, etc.) unless the method
has a documented contract around null inputs.

---

## Prose Description

The opening prose (before any tags) describes what the method does and any
contracts or invariants a caller needs to understand.

**Minimal methods:** one or two sentences stating the operation and its primary
contract. Example: "Remove a specific amount of a material from this composition.
Returns the actual amount removed, which may be less than requested if the
composition holds insufficient stock."

**Complex methods:** expand as needed. Use prose to explain:
- The algorithm or formula at a high level, with enough detail that the caller
  understands what they are getting without reading the implementation
- Physical laws or domain conventions the result depends on
- Interactions with other state on the object (side effects, preconditions)
- Behaviour at boundary conditions (zero input, empty state, maximum values)

When the method implements a physics formula, state the formula in plain ASCII.
Example: `v = sqrt(GM / r)` not a verbal description alone. This serves as both
documentation and a correctness anchor for future reviewers.

**Length:** prose should be as long as it needs to be and no longer. A two-line
method with a straightforward contract needs two sentences. A method like
Composition.extract() that implements a multi-step weighted extraction algorithm
with targeting, normalization, and automatic dumping needs a full description of
each concept. Do not pad; do not truncate.

---

## Plain ASCII in Code and Javadoc

All content in .java files, including Javadoc, must use plain ASCII only.

- Use `m/s^2` not `m/s²`
- Use `delta-V` not `Δv`
- Use `M_sun` not `M☉`
- Use `sqrt(GM/r)` not `√(GM/r)`
- Use `H2O` not `H₂O`

Unicode is acceptable in standalone .md documentation files only.

---

## What Does Not Belong in Javadoc

**No change history.** Do not note that a method was added, renamed, modified,
or refactored, and do not reference previous implementations. Changes are tracked
by version control and the accomplishments log. Javadoc describes the current
contract, not the history.

**No implementation reminders to self.** Notes like "TODO: handle edge case" or
"this might be slow" belong in the issue tracker or a code comment, not Javadoc.

**No redundant restatement of the signature.** `@param material the Material`
adds nothing. The tag must add information beyond what the type name provides.

---

## Reference Example: Composition.extract()

This method is the reference for what thorough documentation looks like when a
method has non-obvious semantics.

It covers:
- A plain-English model of the domain concept (relative quantities, targeting
  efficiency, automatic dumping) before any tags
- A @param for `mass` that explains the relationship between input mass and
  actual extracted mass (dumping means output is less)
- A @param for `targetingEfficiency` with worked numerical examples (1.0, 2.0,
  0.5, 5.0) that make the coefficient's effect concrete and testable
- A @param for `targets` that explains the auto-dump rate (95%) and where
  dumped material goes in the simulation
- A @return that describes the composition returned, not just its type

When writing documentation for a method with similar complexity, use this as
the floor, not the ceiling.

---

## Authorship Tag

All files generated or substantially modified by Claude carry:

    Generated by Claude (Sonnet 4.6)

in the class-level Javadoc. Do not add this tag to individual methods. Do not
update this tag when making incremental changes to a file; it identifies the
primary author of the file, not a change log.

---

## Quick Checklist

Before committing Javadoc for a non-trivial public method:

- [ ] Opening prose states what the method does and its primary contract
- [ ] @param present for every parameter, with domain description and valid range
- [ ] @return present if non-void, with units and range
- [ ] @throws present for all explicitly thrown exceptions
- [ ] Physics formulas written in plain ASCII
- [ ] No change history or version notes
- [ ] No Unicode characters (plain ASCII only in .java files)
- [ ] No redundant restatement of the signature in tag descriptions