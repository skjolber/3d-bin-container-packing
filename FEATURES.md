# Feature overview

`3d-bin-container-packing` packs axis-aligned rectangular boxes into one or
more rectangular containers. It is a library for selecting containers and
producing placements; it is not a warehouse-management, shipping, or
visualisation application.

## Core model

- Rectangular boxes and rectangular container loading spaces with integer
  dimensions.
- Per-box identifiers, dimensions, weight, multiplicity, and optional 90-degree
  rotation.
- Per-container identifiers, available count, load dimensions, empty weight,
  maximum load weight, and optional cost calculator.
- A maximum number of containers for each packing operation.
- Multiple container types and finite inventory counts.
- Rectangular obstacles inside a container loading space.
- Application-defined units. Dimensions, weights, costs, and time units are not
  converted by the library.

Containers are not automatically rotated. When different orientations of the
same physical container are valid, provide each orientation as a separate
`ContainerItem`.

## Packing algorithms

| Packager | Intended use |
| --- | --- |
| `PlainPackager` | General-purpose greedy packing with placement controls. |
| `LargestAreaFitFirstPackager` | Fast level-based heuristic for ordinary packing workloads; a level is raised when a box is too tall for it and for a new level on top. |
| `FastLargestAreaFitFirstPackager` | Faster, more restricted LAFF variant. |
| `BruteForcePackager` | Exact search of box orders, rotations, and points for small inputs; can instead rank fitting points and try a configured number per placement step. |
| `FastBruteForcePackager` | Brute-force ordering and rotation search with a faster point choice. |
| `ParallelBruteForcePackager` | Parallel brute-force search for small inputs with available CPU capacity; optionally at a given thread priority (`withThreadPriority(..)`, a scheduler hint). |
| `CompositePackager` | Combines packagers: cheap packagers give a baseline and are tried first for each container; costly packagers run only where the cheaper ones do not fit all remaining boxes, optionally with a time budget. |

The implementation details of each packager are in [docs/heuristics](docs/heuristics/).

Brute-force packagers enforce box load limits when the boxes have them, can
require full support (boxes rest completely on the floor or on the boxes below),
remove duplicate rotations; `BruteForcePackager` and `ParallelBruteForcePackager` can also skip
reverse-equivalent permutations (`withSkipReversePermutations(..)`; not when
the insertion order matters: extraction orders, container access, obstacles, load
limits or full support). A permutation and its reverse give equally good complete
packings, but can differ on partial packings, so the skip only applies when an
unplaceable box aborts the attempt (packing into a single container). Without a box item order, they try every order of the box
item groups for each container; the parallel packager splits the orders between its threads. They are exponential in the number of groups and
of independently ordered boxes; use an interrupt deadline for production requests.

### Feature support

Every packager either packs an input feature, or rejects the input (`getUnsupportedReason(..)`, and the result
builders throw) instead of ignoring it. The table is generated from `PackagerConformanceTest`, which packs random
inputs for each feature with each packager and validates the results:

<!-- feature support table: generated from PackagerConformanceTest -->
| Feature | Plain | LAFF | Fast LAFF | Brute force | Fast brute force | Parallel brute force | Composite |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Box items | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box item groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box item order (`CHRONOLOGICAL`) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box item order, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box item order with skipping (`CHRONOLOGICAL_ALLOW_SKIPPING`) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box item order with skipping, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Container priorities | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Container priorities, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Extraction order | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Extraction order, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Container access through a door (`FRONT`) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Container access through a door, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Obstacles | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Obstacles, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Obstacles in one of two container types | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box load limits | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box load limits, groups | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box load limits, box item order | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Box load limits, box item order with skipping | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Full support (`withRequireFullSupport`) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Full support, groups | ✓ | known gap | known gap | ✓ | ✓ | ✓ | ✓ |
| Custom manifest controls | ✓ | ✓ | ✓ | rejected | rejected | rejected | ✓ |
| Custom manifest controls, groups | ✓ | ✓ | ✓ | rejected | rejected | rejected | ✓ |
<!-- end of feature support table -->

Known gap: with full support, the LAFF packagers place a box which is taller than its level in a new level on top,
where only the level's boxes can support it; a group fails when a later box of it needs the floor beside them.

The composite packager in the table has a plain stage and a fast brute-force stage; it skips stages which reject
the input. Calculated support (`withCalculateSupport(..)`) and custom placement controls
(`withPlacementControlsBuilderFactory(..)`, also with a consumer which configures the default controls) are builder options of the
plain and LAFF packagers only. A custom placement ranking which declares that it reads the supported area
(`PlacementComparator.usesSupportedArea()`, false unless overridden) has it calculated, with or without `withCalculateSupport(..)`.

`VirtualBoxPackager` provides rectangular-assembly preprocessing around packagers:
filled factor grids for repeated items, container-sized grid blocks for counts
which do not fit as one grid (respecting available containers and costs), equal
blocks passed as one counted item, alternative layouts with at least one per
fitting container type, expansion back to original identities, and ungrouped fallback. Optional ungrouped comparison
retains the better result. Selective splitting halves grids along an axis, splits all equal copies at
once, reuses operation-local cached layouts, stops at the container-count lower
bound and obeys refinement and delegate-item limits. Grids check internal loads
analytically, without permutation searches or mutable contact graphs. Any box load
constraint bypasses aggregation for the whole operation, leaving the original
inventory and physical load handling to a load-aware delegate. The wrapper does
not validate loads after packing. Unsupported controls and groups also pass directly
to the delegate.

Without load constraints, filled virtual boxes use one ordinary envelope
insertion each; children are expanded only after a successful delegate attempt.
This path does not prepare physical load contacts or use batch point insertion.
Use `withAggregation(false)` when custom delegate controls need physical child
surfaces or original input items. It bypasses preprocessing and forwards the
operation directly, retaining the delegate's normal result semantics.

## Container selection and allocation

- Ordered selection for the supplied container order, when more than one container may be used
  (`withMaxContainerCount(..)`, default 1; otherwise the result comparator picks).
- Cost-aware selection when container cost calculators are supplied.
- Brute-force container-sequence search for small container-choice spaces.
- Allocation planning that rejects states in which remaining items cannot be
  assigned to available containers.
- Fewest-container and lowest-cost allocation strategies.
- Parallel evaluation of eligible container candidates using an application
  supplied executor.
- Cost estimation and exact cost calculation helpers for box items and groups.
- Custom container packing strategies (`withContainerPackingStrategyFactory(..)` on the packager
  builders), which only need the `api` module. The factory receives the packager's result comparator.

The default strategy chooses an ordered strategy unless cost information is
available. Cost-aware and heuristic strategies aim for good results; they do
not prove a globally minimum-cost packing for arbitrary inputs.

The strategies are described in [docs/container-packing-strategies.md](docs/container-packing-strategies.md),
and container costs in [docs/container-costs.md](docs/container-costs.md).

## Constraints and controls

- Manifest controls and box-item groups for deciding which items may share a
  container. The boxes of a group are inserted together, without boxes of other
  groups between them.
- Point and placement controls for packager-specific candidate-point and
  placement decisions.
- Optional load constraints: supported weight (the total weight resting on a
  box, through all levels above it), pressure, supported box count, and
  identical-box-only stacking.
- Insertion order: the placements of each container are in an order in which the
  boxes can be loaded, after the boxes they rest on and the boxes in their path
  from the container's opening (any, top, or a door at one end).
- Deliveries: an extraction order per box item or group (the boxes of each stop
  can be taken out without moving the boxes for later stops), and a container
  priority (urgent boxes in the first containers). The parallel brute-force
  packager searches box items with container priorities on one thread.
- Optional stability checks: full support, minimum support percentage,
  centre-of-gravity support, and stack centre of gravity.
- Deadlines and custom interruption suppliers for cancellable packing and
  validation.

Controls are extension points, not guarantees that every packager implements
every policy. Select a packager and controls combination that supports the
constraint being enforced, then validate the result when correctness matters.
The brute-force packagers reject manifest and point controls (see
[Feature support](#feature-support)).

See [Writing your own placement controls](DEVELOPER.md#writing-your-own-placement-controls)
for implementation guidance and factory wiring.

## Validation

The optional `validators` module can validate a packing result independently of
the packager. It covers, among other checks:

- container identity and container-count limits;
- box counts and box-item order;
- insertion order, groups inserted together, extraction order and container
  priority;
- containment and box intersection;
- weight, pressure, supported-count, and identical-supporter constraints;
- stability and centre-of-gravity rules;
- manifest rules;
- interruption and diagnostic failure reasons.

Load and stability rules use the placements' positions to find which boxes rest
on which, so results from any packager can be validated.

## Integration and tooling

- Java API module with builder-based model and result APIs.
- Three.js-based visualiser intended for inspecting algorithms and results:
  result summaries, comparison of several results (for example packagers),
  validation reasons on the boxes they concern, colour modes for groups,
  support, load and extraction order, and the centre of gravity.
- JMH benchmarks for packagers, iterators, point calculators, and container
  packing strategies.
- Thread-safe packager instances; selected algorithms also offer explicit
  parallel execution.

## Not currently provided

- Arbitrary geometry: cylinders, irregular or concave solids, curved surfaces,
  and non-rectangular containers. Rectangular obstacles do not change this
  limitation.
- Arbitrary-angle rotation. Only orthogonal box rotations are modelled.
- Automatic rotation of a container definition; callers model permitted
  orientations explicitly.
- A general optimality certificate, lower-bound report, or guaranteed optimal
  result for large packing or cost-allocation problems.
- Automatic physical simulation such as friction, deformation, crushing,
  compression strength, vibration, or dynamic load transfer. Supported-load
  and stability rules are discrete geometric checks.
- Unit conversion, parcel-carrier rules, freight pricing integrations, route
  planning, inventory persistence, or a hosted packing service.
- End-user packing instructions. The visualiser is a development and
  inspection tool, not an operational loading workflow.

## Choosing an approach

- Start with `PlainPackager` or a LAFF packager for normal, latency-sensitive
  requests.
- Use a brute-force packager only for a small number of independently ordered
  boxes and always apply a deadline.
- Supply multiple container items when inventory, orientation, or cost differs.
- Use the validators module for results subject to loading, stability, or
  manifest requirements.
- Benchmark representative production inputs before choosing parallelism; task
  setup and candidate-selection overhead can outweigh parallel work on small
  inputs.
