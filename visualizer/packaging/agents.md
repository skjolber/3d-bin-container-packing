# Visualizer Packaging Module

## Purpose
Converts packed containers into the JSON format of `visualizer/api`, for the viewer (`visualizer/viewer`).

## Key Packages
- `com.github.skjolber.packing.visualizer.packaging`
  - `PackagingResultVisualizerFactory`: factory interface
  - `AbstractPackagingResultVisualizerFactory`: writes the JSON to a stream or file
  - `DefaultPackagingResultVisualizerFactory`: converts `Container`s (with their stacks), optionally with the free points after each placement

## Typical Usage
```java
DefaultPackagingResultVisualizerFactory factory = new DefaultPackagingResultVisualizerFactory(true); // true: calculate points
factory.visualize(result.getContainers(), new File("../viewer/public/assets/containers.json"));
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
