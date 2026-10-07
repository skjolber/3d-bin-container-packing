package com.github.skjolber.packing.jmh;

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
 * Box item groups without a box item order: the brute-force packagers search the orders of the groups for each
 * container (see {@link GroupBruteForceBenchmarkState}).
 */
@Fork(1)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class GroupBruteForceBenchmark {

	@Benchmark
	public int bruteForcePackager(GroupBruteForceBenchmarkState state) {
		return pack(state.getBruteForcePackager(), state);
	}

	@Benchmark
	public int parallelBruteForcePackager(GroupBruteForceBenchmarkState state) {
		return pack(state.getParallelBruteForcePackager(), state);
	}

	static int pack(AbstractPackager<?> packager, GroupBruteForceBenchmarkState state) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(state.getContainerItems())
				.withMaxContainerCount(GroupBruteForceBenchmarkState.CONTAINERS)
				.withBoxItemGroups(state.getBoxItemGroups())
				.build();
		if(!result.isSuccess()) {
			throw new IllegalStateException("Expected the groups to fit");
		}
		return result.size();
	}
}
