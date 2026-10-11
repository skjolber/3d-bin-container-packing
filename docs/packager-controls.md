# Packager controls

The packagers (excluding brute force) can be extended to handle specialized needs via various `control` (plugins) types. 

In a nutshell, the `controls` are stateful objects which are handed various resources from the packagers during construction, and then notified and/or invoked at certain milestones within the packaging process.

The control and strategy interfaces are part of the `api` module. The classes of `core` can be extended too, but are not a stable contract: they may change in any release (see [DEVELOPER.md](../DEVELOPER.md)).

`Controls` must be provided as follows:

 * builder factory
    * builder
       * controls

## Wiring the controls

Manifest controls and point controls belong to a container item, so container types can have different rules. Set the
factories with the container item builder of the result builder:

```java
Container container = ...;
ManifestControlsBuilderFactory manifestControls = ...; // your own implementation
PointControlsBuilderFactory pointControls = ...;       // your own implementation

PackagerResult result = packager
    .newResultBuilder()
    .withContainerItem(b -> b
        .withContainerItem(container, 5)
        .withManifestControlsBuilderFactory(manifestControls)
        .withPointControlsBuilderFactory(pointControls))
    .withBoxItems(products)
    .withMaxContainerCount(5)
    .build();
```

or on a `ContainerItem`, before passing it to `withContainerItems(..)`:

```java
ContainerItem containerItem = new ContainerItem(container, 5);
containerItem.setManifestControlsBuilderFactory(manifestControls);
containerItem.setPointControlsBuilderFactory(pointControls);

PackagerResult result = packager
    .newResultBuilder()
    .withContainerItems(containerItem)
    .withBoxItems(products)
    .withMaxContainerCount(5)
    .build();
```

Placement controls are set on the packager builder instead, with `withPlacementControlsBuilderFactory(..)`. A packager reuses the result
of one container for another only if their manifest controls and point controls factories are the same instance or carry the same id:
implement `getId()` on them, or wrap a lambda with `PointControlsBuilderFactory.of("id", () -> ...)` (see [DEVELOPER.md](../DEVELOPER.md)).

## Manifest-controls
Determines which boxes go into which containers, i.e. in which combinations. 

A classic example would to be to not package both lighters and dynamite in the same container.

## Point-controls
Determines which points are relevant for a specific box. 

For example, heavy items might be require only points at ground level or flammable items might be required to be stacked in a certain zone.

## Placement-controls
Determines the best placement for a box. 

Can consider a range of options, like stability, stacking height, structural integrity and so on; even randomization is possible. Note that these features are not necessarily implemented in the packagers within this project.

## See also

 * [DEVELOPER.md](../DEVELOPER.md#writing-your-own-placement-controls): writing your own controls, and the factory id contract.
 * [FEATURES.md](../FEATURES.md#constraints-and-controls): which packagers support controls.
