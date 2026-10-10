package com.github.skjolber.packing.points;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * Compare the point calculators with those of version 4 (module shadowed-v4 in legacy/v4, the 4.x classes with their packages moved to
 * {@code com.github.skjolber.packing.v4}): both get the same seeded random placements.
 * <p>
 * The 2D calculator must have the same free points as version 4, in the same order, with the same supports, after each placement.
 * <p>
 * Exact point parity with version 4 ended for the 3D calculator: it processes the points which it moves in a canonical total order (see
 * {@linkplain com.github.skjolber.packing.ep.points3d.CustomIntXComparator}), where version 4 left ties to a quicksort, in an arbitrary order. The tie order decides which of several
 * moved points that eclipse each other remain, so the points of the two differ: this version has fewer redundant points, and may tile the same free space into different maximal
 * points. The 3D calculator is pinned by its own checksums ({@linkplain PointCalculatorGoldenMasterTest}) and its order by {@code CustomIntComparatorsTest}.
 * <p>
 * What is compared with version 4 is the free space. After every placement, every free point of version 4 must be covered by the union of the free points of this version: the
 * free space is identical, but may be tiled into different points, so a point of version 4 may be covered by several points of this version, not by a single one. (A single covering
 * point is checked first, as it is cheap; otherwise the point is subtracted by the points of this version, exactly, and nothing may remain.) This version may have more free
 * space than version 4, where version 4 lost a maximal point; that direction is not checked.
 * <p>
 * Free points of version 4 which are below the limits in force are not required: a point with an area (size x times size y) less than the minimum area, or a volume less than the
 * minimum volume, cannot hold any remaining box. Version 4 keeps some such points, and this version may have dropped them, or tiled their space differently, so that their space is
 * not in its points (see {@linkplain PointCalculatorVersion4ComparisonIT} for how often). Other safety nets against version 4 are {@code ReferenceGoldenMasterTest} and the aggregate
 * tests with many orders ({@code PlainVersion4ComparisonIT} and {@code LargestAreaFitFirstVersion4ComparisonIT} in core). The placements are chosen among the points which both
 * calculators have, in a canonical order, so both get the same placements whatever their internal order.
 *
 * <pre>
 *   +-----------------------+        each step: pick a free point, place a box at its corner
 *   |  +---+                |        or offset inside it (floating), sometimes change the
 *   |  | B |   +--+         |        minimum area/volume limits, then compare all points
 *   |  +---+   |B |         |        of 5.x and 4.x
 *   |          +--+         |
 *   +-----------------------+
 * </pre>
 *
 * The placements are generated as in {@linkplain PointCalculatorGoldenMasterTest}, but with more steps, and also with boxes as large as
 * the free point. The slow integration test {@linkplain PointCalculatorVersion4ComparisonIT} compares many more seeds.
 */
public class PointCalculatorVersion4ComparisonTest {

	/** Seeds of this test; {@linkplain PointCalculatorVersion4ComparisonIT} continues with the following seeds */
	static final int SEEDS = 20;
	private static final int STEPS = 300;

	/** The coordinates and supports of a point: min x, y, z, max x, y, z, then six supports */
	private static final int POINT_3D = CanonicalPoints3D.POINT;
	/** min x, y, max x, y, then two supports */
	private static final int POINT_2D = 6;

	@ParameterizedTest(name = "immutable={0}, largest box={1}")
	@CsvSource({ "false, 8", "true, 8", "false, 0", "true, 0" })
	public void calculator3DCoversTheFreeSpaceOfVersion4(boolean immutable, int largestBox) {
		for(int seed = 0; seed < SEEDS; seed++) {
			compare3D(seed, immutable, largestBox);
		}
	}

	@ParameterizedTest(name = "immutable={0}, largest box={1}")
	@CsvSource({ "false, 8", "true, 8", "false, 0", "true, 0" })
	public void calculator2DMatchesVersion4(boolean immutable, int largestBox) {
		for(int seed = 0; seed < SEEDS; seed++) {
			compare2D(seed, immutable, largestBox);
		}
	}

	/**
	 * @param largestBox the largest box side, or 0 for as large as the free point
	 */
	static void compare3D(long seed, boolean immutable, int largestBox) {
		Random random = new Random(seed);
		DefaultPointCalculator3D calculator = new DefaultPointCalculator3D(immutable, STEPS);
		com.github.skjolber.packing.v4.ep.points3d.DefaultPointCalculator3D reference = new com.github.skjolber.packing.v4.ep.points3d.DefaultPointCalculator3D(
				immutable, STEPS);

		int dx = 10 + random.nextInt(40);
		int dy = 10 + random.nextInt(40);
		int dz = 10 + random.nextInt(40);
		calculator.clearToSize(dx, dy, dz);
		reference.clearToSize(dx, dy, dz);

		List<String> history = new ArrayList<>();
		history.add("container " + dx + "x" + dy + "x" + dz);
		int[] points = assertCovers3D(calculator, reference, seed, history);

		for(int step = 0; step < STEPS && !calculator.isEmpty(); step++) {
			if(random.nextInt(10) == 0) {
				long area = random.nextInt(4);
				long volume = random.nextInt(8);
				calculator.setMinimumAreaAndVolumeLimit(area, volume);
				reference.setMinimumAreaAndVolumeLimit(area, volume);
				history.add("limits area " + area + " volume " + volume);
				points = assertCovers3D(calculator, reference, seed, history);
				if(calculator.isEmpty()) {
					// the limits removed the last points
					break;
				}
			}
			// choose among the points which both calculators have, in canonical order, then look the point up in each calculator
			// (the order of the points within a calculator is not part of the comparison)
			int[] referencePoints = CanonicalPoints3D.flatten(reference);
			int[][] common = CanonicalPoints3D.common(CanonicalPoints3D.canonical(points), CanonicalPoints3D.canonical(referencePoints));
			if(common.length == 0) {
				break;
			}
			int[] row = common[random.nextInt(common.length)];
			int index = CanonicalPoints3D.indexOf(points, row);
			int referenceIndex = CanonicalPoints3D.indexOf(referencePoints, row);
			SimplePoint3D point = calculator.get(index);
			int boxDx = 1 + random.nextInt(largest(point.getDx(), largestBox));
			int boxDy = 1 + random.nextInt(largest(point.getDy(), largestBox));
			int boxDz = 1 + random.nextInt(largest(point.getDz(), largestBox));
			int x = point.getMinX();
			int y = point.getMinY();
			int z = point.getMinZ();
			if(random.nextInt(4) == 0) {
				// floating: not at the point's corner
				x += random.nextInt(point.getDx() - boxDx + 1);
				y += random.nextInt(point.getDy() - boxDy + 1);
				z += random.nextInt(point.getDz() - boxDz + 1);
			}
			history.add("point " + Arrays.toString(row) + ": box " + boxDx + "x" + boxDy + "x" + boxDz + " at " + x + "," + y + "," + z);

			calculator.add(index, placement(boxDx, boxDy, boxDz, x, y, z));
			reference.add(referenceIndex, placementVersion4(boxDx, boxDy, boxDz, x, y, z));

			points = assertCovers3D(calculator, reference, seed, history);
		}
	}

	static void compare2D(long seed, boolean immutable, int largestBox) {
		Random random = new Random(seed);
		DefaultPointCalculator2D calculator = new DefaultPointCalculator2D(immutable, STEPS);
		com.github.skjolber.packing.v4.ep.points2d.DefaultPointCalculator2D reference = new com.github.skjolber.packing.v4.ep.points2d.DefaultPointCalculator2D(
				immutable, STEPS);

		int dx = 10 + random.nextInt(40);
		int dy = 10 + random.nextInt(40);
		calculator.clearToSize(dx, dy, 1);
		reference.clearToSize(dx, dy, 1);

		List<String> history = new ArrayList<>();
		history.add("container " + dx + "x" + dy);
		assertSame2D(calculator, reference, seed, history);

		for(int step = 0; step < STEPS && !calculator.isEmpty(); step++) {
			if(random.nextInt(10) == 0) {
				long area = random.nextInt(4);
				calculator.setMinimumAreaLimit(area);
				reference.setMinimumAreaLimit(area);
				history.add("limit area " + area);
				assertSame2D(calculator, reference, seed, history);
				if(calculator.isEmpty()) {
					break;
				}
			}
			int index = random.nextInt(calculator.size());
			SimplePoint2D point = calculator.get(index);
			int boxDx = 1 + random.nextInt(largest(point.getDx(), largestBox));
			int boxDy = 1 + random.nextInt(largest(point.getDy(), largestBox));
			int x = point.getMinX();
			int y = point.getMinY();
			if(random.nextInt(4) == 0) {
				x += random.nextInt(point.getDx() - boxDx + 1);
				y += random.nextInt(point.getDy() - boxDy + 1);
			}
			history.add("point " + index + ": box " + boxDx + "x" + boxDy + " at " + x + "," + y);

			calculator.add(index, placement(boxDx, boxDy, 1, x, y, 0));
			reference.add(index, placementVersion4(boxDx, boxDy, 1, x, y, 0));

			assertSame2D(calculator, reference, seed, history);
		}
	}

	private static int largest(int size, int largestBox) {
		return largestBox == 0 ? size : Math.min(size, largestBox);
	}

	/**
	 * Every free point of version 4 (which is not below the limits) must be covered by the union of the free points of this version.
	 *
	 * @return the points of the calculator, in its own order
	 */
	private static int[] assertCovers3D(DefaultPointCalculator3D calculator, com.github.skjolber.packing.v4.ep.points3d.DefaultPointCalculator3D reference, long seed,
			List<String> history) {
		int[] actual = CanonicalPoints3D.flatten(calculator);
		int[][] actualRows = CanonicalPoints3D.canonical(actual);
		int[][] expectedRows = CanonicalPoints3D.canonical(CanonicalPoints3D.flatten(reference));

		// the points which both have are covered, so only the others need to be checked
		int[] uncovered = CanonicalPoints3D.firstUncovered(CanonicalPoints3D.difference(expectedRows, actualRows), actualRows, calculator.getMinAreaLimit(),
				calculator.getMinVolumeLimit());
		if(uncovered != null) {
			List<String> missing = new ArrayList<>();
			for(int[] part : CanonicalPoints3D.remainder(uncovered, actualRows)) {
				missing.add(Arrays.toString(part));
			}
			assertThat(describe(uncovered, POINT_3D)).as(() -> "seed " + seed + ", after\n" + String.join("\n", history) + "\nlimits area " + calculator.getMinAreaLimit() + " volume "
					+ calculator.getMinVolumeLimit() + "\nparts of the free space of version 4 which are not covered: " + missing + "\nfree point of version 4:").isEmpty();
		}
		return actual;
	}

	private static void assertSame2D(DefaultPointCalculator2D calculator, com.github.skjolber.packing.v4.ep.points2d.DefaultPointCalculator2D reference, long seed,
			List<String> history) {
		int[] actual = new int[calculator.size() * POINT_2D];
		for(int i = 0; i < calculator.size(); i++) {
			SimplePoint2D p = calculator.get(i);
			put(actual, i * POINT_2D, p.getMinX(), p.getMinY(), p.getMaxX(), p.getMaxY());
			put(actual, i * POINT_2D + 4, p.isXSupport(p.getMaxX()), p.isYSupport(p.getMaxY()));
		}
		int[] expected = new int[reference.size() * POINT_2D];
		for(int i = 0; i < reference.size(); i++) {
			com.github.skjolber.packing.v4.ep.points2d.SimplePoint2D p = reference.get(i);
			put(expected, i * POINT_2D, p.getMinX(), p.getMinY(), p.getMaxX(), p.getMaxY());
			put(expected, i * POINT_2D + 4, p.isXSupport(p.getMaxX()), p.isYSupport(p.getMaxY()));
		}
		assertSame(actual, expected, POINT_2D, seed, history);
	}

	private static void put(int[] values, int offset, int... coordinates) {
		System.arraycopy(coordinates, 0, values, offset, coordinates.length);
	}

	private static void put(int[] values, int offset, boolean... supports) {
		for(int i = 0; i < supports.length; i++) {
			values[offset + i] = supports[i] ? 1 : 0;
		}
	}

	/**
	 * Compare the points as numbers, which is fast, and only describe them if they differ.
	 */
	private static void assertSame(int[] actual, int[] expected, int pointLength, long seed, List<String> history) {
		if(!Arrays.equals(actual, expected)) {
			assertThat(describe(actual, pointLength)).as(() -> "seed " + seed + ", after\n" + String.join("\n", history)).isEqualTo(describe(expected, pointLength));
		}
	}

	/**
	 * @return one line per point: the coordinates, then S for each support and - for each missing support
	 */
	private static List<String> describe(int[] values, int pointLength) {
		int coordinates = pointLength == POINT_3D ? 6 : 4;
		List<String> points = new ArrayList<>();
		for(int offset = 0; offset < values.length; offset += pointLength) {
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < coordinates; i++) {
				builder.append(i == coordinates / 2 ? " - " : i == 0 ? "" : ",").append(values[offset + i]);
			}
			builder.append(' ');
			for(int i = coordinates; i < pointLength; i++) {
				builder.append(values[offset + i] == 1 ? 'S' : '-');
			}
			points.add(builder.toString());
		}
		return points;
	}

	private static Placement placement(int dx, int dy, int dz, int x, int y, int z) {
		return new Placement(Box.newBuilder()
				.withSize(dx, dy, dz)
				.withWeight(1)
				.build()
				.getStackValue(0), -1, x, y, z, false);
	}

	private static com.github.skjolber.packing.v4.api.Placement placementVersion4(int dx, int dy, int dz, int x, int y, int z) {
		com.github.skjolber.packing.v4.api.BoxStackValue stackValue = com.github.skjolber.packing.v4.api.Box.newBuilder()
				.withSize(dx, dy, dz)
				.withWeight(1)
				.build()
				.getStackValue(0);
		assertThat(new int[] { stackValue.getDx(), stackValue.getDy(), stackValue.getDz() }).containsExactly(dx, dy, dz);
		return new com.github.skjolber.packing.v4.api.Placement(stackValue, -1, x, y, z);
	}
}
