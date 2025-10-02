package com.github.skjolber.packing.jmh;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
import org.openjdk.jmh.runner.options.TimeValue;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * 
 * Check the impact of using a variable deadline, avoiding doing frequent system calls.
 *
 */

@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class DeadlineBenchmark {

	@Benchmark
	public Object parallelPackagerNoDeadline(BouwkampCodeBruteForcePackagerState state) throws Exception {
		return process(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
	}

	@Benchmark
	public Object parallelPackagerDeadline(BouwkampCodeBruteForcePackagerState state) throws Exception {
		return process(state.getParallelBruteForcePackager(), System.currentTimeMillis() + 10000);
	}

	@Benchmark
	public Object packagerNoDeadline(BouwkampCodeBruteForcePackagerState state) throws Exception {
		return process(state.getBruteForcePackager(), -1L);
	}

	@Benchmark
	public Object packagerDeadline(BouwkampCodeBruteForcePackagerState state) throws Exception {
		return process(state.getBruteForcePackager(), System.currentTimeMillis() + 30000);
	}

	public int process(List<BenchmarkSet> sets, long deadline) {
		int i = 0;
		for (BenchmarkSet set : sets) {
			AbstractPackager packager = set.getPackager();
			List<ContainerItem> containers = set.getContainers();
			List<BoxItem> products = set.getProducts();

			PackagerResult build = packager.newResultBuilder().withContainerItems(containers).withMaxContainerCount(1).withBoxItems(products).withDeadline(deadline).build();
			if(build.isSuccess()) {
				i++;
			} else {
				throw new RuntimeException();
			}
		}

		return i;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(DeadlineBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)

				.forks(1)
				.measurementIterations(1)
				.measurementTime(TimeValue.seconds(90))
				.timeout(TimeValue.seconds(60))

				.build();

		new Runner(opt).run();
	}

}
