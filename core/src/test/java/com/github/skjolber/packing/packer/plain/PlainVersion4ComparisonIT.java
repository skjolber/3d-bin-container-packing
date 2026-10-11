package com.github.skjolber.packing.packer.plain;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.packer.Version4Comparison;
import com.github.skjolber.packing.packer.Version4Comparison.Tally;

/**
 * As {@linkplain PlainVersion4ComparisonTest}, with many more orders: the plain packager must do at least as well as that of version 4 overall,
 * i.e. everything the test requires, and win at least as many orders as it loses. Run with {@code mvn -P slow-tests verify}.
 */
public class PlainVersion4ComparisonIT {

	private static final int SEEDS = 10_000;

	@Test
	void doesNotLoseToVersion4() {
		int from = PlainVersion4ComparisonTest.SEEDS;
		Tally tally = PlainVersion4ComparisonTest.compare(from, from + SEEDS);
		System.out.println("plain: " + tally);
		Version4Comparison.assertMeaningful(tally, SEEDS);
		Version4Comparison.assertFewLosses(tally);
		Version4Comparison.assertWinsAtLeastLosses(tally);
	}
}
