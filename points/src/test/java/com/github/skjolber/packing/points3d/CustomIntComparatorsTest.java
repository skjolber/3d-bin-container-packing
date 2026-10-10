package com.github.skjolber.packing.points3d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.CustomIntArrayList;
import com.github.skjolber.packing.ep.points3d.CustomIntXComparator;
import com.github.skjolber.packing.ep.points3d.CustomIntYComparator;
import com.github.skjolber.packing.ep.points3d.CustomIntZComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * Properties of the canonical total order of the points which are moved to the other side of a placement (see {@linkplain CustomIntXComparator}):
 * <ul>
 * <li>monotone with eclipsing: if the moved form of A eclipses the moved form of B, and they are not geometrically identical, then A is first. So the calculator
 * only keeps maximal moved forms, whatever the order in which the points to move were found.
 * <li>total: points compare equal only if they cannot be told apart, so every sorting algorithm and every start order gives the same result.
 * <li>the volume proxy does not wrap for large containers (the key must stay monotone).
 * <li>of geometrically identical moved forms the one with the richest supports is first.
 * </ul>
 * The points are random, in a small range so that there are many ties, and also scaled up so that the volume proxy does not fit an int.
 *
 * <pre>
 *   A eclipses B (moved to xx):         A and B identical (moved to xx):
 *
 *   y                                   y
 *   |  +-----------+                    |  +-----------+
 *   |  | A  +---+  |                    |  | A | B     |    the first one is kept, the other one dropped:
 *   |  |    | B |  |                    |  +-----------+    prefer the richest supports
 *   |  +----+---+--+
 *   +---------------- x                 +---------------- x
 *      A is sorted before B
 * </pre>
 */
public class CustomIntComparatorsTest {

	private enum Direction {
		X, Y, Z
	}

	/** The point coordinates are scaled by this: 1000 makes the volume proxy exceed an int */
	private static final int[] SCALES = { 1, 1000 };

	private static final int CASES = 40000;

	/** the planes of the points: a point has a plane or not, so that a plane is always the same placement (as in a calculator: placements do not overlap) */
	private static class Planes {
		final Placement yz;
		final Placement xz;
		final Placement xy;

		Planes(int scale) {
			// ends at 3 * scale in the other directions, so a move to a larger coordinate loses the plane
			int size = 3 * scale + 1;
			yz = placement(1, size, size, 0, 0, 0);
			xz = placement(size, 1, size, 0, 0, 0);
			xy = placement(size, size, 1, 0, 0, 0);
		}
	}

	private static Placement placement(int dx, int dy, int dz, int x, int y, int z) {
		return new Placement(Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build().getStackValue(0), -1, x, y, z, false);
	}

	private static DefaultPoint3D point(Random random, int range, int scale, Planes planes) {
		int[] min = new int[3];
		int[] max = new int[3];
		for(int i = 0; i < 3; i++) {
			min[i] = random.nextInt(range) * scale;
			max[i] = min[i] + random.nextInt(range + 1 - min[i] / scale) * scale;
		}
		return new DefaultPoint3D(min[0], min[1], min[2], max[0], max[1], max[2], random.nextBoolean() ? planes.yz : null, random.nextBoolean() ? planes.xz : null,
				random.nextBoolean() ? planes.xy : null);
	}

	private static int min(Direction direction, SimplePoint3D point) {
		switch(direction) {
		case X:
			return point.getMinX();
		case Y:
			return point.getMinY();
		default:
			return point.getMinZ();
		}
	}

	private static int max(Direction direction, SimplePoint3D point) {
		switch(direction) {
		case X:
			return point.getMaxX();
		case Y:
			return point.getMaxY();
		default:
			return point.getMaxZ();
		}
	}

	private static SimplePoint3D move(Direction direction, SimplePoint3D point, int coordinate) {
		switch(direction) {
		case X:
			return point.moveX(coordinate);
		case Y:
			return point.moveY(coordinate);
		default:
			return point.moveZ(coordinate);
		}
	}

	/** The test the calculator uses to drop a moved form: is the moved form of the first point eclipsing the moved form of the second point? */
	private static boolean eclipsesMoved(Direction direction, SimplePoint3D moved, SimplePoint3D other, int coordinate) {
		switch(direction) {
		case X:
			return moved.eclipsesMovedX(other, coordinate);
		case Y:
			return moved.eclipsesMovedY(other, coordinate);
		default:
			return moved.eclipsesMovedZ(other, coordinate);
		}
	}

	private static IntComparator comparator(Direction direction, Point3DFlagList values, int coordinate) {
		switch(direction) {
		case X: {
			CustomIntXComparator comparator = new CustomIntXComparator();
			comparator.setValues(values, coordinate);
			return comparator;
		}
		case Y: {
			CustomIntYComparator comparator = new CustomIntYComparator();
			comparator.setValues(values, coordinate);
			return comparator;
		}
		default: {
			CustomIntZComparator comparator = new CustomIntZComparator();
			comparator.setValues(values, coordinate);
			return comparator;
		}
		}
	}

	/** Sort as the calculator does */
	private static void sortAsCalculator(Direction direction, CustomIntArrayList indexes, IntComparator comparator) {
		switch(direction) {
		case X:
			indexes.insertionSortThis((CustomIntXComparator)comparator);
			break;
		case Y:
			indexes.insertionSortThis((CustomIntYComparator)comparator);
			break;
		default:
			indexes.insertionSortThis(comparator);
			break;
		}
	}

	private static boolean sameGeometry(SimplePoint3D a, SimplePoint3D b) {
		return a.getMinX() == b.getMinX() && a.getMinY() == b.getMinY() && a.getMinZ() == b.getMinZ() && a.getMaxX() == b.getMaxX() && a.getMaxY() == b.getMaxY()
				&& a.getMaxZ() == b.getMaxZ();
	}

	/** Points which cannot be told apart: the same coordinates and supporting planes */
	private static boolean sameContent(SimplePoint3D a, SimplePoint3D b) {
		DefaultPoint3D first = (DefaultPoint3D)a;
		DefaultPoint3D second = (DefaultPoint3D)b;
		return sameGeometry(a, b) && first.getXYPlane() == second.getXYPlane() && first.getXZPlane() == second.getXZPlane() && first.getYZPlane() == second.getYZPlane();
	}

	private static String content(SimplePoint3D point) {
		DefaultPoint3D p = (DefaultPoint3D)point;
		return p.getMinX() + "," + p.getMinY() + "," + p.getMinZ() + "-" + p.getMaxX() + "," + p.getMaxY() + "," + p.getMaxZ() + " xy=" + (p.getXYPlane() != null) + " xz="
				+ (p.getXZPlane() != null) + " yz=" + (p.getYZPlane() != null);
	}

	private static Point3DFlagList list(SimplePoint3D... points) {
		Point3DFlagList values = new Point3DFlagList(points.length + 1);
		for(SimplePoint3D point : points) {
			values.add(point);
		}
		return values;
	}

	@ParameterizedTest
	@EnumSource(Direction.class)
	public void movedFormWhichEclipsesAnotherIsFirst(Direction direction) {
		for(int scale : SCALES) {
			Random random = new Random(direction.ordinal() * 31 + scale);
			Planes planes = new Planes(scale);

			int eclipsing = 0;
			int identical = 0;
			for(int i = 0; i < CASES; i++) {
				// half of the pairs in a dense range, with many identical moved forms
				int range = i % 2 == 0 ? 6 : 3;
				SimplePoint3D a = point(random, range, scale, planes);
				SimplePoint3D b = point(random, range, scale, planes);

				// a coordinate which both can be moved to
				int low = Math.max(min(direction, a), min(direction, b)) + 1;
				int high = Math.min(max(direction, a), max(direction, b));
				if(low > high) {
					continue;
				}
				int coordinate = low + random.nextInt(high - low + 1);

				SimplePoint3D movedA = move(direction, a, coordinate);
				SimplePoint3D movedB = move(direction, b, coordinate);

				IntComparator comparator = comparator(direction, list(a, b), coordinate);
				int compare = comparator.compare(0, 1);
				assertThat(Integer.signum(comparator.compare(1, 0))).as("antisymmetric").isEqualTo(-Integer.signum(compare));

				if(eclipsesMoved(direction, movedA, b, coordinate)) {
					if(sameGeometry(movedA, movedB)) {
						identical++;
					} else {
						eclipsing++;
						final int moveTo = coordinate;
						assertThat(compare).as(() -> movedA + " eclipses " + movedB + " when moved to " + moveTo).isNegative();
					}
				}
			}
			// the test must not be vacuous
			assertThat(eclipsing).as("eclipsing pairs, scale " + scale).isGreaterThan(500);
			assertThat(identical).as("identical pairs, scale " + scale).isGreaterThan(20);
		}
	}

	@ParameterizedTest
	@EnumSource(Direction.class)
	public void orderIsTotalAndIndependentOfTheSortingAlgorithm(Direction direction) {
		for(int scale : SCALES) {
			Random random = new Random(direction.ordinal() * 17 + scale);
			Planes planes = new Planes(scale);

			for(int i = 0; i < 2000; i++) {
				// dense: many equal keys and exact duplicates
				int size = 2 + random.nextInt(40);
				SimplePoint3D[] points = new SimplePoint3D[size];
				for(int k = 0; k < size; k++) {
					points[k] = point(random, 3, scale, planes);
				}
				Point3DFlagList values = list(points);
				int coordinate = 1 + random.nextInt(3 * scale);
				IntComparator comparator = comparator(direction, values, coordinate);

				// points which compare equal cannot be told apart
				for(int a = 0; a < size; a++) {
					assertThat(comparator.compare(a, a)).isZero();
					for(int b = a + 1; b < size; b++) {
						int compare = comparator.compare(a, b);
						assertThat(Integer.signum(comparator.compare(b, a))).isEqualTo(-Integer.signum(compare));
						if(compare == 0) {
							assertThat(sameContent(points[a], points[b])).as("%s and %s compare equal", points[a], points[b]).isTrue();
						}
					}
				}

				// quicksort, the sort of the calculator, and each of them from a shuffled and from the reverse start order
				List<String> expected = null;
				for(int variant = 0; variant < 4; variant++) {
					int[] order = new int[size];
					for(int k = 0; k < size; k++) {
						order[k] = variant >= 2 ? size - 1 - k : k;
					}
					if(variant == 1 || variant == 3) {
						for(int k = size - 1; k > 0; k--) {
							int j = random.nextInt(k + 1);
							int swap = order[k];
							order[k] = order[j];
							order[j] = swap;
						}
					}
					CustomIntArrayList indexes = new CustomIntArrayList();
					for(int index : order) {
						indexes.add(index);
					}
					if(variant < 2) {
						indexes.sortThis(comparator);
					} else {
						sortAsCalculator(direction, indexes, comparator);
					}

					List<String> actual = new ArrayList<>();
					for(int k = 0; k < size; k++) {
						actual.add(content(points[indexes.get(k)]));
						// a total order: all later points compare larger or equal
						for(int j = k + 1; j < size; j++) {
							assertThat(comparator.compare(indexes.get(k), indexes.get(j))).isLessThanOrEqualTo(0);
						}
					}
					if(expected == null) {
						expected = actual;
					} else {
						assertThat(actual).as("variant %d", variant).isEqualTo(expected);
					}
				}
			}
		}
	}

	@Test
	public void volumeProxyDoesNotWrapForLargeContainers() {
		// dy * dz * maxX is 2,195,310,000 (more than an int) for the first point and 2,145,024,900 for the second, which it contains
		SimplePoint3D large = new DefaultPoint3D(0, 0, 0, 1299, 1299, 1299);
		SimplePoint3D small = new DefaultPoint3D(0, 0, 0, 1289, 1289, 1289);

		for(Direction direction : Direction.values()) {
			// the large point is moved to a coordinate in both
			IntComparator comparator = comparator(direction, list(large, small), 1000);
			assertThat(comparator.compare(0, 1)).as(direction.name()).isNegative();
			assertThat(comparator.compare(1, 0)).as(direction.name()).isPositive();
		}
	}

	@Test
	public void maxCoordinatesBreakTiesOfTheVolumeProxy() {
		// the same volume proxy dy * dz * maxX = 3 * 2 * 5 = 2 * 3 * 5, so the max coordinates decide: larger max y first
		SimplePoint3D tall = new DefaultPoint3D(0, 0, 0, 5, 2, 1);
		SimplePoint3D wide = new DefaultPoint3D(0, 0, 0, 5, 1, 2);
		CustomIntXComparator x = new CustomIntXComparator();
		x.setValues(list(tall, wide), 3);
		assertThat(x.compare(0, 1)).isNegative();
		assertThat(x.compare(1, 0)).isPositive();
	}

	@Test
	public void geometricallyIdenticalMovedFormsRichestSupportsFirst() {
		Placement xy = placement(10, 10, 1, 0, 0, 0);
		Placement xz = placement(10, 1, 10, 0, 0, 0);
		Placement yz = placement(1, 10, 10, 0, 0, 0);
		// ends at x = 1
		Placement shortXy = placement(2, 10, 1, 0, 0, 0);
		Placement shortXz = placement(2, 1, 10, 0, 0, 0);

		SimplePoint3D none = new DefaultPoint3D(0, 0, 0, 9, 9, 9);
		SimplePoint3D onlyXz = new DefaultPoint3D(0, 0, 0, 9, 9, 9, null, xz, null);
		SimplePoint3D onlyXy = new DefaultPoint3D(0, 0, 0, 9, 9, 9, null, null, xy);
		SimplePoint3D both = new DefaultPoint3D(0, 0, 0, 9, 9, 9, null, xz, xy);
		SimplePoint3D lostXz = new DefaultPoint3D(0, 0, 0, 9, 9, 9, null, shortXz, null);
		SimplePoint3D onlyYz = new DefaultPoint3D(0, 0, 0, 9, 9, 9, yz, null, null);
		SimplePoint3D shortXyOnly = new DefaultPoint3D(0, 0, 0, 9, 9, 9, null, null, shortXy);

		// moved to x = 5: the short planes are lost, the others are kept; the yz plane is always lost
		Point3DFlagList values = list(none, onlyXz, onlyXy, both, lostXz, onlyYz, shortXyOnly);
		CustomIntXComparator comparator = new CustomIntXComparator();
		comparator.setValues(values, 5);

		CustomIntArrayList indexes = new CustomIntArrayList();
		for(int i = 0; i < values.size(); i++) {
			indexes.add(i);
		}
		indexes.insertionSortThis(comparator);

		// both planes, xy only, xz only; then those which moved without any plane: by the supports of the source point
		assertThat(indexes.toArray()).containsExactly(3, 2, 1, 6, 4, 5, 0);

		// moved to x = 1: the short planes are kept, and the xy plane is worth more than the xz plane
		comparator.setValues(values, 1);
		indexes.clear();
		indexes.addAll(1, 6);
		indexes.insertionSortThis(comparator);
		assertThat(indexes.toArray()).containsExactly(6, 1);
		// moved to x = 5: the short xy plane is lost, the xz plane is kept
		comparator.setValues(values, 5);
		indexes.clear();
		indexes.addAll(6, 1);
		indexes.insertionSortThis(comparator);
		assertThat(indexes.toArray()).containsExactly(1, 6);
	}

	@Test
	public void nonOverlappingPointsOrderByMinimumCornerFirst() {
		// the leading keys of the earlier order are kept: min y, then min z, then larger volume proxy
		SimplePoint3D lowY = new DefaultPoint3D(0, 0, 5, 9, 9, 9);
		SimplePoint3D highY = new DefaultPoint3D(0, 1, 0, 9, 9, 9);
		SimplePoint3D lowZ = new DefaultPoint3D(0, 0, 1, 9, 9, 9);
		CustomIntXComparator x = new CustomIntXComparator();
		x.setValues(list(lowY, highY, lowZ), 3);
		assertThat(x.compare(0, 1)).isNegative();
		assertThat(x.compare(2, 0)).isNegative();
		assertThat(x.compare(2, 1)).isNegative();
	}
}
