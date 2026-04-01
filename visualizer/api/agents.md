# Visualizer API Module

## Purpose
Defines the JSON serialisation interfaces and data structures used to represent packing results for the Three.js viewer. Acts as the contract between the Java back-end and the front-end visualizer.

## Key Packages
- `com.github.skjolber.packing.visualizer.api.packaging` — Core visualizer types:
  - `AbstractVisualizer`, `PackagingResultVisualizer` — root visualizer interfaces
  - `BoxVisualizer`, `ContainerVisualizer`, `StackVisualizer`, `StackPlacementVisualizer` — per-object serialisation wrappers
  - `PointVisualizer` — free-point visualisation
  - `VisualizerPlugin` — extension point for custom rendering data

## Architecture Notes
- Depends on **core** (consumes `PackagerResult`, `StackPlacement`, etc.).
- Jackson annotations drive JSON output format; the viewer (`visualizer/viewer`) parses this JSON — any field renaming is a breaking change to the front-end.
- `VisualizerPlugin` follows the plugin pattern: implement and register to attach extra data to the JSON output.

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | core |
| compile | jackson-databind |

## Build
```bash
mvn test -pl visualizer/api -am
```
