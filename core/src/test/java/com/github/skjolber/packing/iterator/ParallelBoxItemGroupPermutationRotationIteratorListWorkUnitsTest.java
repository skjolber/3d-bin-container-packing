package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * The work units are created when requested, from the remaining boxes: the order in which they are requested, and
 * whether boxes were removed before, does not change which permutations they iterate. Like
 * {@linkplain ParallelBoxItemPermutationRotationIteratorListTest}, for box item groups (whose work units are chained: each ends at
 * the first permutation of the next).
 */
public class ParallelBoxItemGroupPermutationRotationIteratorListWorkUnitsTest {

	private static BoxItem boxItem(String id, int count) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(1, 2, 1).withWeight(1).build(), count);
	}

	/**
	 * @return groups of box items with the given counts (a group per array, a box item per count)
	 */
	private static List<BoxItemGroup> groups(int[]... counts) {
		List<BoxItemGroup> groups = new ArrayList<>();
		for (int g = 0; g < counts.length; g++) {
			List<BoxItem> items = new ArrayList<>();
			for (int i = 0; i < counts[g].length; i++) {
				items.add(boxItem("box-" + g + "-" + i, counts[g][i]));
			}
			groups.add(new BoxItemGroup("group-" + g, items));
		}
		return groups;
	}

	private static ParallelBoxItemGroupPermutationRotationIteratorList list(int parallelizationCount, List<BoxItemGroup> groups) {
		return ParallelBoxItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(10, 30, 1)
				.withMaxLoadWeight(100)
				.withBoxItemGroups(groups)
				.withParallelizationCount(parallelizationCount)
				.build();
	}

	/**
	 * Group 0: box-0-0 twice and box-0-1 once, i.e. 3 permutations; group 1: two boxes, i.e. 2 permutations; 6 in all.
	 */
	private static List<BoxItemGroup> sixPermutations() {
		return groups(new int[] { 2, 1 }, new int[] { 1, 1 });
	}

	/**
	 * @return the permutations of the work unit, from its first permutation to its last
	 */
	private static List<String> permutations(ParallelBoxItemGroupPermutationRotationIterator workUnit) {
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
	private static List<List<String>> permutations(ParallelBoxItemGroupPermutationRotationIteratorList list, int units, int... order) {
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
	void workUnitsAreTheSameInAnyOrderOfRequest() {
		List<List<String>> inOrder = permutations(list(4, sixPermutations()), 4, 0, 1, 2, 3);
		List<List<String>> reversed = permutations(list(4, sixPermutations()), 4, 3, 2, 1, 0);
		List<List<String>> mixed = permutations(list(4, sixPermutations()), 4, 2, 0, 3, 1);

		assertThat(reversed).isEqualTo(inOrder);
		assertThat(mixed).isEqualTo(inOrder);
		// the work units split the permutations of the groups between them
		assertThat(inOrder).flatExtracting(permutations -> permutations).hasSize(6).doesNotHaveDuplicates();
	}

	@Test
	void workUnitIsCreatedOnce() {
		ParallelBoxItemGroupPermutationRotationIteratorList list = list(4, sixPermutations());

		assertThat(list.getIterator(2)).isSameAs(list.getIterator(2));
		assertThat(list.getIterators()).hasSize(4).doesNotContainNull();
		assertThat(list.getIterators()[2]).isSameAs(list.getIterator(2));
	}

	@Test
	void workUnitsOnlyHoldTheRemainingBoxesAfterRemovingGroups() {
		ParallelBoxItemGroupPermutationRotationIteratorList list = list(3, groups(new int[] { 1, 1, 1 }, new int[] { 2 }, new int[] { 1, 1 }));

		// one work unit existed before the removal
		ParallelBoxItemGroupPermutationRotationIterator before = list.getIterator(1);
		assertThat(before.length()).isEqualTo(7);

		// the middle group
		assertThat(list.removeGroups(List.of(1))).isEqualTo(2);

		// 5 boxes in groups of 3 and 2 boxes: 3! * 2! = 12 permutations; each of the work units is created for the remaining boxes
		for (ParallelBoxItemGroupPermutationRotationIterator workUnit : list.getIterators()) {
			assertThat(workUnit.length()).isEqualTo(5);
		}
		assertThat(list.getIterator(1)).isNotSameAs(before);
		assertThat(list.countPermutations()).isEqualTo(12);

		List<List<String>> permutations = permutations(list, 3, 2, 1, 0);
		assertThat(permutations).flatExtracting(p -> p).hasSize(12).doesNotHaveDuplicates();
	}

	@Test
	void workUnitsOnlyHoldTheRemainingBoxesAfterRemovingPermutations() {
		ParallelBoxItemGroupPermutationRotationIteratorList list = list(3, groups(new int[] { 1, 1, 1 }, new int[] { 1, 1 }));

		ParallelBoxItemGroupPermutationRotationIterator before = list.getIterator(2);
		assertThat(before.length()).isEqualTo(5);

		// the first box of the first group
		list.removePermutations(1);

		// 2 boxes in the first group and 2 in the second: 2! * 2! = 4 permutations
		for (ParallelBoxItemGroupPermutationRotationIterator workUnit : list.getIterators()) {
			assertThat(workUnit.length()).isEqualTo(4);
		}
		assertThat(list.getIterator(2)).isNotSameAs(before);

		// the second and third box item of the first group, then the box items of the second group
		List<List<String>> permutations = permutations(list, 3, 2, 1, 0);
		assertThat(permutations).flatExtracting(p -> p).containsExactlyInAnyOrder("[1, 2, 3, 4]", "[1, 2, 4, 3]", "[2, 1, 3, 4]", "[2, 1, 4, 3]");
	}

	@Test
	void forkDoesNotShareWorkUnits() {
		ParallelBoxItemGroupPermutationRotationIteratorList source = list(2, sixPermutations());
		ParallelBoxItemGroupPermutationRotationIterator sourceUnit = source.getIterator(0);

		ParallelBoxItemGroupPermutationRotationIteratorList fork = source.fork();
		assertThat(fork.getIterator(0)).isNotSameAs(sourceUnit);
		assertThat(permutations(fork.getIterator(0))).isEqualTo(permutations(sourceUnit));

		// a work unit which the source did not create is created by the fork on its own
		assertThat(permutations(fork.getIterator(1))).isEqualTo(permutations(source.getIterator(1)));
		assertThat(fork.getIterator(1)).isNotSameAs(source.getIterator(1));
	}

	@Test
	void forkDoesNotShareTheRemainingBoxes() {
		ParallelBoxItemGroupPermutationRotationIteratorList source = list(2, sixPermutations());
		ParallelBoxItemGroupPermutationRotationIteratorList fork = source.fork();

		fork.removeGroups(List.of(0));

		assertThat(fork.getIterator(0).length()).isEqualTo(2);
		assertThat(fork.countPermutations()).isEqualTo(2);
		assertThat(source.getIterator(0).length()).isEqualTo(5);
		assertThat(source.countPermutations()).isEqualTo(6);
	}

	@Test
	void resetOnlyAffectsTheWorkUnitsWhichExist() {
		ParallelBoxItemGroupPermutationRotationIteratorList list = list(3, sixPermutations());

		// no work units: nothing to reset
		list.reset();
		assertThat(list.workUnits).containsOnlyNulls();

		List<String> first = permutations(list.getIterator(1));
		list.reset();
		assertThat(list.workUnits[0]).isNull();
		assertThat(list.workUnits[2]).isNull();
		assertThat(permutations(list.getIterator(1))).isEqualTo(first);
	}

	@Test
	void resetRewindsTheListAsASingleIterator() {
		ParallelBoxItemGroupPermutationRotationIteratorList list = list(3, sixPermutations());

		List<String> first = new ArrayList<>();
		do {
			first.add(Arrays.toString(list.getPermutations()));
		} while (list.nextPermutation() != -1);
		// the work units follow one another, and split the permutations between them
		assertThat(first).hasSize(6).doesNotHaveDuplicates();

		// the list is exhausted: reset starts again at the first work unit
		list.reset();

		List<String> second = new ArrayList<>();
		do {
			second.add(Arrays.toString(list.getPermutations()));
		} while (list.nextPermutation() != -1);
		assertThat(second).isEqualTo(first);
	}

	@Test
	void workUnitsFromAnIteratorAreTheSameAsFromTheBuilder() {
		DefaultBoxItemGroupPermutationRotationIterator iterator = DefaultBoxItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 30, 1)
				.withMaxLoadWeight(100)
				.withBoxItemGroups(sixPermutations())
				.build();

		ParallelBoxItemGroupPermutationRotationIteratorList fromIterator = ParallelBoxItemGroupPermutationRotationIteratorList.of(iterator, 4);

		assertThat(permutations(fromIterator, 4, 0, 1, 2, 3)).isEqualTo(permutations(list(4, sixPermutations()), 4, 0, 1, 2, 3));

		// the list has its own boxes
		iterator.removeGroups(List.of(0));
		assertThat(iterator.length()).isEqualTo(2);
		assertThat(fromIterator.countPermutations()).isEqualTo(6);
		assertThat(fromIterator.getBoxItemGroups()[0]).isNotNull();
	}

	@Test
	void excludedGroupsAreNotSplit() {
		List<BoxItemGroup> groups = sixPermutations();
		// does not fit the container
		groups.add(new BoxItemGroup("too-large", List.of(new BoxItem(Box.newBuilder().withId("large").withSize(11, 31, 2).withWeight(1).build()))));

		ParallelBoxItemGroupPermutationRotationIteratorList list = list(3, groups);

		assertThat(list.getBoxItemGroups()[2]).isNull();
		assertThat(permutations(list, 3, 0, 1, 2)).flatExtracting(p -> p).hasSize(6).doesNotHaveDuplicates();
	}

	@Test
	void tooManyPermutationsAreRejectedWhenSplitting() {
		// 21! does not fit in a long
		int[] counts = new int[21];
		Arrays.fill(counts, 1);
		assertThatThrownBy(() -> list(2, groups(counts))).isInstanceOf(IllegalArgumentException.class);

		DefaultBoxItemGroupPermutationRotationIterator iterator = DefaultBoxItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 30, 1)
				.withMaxLoadWeight(100)
				.withBoxItemGroups(groups(counts))
				.build();
		assertThatThrownBy(() -> ParallelBoxItemGroupPermutationRotationIteratorList.of(iterator, 2)).isInstanceOf(IllegalArgumentException.class);
	}
}
