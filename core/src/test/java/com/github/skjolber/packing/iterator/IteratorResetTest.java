package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.AbstractPackagerSession;

/**
 * After iterating all permutations and rotations, reset() goes back to the first permutation and rotations, so that
 * the next packaging attempt iterates all of them again.
 */
public class IteratorResetTest {

	private static List<BoxItem> boxItems(String... ids) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			// different sizes, and two rotations each
			boxItems.add(new BoxItem(Box.newBuilder().withId(ids[i]).withSize(1, 2 + i, 1).withRotate2D().withWeight(1).build(), 1));
		}
		return boxItems;
	}

	private static List<BoxItemGroup> groups() {
		return List.of(new BoxItemGroup("1", boxItems("a", "b", "c")), new BoxItemGroup("2", boxItems("d", "e")));
	}

	/**
	 * @return all permutations and rotations, from the current permutation
	 */
	private static List<String> iterate(BoxItemPermutationRotationIterator iterator) {
		List<String> states = new ArrayList<>();
		do {
			do {
				PermutationRotationState state = iterator.getState();
				states.add(Arrays.toString(state.getPermutations()) + Arrays.toString(state.getRotations()));
			} while (iterator.nextRotation() != -1);
		} while (iterator.nextPermutation() != -1);
		return states;
	}

	private static void assertIteratesAgainAfterReset(BoxItemPermutationRotationIterator iterator, int expectedCount) {
		List<String> first = iterate(iterator);
		assertThat(first).hasSize(expectedCount);

		iterator.reset();
		assertThat(iterate(iterator)).isEqualTo(first);
	}

	@Test
	public void boxItemIteratorIteratesAgainAfterReset() {
		DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 10, 1)
				.withBoxItems(AbstractPackagerSession.toRemainingBoxItems(boxItems("a", "b", "c")))
				.withMaxLoadWeight(100)
				.build();

		// 3! permutations, 2^3 rotations
		assertIteratesAgainAfterReset(iterator, 6 * 8);
	}

	@Test
	public void boxItemGroupIteratorIteratesAgainAfterReset() {
		DefaultBoxItemGroupPermutationRotationIterator iterator = DefaultBoxItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 10, 1)
				.withBoxItemGroups(AbstractPackagerSession.toRemainingBoxItemGroups(groups()))
				.withMaxLoadWeight(100)
				.build();

		// 3! * 2! permutations, 2^5 rotations
		assertIteratesAgainAfterReset(iterator, 12 * 32);
	}

	@Test
	public void parallelWorkUnitsIterateAgainAfterReset() {
		ParallelBoxItemPermutationRotationIteratorList list = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
				.withLoadSize(10, 10, 1)
				.withBoxItems(AbstractPackagerSession.toRemainingBoxItems(boxItems("a", "b", "c")))
				.withMaxLoadWeight(100)
				.withParallelizationCount(2)
				.build();

		List<List<String>> first = new ArrayList<>();
		for (ParallelBoxItemPermutationRotationIterator workUnit : list.getIterators()) {
			first.add(iterate(workUnit));
		}
		assertThat(first).flatExtracting(states -> states).hasSize(6 * 8).doesNotHaveDuplicates();

		list.reset();
		for (int i = 0; i < first.size(); i++) {
			assertThat(iterate(list.getIterator(i))).as("work unit %d", i).isEqualTo(first.get(i));
		}
	}

	@Test
	public void parallelGroupWorkUnitsIterateAgainAfterReset() {
		ParallelBoxItemGroupPermutationRotationIteratorList list = ParallelBoxItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(10, 10, 1)
				.withBoxItemGroups(AbstractPackagerSession.toRemainingBoxItemGroups(groups()))
				.withMaxLoadWeight(100)
				.withParallelizationCount(2)
				.build();

		List<List<String>> first = new ArrayList<>();
		for (ParallelBoxItemGroupPermutationRotationIterator workUnit : list.getIterators()) {
			first.add(iterate(workUnit));
		}
		assertThat(first).flatExtracting(states -> states).hasSize(12 * 32).doesNotHaveDuplicates();

		list.reset();
		for (int i = 0; i < first.size(); i++) {
			assertThat(iterate(list.getIterator(i))).as("work unit %d", i).isEqualTo(first.get(i));
		}
	}
}
