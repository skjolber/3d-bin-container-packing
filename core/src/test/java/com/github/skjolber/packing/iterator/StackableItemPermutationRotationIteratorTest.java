package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.iterator.DefaultStackableItemPermutationRotationIterator.Builder;

class StackableItemPermutationRotationIteratorTest extends AbstractStackableItemPermutationRotationIteratorTest<DefaultStackableItemPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultStackableItemPermutationRotationIterator.newBuilder();
	}

	@Test
	public void testPermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));

		StackableItemPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		do {
			System.out.println(Arrays.toString(rotator.getPermutations()));
		} while (rotator.nextPermutation() != -1);

	}

}
