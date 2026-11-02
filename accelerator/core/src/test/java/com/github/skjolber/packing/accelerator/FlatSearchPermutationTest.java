package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

/**
 * For random boxes, the search of every permutation (and a random number of leading boxes) on the flat points must give the same count, load volume and
 * rotations as the search of the fast brute force packager.
 */
class FlatSearchPermutationTest {

	private static final int P_MAX = 256;

	private final FlatSearchOracle oracle = new FlatSearchOracle();

	@Test
	void everyPermutationMatchesTheObjectSearch() {
		long searches = 0;
		long placed = 0;
		long fullyPlaced = 0;
		int maxPoints = 0;
		for (int seed = 0; seed < 600; seed++) {
			long[] result = run(seed);
			searches += result[0];
			placed += result[1];
			fullyPlaced += result[2];
			maxPoints = (int)Math.max(maxPoints, result[3]);
		}
		System.out.println("flat search: " + searches + " searches, " + placed + " boxes placed, " + fullyPlaced + " permutations fully placed, at most " + maxPoints
				+ " free points");
		assertThat(searches).isGreaterThan(100_000);
		// both the arrangements which place all boxes and those which end early (with backtracking) must be well covered
		assertThat(fullyPlaced).isGreaterThan(searches / 10);
		assertThat(fullyPlaced).isLessThan(searches * 6 / 10);
	}

	/** @return a size between 1 and the max, biased towards the max so that the boxes do not all fit */
	private static int size(Random random, int max) {
		return Math.max(1 + random.nextInt(max), 1 + random.nextInt(max));
	}

	private long[] run(int seed) {
		Random random = new Random(seed);

		int cx = 3 + random.nextInt(6);
		int cy = 3 + random.nextInt(6);
		int cz = 3 + random.nextInt(6);
		int max = Math.min(cx, Math.min(cy, cz));

		List<BoxItem> boxItems = new ArrayList<>();
		int remaining = 6 + random.nextInt(2);
		int itemCount = 2 + random.nextInt(4);
		for (int i = 0; i < itemCount && remaining > 0; i++) {
			int count = Math.min(remaining, 1 + random.nextInt(3));
			remaining -= count;
			Box.Builder builder = Box.newBuilder().withId(Integer.toString(i)).withSize(size(random, max), size(random, max), size(random, max)).withWeight(1);
			int rotation = random.nextInt(3);
			if(rotation == 1) {
				builder.withRotate2D();
			} else if(rotation == 2) {
				builder.withRotate3D();
			}
			boxItems.add(new BoxItem(builder.build(), count));
		}

		Container container = Container.newBuilder().withId("c").withEmptyWeight(1).withSize(cx, cy, cz).withMaxLoadWeight(1000).build();
		DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(cx, cy, cz)
				.withBoxItems(boxItems)
				.withMaxLoadWeight(1000)
				.build();

		int n = iterator.length();
		FlatProblem problem = FlatProblem.of(iterator, cx, cy, cz);
		FlatLayout layout = new FlatLayout(n, P_MAX);
		int[] s = layout.newScratch();
		long[] sl = new long[layout.longSize];

		long searches = 0;
		long placed = 0;
		long fullyPlaced = 0;
		int maxPoints = 0;
		do {
			int[] permutation = iterator.getPermutations();
			for (int i = 0; i < n; i++) {
				s[layout.perm + i] = permutation[i];
			}
			for (int variant = 0; variant < 2; variant++) {
				int maxPackableCount = variant == 0 ? n : 1 + random.nextInt(n);
				FlatSearchOracle.Search expected = oracle.search(iterator, container, maxPackableCount);

				int count = FlatSearch.searchPermutation(layout, problem.ints(), problem.longs(), s, sl, 0, 0, maxPackableCount);
				String message = "seed " + seed + ", permutation " + java.util.Arrays.toString(permutation) + ", count " + maxPackableCount;
				assertThat(count).as(message).isEqualTo(expected.count);
				assertThat(sl[layout.bestVolume]).as(message).isEqualTo(expected.loadVolume);
				for (int i = 0; i < count; i++) {
					assertThat(s[layout.bestRotation + i]).as(message + ", rotation " + i).isEqualTo(expected.rotations[i]);
				}
				assertThat(FlatPoints.isOverflow(layout, s, 0)).isFalse();
				for (int i = 0; i < n; i++) {
					assertThat(sl[layout.minVolume + i]).as(message + ", minimum volume " + i).isEqualTo(iterator.getMinBoxVolume(i));
				}
				searches++;
				placed += count;
				if(count == maxPackableCount) {
					fullyPlaced++;
				}
				maxPoints = Math.max(maxPoints, s[layout.control + FlatLayout.C_MAX_POINTS]);
			}
		} while (iterator.nextPermutation() != -1);

		return new long[] { searches, placed, fullyPlaced, maxPoints };
	}
}
