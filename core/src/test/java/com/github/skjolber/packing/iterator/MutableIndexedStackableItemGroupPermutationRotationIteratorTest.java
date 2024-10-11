package com.github.skjolber.packing.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.StackableItemGroup;
import com.github.skjolber.packing.api.packager.StackableItems;
import com.github.skjolber.packing.iterator.MutableIndexedStackableItemPermutationRotationIterator.Builder;
import com.github.skjolber.packing.iterator.MutableIndexedStackableItemPermutationRotationIterator.DelegateBuilder;

class MutableIndexedStackableItemGroupPermutationRotationIteratorTest extends AbstractStackableItemGroupPermutationRotationIteratorTest<MutableIndexedStackableItemGroupPermutationRotationIterator.DelegateBuilder> {

	@Override
	public MutableIndexedStackableItemGroupPermutationRotationIterator.DelegateBuilder newBuilder() {
		return new MutableIndexedStackableItemGroupPermutationRotationIterator.DelegateBuilder(DefaultStackableItemGroupPermutationRotationIterator.newBuilder());
	}
	
	@Test
	void testMutableRotationCount() {
		for (int i = 1; i <= 8; i++) {
			Dimension container = new Dimension(null, 3 * (i + 1), 3, 1);

			List<StackableItemGroup> groups = new ArrayList<>();

			List<StackableItem> products1 = new ArrayList<>();

			for (int k = 0; k < i; k++) {
				Box box = Box.newBuilder().withSize(3, 1, 1).withRotate3D().withId(Integer.toString(k)).withWeight(1).build();

				StackableItem item = new StackableItem(box);

				products1.add(item);
			}
			
			groups.add(new StackableItemGroup("1", products1));

			MutableIndexedStackableItemGroupPermutationRotationIterator rotator = 
					newBuilder()
					.withLoadSize(container)
					.withStackableItemGroups(groups)
					.withMaxLoadWeight(products1.size())
					.build();
			
			StackableItems items = rotator;

			long unmodifiedRotationsCount = rotator.getIterator().countRotations();
			
			long modifiedRotationsCount = rotator.countRotations();

			assertTrue(unmodifiedRotationsCount >= modifiedRotationsCount);
			
			long rotate = 0;
			do {
				// removing items do not affect the number of rotations
				assertEquals(items.size(), products1.size());
				
				items.remove(0, 1);
				for(int k = 0; k < items.size(); k++) {
					StackableItem item = items.get(k);
					assertFalse(item.getStackable().getId().equals("0"));
				}
				
				rotate++;
			} while (rotator.nextRotation() != -1);

			assertEquals(unmodifiedRotationsCount, rotate);
		}
	}
	
	@Test
	void testMutablePermutationsWithMultipleBoxes() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withId("0").withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withId("1").withWeight(1).build(), 4));

		List<StackableItemGroup> groups = new ArrayList<>();
		groups.add(new StackableItemGroup("1", products));
		
		MutableIndexedStackableItemGroupPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products.size())
				.build();

		
		int count = 0;
		do {
			assertEquals(rotator.size(), products.size());

			// removing items do not affect the number of permutations
			rotator.remove(0, 1);
			
			// still two types of loadable items
			assertEquals(rotator.size(), 2);

			count++;
		} while (rotator.nextPermutation() != -1);

		assertEquals((6 * 5 * 4 * 3 * 2 * 1) / ((4 * 3 * 2 * 1) * (2 * 1)), count);
	}

	@Test
	void testLoadableItems() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withId("0").withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withId("1").withWeight(1).build(), 4));

		List<StackableItemGroup> groups = new ArrayList<>();
		groups.add(new StackableItemGroup("1", products));

		MutableIndexedStackableItemGroupPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products.size())
				.build();

		rotator.remove(0, 1);
		
		// still two types of loadable items
		assertEquals(rotator.size(), 2);

		int[] frequencies = toFrequency(rotator, 2);
		
		assertEquals(1, frequencies[0]);
		assertEquals(4, frequencies[1]);

		// still two types of loadable items
		rotator.remove(1, 2);
		assertEquals(rotator.size(), 2);

		frequencies = toFrequency(rotator, 2);
		assertEquals(1, frequencies[0]);
		assertEquals(2, frequencies[1]);

		rotator.remove(0, 1);
		// 0 exhausted
		assertEquals(rotator.size(), 1);

		frequencies = toFrequency(rotator, 2);
		assertEquals(0, frequencies[0]);
		assertEquals(2, frequencies[1]);

		rotator.remove(0, 2);
		// 1 exhausted
		assertEquals(rotator.size(), 0);
	}
	
	public int[] toFrequency(StackableItemGroupPermutationRotationIterator rotator, int size) {
		int[] counts = new int[size];
		for (int i : rotator.getPermutations()) {
			counts[i]++;
		}
		return counts;
	}




}
