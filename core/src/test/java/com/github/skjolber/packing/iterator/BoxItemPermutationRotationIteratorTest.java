package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator.Builder;
import com.github.skjolber.packing.packer.AbstractPackagerSession;
import com.github.skjolber.packing.packer.Dimension;

class BoxItemPermutationRotationIteratorTest extends AbstractBoxItemPermutationRotationIteratorTest<DefaultBoxItemPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultBoxItemPermutationRotationIterator.newBuilder();
	}

	@Test
	public void testPermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));

		BoxItemPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(AbstractPackagerSession.toRemainingBoxItems(products))
				.withMaxLoadWeight(products.size())
				.build();

		do {
			System.out.println(Arrays.toString(rotator.getPermutations()));
		} while (rotator.nextPermutation() != -1);

	}

	private List<String> permutations(List<BoxItem> items, boolean maxIndex) {
		DefaultBoxItemPermutationRotationIterator iterator = newBuilder()
				.withLoadSize(9, 1, 1)
				.withBoxItems(AbstractPackagerSession.toRemainingBoxItems(items))
				.withMaxLoadWeight(items.size())
				.build();
		List<String> permutations = new ArrayList<>();
		do {
			permutations.add(Arrays.toString(iterator.getPermutations()));
		} while ((maxIndex ? iterator.nextPermutation(iterator.length() - 1) : iterator.nextPermutation()) != -1);
		return permutations;
	}

	private static BoxItem box(String id, int containerPriority) {
		return new BoxItem(Box.newBuilder().withSize(1, 1, 1).withId(id).withWeight(1).build()).withContainerPriority(containerPriority);
	}

	/**
	 * Boxes of a lower container priority come first: the boxes are only permuted within their priority.
	 *
	 * <pre>
	 *   priority 0: a, b      priority 1: c, d
	 *
	 *   a b | c d    a b | d c    b a | c d    b a | d c
	 * </pre>
	 */
	@Test
	public void permutesWithinContainerPriorities() {
		List<BoxItem> items = List.of(box("a", 0), box("b", 0), box("c", 1), box("d", 1));
		List<String> expected = List.of("[0, 1, 2, 3]", "[0, 1, 3, 2]", "[1, 0, 2, 3]", "[1, 0, 3, 2]");
		assertThat(permutations(items, false)).containsExactlyElementsOf(expected);
		assertThat(permutations(items, true)).containsExactlyElementsOf(expected);
	}

	@Test
	public void skipsPermutationsWithinContainerPriorities() {
		List<BoxItem> items = List.of(box("a", 0), box("b", 0), box("c", 1), box("d", 1));
		DefaultBoxItemPermutationRotationIterator iterator = newBuilder()
				.withLoadSize(9, 1, 1)
				.withBoxItems(AbstractPackagerSession.toRemainingBoxItems(items))
				.withMaxLoadWeight(items.size())
				.build();
		// a b c d: the box at index 0 could not be placed, so the next permutation changes it
		assertThat(iterator.nextPermutation(0)).isEqualTo(0);
		assertThat(iterator.getPermutations()).containsExactly(1, 0, 2, 3);
		assertThat(iterator.nextPermutation(0)).isEqualTo(-1);
	}
}
