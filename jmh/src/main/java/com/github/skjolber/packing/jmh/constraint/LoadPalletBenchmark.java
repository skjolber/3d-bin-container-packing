package com.github.skjolber.packing.jmh.constraint;

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
 * Cost of load calculations: a 48-box pallet packed with and without load limits.
 *
 * @see LoadPalletBenchmarkState
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class LoadPalletBenchmark {

	@Benchmark
	public int plainPackager(LoadPalletBenchmarkState state) {
		return pack(state.getPlainPackager(), state);
	}

	@Benchmark
	public int laffPackager(LoadPalletBenchmarkState state) {
		return pack(state.getLaffPackager(), state);
	}

	private static int pack(AbstractPackager<?> packager, LoadPalletBenchmarkState state) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(state.getContainers())
				.withMaxContainerCount(1)
				.withBoxItems(state.getItems())
				.build();
		return result.isSuccess() ? result.get(0).getStack().size() : 0;
	}
}
