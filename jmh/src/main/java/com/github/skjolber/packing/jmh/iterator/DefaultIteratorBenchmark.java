package com.github.skjolber.packing.jmh.iterator;

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

import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;

@Fork(value = 1, warmups = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Warmup(iterations = 1, time = 7, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 8, timeUnit = TimeUnit.SECONDS)
public class DefaultIteratorBenchmark {

	private static final long MAX_COUNT = 8 * 1024 * 1024L;

	@Benchmark
	public long rotations(IteratorState state) throws Exception {

		DefaultPermutationRotationIterator iterator = state.getIterator();
		int index = iterator.length() / 2;

		long count = 0;
		do {
			while (iterator.nextRotation(index) != -1) {
				count++;
				if(count >= MAX_COUNT) {
					return count;
				}
			}
			iterator.resetRotations();
		} while (true);
	}

	@Benchmark
	public long permutations(IteratorState state) throws Exception {

		DefaultPermutationRotationIterator iterator = state.getIterator();

		int index = iterator.length() / 2;

		long count = 0;
		do {
			count++;
			if(count >= MAX_COUNT) {
				return count;
			}

			if(iterator.nextPermutation(index) == -1) {
				throw new RuntimeException();
			}
		} while (true);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(DefaultIteratorBenchmark.class.getSimpleName())
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
