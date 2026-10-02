package com.github.skjolber.packing.jmh.strategy;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/** Compares serial and fork-per-container-item parallel candidate packing. */
@Fork(1)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class ContainerStrategyBenchmark {

	@Benchmark
	public int plainRotatedOrdered(ContainerStrategyBenchmarkState state) {
		return pack(state.getOrderedPackager(), state, state.getRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int plainRotatedParallel(ContainerStrategyBenchmarkState state) {
		return pack(state.getParallelPackager(), state, state.getRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceOrdered(ContainerStrategyBenchmarkState state) {
		return pack(state.getOrderedBruteForcePackager(), state, state.getContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceParallel(ContainerStrategyBenchmarkState state) {
		return pack(state.getParallelBruteForcePackager(), state, state.getContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	private static int pack(AbstractPackager<?> packager, ContainerStrategyBenchmarkState state,
			java.util.List<com.github.skjolber.packing.api.ContainerItem> containerItems,
			java.util.List<com.github.skjolber.packing.api.BoxItem> boxItems, int maxContainerCount) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(containerItems)
				.withMaxContainerCount(maxContainerCount)
				.withBoxItems(boxItems)
				.build();
		if(!result.isSuccess()) {
			throw new IllegalStateException("Bouwkamp input must fit in the benchmark container");
		}
		return result.getContainers().size();
	}
}
