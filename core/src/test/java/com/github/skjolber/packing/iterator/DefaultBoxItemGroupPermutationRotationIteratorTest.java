package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator.Builder;

public class DefaultBoxItemGroupPermutationRotationIteratorTest extends AbstractBoxItemGroupsPermutationRotationIteratorTest<DefaultBoxItemGroupPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultBoxItemGroupPermutationRotationIterator.newBuilder();
	}

	/**
	 * After the last permutation, the groups are back at their first permutation: the rotations of the last permutation
	 * must not be kept, as the boxes have different rotations.
	 *
	 * <pre>
	 *   group: a (2 x 1 x 1, two rotations), b (1 x 1 x 1, one rotation)
	 *
	 *   b a, a rotated  --  no more permutations  -->  a b, not rotated
	 * </pre>
	 */
	@Test
	public void resetsRotationsAfterTheLastPermutation() {
		BoxItem a = new BoxItem(Box.newBuilder().withId("a").withSize(2, 1, 1).withRotate2D().withWeight(1).build());
		BoxItem b = new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withRotate2D().withWeight(1).build());
		DefaultBoxItemGroupPermutationRotationIterator iterator = newBuilder()
				.withLoadSize(3, 3, 1)
				.withBoxItemGroups(List.of(new BoxItemGroup("g", List.of(a, b))))
				.withMaxLoadWeight(10)
				.build();

		assertThat(iterator.nextPermutation()).isEqualTo(0);
		assertThat(iterator.getStackValue(1).getBox().getId()).isEqualTo("a");
		assertThat(iterator.nextRotation()).isEqualTo(1);

		assertThat(iterator.nextPermutation(1)).isEqualTo(-1);
		assertThat(iterator.getStackValue(0).getBox().getId()).isEqualTo("a");
		assertThat(iterator.getStackValue(1).getBox().getId()).isEqualTo("b");
		assertThat(iterator.getState().getRotations()).containsOnly(0);
	}
}
