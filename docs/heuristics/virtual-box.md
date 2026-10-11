# Virtual-box preprocessing

`VirtualBoxPackager` in `com.github.skjolber.packing.virtualbox` wraps a packager
and replaces suitable inventories with filled rectangular assemblies. Each
virtual item stands for one assembly; equal assemblies share one item with a
count. Its alternative stack values represent different
layouts of the same original boxes. Returned containers are expanded back into
original boxes, orientations and identities. Input counts and indexes are not
changed by the aggregated path or its ungrouped fallback.

The wrapper does not own or close its delegate. Configure packager-specific
options on the delegate before wrapping it.

```java
try (BruteForcePackager delegate = BruteForcePackager.newBuilder().build();
     VirtualBoxPackager packager = new VirtualBoxPackager(delegate)) {
    PackagerResult result = packager.newResultBuilder()
        .withBoxItems(items)
        .withContainerItems(containers)
        .withMaxContainerCount(3)
        .withMaxLayouts(8)
        .withMaxRefinements(4)
        .withMaxDelegateBoxes(20)
        .withInterruptDuration(1000)
        .build();
}
```

## Factor grids

Preprocessing first constructs factor grids for entire repeated box items:
`columns * rows * layers == count`. It prefers container axes which are matched
exactly, or which leave a strip narrower than every box, then compact envelopes.
Only permitted original orientations are used. Default limits are 10,000 physical
boxes per grid and eight layouts. Every container type which can hold the grid keeps
at least one layout of its own, even beyond that limit, so a virtual box always has a
layout for each container it fits.

## Counts which do not form a grid

If the whole count forms no fitting grid (prime counts, or more boxes than one
container holds), the item is split into container-sized grids, like a loaded
container: full blocks, then whole layers, whole rows and a line. For example,
40 boxes in containers holding 18 become `18 + 18 + 3 + 1`. Equal blocks are
handed to the delegate as one item with a count, so brute force does not
enumerate orders of interchangeable blocks. The split prefers block sizes which the
available containers can hold (container counts and the maximum container count):
with one large and three small containers, 36 boxes become three small-container
blocks rather than two large-container blocks. When containers have costs, blocks
which fit more container types are preferred, so cheaper containers remain usable.

Distinct item types are not combined, even if their dimensions match. This keeps
preprocessing cheap and preserves original identical-item semantics.

## Interrupts

Preprocessing, refinement and delegate attempts share one supplier created
by `PackagerInterruptSupplierBuilder`, using `withInterruptDeadline(...)` or
`withInterruptDuration(...)` and any caller-provided interrupt. There are no
separate preprocessing budgets or reserved time slices. Fallback is
attempted only while the shared operation deadline has not expired. Set
`withAggregation(false)` to disable aggregation.

## Refinement

Aggregation can reduce packing flexibility. The wrapper selectively splits a
large virtual box after failure, or when a successful result might use fewer or
cheaper containers. Without container costs, a result already at the volume and
weight lower bound is not refined if all containers have the same volume. A split
halves the longest axis of the virtual box's grid, so both parts are grids which fit
the containers the original fitted. All equal copies of that virtual box are split
in the same step, or as many as the delegate-item limit allows.
Four refinement steps are allowed by default;
`withMaxRefinements(0)` disables them. Each split preserves original counts,
reuses unchanged assemblies, and retains the best valid packing found so far.
Layouts and unsuccessful grid generations are cached by original inventory and
counts within the operation; repeated child inventories reuse the same layouts.
The delegate's outer packing search is restarted with fresh inventory.

## Delegate-item limit

`withMaxDelegateBoxes(...)` limits the number of physical items handed to the
delegate, including refined and ungrouped attempts. The default is unlimited;
set it when wrapping a brute-force packager. If no permissible representation fits,
the operation may fail without attempting every original box individually.

## Ungrouped fallback

If refinement fails, the wrapper retries the original inventory without
grouping, subject to that count limit and the shared deadline.
Set `withCompareUngrouped(true)` to
also try ungrouped packing after success, retaining lower reported cost, then
fewer containers, then less container volume. This costs another packing
attempt and is disabled by default.

## Load constraints

The standalone grid generator checks internal weight, pressure and stack-depth
limits before constructing placements. An envelope cannot safely represent loads
between assemblies, physical stack depth or original box identities. Consequently,
if any input box has a load constraint, the wrapper bypasses aggregation for the
whole operation and forwards the original inventory to the delegate. Use a
load-aware delegate to enforce those constraints during packing. There is no
post-pack load validation, graph rebuilding or load-validation retry; virtual-box
refinement and delegate-item limits do not apply to this bypass path.

## Other bypasses

Groups, chronological ordering, controlled containers, initial points/obstacles,
motion and existing placements bypass aggregation and retain the delegate's
ordinary behavior. Delegate-specific callbacks must tolerate synthetic boxes
when aggregation is enabled.

## Components

The standalone `GridVirtualBoxLayoutGenerator` exposes the same fast generation.
It checks each candidate's internal load limits analytically before creating
placements. Generated grids skip general overlap validation, create their child
placements only when first accessed, and do not allocate contact graphs or
physical search state. `partition(...)` exposes the container-sized split. Refinement reuses
original orientations without cloning boxes or remapping child placements.
`VirtualBoxLayout` retains a `List<Placement>` whose coordinates are relative to
the virtual box origin. The list and its placements are shared and must not be
modified; expansion creates separate placements at container coordinates.
`VirtualBox` turns equivalent filled layouts into a delegate item, with a count for
equal copies. Expansion mappings use
operation-global item indexes and stack-value indexes, not IDs or mutable local
indexes; custom delegate implementations must preserve those indexes.

## Tests

 * `VirtualBoxPackagerTest`, `VirtualBoxRefinementTest`, `VirtualBoxLoadTest`, `VirtualBoxEnvelopePackingTest`, `VirtualBoxLayoutTest`, `VirtualBoxLayoutPreparationTest` and `GridVirtualBoxLayoutGeneratorTest` in `core` (package `virtualbox`).
 * `VirtualBoxVisualizationTest` in `visualizer/packaging` writes virtual-box scenarios for the [visualizer](../visualizer.md).

## See also

 * [FEATURES.md](../../FEATURES.md#packing-algorithms): the feature summary.
 * [Brute-force packagers](brute-force.md): the delegate of the example above.
