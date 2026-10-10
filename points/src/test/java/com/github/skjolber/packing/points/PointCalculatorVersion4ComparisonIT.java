package com.github.skjolber.packing.points;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * As {@linkplain PointCalculatorVersion4ComparisonTest}, with many more seeds: the 2D point calculator must give the same free points as that of version 4, and the free points of
 * the 3D point calculator must cover the free space of version 4 (the union of the points; version 4 points below the limits in force are not required). Run with
 * {@code mvn -P slow-tests verify}.
 * <p>
 * How often the 3D points of the two differ in the free space, in the four configurations of 1,000 seeds each (4,000 sequences of up to 300 placements, 1.3 million checks, when this
 * was written): a free point of version 4 is not within a single point of this version in 51 checks (in 4 sequences). In 23 of those the union of the points of this version covers it
 * (a different tiling of the same free space; 5 of the 23 with the limits met). In the other 28 checks (2 sequences, both mutable, i.e. {@code immutable=false}) part of the point
 * is in no point of this version, an uncovered cell is free, and the point is always below the limits in force, so no remaining box fits into it. None of the points of version 4
 * which meet the limits is lost.
 */
public class PointCalculatorVersion4ComparisonIT {

	private static final int SEEDS = 1000;

	@ParameterizedTest(name = "immutable={0}, largest box={1}")
	@CsvSource({ "false, 8", "true, 8", "false, 0", "true, 0" })
	public void calculator3DCoversTheFreeSpaceOfVersion4(boolean immutable, int largestBox) {
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
