# Visualizer API Module

## Purpose
JSON data types for packing results, written by `visualizer/packaging` and read by the viewer (`visualizer/viewer`).

## Key Packages
- `com.github.skjolber.packing.visualizer.api.packaging`:
  - `PackagingResultVisualizer`: the root object (`toJson()`), a list of containers
  - `ContainerVisualizer`, `StackVisualizer`, `StackPlacementVisualizer`, `BoxVisualizer`: a container, its stack, the placements and their boxes
  - `PointVisualizer`: a free point (extreme point) after a placement
- `com.github.skjolber.packing.visualizer.api.VisualizerPlugin`: extension point for extra data

## Architecture Notes
- Plain Jackson beans; the field names are the JSON format.
- The viewer parses the JSON in `visualizer/viewer/src/model.ts` (`parsePackaging`). When changing the format, update the parser and regenerate the shared sample
  (see `visualizer/packaging/agents.md`). Backwards compatibility is not needed: the viewer runs locally, from the same checkout.

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | core |
| compile | jackson-databind |
