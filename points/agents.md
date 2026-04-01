# Points Module

## Purpose
Manages free-space bookkeeping during packing. Tracks 2D and 3D points that represent candidate placement locations within a container. Provides point calculators that maintain and update the set of available positions as boxes are placed.

## Key Packages
- `com.github.skjolber.packing.ep.points2d` — 2D point types: `Point2D`, `DefaultPoint2D`, `XSupportPoint2D`, `YSupportPoint2D`, `Point2DList`, `Point2DFlagList`
- `com.github.skjolber.packing.ep.points3d` — 3D point types: `Point3D`, `DefaultPoint3D`, plane-specific variants (`XYPlanePoint3D`, `XZPlanePoint3D`, `YZPlanePoint3D`)
- Calculator strategies: `DefaultPointCalculator2D`, `MarkResetPointCalculator2D`

## Architecture Notes
- Depends only on **api**; no dependency on **core**.
- Uses **Eclipse Collections** for performance-optimized list operations — prefer these over standard `java.util` collections.
- `Point2DFlagList` uses bitmask flags to avoid object allocation in hot paths.
- Plane-specific 3D variants encode which container walls support a placement, enabling smarter candidate filtering.

## Testing
- JUnit 5, AssertJ, jQwik (property-based tests verify point calculator invariants)
- Run: `mvn test -pl points`

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api, eclipse-collections |
| test    | test module, junit-jupiter, assertj-core, jqwik |

## Build
```
mvn test -pl points
```
