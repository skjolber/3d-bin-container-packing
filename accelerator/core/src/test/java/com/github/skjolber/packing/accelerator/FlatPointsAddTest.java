package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.bruteforce.FastPointCalculator3DStack;

/**
 * Random placement sequences with backtracking, on the flat points and on the point calculator which the brute force packager uses: the free points
 * must be the same, in the same order and with the same supporting planes, after every add and every restore.
 */
class FlatPointsAddTest {

	private static final int P_MAX = 512;

	@Test
	void randomWalksMatchTheCalculator() {
		int walks = 0;
		long adds = 0;
		for (int seed = 0; seed < 4000; seed++) {
			adds += walk(seed);
			walks++;
		}
		assertThat(walks).isEqualTo(4000);
		assertThat(adds).isGreaterThan(20000);
	}

	@Test
	void randomWalksWithoutLimitsMatchTheCalculator() {
		long adds = 0;
		for (int seed = 10000; seed < 12000; seed++) {
			adds += walk(seed, false);
		}
		assertThat(adds).isGreaterThan(10000);
	}

	@Test
	void twoDimensionalContainersMatchTheCalculator() {
		long adds = 0;
		for (int seed = 20000; seed < 22000; seed++) {
			adds += walk(seed, true, true);
		}
		assertThat(adds).isGreaterThan(5000);
	}

	@Test
	void tinyFramesReportOverflow() {
		FlatLayout layout = new FlatLayout(3, 1);
		int[] s = layout.newScratch();
		FlatPoints.clear(layout, s, 0, 4, 4, 4);
		FlatPoints.setPlacement(layout, s, 0, 0, 0, 0, 0, 0, 0, 0);
		// one box in a corner of the container needs three free points
		assertThat(FlatPoints.add(layout, s, 0, 0, 0, 0, 0, 0)).isEqualTo(-1);
		assertThat(FlatPoints.isOverflow(layout, s, 0)).isTrue();
	}

	private long walk(int seed) {
		return walk(seed, true);
	}

	private long walk(int seed, boolean limits) {
		return walk(seed, limits, false);
	}

	private long walk(int seed, boolean limits, boolean flat) {
		Random random = new Random(seed);

		int cx = 2 + random.nextInt(9);
		int cy = 2 + random.nextInt(9);
		int cz = flat ? 1 : 1 + random.nextInt(9);
		int n = 3 + random.nextInt(8);

		FlatLayout layout = new FlatLayout(n, P_MAX);
		int[] s = layout.newScratch();

		long[] minArea = new long[n + 1];
		long[] minVolume = new long[n + 1];
		if(limits && random.nextInt(4) != 0) {
			long area = Long.MAX_VALUE;
			long volume = Long.MAX_VALUE;
			for (int i = n - 1; i >= 0; i--) {
				int bx = 1 + random.nextInt(Math.max(1, cx / 2));
				int by = 1 + random.nextInt(Math.max(1, cy / 2));
				int bz = flat ? 1 : 1 + random.nextInt(Math.max(1, cz / 2));
				area = Math.min(area, (long)bx * by);
				volume = Math.min(volume, (long)bx * by * bz);
				minArea[i] = area;
				minVolume[i] = volume;
			}
			minArea[n] = minArea[n - 1];
			minVolume[n] = minVolume[n - 1];
		}

		FastPointCalculator3DStack calculator = new FastPointCalculator3DStack(64);
		calculator.clearToSize(cx, cy, cz);
		Placement[] placed = new Placement[n + 1];
		placed[n] = ((DefaultPoint3D)calculator.get(0)).getXYPlane();

		FlatPoints.clear(layout, s, 0, cx, cy, cz);

		calculator.setMinimumAreaAndVolumeLimit(minArea[0], minVolume[0]);
		if(minArea[0] != 0 || minVolume[0] != 0) {
			FlatPoints.filterMinimums(layout, s, 0, 0, minArea[0], minVolume[0]);
		}
		assertSame(layout, s, 0, calculator, placed, seed, "start");

		long adds = 0;
		int level = 0;
		for (int step = 0; step < 120; step++) {
			int size = calculator.size();
			if(level < n && size > 0 && random.nextInt(10) < 7) {
				int index = random.nextInt(size);
				SimplePoint3D point = calculator.get(index);

				int dx = random.nextInt(3) == 0 ? point.getDx() : 1 + random.nextInt(point.getDx());
				int dy = random.nextInt(3) == 0 ? point.getDy() : 1 + random.nextInt(point.getDy());
				int dz = random.nextInt(3) == 0 ? point.getDz() : 1 + random.nextInt(point.getDz());

				BoxStackValue stackValue = Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build().getStackValue(0);
				assertThat(new int[] { stackValue.getDx(), stackValue.getDy(), stackValue.getDz() }).containsExactly(dx, dy, dz);

				Placement placement = new Placement(stackValue, -1, point.getMinX(), point.getMinY(), point.getMinZ(), false);
				placed[level] = placement;

				FlatPoints.setPlacement(layout, s, 0, level, point.getMinX(), point.getMinY(), point.getMinZ(), point.getMinX() + dx - 1, point.getMinY() + dy - 1,
						point.getMinZ() + dz - 1);
				int count = FlatPoints.add(layout, s, 0, level, index, level, minArea[level], minVolume[level]);
				assertThat(count).as("overflow, seed " + seed).isNotNegative();

				calculator.add(index, placement);
				calculator.setMinimumAreaAndVolumeLimit(minArea[level + 1], minVolume[level + 1]);
				if(minArea[level + 1] != minArea[level] || minVolume[level + 1] != minVolume[level]) {
					FlatPoints.filterMinimums(layout, s, 0, level + 1, minArea[level + 1], minVolume[level + 1]);
				}
				level++;
				adds++;
				assertSame(layout, s, level, calculator, placed, seed, "add " + adds);
			} else if(level > 0) {
				level = random.nextInt(4) == 0 ? random.nextInt(level) : level - 1;
				calculator.setStackSize(level);
				assertSame(layout, s, level, calculator, placed, seed, "restore to " + level);
			} else if(size == 0) {
				break;
			}
		}
		assertThat(FlatPoints.isOverflow(layout, s, 0)).isFalse();
		return adds;
	}

	private static void assertSame(FlatLayout layout, int[] s, int level, FastPointCalculator3DStack calculator, Placement[] placed, int seed, String step) {
		List<String> expected = new ArrayList<>();
		for (int i = 0; i < calculator.size(); i++) {
			DefaultPoint3D p = (DefaultPoint3D)calculator.get(i);
			expected.add(p.getMinX() + "," + p.getMinY() + "," + p.getMinZ() + " " + p.getMaxX() + "," + p.getMaxY() + "," + p.getMaxZ() + " yz=" + id(placed, p.getYZPlane())
					+ " xz=" + id(placed, p.getXZPlane()) + " xy=" + id(placed, p.getXYPlane()));
		}

		List<String> actual = new ArrayList<>();
		int count = FlatPoints.count(layout, s, 0, level);
		for (int i = 0; i < count; i++) {
			int o = FlatPoints.pointOffset(layout, 0, level, i);
			int planes = s[o + FlatGeometry.PLANES];
			actual.add(s[o] + "," + s[o + 1] + "," + s[o + 2] + " " + s[o + 3] + "," + s[o + 4] + "," + s[o + 5] + " yz=" + plane(FlatLayout.yz(planes)) + " xz="
					+ plane(FlatLayout.xz(planes)) + " xy=" + plane(FlatLayout.xy(planes)));
		}
		assertThat(actual).as("seed " + seed + ", " + step + ", level " + level).isEqualTo(expected);
	}

	private static String plane(int id) {
		return id == FlatLayout.PLANE_NONE ? "-" : Integer.toString(id);
	}

	private static String id(Placement[] placed, Placement plane) {
		if(plane == null) {
			return "-";
		}
		for (int i = 0; i < placed.length; i++) {
			if(placed[i] == plane) {
				return Integer.toString(i);
			}
		}
		return "?";
	}
}
