package com.github.skjolber.packing.jmh.iterator;

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
		java.util.concurrent.atomic.LongAdder counter = new LongAdder();
		do {
			while (iterator.nextRotation() != -1) {
				counter.add(1);
				if(counter.longValue() >= MAX_COUNT) {
					return counter.longValue();
				}
			}
			iterator.resetRotations();
		} while (true);
	}

	@Benchmark
	public long permutations(IteratorState state) throws Exception {

		DefaultPermutationRotationIterator iterator = state.getIterator();
		java.util.concurrent.atomic.LongAdder counter = new LongAdder();
		do {
			counter.add(1);
			if(counter.longValue() >= MAX_COUNT) {
				return counter.longValue();
			}
		} while (iterator.nextPermutation(iterator.length() - 1) != -1);

		return counter.longValue();
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
