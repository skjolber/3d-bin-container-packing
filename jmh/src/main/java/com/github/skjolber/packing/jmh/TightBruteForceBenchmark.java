package com.github.skjolber.packing.jmh;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * The brute force packagers on tight random problems (see {@link TightBruteForceBenchmarkState}): one operation packs all the instances.
 */
@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class TightBruteForceBenchmark {

	@Benchmark
	public int packager(TightBruteForceBenchmarkState state) {
		return process(state.getBruteForceSets());
	}

	@Benchmark
	public int fastPackager(TightBruteForceBenchmarkState state) {
		return process(state.getFastSets());
	}

	@Benchmark
	public int parallelPackager(TightBruteForceBenchmarkState state) {
		return process(state.getParallelSets());
	}

	private static int process(List<BenchmarkSet> sets) {
		int boxes = 0;
		for (BenchmarkSet set : sets) {
			AbstractPackager<?> packager = set.getPackager();
			PackagerResult result = packager.newResultBuilder().withContainerItems(set.getContainers()).withMaxContainerCount(8).withBoxItems(set.getProducts()).withInterruptDuration(60_000)
					.build();
			boxes += result.getContainers().size();
		}
		return boxes;
	}
}
