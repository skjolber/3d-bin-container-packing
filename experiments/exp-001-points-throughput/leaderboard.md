# Experiment Leaderboard: exp-001-points-throughput
Last updated: 2026-04-15

| Rank | Iteration | 2D (ops/s) | 3D (ops/s) | Combined | Delta | Strategy | Status |
|------|-----------|------------|------------|---------|-------|----------|--------|
| 1 | iter-009 | 2487 | 2846 | 2666.4 | +2.67% | hoist-volume-area-size-in-isEclipsed | ✅ Kept |
| 2 | iter-007 | 2458 | 2848 | 2653 | +2.15% | merge-removeflagged-calls | ✅ Kept |
| 3 | iter-006 | 2431 | 2827 | 2629 | +1.22% | remove-redundant-isflag-in-removeeclipsed | ✅ Kept |
| 4 | iter-003 | 2427 | 2799 | 2613 | +0.61% | isEclipsed-early-break-minX | ✅ Kept |
| 5 | baseline | 2362 | 2832 | 2597 | — | — | Baseline |
| — | iter-008 | 2458 | 2755 | 2606.5 | +0.37% | insertion-sort-small-lists | ❌ Dropped |
| — | iter-001 | 2385 | 2805 | 2595 | -0.07% | remove-updateindexes-conditional-plus-binary-search | ❌ Dropped |
| — | iter-002 | 2319 | 2808 | 2564 | -1.27% | merge-sorted-segments-2d | ❌ Dropped |
| — | iter-005 | 2407 | 2781 | 2594 | -0.12% | isEclipsedAtXX-skip-only | ❌ Dropped |
| — | iter-004 | 2387 | 2734 | 2560 | -1.42% | eclipse-check-inline-and-isEclipsedAtXX-skip | ❌ Dropped |
| — | iter-010 | 2336 | 2780 | 2558 | -1.50% | pre-compact-before-removeeclipsed | ❌ Dropped |
