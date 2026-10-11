package com.github.skjolber.packing.packer.laff;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.packer.Version4Comparison;
import com.github.skjolber.packing.packer.Version4Comparison.Tally;

/**
 * As {@linkplain LargestAreaFitFirstVersion4ComparisonTest}, with many more orders: the largest area fit first packagers must do at least as
 * well as those of version 4 overall, i.e. everything the test requires, and win at least as many orders as they lose. Run with
 * {@code mvn -P slow-tests verify}.
 */
public class LargestAreaFitFirstVersion4ComparisonIT {

	private static final int SEEDS = 10_000;

	@ParameterizedTest(name = "fast={0}")
	@ValueSource(booleans = { false, true })
	void doesNotLoseToVersion4(boolean fast) {
		int from = LargestAreaFitFirstVersion4ComparisonTest.SEEDS;
		Tally tally = LargestAreaFitFirstVersion4ComparisonTest.compare(fast, from, from + SEEDS);
		System.out.println("fast " + fast + ": " + tally);
		Version4Comparison.assertMeaningful(tally, SEEDS);
		Version4Comparison.assertFewLosses(tally);
		Version4Comparison.assertWinsAtLeastLosses(tally);
	}
}
