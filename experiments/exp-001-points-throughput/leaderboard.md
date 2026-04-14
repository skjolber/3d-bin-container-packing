# Experiment Leaderboard: exp-001-points-throughput
Last updated: 2026-04-15

| Rank | Iteration | 2D (ops/s) | 3D (ops/s) | Combined | Delta | Strategy | Status |
|------|-----------|------------|------------|---------|-------|----------|--------|
| 1 | iter-003 | 2427 | 2799 | 2613 | +0.61% | isEclipsed-early-break-minX | ✅ Kept |
| 2 | baseline | 2362 | 2832 | 2597 | — | — | Baseline |
| — | iter-001 | 2385 | 2805 | 2595 | -0.07% | remove-updateindexes-conditional-plus-binary-search | ❌ Dropped |
| — | iter-002 | 2319 | 2808 | 2564 | -1.27% | merge-sorted-segments-2d | ❌ Dropped |
| — | iter-004 | 2387 | 2734 | 2560 | -1.42% | eclipse-check-inline-and-isEclipsedAtXX-skip | ❌ Dropped |
