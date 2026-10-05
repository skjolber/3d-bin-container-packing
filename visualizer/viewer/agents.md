# Visualizer Viewer

## Purpose
Interactive 3D front-end for visualising packing results. Renders packed containers and boxes in a Three.js scene, supports step-through navigation of results, and can display free placement points.

## Technology Stack
| Category | Technology |
|----------|-----------|
| Framework | React 18 |
| 3D Graphics | Three.js 0.180 |
| Language | TypeScript 4.9 + JavaScript |
| Build tooling | Create React App (react-scripts 5) |
| GUI controls | dat.GUI 0.7 |
| Performance HUD | Stats.js |

## Key Source Files
- `src/index.js`: React entry point
- `src/model.ts`: data model, `parsePackagings(json)` (all results) and `parsePackaging(json)` (one result), without three.js imports so that it can be unit tested
- `src/api.ts`: three.js rendering of containers, boxes and points, and colour schemes
- `src/ThreeScene.js`: scene, camera, controls, loading (polls `/assets/containers.json`) and the info panels
- `src/SupportingPlacementsView.js`: hover popup with the supporting boxes and loads
- `src/utils.ts`: load calculations
- `src/colorModes.ts`: colour modes (box item, group, support, load), without three.js imports
- `src/ResultSummaryView.js`: result summary, validation and colour mode panel
- `src/setupProxy.js`: serves `public/assets/containers.json` without caching

## Input Data Format
The JSON written by `visualizer/packaging` (`DefaultPackagingResultVisualizerFactory`). `src/fixtures/containers.json` is a sample
written by the Java tests; `src/model.test.ts` parses it. Backwards compatibility is not needed: the viewer runs locally, from the same checkout.

## Keyboard Controls
| Key | Action |
|-----|--------|
| A / D | Previous / next packaging step |
| W / S | Previous / next point step |
| P | Toggle free placement points |
| C | Next colour mode: box item, group, support, load (see `src/colorModes.ts`) |
| R | Next result, when the file holds several (or click a row in the comparison table) |
| 1 / 2 | Rotate XY plane |
| Mouse wheel | Zoom |
| Left-drag | Rotate view |
| Right-drag | Pan view |

## Development
```bash
cd visualizer/viewer
npm install
npm start        # dev server at http://localhost:3000
```

## Production Build
```bash
npm test -- --watchAll=false   # unit tests (jest)
npm run build    # output in build/
```

## Notes
- This directory is **not** a Maven module; it is built independently with npm.
- No Java code lives here — all Java integration is via the JSON HTTP response from the server.
