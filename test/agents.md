# Test Module

## Purpose
Shared testing utilities consumed by every other module's test scope. Provides custom AssertJ assertions, Bouwkamp-code-based test data, and item/box generators for property-based and academic benchmark tests.

## Key Packages
- `com.github.skjolber.packing.test.assertj` — Fluent custom assertions: `ContainerAssert`, `PackagerAssert`, `StackAssert`, `StackPlacementAssert`, `Point3DAssert`
- `com.github.skjolber.packing.test.bouwkamp` — Bouwkamp codes for squared-rectangle test cases: `BouwkampCodes`, `BouwkampCodeParser`, `BouwkampCodeDirectory`
- `com.github.skjolber.packing.test` — Generic box/item generators used in property-based tests

## Architecture Notes
- This module is a **test utility library**, not a runnable application.
- Consumed as a `test`-scoped dependency by **core**, **points**, **jmh**, and others.
- Bouwkamp codes provide mathematically exact perfect-rectangle test cases (from http://www.squaring.net/) — useful for verifying exact fit without gaps.
- `AssertJ` custom assertions should follow the `AbstractAssert<SELF, ACTUAL>` pattern.

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api |
| compile | assertj-core, truth, truth-java8-extension |
| compile | jackson-databind (test data serialization) |
| compile | commons-math3 (statistical helpers) |
| test    | junit-jupiter, jqwik, mockito-core |

## Build
```
mvn test -pl test
```
