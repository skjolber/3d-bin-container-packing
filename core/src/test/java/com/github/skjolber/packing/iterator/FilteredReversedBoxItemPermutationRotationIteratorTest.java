package com.github.skjolber.packing.iterator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

class FilteredReversedBoxItemPermutationRotationIteratorTest {

	@Test
	void filtersReverseOfDistinctPermutations() {
		FilteredReversedBoxItemPermutationRotationIterator iterator = iterator(1, 1, 1);

		List<int[]> permutations = collect(iterator);

		assertEquals(3, iterator.countPermutations());
		assertEquals(3, permutations.size());
		assertArrayEquals(new int[] {0, 1, 2}, permutations.get(0));
		assertArrayEquals(new int[] {0, 2, 1}, permutations.get(1));
		assertArrayEquals(new int[] {1, 0, 2}, permutations.get(2));
	}

	@Test
	void retainsPalindromicPermutation() {
		FilteredReversedBoxItemPermutationRotationIterator iterator = iterator(2, 1);

		List<int[]> permutations = collect(iterator);

		assertEquals(2, iterator.countPermutations());
		assertEquals(2, permutations.size());
		assertArrayEquals(new int[] {0, 0, 1}, permutations.get(0));
		assertArrayEquals(new int[] {0, 1, 0}, permutations.get(1));
	}

	private List<int[]> collect(BoxItemPermutationRotationIterator iterator) {
		List<int[]> result = new ArrayList<>();
		do {
			result.add(iterator.getPermutations());
		} while(iterator.nextPermutation() != -1);
		return result;
	}

	private FilteredReversedBoxItemPermutationRotationIterator iterator(int... counts) {
		List<BoxItem> items = new ArrayList<>();
		for (int i = 0; i < counts.length; i++) {
			Box box = Box.newBuilder().withSize(1, 1, 1).withWeight(1).withId(Integer.toString(i)).build();
			items.add(new BoxItem(box, counts[i], i));
		}
		DefaultBoxItemPermutationRotationIterator delegate = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 10, 10)
				.withMaxLoadWeight(100)
				.withBoxItems(items)
				.build();
		return new FilteredReversedBoxItemPermutationRotationIterator(delegate);
	}
}
