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
 * {@code com.github.skjolber.packing.v4}): both get the same seeded random placements, and after each placement they must have the
 * same free points, in the same order, with the same supports.
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
	private static final int POINT_3D = 12;
	/** min x, y, max x, y, then two supports */
	private static final int POINT_2D = 6;

	@ParameterizedTest(name = "immutable={0}, largest box={1}")
	@CsvSource({ "false, 8", "true, 8", "false, 0", "true, 0" })
	public void calculator3DMatchesVersion4(boolean immutable, int largestBox) {
		int complete = 0;
		for(int seed = 0; seed < SEEDS; seed++) {
			if(compare3D(seed, immutable, largestBox)) {
				complete++;
			}
		}
		// version 4 fails on some seeds, see isVersion4ListOverflow(..)
		assertThat(complete).isGreaterThanOrEqualTo(SEEDS * 9 / 10);
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
	 * @return true if all the steps were compared, false if version 4 failed with the overflow which 5.x fixed
	 */
	static boolean compare3D(long seed, boolean immutable, int largestBox) {
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
		assertSame3D(calculator, reference, seed, history);

		for(int step = 0; step < STEPS && !calculator.isEmpty(); step++) {
			if(random.nextInt(10) == 0) {
				long area = random.nextInt(4);
				long volume = random.nextInt(8);
				calculator.setMinimumAreaAndVolumeLimit(area, volume);
				reference.setMinimumAreaAndVolumeLimit(area, volume);
				history.add("limits area " + area + " volume " + volume);
				assertSame3D(calculator, reference, seed, history);
				if(calculator.isEmpty()) {
					// the limits removed the last points
					break;
				}
			}
			int index = random.nextInt(calculator.size());
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
			history.add("point " + index + ": box " + boxDx + "x" + boxDy + "x" + boxDz + " at " + x + "," + y + "," + z);

			calculator.add(index, placement(boxDx, boxDy, boxDz, x, y, z));
			try {
				reference.add(index, placementVersion4(boxDx, boxDy, boxDz, x, y, z));
			} catch (ArrayIndexOutOfBoundsException e) {
				if(isVersion4ListOverflow(e)) {
					// a bug in version 4 which 5.x fixed: there is nothing more to compare with
					return false;
				}
				throw e;
			}

			assertSame3D(calculator, reference, seed, history);
		}
		return true;
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

	/**
	 * Version 4 adds the points constrained by a placement which is not at the corner of its point to a list without growing it
	 * ({@code constrainFloatingMax}), so more than 16 such points overflow the list. 5.x grows the lists first. Once thrown often, the
	 * JVM throws this exception without a stack trace, so an exception without one is taken to be the same.
	 */
	private static boolean isVersion4ListOverflow(ArrayIndexOutOfBoundsException e) {
		StackTraceElement[] stack = e.getStackTrace();
		if(stack.length == 0) {
			return true;
		}
		return stack.length > 1 && stack[0].getClassName().equals(com.github.skjolber.packing.v4.ep.points3d.Point3DList.class.getName())
				&& stack[1].getMethodName().equals("constrainFloatingMax");
	}

	private static int largest(int size, int largestBox) {
		return largestBox == 0 ? size : Math.min(size, largestBox);
	}

	private static void assertSame3D(DefaultPointCalculator3D calculator, com.github.skjolber.packing.v4.ep.points3d.DefaultPointCalculator3D reference, long seed,
			List<String> history) {
		int[] actual = new int[calculator.size() * POINT_3D];
		for(int i = 0; i < calculator.size(); i++) {
			SimplePoint3D p = calculator.get(i);
			put(actual, i * POINT_3D, p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
			put(actual, i * POINT_3D + 6, p.isSupportedXYPlane(), p.isSupportedXZPlane(), p.isSupportedYZPlane(), p.isSupportedXYPlane(p.getMaxX(), p.getMaxY()),
					p.isSupportedXZPlane(p.getMaxX(), p.getMaxZ()), p.isSupportedYZPlane(p.getMaxY(), p.getMaxZ()));
		}
		int[] expected = new int[reference.size() * POINT_3D];
		for(int i = 0; i < reference.size(); i++) {
			com.github.skjolber.packing.v4.ep.points3d.SimplePoint3D p = reference.get(i);
			put(expected, i * POINT_3D, p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
			put(expected, i * POINT_3D + 6, p.isSupportedXYPlane(), p.isSupportedXZPlane(), p.isSupportedYZPlane(), p.isSupportedXYPlane(p.getMaxX(), p.getMaxY()),
					p.isSupportedXZPlane(p.getMaxX(), p.getMaxZ()), p.isSupportedYZPlane(p.getMaxY(), p.getMaxZ()));
		}
		assertSame(actual, expected, POINT_3D, seed, history);
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
