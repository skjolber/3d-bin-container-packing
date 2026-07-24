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
 * Benchmarks for the {@code maxLoadPressure} box constraint.
 * <p>
 * Packs 20 boxes (4 columns × 5 layers of 10×10×1) into a single 20×20×5
 * container.  Each box declares {@code maxLoadPressure=1} on a 10×10 base
 * (area=100).  Each placement adds weight 2, well within the limit of
 * 1×100=100.  The pressure arithmetic fires on every placement, making this
 * a focused measure of the floating-point check overhead.
 *
 * <pre>
 *   z
 *   5 +----------+  w=2  pressure check: 2 ≤ maxLoadPressure×area = 1×100 = 100  ✓
 *   4 +----------+  w=2
 *   3 +----------+  w=2
 *   2 +----------+  w=2
 *   1 +----------+  w=2  10×10 base (area=100), maxLoadPressure=1
 *   0
 *       0        10 x  (×4 columns in 20×20 container)
 * </pre>
 */
@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class PressureConstraintBenchmark {

	/**
	 * Packs the pressure-constraint scenario with PlainPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packPlain(PressureConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	/**
	 * Packs the pressure-constraint scenario with LargestAreaFitFirstPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packLaff(PressureConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	@Benchmark
	public int plainPackager(PressureConstraintBenchmarkState state) {
		return packPlain(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager(PressureConstraintBenchmarkState state) {
		return packLaff(state).getContainers().size();
	}

	/**
	 * Packs the mixed pressure-constraint scenario (two box types, different
	 * {@code maxLoadPressure} limits) with PlainPackager.
	 */
	public PackagerResult packPlainMixed(PressureConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	/**
	 * Packs the mixed pressure-constraint scenario with LargestAreaFitFirstPackager.
	 */
	public PackagerResult packLaffMixed(PressureConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	@Benchmark
	public int plainPackager_mixed(PressureConstraintBenchmarkState state) {
		return packPlainMixed(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager_mixed(PressureConstraintBenchmarkState state) {
		return packLaffMixed(state).getContainers().size();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(PressureConstraintBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)
				.build();
		new Runner(opt).run();
	}
}

