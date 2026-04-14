# Experiment Plan

## Experiment ID
exp-001-points-throughput

## Optimization Goal
Improve throughput (ops/sec) of both the 2D and 3D point calculator implementations in the `points` module.

## Scope
- `points/src/main/java/**/*.java`
- Excludes: `experiments/exp-001-points-throughput/tests/` and `experiments/exp-001-points-throughput/bench/`

## Metric
- Name: throughput (ops/sec) for PointsBenchmark2D and PointsBenchmark3D
- Direction: higher is better

## Success Criteria (BINDING — the loop does not start without these numbers)
- Target: 20% improvement in throughput over baseline (for combined 2D+3D average)
- Iteration threshold: minimum 0.5% improvement to keep a change

## Regression Test Command
`mvn test -pl points -q 2>&1 | tail -5`

## Benchmark Test Command
`mvn clean package -pl jmh -am -q 2>&1 | tail -1 && java -jar jmh/target/benchmark.jar "PointsBenchmark" -f 1 -wi 1 -w 10 -i 1 -r 30 -rf json -rff experiments/exp-001-points-throughput/bench/latest-result.json 2>&1 | grep -E "Benchmark|ops" | tail -20`

## Iterations
10 iterations (or until target is reached)

## Strategies to Explore
- (populated during Phase 2 loop)

## Created
2026-04-14
