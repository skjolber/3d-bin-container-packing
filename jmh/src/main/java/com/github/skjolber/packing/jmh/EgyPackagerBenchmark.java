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

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;

@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class EgyPackagerBenchmark {

	/*
	@Benchmark
	public int plainPackager(EgyPackagerState state) throws Exception {
		return process(state.getPlainPackager(), Long.MAX_VALUE);
	}
	 */
	@Benchmark
	public int parallelPackager(EgyPackagerState state) throws Exception {
		return process(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
	}

	/*    
	@Benchmark
	public int packager(EgyPackagerState state) throws Exception {
		return process(state.getBruteForcePackager(), Long.MAX_VALUE);
	}

	@Benchmark
	public int fastPackager(EgyPackagerState state) throws Exception {
		return process(state.getFastBruteForcePackager(), Long.MAX_VALUE);
	}
	 */
	public int process(List<BenchmarkSet> sets, long deadline) {
		BooleanSupplier booleanSupplier = BooleanSupplierBuilder.builder().withDeadline(deadline, 1).build();

		int i = 0;
		for (BenchmarkSet set : sets) {
			 List<ContainerItem> containers = set.getContainers();
			 List<StackableItem> products = set.getProducts();
			 
			if(set.getPackager().pack(products, containers, booleanSupplier) != null) {
				i++;
			}
		}

		return i;
	}

	public static void main(String[] args) throws RunnerException {
		System.setProperty("jmh.ignoreLock", "true ");
		Options opt = new OptionsBuilder()
				.include(EgyPackagerBenchmark.class.getSimpleName())
				.mode(Mode.Throughput)
				/*
				.forks(1)
				.measurementIterations(1)
				.measurementTime(TimeValue.seconds(15))
				.timeout(TimeValue.seconds(10))
				 */
				.build();

		new Runner(opt).run();
	}

}
