package com.github.skjolber.packing.points;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * As {@linkplain PointCalculatorVersion4ComparisonTest}, with many more seeds: the point calculators must give the same free points as
 * those of version 4. Run with {@code mvn -P slow-tests verify}.
 */
public class PointCalculatorVersion4ComparisonIT {

	private static final int SEEDS = 1000;

	@ParameterizedTest(name = "immutable={0}, largest box={1}")
	@CsvSource({ "false, 8", "true, 8", "false, 0", "true, 0" })
	public void calculator3DMatchesVersion4(boolean immutable, int largestBox) {
		for(int seed = PointCalculatorVersion4ComparisonTest.SEEDS; seed < PointCalculatorVersion4ComparisonTest.SEEDS + SEEDS; seed++) {
			PointCalculatorVersion4ComparisonTest.compare3D(seed, immutable, largestBox);
		}
	}

	@ParameterizedTest(name = "immutable={0}, largest box={1}")
	@CsvSource({ "false, 8", "true, 8", "false, 0", "true, 0" })
	public void calculator2DMatchesVersion4(boolean immutable, int largestBox) {
		for(int seed = PointCalculatorVersion4ComparisonTest.SEEDS; seed < PointCalculatorVersion4ComparisonTest.SEEDS + SEEDS; seed++) {
			PointCalculatorVersion4ComparisonTest.compare2D(seed, immutable, largestBox);
		}
	}
}
