package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * The work units are created when requested, from the remaining boxes: the order in which they are requested, and
 * whether boxes were removed before, does not change which permutations they iterate.
 */
public class ParallelBoxItemPermutationRotationIteratorListTest {

	private static List<BoxItem> boxItems(int... counts) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < counts.length; i++) {
			boxItems.add(new BoxItem(Box.newBuilder().withId("box-" + i).withSize(1, 2 + i, 1).withWeight(1).build(), counts[i]));
		}
		return boxItems;
	}

	private static ParallelBoxItemPermutationRotationIteratorList list(int parallelizationCount, int... counts) {
		return ParallelBoxItemPermutationRotationIteratorList.newBuilder()
				.withLoadSize(10, 30, 1)
				.withMaxLoadWeight(100)
				.withBoxItems(boxItems(counts))
				.withParallelizationCount(parallelizationCount)
				.build();
	}

	/**
	 * @return the permutations of the work unit, from its first permutation to its last
	 */
	private static List<String> permutations(ParallelBoxItemPermutationRotationIterator workUnit) {
		List<String> permutations = new ArrayList<>();
		do {
			permutations.add(Arrays.toString(workUnit.getPermutations()));
		} while (workUnit.nextPermutation() != -1);
		return permutations;
	}

	/**
	 * @param units the number of work units
	 * @param order the order in which to request (and iterate) the work units
	 * @return the permutations of each work unit
	 */
	private static List<List<String>> permutations(ParallelBoxItemPermutationRotationIteratorList list, int units, int... order) {
		List<List<String>> result = new ArrayList<>();
		for (int i = 0; i < units; i++) {
			result.add(null);
		}
		for (int i : order) {
			result.set(i, permutations(list.getIterator(i)));
		}
		return result;
	}

	@Test
	public void workUnitsAreTheSameInAnyOrderOfRequest() {
		// 4 boxes of 3 kinds: 4! / 2! = 12 permutations
		List<List<String>> inOrder = permutations(list(4, 2, 1, 1), 4, 0, 1, 2, 3);
		List<List<String>> reversed = permutations(list(4, 2, 1, 1), 4, 3, 2, 1, 0);
		List<List<String>> mixed = permutations(list(4, 2, 1, 1), 4, 2, 0, 3, 1);

		assertThat(reversed).isEqualTo(inOrder);
		assertThat(mixed).isEqualTo(inOrder);
		assertThat(inOrder).flatExtracting(permutations -> permutations).hasSize(12).doesNotHaveDuplicates();
	}

	@Test
	public void workUnitIsCreatedOnce() {
		ParallelBoxItemPermutationRotationIteratorList list = list(4, 1, 1, 1);

		assertThat(list.getIterator(2)).isSameAs(list.getIterator(2));
		assertThat(list.getIterators()).hasSize(4).doesNotContainNull();
		assertThat(list.getIterators()[2]).isSameAs(list.getIterator(2));
	}

	@Test
	public void workUnitsOnlyHoldTheRemainingBoxesAfterRemoval() {
		ParallelBoxItemPermutationRotationIteratorList list = list(3, 2, 1, 1);

		// one work unit existed before the removal
		ParallelBoxItemPermutationRotationIterator before = list.getIterator(1);
		assertThat(before.length()).isEqualTo(4);

		list.removePermutations(List.of(0));

		// 3 boxes of 3 kinds: 3! = 6 permutations; each of the work units is created for the remaining boxes
		for (ParallelBoxItemPermutationRotationIterator workUnit : list.getIterators()) {
			assertThat(workUnit.length()).isEqualTo(3);
		}
		assertThat(list.getIterator(1)).isNotSameAs(before);

		List<List<String>> permutations = permutations(list, 3, 2, 1, 0);
		assertThat(permutations).flatExtracting(p -> p).hasSize(6).doesNotHaveDuplicates();
		assertThat(permutations).isEqualTo(permutations(list(3, 1, 1, 1), 3, 0, 1, 2));
	}

	@Test
	public void forkDoesNotShareWorkUnits() {
		ParallelBoxItemPermutationRotationIteratorList source = list(2, 2, 1);
		ParallelBoxItemPermutationRotationIterator sourceUnit = source.getIterator(0);

		ParallelBoxItemPermutationRotationIteratorList fork = source.fork();
		assertThat(fork.getIterator(0)).isNotSameAs(sourceUnit);
		assertThat(permutations(fork.getIterator(0))).isEqualTo(permutations(sourceUnit));
	}

	@Test
	public void resetOnlyAffectsTheWorkUnitsWhichExist() {
		ParallelBoxItemPermutationRotationIteratorList list = list(3, 1, 1, 1);

		// no work units: nothing to reset
		list.reset();

		List<String> first = permutations(list.getIterator(1));
		list.reset();
		assertThat(permutations(list.getIterator(1))).isEqualTo(first);
	}

	@Test
	public void workUnitsFromAnIteratorAreTheSameAsFromTheBuilder() {
		DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 30, 1)
				.withMaxLoadWeight(100)
				.withBoxItems(boxItems(2, 1, 1))
				.build();

		ParallelBoxItemPermutationRotationIteratorList fromIterator = ParallelBoxItemPermutationRotationIteratorList.of(iterator, 4);

		assertThat(permutations(fromIterator, 4, 0, 1, 2, 3)).isEqualTo(permutations(list(4, 2, 1, 1), 4, 0, 1, 2, 3));

		// the list has its own boxes
		iterator.removePermutations(2);
		assertThat(iterator.length()).isEqualTo(2);
		assertThat(fromIterator.countPermutations()).isEqualTo(12);
		fromIterator.reset();
		assertThat(permutations(fromIterator, 4, 0, 1, 2, 3)).isEqualTo(permutations(list(4, 2, 1, 1), 4, 0, 1, 2, 3));
	}

	@Test
	public void tooManyPermutationsAreRejectedWhenSplitting() {
		// 21! does not fit in a long
		int[] counts = new int[21];
		Arrays.fill(counts, 1);
		assertThatThrownBy(() -> list(2, counts)).isInstanceOf(IllegalArgumentException.class);
	}
}
