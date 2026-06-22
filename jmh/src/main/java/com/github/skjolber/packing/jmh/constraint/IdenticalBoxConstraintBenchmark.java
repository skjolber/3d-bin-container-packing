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
 * Benchmarks for the {@code maxLoadIdenticalBoxCount} box constraint.
 * <p>
 * Packs 20 identical boxes (4 columns × 5 layers of 10×10×1) into a single
 * 20×20×5 container.  Each box declares {@code maxLoadIdenticalBoxCount=4};
 * each column's identical chain reaches depth=4 exactly (at the limit).  The
 * type-identity and depth checks fire on every stacking attempt, measuring
 * their overhead without triggering rejection.
 *
 * <pre>
 *   z
 *   5 +----------+  A5 — 4th identical above A1  depth=4 ≤ 4  ✓  (at limit)
 *   4 +----------+  A4 — depth=3  ✓
 *   3 +----------+  A3 — depth=2  ✓
 *   2 +----------+  A2 — depth=1  ✓
 *   1 +----------+  A1  maxLoadIdenticalBoxCount=4
 *   0
 *       0        10 x  (×4 columns in 20×20 container)
 * </pre>
 */
@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class IdenticalBoxConstraintBenchmark {

	/**
	 * Packs the identical-box-constraint scenario with PlainPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packPlain(IdenticalBoxConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	/**
	 * Packs the identical-box-constraint scenario with LargestAreaFitFirstPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packLaff(IdenticalBoxConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	@Benchmark
	public int plainPackager(IdenticalBoxConstraintBenchmarkState state) {
		return packPlain(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager(IdenticalBoxConstraintBenchmarkState state) {
		return packLaff(state).getContainers().size();
	}

	/**
	 * Packs the mixed identical-box-constraint scenario (type A with tight limit,
	 * type B with generous limit) with PlainPackager.
	 */
	public PackagerResult packPlainMixed(IdenticalBoxConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	/**
	 * Packs the mixed identical-box-constraint scenario with LargestAreaFitFirstPackager.
	 */
	public PackagerResult packLaffMixed(IdenticalBoxConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	@Benchmark
	public int plainPackager_mixed(IdenticalBoxConstraintBenchmarkState state) {
		return packPlainMixed(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager_mixed(IdenticalBoxConstraintBenchmarkState state) {
		return packLaffMixed(state).getContainers().size();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(IdenticalBoxConstraintBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)
				.build();
		new Runner(opt).run();
	}
}
