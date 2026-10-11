package com.github.skjolber.packing.points3d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.eclipse.collections.impl.utility.primitive.IntQuickSort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.CustomIntXComparator;
import com.github.skjolber.packing.ep.points3d.CustomIntYComparator;
import com.github.skjolber.packing.ep.points3d.CustomIntZComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * The order in which the 3D point calculator processes the points it moves is a canonical total order (see {@linkplain CustomIntXComparator}), so the free points do not depend on the
 * sorting algorithm. This test runs the same seeded random placements through three calculators in lockstep: one as in production (insertion sort), one which uses
 * the eclipse collections quicksort for the x and y moves, and one which first shuffles the points to move and then uses the quicksort. Quicksort (unlike insertion sort) is not stable, so it
 * orders points which compare equal arbitrarily, and the lists of the free points, with their supporting planes, must still be identical after every placement.
 * Also, no two points which can be told apart may compare equal (in any move, including the z move which is not replaced), so that no sorting algorithm can make a difference.
 *
 * <pre>
 *   +-----------------------+        each step: pick a free point, place a box at its corner
 *   |  +---+                |        or offset inside it (floating), sometimes change the
 *   |  | B |   +--+         |        minimum area/volume limits, then compare all points
 *   |  +---+   |B |         |        of the three calculators
 *   |          +--+         |
 *   +-----------------------+
 * </pre>
 */
public class PointCalculator3DCanonicalOrderTest {

	private static final int STEPS = 150;

	/** Counts the pairs of different points, which can be told apart, which compare equal */
	private static class Ties {
		int ties;
		int comparisons;
		String first;

		void compare(SimplePoint3D a, SimplePoint3D b, int result) {
			comparisons++;
			if(result == 0 && a != b && !sameContent(a, b)) {
				ties++;
				if(first == null) {
					first = a + " and " + b;
				}
			}
		}
	}

	private static boolean sameContent(SimplePoint3D a, SimplePoint3D b) {
		DefaultPoint3D first = (DefaultPoint3D)a;
		DefaultPoint3D second = (DefaultPoint3D)b;
		return a.getMinX() == b.getMinX() && a.getMinY() == b.getMinY() && a.getMinZ() == b.getMinZ() && a.getMaxX() == b.getMaxX() && a.getMaxY() == b.getMaxY()
				&& a.getMaxZ() == b.getMaxZ() && first.getXYPlane() == second.getXYPlane() && first.getXZPlane() == second.getXZPlane() && first.getYZPlane() == second.getYZPlane();
	}

	private static void shuffle(int[] indexes, int size, Random random) {
		for(int i = size - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int swap = indexes[i];
			indexes[i] = indexes[j];
			indexes[j] = swap;
		}
	}

	private static class XComparator extends CustomIntXComparator {
		private final Ties ties;
		private final Random shuffle;

		XComparator(Ties ties, Random shuffle) {
			this.ties = ties;
			this.shuffle = shuffle;
		}

		@Override
		public int compare(SimplePoint3D o1, SimplePoint3D o2) {
			int result = super.compare(o1, o2);
			ties.compare(o1, o2, result);
			return result;
		}

		@Override
		public void insertionSort(int[] indexes, int size) {
			if(shuffle != null) {
				shuffle(indexes, size, shuffle);
			}
			IntQuickSort.sort(indexes, 0, size - 1, this);
		}
	}

	private static class YComparator extends CustomIntYComparator {
		private final Ties ties;
		private final Random shuffle;

		YComparator(Ties ties, Random shuffle) {
			this.ties = ties;
			this.shuffle = shuffle;
		}

		@Override
		public int compare(SimplePoint3D o1, SimplePoint3D o2) {
			int result = super.compare(o1, o2);
			ties.compare(o1, o2, result);
			return result;
		}

		@Override
		public void insertionSort(int[] indexes, int size) {
			if(shuffle != null) {
				shuffle(indexes, size, shuffle);
			}
			IntQuickSort.sort(indexes, 0, size - 1, this);
		}
	}

	private static class ZComparator extends CustomIntZComparator {
		private final Ties ties;

		ZComparator(Ties ties) {
			this.ties = ties;
		}

		@Override
		public int compare(SimplePoint3D o1, SimplePoint3D o2) {
			int result = super.compare(o1, o2);
			ties.compare(o1, o2, result);
			return result;
		}
	}

	private static class Calculator extends DefaultPointCalculator3D {

		/**
		 * @param quicksort replace the insertion sort of the x and y moves with a quicksort
		 * @param shuffle   shuffle the points to move before sorting, or null
		 */
		Calculator(boolean immutable, int capacity, Ties ties, boolean quicksort, Random shuffle) {
			super(immutable, capacity);
			if(quicksort) {
				xxComparator = new XComparator(ties, shuffle);
				yyComparator = new YComparator(ties, shuffle);
			}
			zzComparator = new ZComparator(ties);
		}
	}

	/** The coordinates and all supporting planes of the points, in the order of the calculator */
	private static int[] flatten(DefaultPointCalculator3D calculator) {
		List<Integer> values = new ArrayList<>();
		for(int i = 0; i < calculator.size(); i++) {
			DefaultPoint3D p = (DefaultPoint3D)calculator.get(i);
			values.add(p.getMinX());
			values.add(p.getMinY());
			values.add(p.getMinZ());
			values.add(p.getMaxX());
			values.add(p.getMaxY());
			values.add(p.getMaxZ());
			plane(values, p.getXYPlane());
			plane(values, p.getXZPlane());
			plane(values, p.getYZPlane());
		}
		int[] result = new int[values.size()];
		for(int i = 0; i < result.length; i++) {
			result[i] = values.get(i);
		}
		return result;
	}

	private static void plane(List<Integer> values, Placement plane) {
		if(plane == null) {
			values.add(0);
		} else {
			values.add(1);
			values.add(plane.getAbsoluteX());
			values.add(plane.getAbsoluteY());
			values.add(plane.getAbsoluteZ());
			values.add(plane.getAbsoluteEndX());
			values.add(plane.getAbsoluteEndY());
			values.add(plane.getAbsoluteEndZ());
		}
	}

	/**
	 * @param scale     0 for containers of 10-50 per side, otherwise containers about this size per side: the volume proxy of the keys does not fit an int
	 * @param largestBox the largest box side, or 0 for as large as the free point
	 */
	private static void lockstep(long seed, boolean immutable, int largestBox, int scale) {
		Random random = new Random(seed);
		Ties ties = new Ties();
		Calculator production = new Calculator(immutable, STEPS, ties, false, null);
		Calculator quicksort = new Calculator(immutable, STEPS, ties, true, null);
		Calculator shuffled = new Calculator(immutable, STEPS, ties, true, new Random(seed));

		int dx = scale == 0 ? 10 + random.nextInt(40) : scale / 2 + random.nextInt(scale);
		int dy = scale == 0 ? 10 + random.nextInt(40) : scale / 2 + random.nextInt(scale);
		int dz = scale == 0 ? 10 + random.nextInt(40) : scale / 2 + random.nextInt(scale);
		production.clearToSize(dx, dy, dz);
		quicksort.clearToSize(dx, dy, dz);
		shuffled.clearToSize(dx, dy, dz);

		for(int step = 0; step < STEPS && !production.isEmpty(); step++) {
			if(random.nextInt(10) == 0) {
				int area = random.nextInt(4);
				int volume = random.nextInt(8);
				production.setMinimumAreaAndVolumeLimit(area, volume);
				quicksort.setMinimumAreaAndVolumeLimit(area, volume);
				shuffled.setMinimumAreaAndVolumeLimit(area, volume);
				if(production.isEmpty()) {
					break;
				}
			}
			int index = random.nextInt(production.size());
			SimplePoint3D point = production.get(index);
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
			production.add(index, placement(boxDx, boxDy, boxDz, x, y, z));
			quicksort.add(index, placement(boxDx, boxDy, boxDz, x, y, z));
			shuffled.add(index, placement(boxDx, boxDy, boxDz, x, y, z));

			int[] expected = flatten(production);
			String description = "seed " + seed + ", immutable " + immutable + ", step " + step + ", box " + boxDx + "x" + boxDy + "x" + boxDz + " at " + x + "," + y + "," + z;
			assertThat(production.size()).as(description).isEqualTo(quicksort.size()).isEqualTo(shuffled.size());
			assertThat(Arrays.equals(expected, flatten(quicksort))).as("quicksort, " + description).isTrue();
			assertThat(Arrays.equals(expected, flatten(shuffled))).as("shuffled quicksort, " + description).isTrue();
		}
		assertThat(ties.ties).as("points which can be told apart and compare equal, first: " + ties.first).isZero();
		assertThat(ties.comparisons).isPositive();
	}

	private static int largest(int size, int largestBox) {
		return largestBox == 0 ? size : Math.min(size, largestBox);
	}

	@ParameterizedTest(name = "immutable={0}, largest box={1}, container size={2}, seeds={3}")
	@CsvSource({ "false, 8, 0, 6", "true, 8, 0, 6", "false, 0, 0, 6", "true, 0, 0, 6", "false, 400, 1500, 1", "true, 0, 2500, 1" })
	public void sortingAlgorithmDoesNotChangeTheFreePoints(boolean immutable, int largestBox, int scale, int seeds) {
		for(int seed = 0; seed < seeds; seed++) {
			lockstep(seed, immutable, largestBox, scale);
		}
	}

	private static Placement placement(int dx, int dy, int dz, int x, int y, int z) {
		return new Placement(Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build().getStackValue(0), -1, x, y, z, false);
	}
}
