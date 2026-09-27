package com.github.skjolber.packing.jmh.boundingbox;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxSearch;

/**
 * Complete one-shot searches, including construction, preparation and result snapshots.
 * Input/objective construction and public-builder validation are outside the measured work.
 * No deadlines truncate exhaustive searches. Returned objects are consumed by JMH.
 */
@Fork(2)
@Threads(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BruteForceBoundingBoxSearchBenchmark {

	/** Construction-only baseline, without preparation or traversal. */
	@Benchmark
	public BruteForceBoundingBoxSearch constructSingleObjective(BruteForceBoundingBoxSearchState state) {
		return state.newSingleObjectiveSearch();
	}

	@Benchmark
	public BruteForceBoundingBoxResult singleObjectiveExhaustive(BruteForceBoundingBoxSearchState state) {
		long start = System.nanoTime();
		return state.newSingleObjectiveSearch().pack(start);
	}

	/** Same objective and stopping rules as singleObjectiveExhaustive; only the search implementation differs. */
	@Benchmark
	public BruteForceBoundingBoxResult singleObjectiveViaMultiExhaustive(BruteForceBoundingBoxSearchState state) {
		long start = System.nanoTime();
		return state.newSingleObjectiveViaMultiSearch().pack(start);
	}

	/** All four objectives are optimized in one traversal. */
	@Benchmark
	public BruteForceBoundingBoxResult volumeAndAxesExhaustive(BruteForceBoundingBoxSearchState state) {
		long start = System.nanoTime();
		return state.newVolumeAndAxesSearch().pack(start);
	}

	/** Stop at a complete rectangular assembly with no unused bounding volume. */
	@Benchmark
	public BruteForceBoundingBoxResult filledVolumeGoal(BruteForceBoundingBoxSearchState state) {
		long start = System.nanoTime();
		return state.newFilledGoalSearch().pack(start);
	}
}
