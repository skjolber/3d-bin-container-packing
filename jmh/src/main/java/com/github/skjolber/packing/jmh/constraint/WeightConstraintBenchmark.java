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
 * Benchmarks for the {@code maxLoadWeight} box constraint.
 * <p>
 * Packs 20 boxes (4 columns × 5 layers of 10×10×1) into a single 20×20×5
 * container.  Each box declares {@code maxLoadWeight=100}; the maximum
 * accumulated weight on any base box (4×5=20) is well within the limit so
 * all boxes fit.  The weight check fires on every placement, making this a
 * focused measure of constraint-evaluation overhead in the hot path.
 *
 * <pre>
 *   z
 *   5 +----------+  w=5  cumulative load on base = 20 ≤ maxLoadWeight=100  ✓
 *   4 +----------+  w=5
 *   3 +----------+  w=5
 *   2 +----------+  w=5
 *   1 +----------+  w=5  maxLoadWeight=100
 *   0
 *       0        10 x  (×4 columns in 20×20 container)
 * </pre>
 */
@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class WeightConstraintBenchmark {

	/**
	 * Packs the weight-constraint scenario with PlainPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packPlain(WeightConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	/**
	 * Packs the weight-constraint scenario with LargestAreaFitFirstPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packLaff(WeightConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	@Benchmark
	public int plainPackager(WeightConstraintBenchmarkState state) {
		return packPlain(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager(WeightConstraintBenchmarkState state) {
		return packLaff(state).getContainers().size();
	}

	/**
	 * Packs the mixed weight-constraint scenario (two box types, different
	 * {@code maxLoadWeight} limits) with PlainPackager.
	 */
	public PackagerResult packPlainMixed(WeightConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	/**
	 * Packs the mixed weight-constraint scenario with LargestAreaFitFirstPackager.
	 */
	public PackagerResult packLaffMixed(WeightConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	@Benchmark
	public int plainPackager_mixed(WeightConstraintBenchmarkState state) {
		return packPlainMixed(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager_mixed(WeightConstraintBenchmarkState state) {
		return packLaffMixed(state).getContainers().size();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(WeightConstraintBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)
				.build();
		new Runner(opt).run();
	}
}

