package com.github.skjolber.packing.jmh.shipping;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * Shipping order packing as in GitHub issue #1158: the order is packed by brute force, LAFF and
 * plain packagers, and the result with the fewest containers (then the smallest total volume) is
 * used.
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class ShippingOrderBenchmark {

	private static final int MAX_CONTAINERS = 5;

	@Benchmark
	public PackagerResult bruteForcePackager(ShippingOrderBenchmarkState state) {
		return pack(state.getBruteForcePackager(), state.getContainers(), state.getOrder());
	}

	@Benchmark
	public PackagerResult laffPackager(ShippingOrderBenchmarkState state) {
		return pack(state.getLaffPackager(), state.getContainers(), state.getOrder());
	}

	@Benchmark
	public PackagerResult plainPackager(ShippingOrderBenchmarkState state) {
		return pack(state.getPlainPackager(), state.getContainers(), state.getOrder());
	}

	/** All three packagers, keeping the best result. */
	@Benchmark
	public PackagerResult bestOf(ShippingOrderBenchmarkState state) {
		PackagerResult best = pack(state.getBruteForcePackager(), state.getContainers(), state.getOrder());
		best = best(best, pack(state.getLaffPackager(), state.getContainers(), state.getOrder()));
		return best(best, pack(state.getPlainPackager(), state.getContainers(), state.getOrder()));
	}

	public static PackagerResult pack(AbstractPackager<?> packager, List<ContainerItem> containers, List<BoxItem> order) {
		return packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItems(order)
				.withMaxContainerCount(MAX_CONTAINERS)
				.build();
	}

	/** Fewest containers, then the smallest total container volume. */
	public static PackagerResult best(PackagerResult a, PackagerResult b) {
		if(a.isSuccess() != b.isSuccess()) {
			return a.isSuccess() ? a : b;
		}
		if(a.size() != b.size()) {
			return a.size() < b.size() ? a : b;
		}
		return volume(b) < volume(a) ? b : a;
	}

	public static long volume(PackagerResult result) {
		long volume = 0;
		for(int i = 0; i < result.size(); i++) {
			volume += result.get(i).getVolume();
		}
		return volume;
	}
}
