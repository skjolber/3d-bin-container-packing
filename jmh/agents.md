# JMH Benchmarks Module

## Purpose
Microbenchmark suite using the OpenJDK JMH framework. Measures throughput and latency of packagers, iterators, and point calculators to detect regressions and guide performance optimisation.

## Key Packages
- `com.github.skjolber.packing.jmh` — Packager benchmarks (`PackagerBenchmark`, `DeadlineBenchmark`, `EgyPackagerBenchmark`, `TychoBenchmark`) and shared state classes
- `com.github.skjolber.packing.jmh.iterator` — Iterator benchmarks (`DefaultIteratorBenchmark`, `ParallelIteratorBenchmark`)
- `com.github.skjolber.packing.jmh.ep` — Enhanced-point calculator benchmarks

## Architecture Notes
- `@State(Scope.Benchmark)` classes set up packagers and test data once per benchmark run.
- `BouwkampConverter` bridges test-module Bouwkamp codes into JMH benchmark inputs.
- The Maven Shade plugin produces a fat JAR (`benchmarks.jar`) for running benchmarks in isolation.
- JMH requires that benchmark methods are **not** inlined by the JIT — annotate with `@Benchmark`, never call them directly.
- Output JSON results can be visualised at https://jmh.morethan.io.

## Running Benchmarks
```bash
# Build the fat JAR
mvn package -pl jmh -am -DskipTests

# Run all benchmarks
java -jar jmh/target/benchmarks.jar

# Run a specific benchmark with custom settings
java -jar jmh/target/benchmarks.jar PackagerBenchmark -f 1 -wi 3 -i 5 -rf json -rff results.json
```

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | core, points |
| test    | test module, commons-io |
| provided | jmh-core, jmh-generator-annprocess |

## Build
```
mvn package -pl jmh -am -DskipTests
```
