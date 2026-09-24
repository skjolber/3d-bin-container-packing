package com.github.skjolber.packing.jmh.constraint;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.packing.api.PackagerResult;

/**
 * Benchmarks for the {@code maxLoadBoxCount} box constraint.
 * <p>
 * Packs 20 boxes (4 columns × 5 layers of 10×10×1) into a single 20×20×5
 * container.  Each box declares {@code maxLoadBoxCount=4}; each 5-layer column
 * reaches depth=4 exactly (at the limit).  The count check fires on every
 * placement above a constrained box, measuring the overhead of the depth
 * traversal without triggering rejection.
 *
 * <pre>
 *   z
 *   5 +----------+  depth=4  ✓  (at limit — 4 boxes above the base)
 *   4 +----------+  depth=3  ✓
 *   3 +----------+  depth=2  ✓
 *   2 +----------+  depth=1  ✓
 *   1 +----------+  maxLoadBoxCount=4
 *   0
 *       0        10 x  (×4 columns in 20×20 container)
 * </pre>
 */
@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class BoxCountConstraintBenchmark {

	/**
	 * Packs the box-count-constraint scenario with PlainPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packPlain(BoxCountConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	/**
	 * Packs the box-count-constraint scenario with LargestAreaFitFirstPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packLaff(BoxCountConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	@Benchmark
	public int plainPackager(BoxCountConstraintBenchmarkState state) {
		return packPlain(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager(BoxCountConstraintBenchmarkState state) {
		return packLaff(state).getContainers().size();
	}

	/**
	 * Packs the mixed box-count-constraint scenario (two box types, different
	 * {@code maxLoadBoxCount} limits) with PlainPackager.
	 */
	public PackagerResult packPlainMixed(BoxCountConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	/**
	 * Packs the mixed box-count-constraint scenario with LargestAreaFitFirstPackager.
	 */
	public PackagerResult packLaffMixed(BoxCountConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	@Benchmark
	public int plainPackager_mixed(BoxCountConstraintBenchmarkState state) {
		return packPlainMixed(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager_mixed(BoxCountConstraintBenchmarkState state) {
		return packLaffMixed(state).getContainers().size();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BoxCountConstraintBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)
				.build();
		new Runner(opt).run();
	}
}
