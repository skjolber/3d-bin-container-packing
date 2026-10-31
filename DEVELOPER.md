# Developer guide

This guide shows how to extend the packagers with your own rules. Each section starts from a small example that lives in
this repository, then explains the rules the example follows. Where a snippet is shortened, the full class is named: copy
from that.

## Extending core

Custom controls, comparators and container packing strategies only need the `api` module. It is the stable contract.

```xml
<dependency>
    <groupId>com.github.skjolber.3d-bin-container-packing</groupId>
    <artifactId>api</artifactId>
    <version>${3d-bin-container-packing.version}</version>
</dependency>
```

The `core` module goes further. It deliberately exports all its packages, so that custom algorithms can build on its
internals: packagers, sessions, iterators and point calculators. Those internals may change in any release, without
migration notes. Extend them at your own risk. The examples below say when they build on `core`.

## Writing your own placement controls

Controls add application-specific rules to a packager, without writing a new packing algorithm. There are three kinds, and
each decides something different:

| Controls | Decide | Set on |
|---|---|---|
| Manifest controls | which boxes may share a container | the container item |
| Point controls | which free points a box may use | the container item |
| Placement controls | which box, orientation and position comes next | the packager |

The interfaces are in the `api` module, in the packages `manifest`, `point` and `placement` under
`com.github.skjolber.packing.api.packager.control`.

Every kind is created for each packing attempt, in three steps: a factory creates a builder, the packager hands the builder
what it knows, and the builder builds the controls. Only the plain and LAFF packagers use controls. The brute-force
packagers do not support them and reject such inputs (see `getUnsupportedReason(..)`); they have their own point-filter and
comparator extension points instead.

Pick the lightest tool that does the job:

1. A packager or box option, if the rule already exists.
2. A comparator, if you only want to rank the candidates differently.
3. Point controls, if the rule depends on which box it is.
4. Manifest controls, if the rule is about which boxes share a container.
5. Placement controls, if the rule depends on what is already in the container.

### Check what the packager already does

Many rules that look like they need custom code are options. Here are three of them, as `PlacementRulesExampleTest` uses
them:

```java
// Full support: no box overhangs. An option of the packager.
PlainPackager packager = PlainPackager.newBuilder()
    .withRequireFullSupport(true)
    .build();

// Keep a region free: the obstacles of the container item. This one is a column from the floor
// to the roof, which no box can be placed in or over.
PackagerResult result = packager.newResultBuilder()
    .withContainerItem(b -> {
        b.withContainerItem(container, 1);
        b.withObstacles(o -> o.withObstacle(4, 0, 0, 1, 1, container.getLoadDz()));
    })
    .withMaxContainerCount(1)
    .withBoxItems(boxItems)
    .build();

// A box which carries boxes of its own type only, up to two levels high. A load constraint of the box.
Box tray = Box.newBuilder()
    .withId("tray")
    .withSize(1, 1, 1)
    .withWeight(2)
    .withMaxLoadIdenticalBoxCount(2)
    .build();
```

`PlacementRulesExampleTest` is in the test sources of `core`, in the package `com.github.skjolber.packing.packer.example`.
It packs each rule next to the same input without the rule, in a single container, and checks the layouts with the
validators.

Custom placement controls replace the default ones, so they keep none of these rules. If the rule exists, configure the
packager instead.

### Ranking candidates with a comparator

If you only want boxes and positions ranked differently, you do not need new controls. A placement comparator is enough.
This one loads a container from the back wall towards the doors, bottom up:

```java
public class BackToFrontPlacementComparator implements PlacementComparator {

    @Override
    public int compare(Placement a, Placement b) {
        // a positive value means a is better
        int result = Integer.compare(b.getAbsoluteX(), a.getAbsoluteX());
        if(result != 0) {
            return result;
        }
        result = Integer.compare(b.getAbsoluteZ(), a.getAbsoluteZ());
        if(result != 0) {
            return result;
        }
        return Integer.compare(b.getAbsoluteY(), a.getAbsoluteY());
    }
}
```

The ranking interfaces (`PlacementComparator`, `PlacementComparatorFactory` and `PlacementComparatorAttribute`) are in the
`api` module, so the comparator itself needs no `core`. The class above is excerpted from the `test` module (package
`com.github.skjolber.packing.test.example`), which holds complete examples that depend on `api` only. `ApiExtensionTest`
in `core` configures them on the packagers.

The plain and LAFF builders take a consumer that configures their default controls. Pick one of three ways to rank:

```java
// A fixed comparator
PlainPackager fixed = PlainPackager.newBuilder()
    .withPlacementControlsBuilderFactory(b -> b.withPlacementComparator(new BackToFrontPlacementComparator()))
    .build();

// The library's DefaultPlacementComparatorFactory.Builder: ranking attributes, in the order you list them
PlainPackager byAttributes = PlainPackager.newBuilder()
    .withPlacementControlsBuilderFactory(b -> b.withPlacementComparators(r -> r.lowerZIsBetter()))
    .build();

// Your own PlacementComparatorFactory
PlainPackager byFactory = PlainPackager.newBuilder()
    .withPlacementControlsBuilderFactory(b -> b.withPlacementComparatorFactory(myFactory))
    .build();
```

LAFF builders also take `withFirstPlacementControlsBuilderFactory(b -> ..)` for the first placement of each level. Configure
both when the ranking must apply throughout the packing.

The consumer also has `withCalculateSupport(..)` and `withRequireFullSupport(..)`, with the same effect as the packager
options of the same name. There is one catch. A comparator of your own replaces the default ranking, which includes
support, so rank by support yourself, for example with `higherSupportIsBetter()` in the second form above.

A comparator that reads how much of a box is supported must say so:

```java
public class BestSupportedPlacementComparator implements PlacementComparator {

    @Override
    public int compare(Placement a, Placement b) {
        // a positive value means a is better
        return Long.compare(a.getSupportedArea(), b.getSupportedArea());
    }

    @Override
    public boolean usesSupportedArea() {
        // Reads the supported area, so the controls calculate it for each candidate (a box on the floor is
        // fully supported), whether or not the packager is set to calculate support. Without this, the
        // supported area reads as zero and the calculation is skipped. A lambda cannot say it: it defaults to false.
        return true;
    }

    @Override
    public boolean prefersHigherSupportedArea() {
        // Less support never compares better, so the controls can skip load validation and the support
        // calculation for candidates which cannot be selected.
        return true;
    }
}
```

### Point controls: which points a box may use

Point controls filter the free points the packager offers to each box. Use them when a rule depends on which box it is,
for example "barrels stay on the floor". The packagers have no such rule, so it needs code.

`GroundLevelBoxIdPointControls` keeps the boxes with given ids on the floor, by giving them only the points at the floor:

```java
// DefaultPointControls gives every box all the points. This class overrides getPoints(..) to give some boxes fewer.
public class GroundLevelBoxIdPointControls extends DefaultPointControls {

    public static PointControlsBuilderFactory newFactory(String... boxIds) {
        ...
        // the ids are part of the factory's id: factories for the same boxes build the same controls
        return PointControlsBuilderFactory.of("groundLevelBoxId:" + ids, () -> new Builder(immutableIds));
    }

    // The packager hands the builder the points (and more), then asks it to build
    private static class Builder extends AbstractPointControlsBuilder<Builder> {
        ...
        @Override
        public PointControls build() {
            return new GroundLevelBoxIdPointControls(points, boxIds);
        }
    }

    // Every box gets all the points, except the boxes with the given ids
    @Override
    public PointSource getPoints(BoxItem boxItem) {
        String id = boxItem.getBox().getId();
        if(id == null || !boxIds.contains(id)) {
            return filteredPoints;
        }
        // a source of its own with the points at the floor; the packager keeps maintaining the source it handed to the builder
        List<Point> floor = new ArrayList<>();
        for(int i = 0; i < filteredPoints.size(); i++) {
            Point point = filteredPoints.get(i);
            if(point.getMinZ() == 0) {
                floor.add(point);
            }
        }
        return new DefaultPointSource(floor);
    }
}
```

The filter only has an effect if the placement controls ask for the points. The built-in placement controls do. Placement
controls of your own must take the candidate points of each box item from `PointControls.getPoints(boxItem)`, not from the
point calculator, as the `getPlacement` example further down does.

The class is in the test sources of `core`, in the same package as `PlacementRulesExampleTest`, which uses it. See
[Packager controls](docs/packager-controls.md) for the other kinds of controls.

### Manifest controls: which boxes share a container

Manifest controls decide which boxes may travel together, for example no lighters with petrol, or at most one fire hazard
per container. They work by removing box items from the shared box item source. The packager reads the source again before
every placement, so there is nothing to return from the callbacks.

This one allows a number of fire hazards, and then removes the rest from the source:

```java
public class MaxFireHazardBoxItemPerContainerManifestControls implements ManifestControls {

    public static final ManifestControlsBuilderFactory newFactory(int maxCount) {
        return () -> new Builder().withMaxCount(maxCount);
    }

    @Override
    public void accepted(BoxItem boxItem) {
        if(isFireHazard(boxItem)) {
            count++;

            if(count >= maxCount) {
                removeFireHazards();
            }
        }
    }

    private void removeFireHazards() {
        for(int i = 0; i < items.size(); i++) {
            if(isFireHazard(items.get(i))) {
                items.remove(i);
                i--;
            }
        }
    }

    ...
}
```

Manifest controls are created for each packing attempt in a container, so they can keep state, as the counter does.
`MaxFireHazardBoxItemPerContainerManifestControls` and, for box item groups, `NoLighterWithPetrolManifestControls` are in
the test sources of `core`, in the package `com.github.skjolber.packing.packer.plain`.

The factory above is a lambda, so it has no id: see [Reusing results](#reusing-results-factory-ids).

### Wiring controls to container items

Manifest and point controls belong to a container item, so different container types can have different rules. Set the
factories with the container item builder of the result builder:

```java
PackagerResult result = packager.newResultBuilder()
    .withContainerItem(b -> b
        .withContainerItem(container, 5)
        .withManifestControlsBuilderFactory(MaxFireHazardBoxItemPerContainerManifestControls.newFactory(1))
        .withPointControlsBuilderFactory(GroundLevelBoxIdPointControls.newFactory("drum")))
    .withBoxItems(boxItems)
    .withMaxContainerCount(5)
    .build();
```

Or set them on a container item you pass to `withContainerItems(..)`:

```java
ContainerItem containerItem = new ContainerItem(container, 5);
containerItem.setManifestControlsBuilderFactory(MaxFireHazardBoxItemPerContainerManifestControls.newFactory(1));
containerItem.setPointControlsBuilderFactory(GroundLevelBoxIdPointControls.newFactory("drum"));

PackagerResult result = packager.newResultBuilder()
    .withContainerItems(containerItem)
    .withBoxItems(boxItems)
    .withMaxContainerCount(5)
    .build();
```

Placement controls are set on the packager builder instead, see below. Another wiring example is in
[Packager controls](docs/packager-controls.md).

### Reusing results: factory ids

Packagers reuse the result of one container for another container when the two containers have the same manifest controls
factory and the same point controls factory. Factories match if they are the same instance, or carry the same id. The id
comes from `getId()`, and the default is `null`, which never matches another instance. Containers without controls match
each other, but a result that was packed without controls is not reused for a container with them.

The id must cover everything that changes the behaviour of the controls, so put the configuration in it. A lambda has no
id, so wrap it to let it take part in reuse:

```java
// No id: never reused for another container item, even with the same settings
PointControlsBuilderFactory withoutId = () -> new MyPointControlsBuilder(boxIds);

// The id covers the configuration: container items with the same box ids share results
PointControlsBuilderFactory withId = PointControlsBuilderFactory.of("groundLevelBoxId:" + boxIds, () -> new MyPointControlsBuilder(boxIds));
```

`GroundLevelBoxIdPointControls.newFactory(..)` does the second.

### Placement controls: choosing the next placement

Placement controls choose the next box, its orientation and its position within a container. Write them when a rule depends
on what is already in the container, for example a weight limit for the half of the container by the doors.

Prefer extending existing controls over starting from scratch. This example builds on `core`: it extends
`ComparatorPlacementControls`, which iterates over the candidates for you. The example only vetoes the candidates that break
the rule, and it keeps state across the choices that the packager makes:

```java
public class DoorHalfWeightLimitControls extends ComparatorPlacementControls {

    public static class Builder extends ComparatorPlacementControlsBuilder {

        private final long maxWeight;

        public Builder(long maxWeight) {
            this.maxWeight = maxWeight;
        }

        @Override
        public PlacementControls build() {
            // rules which these controls do not implement are rejected, not ignored
            if(fullSupport || maxLoadWeight || maxLoadPressure || maxLoadBoxCount || loadIdenticalBox) {
                throw new IllegalStateException("Full support and load limits are not supported");
            }
            // the packager has populated the builder with the box items, point controls, point calculator,
            // container, stack, order and the load-constraint flags
            return new DoorHalfWeightLimitControls(maxWeight, boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
        }
    }

    private final long maxWeight;
    private long weight; // placed in the door half so far

    private DoorHalfWeightLimitControls(long maxWeight, BoxItemSource boxItems, PointControls pointControls, PointCalculator pointCalculator, Container container,
            Stack stack, Order order, PlacementComparator placementComparator, BoxItemComparator boxItemComparator) {
        super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
        this.maxWeight = maxWeight;
    }

    // createPlacement(..) and selectPlacement(..) are the hooks of ComparatorPlacementControls. Every candidate
    // passes createPlacement(..), and returning null rejects it.
    @Override
    protected Placement createPlacement(Point point, BoxItem boxItem, BoxStackValue stackValue) {
        // Candidates are only looked at here, so change no state. The packager adds the chosen placement to
        // its stack and point calculator and updates the inventory: do not do that in your controls.
        if(isInDoorHalf(point.getMinX(), stackValue.getDx()) && weight + boxItem.getBox().getWeight() > maxWeight) {
            return null; // rejects this candidate
        }
        return super.createPlacement(point, boxItem, stackValue);
    }

    @Override
    public void accepted(Placement placement) {
        // Called after the placement was accepted and added to the stack: this is the place for state changes
        // which follow from a choice. (The built-in load-aware controls link supporters and supportees here.)
        if(isInDoorHalf(placement.getAbsoluteX(), placement.getStackValue().getDx())) {
            weight += placement.getWeight();
        }
    }

    @Override
    public void undo(List<Placement> placements) {
        // A group did not fit, and its placements are rolled back: reverse what accepted(..) did
        for(Placement placement : placements) {
            if(isInDoorHalf(placement.getAbsoluteX(), placement.getStackValue().getDx())) {
                weight -= placement.getWeight();
            }
        }
    }

    private boolean isInDoorHalf(int x, int dx) {
        // the doors are at the high end of x: a box counts if its center is past the middle
        return 2 * x + dx > container.getLoadDx();
    }
}
```

Placement controls are set on the packager builder:

```java
// A new builder, and so new controls, for every request from the packager. Do not share mutable
// packing state across attempts or parallel workers.
PlacementControlsBuilderFactory controlsFactory = () -> new DoorHalfWeightLimitControls.Builder(5000)
    .withPlacementComparator(new BackToFrontPlacementComparator())
    .withBoxItemComparator(VolumeThenWeightBoxItemComparator.getInstance());

try (PlainPackager packager = PlainPackager.newBuilder()
        .withPlacementControlsBuilderFactory(controlsFactory)
        .build()) {
    PackagerResult result = packager.newResultBuilder()
        .withContainerItems(containerItems)
        .withBoxItems(boxItems)
        .build();
}
```

The plain and LAFF packagers take this factory. LAFF also has `withFirstPlacementControlsBuilderFactory(..)` for the first
placement of a level: set both when the rule must apply throughout the packing.

Custom placement controls replace the default ones, and keep none of their checks. Implement the constraints that are
requested, or reject unsupported configurations in your builder, as the example does. For the same reason, the packager
options that configure the default controls (`withCalculateSupport(..)` and `withRequireFullSupport(..)`) cannot be combined
with custom controls: `build()` throws.

Box load limits (maximum load weight, pressure and box count, and identical boxes only) need a declaration. Packagers reject
inputs that have them unless the factory's `supportsLoad()` returns true. Return true only if your controls respect the
load-constraint flags that the packager sets on the builder. The support-aware and load-aware controls in
`com.github.skjolber.packing.packer` show how to build physical constraints.

#### Implementing getPlacement yourself

If you implement `PlacementControls` from scratch, `getPlacement(..)` has the same shape as the one in
`ComparatorPlacementControls`. This is the part of it that matters (abridged):

```java
public Placement getPlacement(int offset, int length) {
    Placement result = null;

    // Only the box items in the range: the current group when packing groups, otherwise the box items which
    // may be placed in the container now. Do not search outside it. The indexes of the box item source are
    // local and change as inventory is removed, so keep the identity of the selected item and use current indexes.
    for (int i = offset; i < offset + length; i++) {
        BoxItem boxItem = boxItems.get(i);

        // Candidate points come from the point controls, so that their filter has an effect
        PointSource points = pointControls.getPoints(boxItem);

        for (BoxStackValue stackValue : boxItem.getBox().getStackValues()) { // each orientation
            for (Point point : points) {
                if(!point.fits3D(stackValue)) {
                    continue;
                }
                // Keep this loop light on allocation. Never recycle an accepted placement, or change its box or
                // coordinates, while evaluating candidates.
                Placement candidate = createPlacement(point, boxItem, stackValue);
                if(candidate == null) {
                    continue;
                }
                result = selectPlacement(result, candidate); // keeps the better of the two
            }
        }

        // Respect the requested order. With Order.NONE, every box item in the range is tried, and the best candidate wins.
        if(order == Order.CHRONOLOGICAL) {
            break; // only the first box item is tried, even if it did not fit
        }
        if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING && result != null) {
            break; // box items which do not fit are skipped, and the first which fits is chosen
        }
    }
    return result; // null if nothing fits
}
```

### When a group does not fit

When you pack box item groups, a group that is only partly placed is rolled back. The controls hear about it in this order:

```
undo(boxItems)           on the manifest controls, then on the point controls
undo(placements)         on the placement controls
attemptFailure(group)    on the manifest controls, then on the point controls
```

The undo calls are skipped if nothing was placed. Point controls are notified like manifest controls, because both are
manifest listeners, and every callback is optional. The notification `attempt(group, offset, length)` goes to the manifest
controls only.

### Thread safety

Packagers run concurrently, for example under the parallel container packing strategy and the parallel brute-force
packager. Everything you give a packager as a factory or a comparator must be safe for concurrent use: the controls
factories, the placement comparator factory, the box item and box item group comparators, and the result comparator.
Stateless ones are. The controls themselves are created for each attempt, so they can keep state.

### Testing your controls

Test the cases that break custom controls:

 * attempts with no candidate, and rotated boxes;
 * the order of the box items;
 * rolling back a group, and repeated attempts;
 * concurrent use.

When your rules are about load or stability, also validate the final layouts independently of your code, with the
validators module. `PlacementRulesExampleTest` does so for every rule.

## Writing your own container packing strategy

A container packing strategy decides which containers to use, and in which order. The built-in ones pick the first
container that holds all the boxes, or the cheapest combination. Write your own when neither matches how you ship. The
interfaces live in `com.github.skjolber.packing.api.packager.strategy`, so a strategy only needs the `api` module.

This one fills the largest container first, then the next largest, until all boxes are packed:

```java
public class LargestContainerFirstStrategy implements ContainerPackingStrategy {

    @Override
    public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerSession session) throws PackagerInterruptedException {
        // the session holds the remaining boxes and the available containers
        ContainerInventory inventory = session.getContainerInventory();

        List<Container> containers = new ArrayList<>();
        while(session.countRemainingBoxes() > 0) {
            // Check the interrupt regularly. The exception carries no stack trace, so throwing it is cheap.
            // session.attempt(..) throws it too.
            if(interrupt.getAsBoolean()) {
                throw new PackagerInterruptedException();
            }
            int index = getLargestAvailableContainer(inventory);
            if(index == -1) {
                // no more containers: the boxes cannot be packed
                return null;
            }
            // Pack as many of the remaining boxes as possible into a container of this type, without
            // changing the session. No best result to beat, and no giving up if a box is too big.
            IntermediatePackagerResult result = session.attempt(index, null, false);
            if(result.isEmpty()) {
                return null;
            }
            // Accept the result: removes its boxes from the remaining boxes, uses up the container
            // and returns the packed container.
            containers.add(session.accept(result));
        }
        // once no boxes remain: the accepted containers, and what they cost
        return new ContainerResult(inventory.getCost(), containers);
    }

    ...
}
```

The full class, with `getLargestAvailableContainer(..)`, is in the `test` module (package
`com.github.skjolber.packing.test.example`), and it depends on the `api` module only. `ApiExtensionTest` in `core`
configures it on the packagers.

A strategy can try alternatives before it accepts one. `fork()` copies the session at its current state, and each fork is
independent of the original, so the forks can run on one thread each. `ParallelContainerPackingStrategy` in `core` does
this (abridged):

```java
// the number of containers which can still be used for the remaining boxes
int limit = session.getMaxContainerCount();
...
// the container types which can potentially hold the remaining boxes
List<Integer> containerIndexes = session.getContainers();
...
for(int containerIndex : containerIndexes) {
    // an independent copy of the session, for one thread
    PackagerSession fork = session.fork();
    futures.add(executorService.submit(() -> fork.attempt(containerIndex, null, abortOnAnyBoxTooBig)));
}
...
// the best result by the packager's own comparator, accepted in the original session
containers.add(session.accept(best));
```

The rest of the session and inventory, in short:

```java
ContainerInventory inventory = session.getContainerInventory();

// what fits
inventory.canLoad(boxItem, containerIndex); // can this container type load the box item?
inventory.getContainers(boxItems); // the container types which can potentially hold the boxes
inventory.isFeasible(boxItems); // can the boxes be loaded at all, by the containers that are left?

// cost
if(inventory.hasCost()) { // do the container types have cost calculators?
    inventory.getCost(); // the cost of the containers used so far
}

// reuse an existing result for another container type, if its boxes fit unchanged; null otherwise
session.peek(containerIndex, existing);

// independent sessions, one thread each: fork() continues from here, fresh() restarts the packaging operation
PackagerSession fork = session.fork();
PackagerSession fresh = session.fresh();
```

The [Container packing strategies](docs/container-packing-strategies.md) page explains what each built-in strategy does
with these calls, and [Container costs](docs/container-costs.md) covers the cost calculators.

### Configuring the strategy

Set the strategy with a factory, which the packager calls once for each packaging operation:

```java
try (PlainPackager packager = PlainPackager.newBuilder()
        .withContainerPackingStrategyFactory((inventory, boxItems, boxItemGroups, comparator, emptyResult) -> new LargestContainerFirstStrategy())
        .build()) {
    // ...
}
```

The factory receives the available containers, the remaining box items (null when packing box item groups) or box item
groups (null when packing box items), and the packager's own result comparator and empty-result supplier. A strategy
that compares results, or returns an empty result, should use these two, so that it follows a custom comparator that the
packager was configured with. The built-in strategies take them as constructor arguments, like
`ParallelContainerPackingStrategy` here:

```java
.withContainerPackingStrategyFactory((inventory, boxItems, boxItemGroups, comparator, emptyResult) ->
    new ParallelContainerPackingStrategy(executor, comparator))
```

Return a new strategy for every call, or a stateless one. The packager uses a strategy for one packaging operation and
does not share it. Packagers are thread-safe, so the factory can be called concurrently: implementations must be safe for
concurrent use.

All packager builders have `withContainerPackingStrategyFactory(..)`. For `CompositePackager`, the strategy also applies to
the baseline packagers. The built-in strategies are in the `core` package `com.github.skjolber.packing.packer.strategy`, and
`DefaultContainerPackingStrategyFactory` chooses between them.

### Comparing results

Results of packing attempts are compared with an `IntermediatePackagerResultComparator`, which is also in the `api`
module. Compare by load volume, load weight and box count, and not by the stacks of the results: the brute-force packagers
compare results whose stacks are not built.

If load volume comes first, say so. The packager can then skip searches that cannot load more than the best result so far:

```java
IntermediatePackagerResultComparator comparator = new IntermediatePackagerResultComparator() {

    @Override
    public int compare(IntermediatePackagerResult a, IntermediatePackagerResult b) {
        // a positive value means a is better: load volume first
        return Long.compare(a.getLoadVolume(), b.getLoadVolume());
    }

    @Override
    public boolean prefersHigherLoadVolume() {
        // a result with less load volume always compares worse
        return true;
    }
};
```

The composite and the ordered container packing strategy then pass the best result so far to `attempt(..)`, which is how a
costly packager gets to skip work.

## Packagers in a composite

`CompositePackager` combines packagers: a cheap one packs the whole order first, and costlier ones only take over for the
containers the cheap one could not fill. It works with any `AbstractPackager`, in the package
`com.github.skjolber.packing.packer.composite`, so your own packager can take part:

```java
CompositePackager packager = CompositePackager.newBuilder()
    .withPackager(PlainPackager.newBuilder().build())                // tried first, for every container
    .withPackager(FastBruteForcePackager.newBuilder().build(), 200)  // only where plain does not fit all boxes, for at most 200 ms
    .build();
```

For each packaging operation, the composite creates a session for every packager with `createSession(PackagerInput,
interrupt)`, and keeps the sessions in sync: a result from one packager's session is accepted by all of them. A packager
takes part by following that session contract. The two overrides are small (`MySession` stands for your own session class):

```java
@Override
protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
    // The box items and container items of the input are copies which belong to the new session.
    // The boxes are shared, as they are never modified.
    return new MySession(input, interrupt);
}

@Override
public String getUnsupportedReason(PackagerInput input) {
    // null means the input is supported. A reason makes supports(..) false, and the composite skips this packager for the input.
    if(input.getBoxItemGroups() != null) {
        return "Box item groups are not supported";
    }
    return super.getUnsupportedReason(input);
}
```

The session has to accept results from the sessions of other packagers. Identify box items by their global index, which is
the same in all sessions for the same input. Local indexes change during packing. This is how the sessions of the built-in
packagers do it (`AbstractBoxItemSession` in `core`, abridged):

```java
@Override
public Container accept(IntermediatePackagerResult result) {
    ...
    for (Placement stackPlacement : stack.getPlacements()) {
        // look the box item up by its global index
        BoxItem boxItem = findRemainingBoxItem(stackPlacement.getBoxItem().getGlobalIndex());
        boxItem.decrementResetCount();
        boxItem.reset();
    }
    ...
}

private BoxItem findRemainingBoxItem(int globalIndex) {
    for(BoxItem boxItem : remainingBoxItems) {
        if(boxItem.getGlobalIndex() == globalIndex) {
            return boxItem;
        }
    }
    throw new IllegalArgumentException("Result contains unknown box item global index " + globalIndex);
}
```

When you pack box item groups, groups are accepted in order: a result must hold a prefix of the remaining groups.

To let a costly packager skip work, compare results by load volume first and say so, as shown under
[Comparing results](#comparing-results). See also [Composite packager](docs/heuristics/composite.md) for budgets and how
the composite chooses between results.

## Flat search and accelerators

`FastBruteForcePackager` can delegate the search of all box orders to a `BruteForceAccelerator` (see
[accelerated brute force](docs/heuristics/accelerated-brute-force.md)). An accelerator, such as a GPU kernel, gets a
`FlatProblem` (the boxes of one container on primitive arrays) and returns the best box order as an `AcceleratorWinner`:

```java
public interface BruteForceAccelerator extends AutoCloseable {
    boolean supports(FlatProblem problem);
    Optional<AcceleratorWinner> search(FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;
}
```

The winner must be the one which the packager's own loop picks (under `FlatProblem.ANY_BEST`, set from `withTreeSearchAnyBest(true)`, the value of the winner, not the arrangement): the most load
volume, then load weight, then boxes; the
earlier permutation wins a tie, and the first permutation which places all boxes wins at once. The packager places the
winner itself, and searches the box orders itself when the accelerator returns nothing, so an accelerator may give up on
any problem, but must not return a worse or a different winner. Test it as `FlatEngineTest` and
`FlatAcceleratedPackagerTest` do: against a reference loop, and by comparing the placements with and without it.

Code which should run on a device is written in the style of the `flat` package, so that it can be compiled to a kernel:

 * `static` methods on `int[]` and `long[]` and offsets into them, with no objects, allocation, `System.arraycopy`, recursion,
   exceptions, assertions, strings, boxing, interfaces or virtual calls; copy with loops
 * small methods (a kernel compiler inlines them), and statistics in a counter array, not by logging
 * the same tie-breaking as the object version; the comparator which picks the best point is not a total order, so
   copy it as it is
 * the flat package has two searches of an item: `FlatSubtree` (the orders one at a time) and `FlatTree` (the default: the orders as one tree, with the bound table `FlatBound` computed once per
   problem on the host, and the free space bound `FlatSpace`, a port of `BruteForceTreeFreeSpace`); `FlatEngine.Builder.withTree(false)` selects the first
 * the kernel of the tornado module (`accelerator/tornado/src/main/resources/.../subtree.cl`) is a hand-written OpenCL C port of the flat package, function
   by function, with the same names and the same order of statements (`runTree` is the port of `FlatTree`, `run` of `FlatSubtree`): a change to `FlatPoints`, `FlatSearch`, `FlatPermutation`,
   `FlatSubtree`, `FlatTree`, `FlatBound` or `FlatSpace` is made
   in both (`isHopelessInSpace` of `FlatTree` and the `// FlatSpace` section of the kernel port `FlatSpace`), and `KernelParityTest` (with a device) shows that they still agree. It is written
   by hand because the TornadoVM JIT (7.3.1, OpenCL) compiles the
   search but miscompiles its control flow without an error (a kernel which never ends; see the README of the module). The flat Java therefore keeps a
   C-like shape: one exit per method, no `return` or `continue` in a loop, boolean predicates (`isAfterX(..)`) instead of three-way comparators, which is
   also what the port needs
 * scratch in one array per work item, with the sizes set up front (`FlatLayout`), and a flag, not an exception, when
   the scratch is too small, so that the packager can fall back to its own search
 * write the test with the code: compare each function with the object version on random input, as `FlatPointsAddTest` does
