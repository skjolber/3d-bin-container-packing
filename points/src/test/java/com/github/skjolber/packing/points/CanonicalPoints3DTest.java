package com.github.skjolber.packing.points;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The coverage check which {@linkplain PointCalculatorVersion4ComparisonTest} and its IT rely on must itself be exact: a free point is covered if it is within the union of the
 * other points, whatever way they tile it.
 */
public class CanonicalPoints3DTest {

	/**
	 * A point of 2x2 cells is tiled by two points, neither of which contains it:
	 *
	 * <pre>
	 *   y
	 *   1  [ A A ]        A: x 3..5, y 1      (the cell x 3 is outside the point)
	 *   0  [ B B B B ]    B: x 4..10, y 0
	 *        4 5
	 * </pre>
	 */
	@Test
	public void pointIsCoveredByTheUnionOfTwoPoints() {
		int[][] cover = { row(3, 1, 7, 5, 1, 7), row(4, 0, 7, 10, 0, 7) };
		int[] point = row(4, 0, 7, 5, 1, 7);

		assertThat(CanonicalPoints3D.remainder(point, cover)).isEmpty();
		assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 0, 0)).isNull();
	}

	/**
	 * One cell of the point is in no other point (and the point is not within any single one of them).
	 */
	@Test
	public void missingCellIsFound() {
		int[][] cover = { row(6, 16, 14, 6, 16, 15), row(7, 17, 14, 7, 17, 14) };
		int[] point = row(6, 16, 14, 6, 17, 14);

		List<int[]> remainder = CanonicalPoints3D.remainder(point, cover);
		assertThat(remainder).hasSize(1);
		assertThat(remainder.get(0)).containsExactly(6, 17, 14, 6, 17, 14);

		assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 0, 0)).isSameAs(point);
		assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 2, 2)).isSameAs(point);
	}

	/**
	 * A point below the limits can hold no remaining box, and is not checked: the area is the size in x times the size in y, the volume is that times the size in z.
	 */
	@Test
	public void pointsBelowTheLimitsAreNotChecked() {
		int[][] cover = { row(6, 16, 14, 6, 16, 15) };
		int[] point = row(6, 16, 14, 6, 17, 14); // 1 x 2 x 1: area 2, volume 2

		assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 3, 0)).isNull();
		assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 0, 3)).isNull();
		assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 2, 2)).isSameAs(point);
		assertThat(CanonicalPoints3D.isBelowLimits(point, 2, 2)).isFalse();
		assertThat(CanonicalPoints3D.isBelowLimits(point, 3, 2)).isTrue();
		assertThat(CanonicalPoints3D.isBelowLimits(point, 2, 3)).isTrue();
		// the area of 1000 x 1000 x 1000 must not wrap
		assertThat(CanonicalPoints3D.isBelowLimits(row(0, 0, 0, 999999, 999999, 999999), 1_000_000_000_000L, 1_000_000_000_000_000_000L)).isFalse();
	}

	/**
	 * The remainder is exactly the cells of the point which no point of the cover has, as boxes which do not overlap.
	 */
	@Test
	public void remainderIsExactlyTheUncoveredCells() {
		Random random = new Random(42);
		int covered = 0;
		int uncovered = 0;
		for(int i = 0; i < 5000; i++) {
			int[] point = randomBox(random, 5, 1);
			int[][] cover = new int[random.nextInt(8)][];
			for(int j = 0; j < cover.length; j++) {
				cover[j] = randomBox(random, 5, 3);
			}

			Set<Integer> expected = new HashSet<>();
			for(int x = point[0]; x <= point[3]; x++) {
				for(int y = point[1]; y <= point[4]; y++) {
					for(int z = point[2]; z <= point[5]; z++) {
						boolean found = false;
						for(int[] other : cover) {
							found |= other[0] <= x && x <= other[3] && other[1] <= y && y <= other[4] && other[2] <= z && z <= other[5];
						}
						if(!found) {
							expected.add(cell(x, y, z));
						}
					}
				}
			}

			Set<Integer> actual = new HashSet<>();
			int cells = 0;
			for(int[] part : CanonicalPoints3D.remainder(point, cover)) {
				assertThat(part).hasSize(6);
				assertThat(part[0]).isLessThanOrEqualTo(part[3]);
				assertThat(part[1]).isLessThanOrEqualTo(part[4]);
				assertThat(part[2]).isLessThanOrEqualTo(part[5]);
				for(int x = part[0]; x <= part[3]; x++) {
					for(int y = part[1]; y <= part[4]; y++) {
						for(int z = part[2]; z <= part[5]; z++) {
							actual.add(cell(x, y, z));
							cells++;
						}
					}
				}
			}
			assertThat(actual).isEqualTo(expected);
			assertThat(cells).as("the parts overlap").isEqualTo(expected.size());

			assertThat(CanonicalPoints3D.firstUncovered(new int[][] { point }, cover, 0, 0) == null).isEqualTo(expected.isEmpty());
			if(expected.isEmpty()) {
				covered++;
			} else {
				uncovered++;
			}
		}
		// not vacuous
		assertThat(covered).isGreaterThan(100);
		assertThat(uncovered).isGreaterThan(100);
	}

	private static int cell(int x, int y, int z) {
		return (x + 10) * 10000 + (y + 10) * 100 + (z + 10);
	}

	private static int[] randomBox(Random random, int size, int smallest) {
		int minX = random.nextInt(size);
		int minY = random.nextInt(size);
		int minZ = random.nextInt(size);
		return row(minX, minY, minZ, minX + random.nextInt(size - minX + smallest), minY + random.nextInt(size - minY + smallest), minZ + random.nextInt(size - minZ + smallest));
	}

	/**
	 * @return a point without supports
	 */
	private static int[] row(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new int[] { minX, minY, minZ, maxX, maxY, maxZ, 0, 0, 0, 0, 0, 0 };
	}
}
