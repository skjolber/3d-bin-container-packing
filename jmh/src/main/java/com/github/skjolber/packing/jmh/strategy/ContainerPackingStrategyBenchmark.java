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

/**
 * Compares serial and fork-per-container-item parallel candidate packing with the cost-aware strategy and the allocation
 * strategies, which all choose among the same container items. The cost-aware strategy and the lowest-cost allocation strategy
 * need a cost calculator on every container item, so they get priced copies of the container items.
 */
@Fork(1)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class ContainerPackingStrategyBenchmark {

	@Benchmark
	public int plainRotatedOrdered(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getOrderedPackager(), state, state.getRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int plainRotatedParallel(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getParallelPackager(), state, state.getRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceOrdered(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getOrderedBruteForcePackager(), state, state.getContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceParallel(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getParallelBruteForcePackager(), state, state.getContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	@Benchmark
	public int plainRotatedLowestCost(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getLowestCostPackager(), state, state.getPricedRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceLowestCost(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getLowestCostBruteForcePackager(), state, state.getPricedContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	@Benchmark
	public int plainRotatedFewestContainersFit(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getFewestContainersFitPackager(), state, state.getRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceFewestContainersFit(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getFewestContainersFitBruteForcePackager(), state, state.getContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	@Benchmark
	public int plainRotatedLowestCostFit(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getLowestCostFitPackager(), state, state.getPricedRotatedContainerItems(), state.getBoxItems(), 1);
	}

	@Benchmark
	public int bruteForceLowestCostFit(ContainerPackingStrategyBenchmarkState state) {
		return pack(state.getLowestCostFitBruteForcePackager(), state, state.getPricedContainerItems(), state.getBruteForceBoxItems(), 1);
	}

	private static int pack(AbstractPackager<?> packager, ContainerPackingStrategyBenchmarkState state,
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
