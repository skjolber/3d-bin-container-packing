# Support

Support (the area resting on boxes below) can be calculated, or full support required:

```java
PlainPackager packager = PlainPackager
    .newBuilder()
    .withCalculateSupport(true)     // prefer better supported placements
    .withRequireFullSupport(true)   // or: only place fully supported boxes
    .build();
```

## Plain and LAFF packagers

The LAFF packager builders have the same options. The options are also available on the placement controls, with
`withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true))` on the plain and LAFF builders (and
`withFirstPlacementControlsBuilderFactory(..)` for the first placement of a LAFF level), where they have the same effect,
and which also take a box item comparator and a placement ranking. A placement ranking which declares that it reads the supported area of
a candidate (`PlacementComparator.usesSupportedArea()`, false unless overridden) gets it calculated, whether or not
`withCalculateSupport(..)` is set; a box on the floor is fully supported.

## Brute-force packagers

The brute-force packager builders have `withRequireFullSupport(true)`:
boxes are placed only where they rest completely on the floor or on the boxes below, at the free points and shifted from
a free point onto the corner of a box below (as the plain packager does when no free point holds a box fully supported).
Boxes do not rest on obstacles. Brute force has no `withCalculateSupport(..)`: it keeps the arrangement with the most
volume, rather than ranking positions.

## See also

 * [FEATURES.md](../FEATURES.md#feature-support): which packager supports full support, including the known gap for the LAFF packagers with groups.
 * [Plain packager](heuristics/plain.md), [LAFF packagers](heuristics/largest-area-fit-first.md) and [brute-force packagers](heuristics/brute-force.md).
