package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ContainerItemPermutationIteratorTest {

	@Test
	void printsEveryBoundedPrefix() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(3);
		List<List<Integer>> permutations = new ArrayList<>();
		iterator.push(List.of(0, 1));
		while(iterator.hasLevel()) {
			if(iterator.hasNext()) {
				iterator.next();
				// Match the output used by the other permutation iterator tests.
				System.out.println(Arrays.toString(iterator.getPermutations()));
				permutations.add(current(iterator));
				if(iterator.length() < 3) {
					iterator.push(List.of(0, 1));
				}
			} else {
				iterator.pop();
			}
		}

		assertThat(permutations).containsExactly(
				List.of(0), List.of(0, 0), List.of(0, 0, 0), List.of(0, 0, 1),
				List.of(0, 1), List.of(0, 1, 0), List.of(0, 1, 1),
				List.of(1), List.of(1, 0), List.of(1, 0, 0), List.of(1, 0, 1),
				List.of(1, 1), List.of(1, 1, 0), List.of(1, 1, 1));
		assertThat(iterator.hasNext()).isFalse();
		assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void doesNotDescendUnlessCallerPushesAnotherLevel() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(3);
		iterator.push(List.of(0, 1));
		assertThat(iterator.next()).isEqualTo(0);
		assertThat(current(iterator)).containsExactly(0);

		assertThat(iterator.next()).isEqualTo(1);
		assertThat(current(iterator)).containsExactly(1);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void hasNextDoesNotChangeTheCurrentPermutation() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(1);
		iterator.push(List.of(0, 1));
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(0);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(current(iterator)).containsExactly(0);
		assertThat(iterator.next()).isEqualTo(1);
		assertThat(iterator.hasNext()).isFalse();
		assertThat(current(iterator)).containsExactly(1);
	}

	@Test
	void respectsContainerIndexesAtEachLevel() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(2);
		iterator.push(List.of(0, 1));
		assertThat(iterator.next()).isEqualTo(0);
		iterator.push(List.of(1));
		assertThat(iterator.next()).isEqualTo(1);
		assertThat(current(iterator)).containsExactly(0, 1);
		assertThat(iterator.hasNext()).isFalse();
		iterator.pop();
		assertThat(iterator.next()).isEqualTo(1);
		assertThat(current(iterator)).containsExactly(1);
	}

	@Test
	void snapshotsContainerIndexesWhenAdapterReusesItsList() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(2);
		List<Integer> containerIndexes = new ArrayList<>(List.of(0, 1));
		iterator.push(containerIndexes);
		assertThat(iterator.next()).isEqualTo(0);
		containerIndexes.clear();
		containerIndexes.add(0);
		assertThat(iterator.getContainerIndexes()).containsExactly(0, 1);
		iterator.push(containerIndexes);
		assertThat(iterator.next()).isEqualTo(0);
		assertThat(current(iterator)).containsExactly(0, 0);
		assertThat(iterator.getContainerIndexes()).containsExactly(0);
		assertThat(iterator.hasNext()).isFalse();
		iterator.pop();
		assertThat(iterator.next()).isEqualTo(1);
		assertThat(current(iterator)).containsExactly(1);
		assertThat(iterator.getContainerIndexes()).containsExactly(0, 1);
	}

	@Test
	void permutationSnapshotCannotChangeIteratorState() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(1);
		iterator.push(List.of(0));
		assertThat(iterator.next()).isEqualTo(0);
		int[] snapshot = iterator.getPermutations();
		snapshot[0] = 42;
		assertThat(iterator.getPermutations()).containsExactly(0);
	}

	@Test
	void explicitPopDiscardsChildContainerIndexesAndReturnsToParentSiblings() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(2);
		iterator.push(List.of(0, 1));
		assertThat(iterator.next()).isEqualTo(0);
		iterator.push(List.of(1, 2));
		assertThat(iterator.next()).isEqualTo(1);
		assertThat(current(iterator)).containsExactly(0, 1);

		iterator.pop();
		assertThat(current(iterator)).containsExactly(0);
		assertThat(iterator.next()).isEqualTo(1);
		assertThat(current(iterator)).containsExactly(1);
	}

	@Test
	void visitsEveryBranchWithPackingDependentContainerIndexes() {
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(3);
		Map<List<Integer>, List<Integer>> containerIndexesAfter = Map.of(
				List.of(0), List.of(0, 2),
				List.of(0, 0), List.of(1),
				List.of(0, 2), List.of(1),
				List.of(1), List.of(2, 0),
				List.of(1, 2), List.of(0),
				List.of(1, 0), List.of(2));
		iterator.push(List.of(0, 1));
		List<List<Integer>> visited = new ArrayList<>();
		while(iterator.hasLevel()) {
			if(iterator.hasNext()) {
				iterator.next();
				List<Integer> prefix = current(iterator);
				visited.add(prefix);
				List<Integer> childContainerIndexes = containerIndexesAfter.get(prefix);
				if(childContainerIndexes != null) {
					iterator.push(childContainerIndexes);
				}
			} else {
				iterator.pop();
			}
		}

		assertThat(visited).containsExactly(
				List.of(0), List.of(0, 0), List.of(0, 0, 1),
				List.of(0, 2), List.of(0, 2, 1),
				List.of(1), List.of(1, 2), List.of(1, 2, 0),
				List.of(1, 0), List.of(1, 0, 2));
	}

	private List<Integer> current(ContainerItemPermutationIterator iterator) {
		int[] permutation = iterator.getPermutations();
		List<Integer> result = new ArrayList<>(permutation.length);
		for(int index : permutation) {
			result.add(index);
		}
		return result;
	}
}
