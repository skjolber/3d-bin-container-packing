# Visualizer Packaging Module

## Purpose
Converts packed containers into the JSON format of `visualizer/api`, for the viewer (`visualizer/viewer`).

## Key Packages
- `com.github.skjolber.packing.visualizer.packaging`
  - `PackagingResultVisualizerFactory`: factory interface
  - `AbstractPackagingResultVisualizerFactory`: writes the JSON to a stream or file
  - `DefaultPackagingResultVisualizerFactory`: converts a `PackagerResult` (or `Container`s), optionally with the free points after each placement

## Validation
Results are validated, and invalid results are still visualized: the reasons are logged, the result is marked `valid: false`, and each
placement lists the reasons which concern it (the viewer outlines those boxes in red).
- The boxes' load limits are always validated. The load validators walk the support graph, which only packagers with load limits
  record, so the factory validates copies of the placements linked from the geometry (`createSupportGraph`).
- `visualize(result, validator.newResultBuilder().withContainerItems(..).withBoxItems(..).withMaxContainerCount(..))` also validates
  the result against the input (box and container counts, intersections, ...).

## Typical Usage
```java
DefaultPackagingResultVisualizerFactory factory = new DefaultPackagingResultVisualizerFactory(true); // true: calculate points
factory.visualize(result, new File("../viewer/public/assets/containers.json"));
```

## Tests
- `*FactoryTest` classes run in the build. `DefaultPackagingResultVisualizerFactoryTest` compares the JSON for a small sample with
  `visualizer/viewer/src/fixtures/containers.json`, which the viewer's tests parse too. After changing the format, regenerate it with
  `./mvnw -B -ntp -Pdev -pl visualizer/packaging -am -Dtest=DefaultPackagingResultVisualizerFactoryTest -Dsurefire.failIfNoSpecifiedTests=false -Dvisualizer.updateSample=true test`.
- The other tests are scenarios which write `visualizer/viewer/public/assets/containers.json` for viewing. They are excluded from the build;
  run them by hand (IDE, or `-Dtest=...`).

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api, core, points |
| compile | visualizer-api |
| compile | jackson-databind |
| test    | test module, junit-jupiter |
