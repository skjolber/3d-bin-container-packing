package com.github.skjolber.packing.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.StackableItemGroup;

public class ParallelStackableItemGroupPermutationRotationIteratorTest {

	@Test
	void testPermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		groups.add(new StackableItemGroup("1", products1));

		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		groups.add(new StackableItemGroup("2", products2));
		
		ParallelStackableItemGroupPermutationRotationIterator rotator = ParallelStackableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();

		int count = 0;
		do {
			count++;
			System.out.println(Arrays.toString(rotator.getPermutations()));
		} while (rotator.nextPermutation() != -1);

		assertEquals((3 * 2 * 1) * (3 * 2 * 1), count);
		
		assertEquals(count, rotator.countPermutations());
	}

	
}
