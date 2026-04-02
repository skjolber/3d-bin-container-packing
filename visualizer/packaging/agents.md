# Visualizer Packaging Module

## Purpose
Converts completed packing results into the JSON format consumed by the Three.js viewer. Bridges the core packing domain with the visualizer API.

## Key Packages
- `com.github.skjolber.packing.visualizer.packaging`
  - `PackagingResultVisualizerFactory` — factory interface; use to produce a `PackagingResultVisualizer` from a `PackagerResult`
  - `DefaultPackagingResultVisualizerFactory` — standard implementation
  - `AbstractPackagingResultVisualizerFactory` — base class for custom factories

## Architecture Notes
- Depends on **core**, **api**, **points**, and **visualizer-api**.
- The factory converts `PackagerResult` → `PackagingResultVisualizer` → JSON (via Jackson in `visualizer-api`).
- To customise output (e.g., add colour coding), extend `AbstractPackagingResultVisualizerFactory` and attach `VisualizerPlugin` instances.
- Keep this module free of Spring/framework dependencies so it can be used in standalone and server contexts.

## Typical Usage
```java
PackagingResultVisualizerFactory factory = new DefaultPackagingResultVisualizerFactory();
PackagingResultVisualizer visualizer = factory.visualizer(packagerResult);
String json = objectMapper.writeValueAsString(visualizer);
```

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | api, core, points |
| compile | visualizer-api |
| compile | jackson-databind |
| test    | test module, junit-jupiter |

## Build
```bash
mvn test -pl visualizer/packaging -am
```
