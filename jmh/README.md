# jmh module
Module for performance-testing using the [JMH](https://openjdk.java.net/projects/code-tools/jmh/) framework.

## Introduction
Performance analysis is complicated due to the dynamic nature of the JVM Hotspot implementation. 

For example the JVM Hotspot is affected by the classes it loads and better optimize when there is only one implementation of an interface in use. 

In practice much of the dynamic nature can be handled by using so-called forks and warmups:

 * forks makes sure the individual benchmarks run alone
 * warmups makes sure optimizations by the JVM Hotspot are finished before the measurements are taken

## Getting started
Get familiar with JMH:

 * [baeldung](https://www.baeldung.com/java-microbenchmark-harness)
 * [jenkov](http://tutorials.jenkov.com/java-performance/jmh.html)
 
Download a profiler like [VisualVM](https://visualvm.github.io/) for drilling down to method level during development.

In this project, there is end-to-end tests in the which touches the most commonly used code paths.

Execute `PackagerBenchmark` using the command

```
mvn clean package && java -jar jmh/target/benchmark.jar PackagerBenchmark -rf json
```

and view the resulting `jmh-result.json` by dropping the file into a [visualizer](https://jmh.morethan.io).

Remember to disable CPU turbo / boost in BIOS, and close active programs competing for CPU resources.

### Bounding-box search

`BruteForceBoundingBoxSearchBenchmark` measures complete, fresh one-shot searches
in microseconds per operation:

| Method | Work |
| --- | --- |
| `constructSingleObjective` | Construction-only allocation baseline; no traversal |
| `singleObjectiveExhaustive` | Specialized single-objective minimum-volume search |
| `singleObjectiveViaMultiExhaustive` | The same objective through the multi-objective implementation |
| `volumeAndAxesExhaustive` | Independent volume, X, Y and Z minima in one traversal |
| `filledVolumeGoal` | Stop at a complete assembly whose bounding volume equals its box volume |

Parameters:

- `workload=identical`: repeated fixed 2 x 1 x 1 boxes.
- `workload=mixed`: equal counts of fixed 2 x 1 x 1 and 2 x 2 x 1 boxes.
- `workload=rotated`: repeated 2 x 1 x 1 boxes with all three distinct orientations.
- `boxCount=4,6`: physical boxes, not item types; overrides must be positive even counts of at least two.
- `load=false,true`: geometric search or load-aware search with maximum load weight 2,
  contact pressure 2, stack depth 2 and identical-only loading.

All inputs have a filled rectangular solution inside a boxCount x 3 x 3 container.
The load variants can reject layouts that geometry alone accepts; their difference
includes both validation overhead and the changed search, not just a single check.
Only the single-objective and single-objective-via-multi methods have identical objectives
and stopping rules. The four-objective and goal methods deliberately do different work.

Inputs and objectives are created at trial setup, separately for each worker thread.
Every timed search includes search-object construction, preparation, traversal and result
snapshots; it excludes public result-builder validation and input construction.
There is no per-invocation setup, deadline or search-state reuse. JMH consumes the returned
search/result. The construction baseline is a lower bound, not a value to subtract mechanically.
Defaults are one worker, two forks, three one-second warmups and five one-second measurements.
The full 60-case matrix takes roughly 16 minutes plus fork overhead, possibly longer for
larger count overrides because individual exhaustive operations are not time-bounded.

Build using the repository's Maven wrapper:

```sh
./mvnw -B -ntp -Pdev -pl jmh -am package -DskipTests
```

For example, compare the single-objective implementations on one input:

```sh
java -jar jmh/target/benchmark.jar '.*BruteForceBoundingBoxSearchBenchmark.singleObjective.*' \
  -p workload=rotated -p boxCount=6 -p load=false -prof gc -rf json -rff /tmp/bounding-box-jmh.json
```

Functional tests exercise every default parameter combination through the search factories,
without invoking JMH benchmark methods. They check exhaustion versus goal termination,
equivalent objective winners, inventory preservation, non-overlap, bounds and physical load validity.
These tests have a safety deadline; the benchmarks do not.

### False sharing
Some resources might be subject to so-called [false sharing](https://www.baeldung.com/java-false-sharing-contended). Run test using the `perf` tools, for example using the command

```
mvn clean install && perf stat -d  java -jar jmh/target/benchmark.jar ParallelIteratorBenchmark -rf json
```

### Performance analysis
Analyze the source code, tweak the end-to-end and noop benchmarks and/or use a profiler to identify hotspots. 

Benchmarks can be executed as standalone programs (using `main(..)` method) directly from your IDE. This is less accurate than running from the command line, but convenient during active development, especially for drilling down using a profiler like [VisualVM](https://visualvm.github.io/) or such. 

## Writing a benchmark
Once a potential hotspot is identified, capture the initial state by writing a baseline benchmark. If missing, add unit tests, so you're sure to be comparing apples to apples. Also add a (as close as possible) no-operation / pass-through benchmark to sanity-check the upper limit on your results. Please note that this will need to be submitted in its own PR.

Then add alternative implementations and their corresponding benchmarks. The benchmarks you want to compare go into the same class file (so that the visualizer presents them together). 

