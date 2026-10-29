package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * {@linkplain DefaultBoxItemPermutationRotationIterator#setPermutation(int[])} moves to a permutation as if reached by iterating, and
 * {@linkplain DefaultBoxItemPermutationRotationIterator#hasBlocks()} tells whether the boxes are permuted in blocks.
 */
class IteratorSetPermutationTest {

	private static BoxItem box(String id, int dz, int count, int containerPriority) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(2, 3, dz).withRotate3D().withWeight(1).build(), count).withContainerPriority(containerPriority);
	}

	private static DefaultBoxItemPermutationRotationIterator iterator(List<BoxItem> boxItems) {
		return DefaultBoxItemPermutationRotationIterator.newBuilder().withLoadSize(10, 10, 10).withBoxItems(boxItems).withMaxLoadWeight(100).build();
	}

	private static List<BoxItem> boxItems() {
		List<BoxItem> boxItems = new ArrayList<>();
		boxItems.add(box("a", 1, 2, 0));
		boxItems.add(box("b", 2, 1, 0));
		boxItems.add(box("c", 3, 2, 0));
		return boxItems;
	}

	@Test
	void setPermutationOfGroupsIsAsIfIterated() {
		List<com.github.skjolber.packing.api.BoxItemGroup> groups = List.of(
				new com.github.skjolber.packing.api.BoxItemGroup("first", List.of(box("a", 1, 2, 0), box("b", 2, 1, 0))),
				new com.github.skjolber.packing.api.BoxItemGroup("second", List.of(box("c", 3, 1, 0), box("d", 4, 1, 0))));
		DefaultBoxItemGroupPermutationRotationIterator iterated = groupIterator(groups);
		do {
			DefaultBoxItemGroupPermutationRotationIterator set = groupIterator(groups);
			set.setPermutation(iterated.getPermutations());
			assertThat(set.getPermutations()).containsExactly(iterated.getPermutations());
			for (int i = 0; i < iterated.length(); i++) {
				assertThat(set.getStackValue(i)).isSameAs(iterated.getStackValue(i));
				assertThat(set.getMinBoxVolume(i)).isEqualTo(iterated.getMinBoxVolume(i));
			}
			assertThat(set.nextPermutation()).isEqualTo(iterated.nextPermutation());
			assertThat(set.getPermutations()).containsExactly(iterated.getPermutations());
		} while (iterated.nextPermutation() != -1);
		// a box moved to another group's block
		DefaultBoxItemGroupPermutationRotationIterator iterator = groupIterator(groups);
		int[] swapped = iterator.getPermutations();
		int tmp = swapped[0];
		swapped[0] = swapped[3];
		swapped[3] = tmp;
		assertThatThrownBy(() -> iterator.setPermutation(swapped)).isInstanceOf(IllegalArgumentException.class);
	}

	private static DefaultBoxItemGroupPermutationRotationIterator groupIterator(List<com.github.skjolber.packing.api.BoxItemGroup> groups) {
		return DefaultBoxItemGroupPermutationRotationIterator.newBuilder().withLoadSize(10, 10, 10).withBoxItemGroups(groups).withMaxLoadWeight(100).build();
	}

	@Test
	void setPermutationIsAsIfIterated() {
		DefaultBoxItemPermutationRotationIterator iterated = iterator(boxItems());
		int permutations = 0;
		do {
			int[] permutation = iterated.getPermutations();

			DefaultBoxItemPermutationRotationIterator set = iterator(boxItems());
			// leave the rotations and the permutation somewhere else first
			set.nextRotation();
			set.nextPermutation(2);
			set.setPermutation(permutation);

			assertThat(set.getPermutations()).isEqualTo(permutation);
			assertThat(set.getState().getRotations()).isEqualTo(iterated.getState().getRotations());
			for (int i = 0; i < permutation.length; i++) {
				assertThat(set.getMinBoxVolume(i)).as("permutation " + permutations + ", minimum volume " + i).isEqualTo(iterated.getMinBoxVolume(i));
				assertThat(set.getStackValue(i).getDz()).isEqualTo(iterated.getStackValue(i).getDz());
			}
			// the iteration continues from the permutation
			int iteratedNext = iterated.nextPermutation(1);
			int setNext = set.nextPermutation(1);
			assertThat(setNext).isEqualTo(iteratedNext);
			if(iteratedNext == -1) {
				break;
			}
			assertThat(set.getPermutations()).isEqualTo(iterated.getPermutations());
			permutations++;
		} while (true);
		assertThat(permutations).isGreaterThan(5);
	}

	@Test
	void setPermutationRejectsOtherBoxes() {
		DefaultBoxItemPermutationRotationIterator iterator = iterator(boxItems());
		int[] permutation = iterator.getPermutations();

		assertThatThrownBy(() -> iterator.setPermutation(new int[permutation.length - 1])).isInstanceOf(IllegalArgumentException.class);

		// the same number of boxes, but not the same boxes
		int[] other = permutation.clone();
		other[other.length - 1] = other[0];
		assertThatThrownBy(() -> iterator.setPermutation(other)).isInstanceOf(IllegalArgumentException.class);

		// not changed by the rejected permutations
		assertThat(iterator.getPermutations()).isEqualTo(permutation);
	}

	@Test
	void hasBlocksWhenTheContainerPrioritiesDiffer() {
		assertThat(iterator(boxItems()).hasBlocks()).isFalse();

		List<BoxItem> prioritized = new ArrayList<>();
		prioritized.add(box("a", 1, 2, 0));
		prioritized.add(box("b", 2, 1, 1));
		assertThat(iterator(prioritized).hasBlocks()).isTrue();
	}

	@Test
	void setPermutationKeepsTheBoxesOfABlock() {
		List<BoxItem> prioritized = new ArrayList<>();
		prioritized.add(box("a", 1, 2, 0));
		prioritized.add(box("b", 2, 1, 0));
		prioritized.add(box("c", 3, 1, 1));
		DefaultBoxItemPermutationRotationIterator iterator = iterator(prioritized);
		int[] permutation = iterator.getPermutations();
		assertThat(permutation).hasSize(4);

		// within the first block
		int[] swapped = permutation.clone();
		int tmp = swapped[0];
		swapped[0] = swapped[2];
		swapped[2] = tmp;
		if(swapped[0] != permutation[0] && swapped[2] != permutation[2]) {
			iterator.setPermutation(swapped);
			assertThat(iterator.getPermutations()).isEqualTo(swapped);
		}

		// across the blocks
		int[] across = permutation.clone();
		tmp = across[0];
		across[0] = across[3];
		across[3] = tmp;
		assertThatThrownBy(() -> iterator.setPermutation(across)).isInstanceOf(IllegalArgumentException.class);
	}
}
