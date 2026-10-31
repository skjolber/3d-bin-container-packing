package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.bruteforce.FastPointCalculator3DStack;

/**
 * The best point for a box size on random free point lists (loaded from the point calculator) must be the one which the fast brute force packager picks,
 * including where the point comparator is not symmetric.
 */
class FlatBestPointTest {

	private final FlatSearchOracle oracle = new FlatSearchOracle();

	@Test
	void bestPointMatchesThePackager() {
		long checks = 0;
		long contested = 0;
		for (int seed = 0; seed < 3000; seed++) {
			Random random = new Random(seed);

			int cx = 2 + random.nextInt(9);
			int cy = 2 + random.nextInt(9);
			int cz = 1 + random.nextInt(9);
			int n = 3 + random.nextInt(8);

			FlatLayout layout = new FlatLayout(n, 512);
			int[] s = layout.newScratch();
			int frame = layout.frameOffset(0);

			FastPointCalculator3DStack calculator = new FastPointCalculator3DStack(64);
			calculator.clearToSize(cx, cy, cz);
			if(random.nextInt(3) == 0) {
				calculator.setMinimumAreaAndVolumeLimit(1 + random.nextInt(4), 1 + random.nextInt(8));
			}

			int level = 0;
			for (int step = 0; step < 40 && calculator.size() > 0; step++) {
				load(s, frame, calculator);
				for (int probe = 0; probe < 6; probe++) {
					SimplePoint3D point = calculator.get(random.nextInt(calculator.size()));
					int dx = random.nextInt(5) == 0 ? 1 + random.nextInt(cx) : 1 + random.nextInt(point.getDx());
					int dy = random.nextInt(5) == 0 ? 1 + random.nextInt(cy) : 1 + random.nextInt(point.getDy());
					int dz = random.nextInt(5) == 0 ? 1 + random.nextInt(cz) : 1 + random.nextInt(point.getDz());
					BoxStackValue stackValue = Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build().getStackValue(0);

					int expected = oracle.bestPoint(calculator, stackValue);
					assertThat(FlatSearch.bestPoint(s, frame, dx, dy, dz)).as("seed " + seed + ", step " + step + ", size " + dx + "x" + dy + "x" + dz).isEqualTo(expected);
					checks++;

					int fits = 0;
					for (int i = 0; i < calculator.size(); i++) {
						if(calculator.get(i).fits3D(stackValue)) {
							fits++;
						}
					}
					if(fits > 1) {
						contested++;
					}
				}

				if(level < n && random.nextInt(10) < 8) {
					int index = random.nextInt(calculator.size());
					SimplePoint3D point = calculator.get(index);
					int dx = random.nextInt(3) == 0 ? point.getDx() : 1 + random.nextInt(point.getDx());
					int dy = random.nextInt(3) == 0 ? point.getDy() : 1 + random.nextInt(point.getDy());
					int dz = random.nextInt(3) == 0 ? point.getDz() : 1 + random.nextInt(point.getDz());
					BoxStackValue stackValue = Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build().getStackValue(0);
					calculator.add(index, new Placement(stackValue, -1, point.getMinX(), point.getMinY(), point.getMinZ(), false));
					level++;
				} else if(level > 0) {
					level = random.nextInt(level);
					calculator.setStackSize(level);
				}
			}
		}
		System.out.println("flat best point: " + checks + " checks, " + contested + " with more than one candidate");
		assertThat(checks).isGreaterThan(50_000);
		assertThat(contested).isGreaterThan(10_000);
	}

	private static void load(int[] s, int frame, FastPointCalculator3DStack calculator) {
		s[frame] = calculator.size();
		for (int i = 0; i < calculator.size(); i++) {
			SimplePoint3D point = calculator.get(i);
			int o = frame + 1 + i * FlatLayout.POINT_INTS;
			s[o] = point.getMinX();
			s[o + 1] = point.getMinY();
			s[o + 2] = point.getMinZ();
			s[o + 3] = point.getMaxX();
			s[o + 4] = point.getMaxY();
			s[o + 5] = point.getMaxZ();
			s[o + 6] = 0;
		}
	}
}
