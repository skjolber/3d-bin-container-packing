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
| `LargestAreaFitFirstPackager` | Fast level-based heuristic for ordinary packing workloads. |
| `FastLargestAreaFitFirstPackager` | Faster, more restricted LAFF variant. |
| `BruteForcePackager` | Exact search of box orders, rotations, and points for small inputs; can instead rank fitting points and try a configured number per placement step. |
| `FastBruteForcePackager` | Brute-force ordering and rotation search with a faster point choice. |
| `ParallelBoxItemBruteForcePackager` | Parallel brute-force search for small inputs with available CPU capacity. |
| Load-aware brute-force variants | Brute-force packing with load constraints. |

Brute-force packagers remove duplicate rotations and can skip reverse-equivalent
permutations. They remain exponential in the number of independently ordered
boxes; use an interrupt deadline for production requests.

## Container selection and allocation

- Ordered selection for the supplied container order.
- Cost-aware selection when container cost calculators are supplied.
- Brute-force container-sequence search for small container-choice spaces.
- Allocation planning that rejects states in which remaining items cannot be
  assigned to available containers.
- Fewest-container and lowest-cost allocation strategies.
- Parallel evaluation of eligible container candidates using an application
  supplied executor.
- Cost estimation and exact cost calculation helpers for box items and groups.

The default strategy chooses an ordered strategy unless cost information is
available. Cost-aware and heuristic strategies aim for good results; they do
not prove a globally minimum-cost packing for arbitrary inputs.

## Constraints and controls

- Manifest controls and box-item groups for deciding which items may share a
  container.
- Point and placement controls for packager-specific candidate-point and
  placement decisions.
- Optional load constraints: supported weight, pressure, supported box count,
  and identical-box-only stacking.
- Optional stability checks: full support, minimum support percentage,
  centre-of-gravity support, and stack centre of gravity.
- Deadlines and custom interruption suppliers for cancellable packing and
  validation.

Controls are extension points, not guarantees that every packager implements
every policy. Select a packager and controls combination that supports the
constraint being enforced, then validate the result when correctness matters.

## Validation

The optional `validators` module can validate a packing result independently of
the packager. It covers, among other checks:

- container identity and container-count limits;
- box counts and box-item order;
- containment and box intersection;
- weight, pressure, supported-count, and identical-supporter constraints;
- stability and centre-of-gravity rules;
- manifest rules;
- interruption and diagnostic failure reasons.

## Integration and tooling

- Java API module with builder-based model and result APIs.
- OpenAPI model, client, server, and test modules.
- Three.js-based visualiser intended for inspecting algorithms and results.
- JMH benchmarks for packagers, iterators, point calculators, and container
  strategies.
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
