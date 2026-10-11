# Largest Area Fit First (LAFF) packagers

`LargestAreaFitFirstPackager` and `FastLargestAreaFitFirstPackager` fill each container level by level, starting each level with the box which covers the largest ground area.

## Algorithm

The implementation is based on [this paper](https://www.drupal.org/files/An%20Efficient%20Algorithm%20for%203D%20Rectangular%20Box%20Packing.pdf), and is not a traditional [bin packing problem](https://en.wikipedia.org/wiki/Bin_packing_problem) solver.

The box which covers the largest ground area of the container is placed first; its height becomes the level height. Boxes which fill the full remaining height take priority. Subsequent boxes are stacked in the remaining space in at the same level, the boxes with the greatest volume first. If box height is lower than level height, the algorithm attempts to place some there as well. 

When no more boxes fit in a level, the level is incremented and the process repeated. If no box fits the new level,
the level below is raised to the top of the container instead, so that a box which is too tall for it can stand beside
its boxes (for example when the boxes of the level cannot carry it). Boxes are rotated, containers not.

## Ranking

The first placement of a level uses its own placement controls: by default the box with the largest area is placed
first, at the lowest z. The other placements of the level use the defaults of the [plain packager](plain.md#ranking):
the box with the highest volume, then the highest weight, at the position with the lowest area, then the lowest z.
Calculated and required support (`withCalculateSupport(..)`, `withRequireFullSupport(..)`) apply to both, see [support](../support.md).

## Variants

 * `LargestAreaFitFirstPackager` stacks in 3D within each level
 * `FastLargestAreaFitFirstPackager` stacks in 2D within each level: it only places boxes along the floor of each level

## Complexity and limits

The algorithm runs reasonably fast, usually in milliseconds. Some customization is possible:

 * the placement controls, with `withPlacementControlsBuilderFactory(..)`, and those of the first placement of a level, with `withFirstPlacementControlsBuilderFactory(..)` (see [packager controls](../packager-controls.md));
 * the order of box item groups when there is no box item order, with `withBoxItemGroupComparator(..)`, and the comparison of results, with `withIntermediatePackagerResultComparator(..)`.

With full support, the LAFF packagers have a known gap for groups, see [FEATURES.md](../../FEATURES.md#feature-support).

## Tests and benchmarks

 * `LargestAreaFitFirstPackagerTest`, `FastLargestAreaFitFirstPackagerTest`, `RaisedLevelTest` (the raised level) and the `LargestAreaFitFirstPackager*ConstraintTest` classes in `core`.
 * `LargestAreaFitFirstVersion4ComparisonTest` (and `LargestAreaFitFirstVersion4ComparisonIT`) in `core`: comparison with 4.x.
 * `TychoBenchmark` and the `constraint` benchmarks in the `jmh` module, and [jmh/PERFORMANCE.md](../../jmh/PERFORMANCE.md).
