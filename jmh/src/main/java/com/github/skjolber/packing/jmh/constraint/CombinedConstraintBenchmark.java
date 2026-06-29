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
 * Benchmarks with all four box constraints active simultaneously:
 * {@code maxLoadWeight}, {@code maxLoadPressure}, and {@code maxLoadBoxCount}.
 * <p>
 * Packs 20 boxes (4 columns × 5 layers of 10×10×1) into a single 20×20×5
 * container.  All limits are generous so every box fits; all three checks fire
 * on every placement, making this the highest per-candidate evaluation cost of
 * all constraint benchmarks.
 *
 * <pre>
 *   z
 *   5 +----------+  w=2  weight ok (8≤100) · pressure ok (2≤100) · count ok (4≤4)
 *   4 +----------+  w=2
 *   3 +----------+  w=2
 *   2 +----------+  w=2
 *   1 +----------+  w=2  maxLoadWeight=100, maxLoadPressure=1, maxLoadBoxCount=4
 *   0
 *       0        10 x  (×4 columns in 20×20 container, 10×10 base area=100)
 * </pre>
 */
@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class CombinedConstraintBenchmark {

	/**
	 * Packs the combined-constraint scenario with PlainPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packPlain(CombinedConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	/**
	 * Packs the combined-constraint scenario with LargestAreaFitFirstPackager.
	 * Returns the full result so tests can inspect and visualize it.
	 */
	public PackagerResult packLaff(CombinedConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
	}

	@Benchmark
	public int plainPackager(CombinedConstraintBenchmarkState state) {
		return packPlain(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager(CombinedConstraintBenchmarkState state) {
		return packLaff(state).getContainers().size();
	}

	/**
	 * Packs the mixed combined-constraint scenario (tight and generous budgets
	 * for all four constraints simultaneously) with PlainPackager.
	 */
	public PackagerResult packPlainMixed(CombinedConstraintBenchmarkState state) {
		return state.getPlainPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	/**
	 * Packs the mixed combined-constraint scenario with LargestAreaFitFirstPackager.
	 */
	public PackagerResult packLaffMixed(CombinedConstraintBenchmarkState state) {
		return state.getLaffPackager()
				.newResultBuilder()
				.withContainerItems(state.getContainers2())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems2())
				.build();
	}

	@Benchmark
	public int plainPackager_mixed(CombinedConstraintBenchmarkState state) {
		return packPlainMixed(state).getContainers().size();
	}

	@Benchmark
	public int laffPackager_mixed(CombinedConstraintBenchmarkState state) {
		return packLaffMixed(state).getContainers().size();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(CombinedConstraintBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)
				.build();
		new Runner(opt).run();
	}
}
