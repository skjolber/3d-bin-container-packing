# API Module

## Purpose
Foundation layer of the 3D bin-packing library. Defines all public interfaces, abstract base classes, and core data structures that the rest of the system depends on. No algorithm logic lives here — only contracts.

## Key Packages
- `com.github.skjolber.packing.api` — Core types: `Packager`, `PackagerResult`, `PackagerResultBuilder`, `BoxStackValue`, `Rotation`, `Surface`
- `com.github.skjolber.packing.api.packager` — Item source abstractions for boxes and containers
- `com.github.skjolber.packing.api.packager.control` — Control framework: `ManifestControls`, `PlacementControls`, `PointControls`
- `com.github.skjolber.packing.api.point` — Point-related interfaces used by free-space tracking
- `com.github.skjolber.packing.api.validator` — Validation interfaces for placement correctness

## Architecture Notes
- This is the **dependency root** — no other internal modules are imported here.
- All packager implementations in `core` implement `Packager<B>`.
- Builder pattern is used heavily (`PackagerResultBuilder`).
- Changes to interfaces here cascade to every module — be conservative.

## Testing
- JUnit 5, AssertJ, Mockito, jQwik (property-based)
- No integration tests; unit tests only

## Dependencies
| Scope | Artifact |
|-------|----------|
| test  | junit-jupiter, assertj-core, mockito-core, jqwik |

## Build
```
mvn test -pl api
```
