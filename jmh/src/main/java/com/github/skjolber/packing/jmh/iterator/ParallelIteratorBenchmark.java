package com.github.skjolber.packing.jmh.iterator;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

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

import com.github.skjolber.packing.iterator.ParallelBoxItemPermutationRotationIterator;

@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 7, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
public class ParallelIteratorBenchmark {

	private static final long MAX_COUNT = 8 * 1024 * 1024L;

	@Benchmark
	public long rotations(IteratorState state) throws Exception {

		ParallelBoxItemPermutationRotationIterator[] iterators = state.getParallelIterator().getIterators();

		ExecutorCompletionService<Long> executorCompletionService = new ExecutorCompletionService<Long>(state.getPool());

		long maxCountPerThread = MAX_COUNT / iterators.length;

		for (int i = 0; i < iterators.length; i++) {
			ParallelBoxItemPermutationRotationIterator iterator = iterators[i];

			Callable<Long> callable = () -> {
				java.util.concurrent.atomic.LongAdder counter = new LongAdder();
				do {
					while (iterator.nextRotation() != -1) {
						counter.add(1);
						if(counter.longValue() >= maxCountPerThread) {
							return counter.longValue();
						}
					}
					iterator.resetRotations();
				} while (true);
			};

			executorCompletionService.submit(callable);
		}

		long count = 0;
		for (int i = 0; i < iterators.length; i++) {
			Future<Long> future = executorCompletionService.take();
			Long result = future.get();
			if(result != null) {
				if(result != maxCountPerThread) {
					throw new RuntimeException();
				}
				count += result;
			}
		}

		return count;
	}

	@Benchmark
	public long parallelPermutations(IteratorState state) throws Exception {

		ParallelBoxItemPermutationRotationIterator[] iterators = state.getParallelIterator().getIterators();

		ExecutorCompletionService<Long> executorCompletionService = new ExecutorCompletionService<Long>(state.getPool());

		long maxCountPerThread = MAX_COUNT / iterators.length;

		for (int i = 0; i < iterators.length; i++) {
			ParallelBoxItemPermutationRotationIterator iterator = iterators[i];

			Callable<Long> callable = () -> {
				java.util.concurrent.atomic.LongAdder counter = new LongAdder();
				do {
					counter.add(1);
					if(counter.longValue() >= maxCountPerThread) {
						return counter.longValue();
					}
				} while (iterator.nextPermutation() != -1);

				throw new RuntimeException("Only got " + counter);
			};

			executorCompletionService.submit(callable);
		}

		long count = 0;
		for (int i = 0; i < iterators.length; i++) {
			Future<Long> future = executorCompletionService.take();
			Long result = future.get();
			if(result != null) {
				if(result != maxCountPerThread) {
					throw new RuntimeException();
				}
				count += result;
			}
		}

		return count;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(ParallelIteratorBenchmark.class.getSimpleName())
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
