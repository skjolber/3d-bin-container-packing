# Test Module

## Purpose
Shared testing utilities consumed by every other module's test scope. Provides custom AssertJ assertions, Bouwkamp-code-based test data, and item/box generators for property-based and academic benchmark tests.

## Key Packages
- `com.github.skjolber.packing.test.assertj` — Fluent custom assertions: `ContainerAssert`, `PackagerAssert`, `StackAssert`, `StackPlacementAssert`, `Point3DAssert`
- `com.github.skjolber.packing.test.ascii` — ASCII art of containers and boxes for documentation and for explaining unit tests: `ContainerAsciiArt` (front, top, side and oblique "3D" views in the `Style`s LIGHT, ROUNDED, HEAVY and ASCII, optionally shaded; drawn with a depth per character so that hidden parts stay hidden, and only as far as the boxes and points reach, unless `withContainerOutline(true)` asks for the whole container), `Figure` and `Figures` (put drawings next to or below each other), `ContainerAsciiArt.Builder.withPoints(..)` (mark free-space points by their index), `withInclusiveCoordinates(true)` (axis values as in the test assertions), `overview()` (3D, front, top and side views in one figure: one row up to 160 characters, otherwise two rows of two views), `withWidth(columns)` and `withHeight(lines)` (limit the size of every view, which is drawn smaller on its own if it is larger than that; by default the views have the same scale, at which the labels fit, and are at most 80 columns wide and 40 lines high, counting the axes; a label which does not fit inside a thin box is written into its top edge), and `FigureRecorder` (run tests with `-Dfigures.record=true` to write a figure into a `// <figure> ... // </figure>` comment block above the line in the test which records it; see `DefaultPointCalculator3DTest` in **points**)
- `com.github.skjolber.packing.test.bouwkamp` — Bouwkamp codes for squared-rectangle test cases: `BouwkampCodes`, `BouwkampCodeParser`, `BouwkampCodeDirectory`
- `com.github.skjolber.packing.test` — Generic box/item generators used in property-based tests
- `com.github.skjolber.packing.test.example` — Example extensions written against **api** only (`LargestContainerFirstStrategy`, `BackToFrontPlacementComparator`); compiling them here proves that custom behaviour does not need **core**

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
