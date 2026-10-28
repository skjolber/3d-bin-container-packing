package com.github.skjolber.packing.packer.bruteforce.flat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

/** The permutation steps and the prefixes of the flat problem must be those of the iterator. */
class FlatPermutationTest {

	private static DefaultBoxItemPermutationRotationIterator iterator(Random random, int boxes) {
		List<BoxItem> boxItems = new ArrayList<>();
		int remaining = boxes;
		int id = 0;
		while (remaining > 0) {
			int count = Math.min(remaining, 1 + random.nextInt(3));
			remaining -= count;
			boxItems.add(new BoxItem(Box.newBuilder().withId(Integer.toString(id++)).withSize(1 + random.nextInt(4), 1 + random.nextInt(4), 1 + random.nextInt(4)).withWeight(1).build(), count));
		}
		return DefaultBoxItemPermutationRotationIterator.newBuilder().withLoadSize(10, 10, 10).withBoxItems(boxItems).withMaxLoadWeight(1000).build();
	}

	@Test
	void nextPermutationIsTheIteratorsForEveryMaxIndex() {
		long steps = 0;
		for (int seed = 0; seed < 600; seed++) {
			Random random = new Random(seed);
			DefaultBoxItemPermutationRotationIterator iterator = iterator(random, 3 + random.nextInt(6));
			int n = iterator.length();
			FlatProblem problem = FlatProblem.of(iterator, 10, 10, 10);

			int[] permutation = new int[n + 2];
			FlatPermutation.first(problem.ints(), permutation, 2);
			assertThat(Arrays.copyOfRange(permutation, 2, n + 2)).as("seed " + seed).containsExactly(iterator.getPermutations());

			while (true) {
				// mostly a small max index, so that few permutations are skipped, and sometimes any
				int maxIndex = random.nextInt(4) == 0 ? random.nextInt(n) : Math.min(n - 1, n - 1 - random.nextInt(2));
				int expected = iterator.nextPermutation(maxIndex);
				int actual = FlatPermutation.next(permutation, 2, n, maxIndex);
				assertThat(actual).as("seed " + seed + ", max index " + maxIndex).isEqualTo(expected);
				assertThat(Arrays.copyOfRange(permutation, 2, n + 2)).as("seed " + seed + ", max index " + maxIndex).containsExactly(iterator.getPermutations());
				steps++;
				if(expected == -1) {
					break;
				}
			}
		}
		System.out.println("flat permutation: " + steps + " steps");
		assertThat(steps).isGreaterThan(10_000);
	}

	@Test
	void prefixesPartitionTheSpaceInOrder() {
		for (int seed = 0; seed < 200; seed++) {
			Random random = new Random(seed);
			DefaultBoxItemPermutationRotationIterator iterator = iterator(random, 3 + random.nextInt(4));
			int n = iterator.length();
			FlatProblem problem = FlatProblem.of(iterator, 10, 10, 10);

			for (int depth = 0; depth <= n; depth++) {
				int[] prefixes = FlatPermutation.prefixes(problem.ints(), depth, 1_000_000);
				int items = prefixes.length / n;

				int item = -1;
				Set<String> seen = new HashSet<>();
				iterator.reset();
				do {
					int[] permutation = iterator.getPermutations();
					String prefix = Arrays.toString(Arrays.copyOf(permutation, depth));
					if(seen.add(prefix)) {
						// the first permutation of a new prefix starts the next item
						item++;
						assertThat(item).as("seed " + seed + ", depth " + depth).isLessThan(items);
						assertThat(Arrays.copyOfRange(prefixes, item * n, (item + 1) * n)).as("seed " + seed + ", depth " + depth + ", item " + item).containsExactly(permutation);
					} else {
						assertThat(Arrays.copyOf(permutation, depth)).as("seed " + seed + ", depth " + depth + ", prefix is of the current item")
								.containsExactly(Arrays.copyOfRange(prefixes, item * n, item * n + depth));
					}
				} while (iterator.nextPermutation() != -1);
				assertThat(item + 1).as("seed " + seed + ", depth " + depth).isEqualTo(items);
			}
		}
	}

	@Test
	void prefixesAreLimitedAndDepthIsChosen() {
		Random random = new Random(7);
		DefaultBoxItemPermutationRotationIterator iterator = iterator(random, 7);
		int n = iterator.length();
		int[] problem = FlatProblem.of(iterator, 10, 10, 10).ints();

		int[] all = FlatPermutation.prefixes(problem, n, 1_000_000);
		int permutations = all.length / n;
		assertThat(FlatPermutation.prefixes(problem, n, permutations - 1)).isNull();
		assertThat(FlatPermutation.prefixes(problem, n, permutations)).hasSize(all.length);

		for (int minItems = 1; minItems <= permutations; minItems *= 3) {
			int depth = FlatPermutation.depth(problem, minItems, 1_000_000);
			assertThat(FlatPermutation.prefixes(problem, depth, 1_000_000).length / n).as("min items " + minItems).isGreaterThanOrEqualTo(Math.min(minItems, permutations));
			if(depth > 1) {
				assertThat(FlatPermutation.prefixes(problem, depth - 1, 1_000_000).length / n).as("min items " + minItems).isLessThan(minItems);
			}
		}
	}
}
