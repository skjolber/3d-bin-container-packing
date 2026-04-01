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
- `src/index.js` — React entry point
- `src/ThreeScene.js` — Three.js scene setup, camera, lighting, box/container mesh creation
- `src/api.ts` — Typed API client for fetching packing JSON from the Java back-end
- `src/utils.ts` — Geometry and colour utilities

## Input Data Format
Expects the JSON produced by `visualizer/packaging` (`DefaultPackagingResultVisualizerFactory`). Changes to that module's JSON schema are breaking changes here.

## Keyboard Controls
| Key | Action |
|-----|--------|
| A / D | Previous / next packaging step |
| W / S | Previous / next point step |
| P | Toggle free placement points |
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
npm run build    # output in build/
```

## Notes
- This directory is **not** a Maven module; it is built independently with npm.
- No Java code lives here — all Java integration is via the JSON HTTP response from the server.
