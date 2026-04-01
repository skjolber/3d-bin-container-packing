# Visualizer Algorithm Module

## Purpose
Captures intermediate algorithm states during packing for step-by-step visualisation. Records per-iteration decisions, metric snapshots, and filter operations so the viewer can replay the algorithm's internal reasoning.

> **Status**: Work in progress — not yet released as a stable API.

## Key Packages
- `com.github.skjolber.packing.visualizer.api.packager` — Algorithm event types:
  - `AlgorithmListener` — callback interface; implement to intercept algorithm events
  - `PackagerAlgorithm`, `PackagerIteration`, `PackagerOperation` — event/state hierarchy
  - `ContainerWorkspace` — snapshot of the container state at a given step
  - `MetricVisualization` — numeric metric capture for charting
  - `BoxFilter`, `ContainerFilter` — filter operation records

## Architecture Notes
- Depends on **core** (hooks into packager internals).
- The `AlgorithmListener` interface is the primary extension point; wire it into a packager builder to receive events.
- Because this module is unstable, avoid depending on it from production code in other modules.

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | core |
| compile | jackson-databind |
| test    | junit-jupiter, assertj-core |

## Build
```bash
mvn test -pl visualizer/algorithm -am
```
