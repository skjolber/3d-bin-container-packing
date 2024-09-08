package com.github.skjolber.packing.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.StackableItemGroup;

public class DefaultLoadableItemGroupPermutationRotationIteratorTest {

	@Test
	void testPermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		StackableItemGroup group1 = new StackableItemGroup();
		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		group1.setItems(products1);
		group1.setId("1");
		groups.add(group1);

		StackableItemGroup group2 = new StackableItemGroup();
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		group2.setItems(products2);
		group2.setId("2");
		
		groups.add(group2);
		
		DefaultLoadableItemGroupPermutationRotationIterator rotator = DefaultLoadableItemGroupPermutationRotationIterator.newBuilder()
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

	@Test
	void testPermutationsRepeatedItems() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		StackableItemGroup group1 = new StackableItemGroup();
		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		group1.setItems(products1);
		group1.setId("1");
		groups.add(group1);

		
		StackableItemGroup group2 = new StackableItemGroup();
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build(), 2));
		group2.setItems(products2);
		group2.setId("2");
		
		groups.add(group2);
		
		DefaultLoadableItemGroupPermutationRotationIterator rotator = DefaultLoadableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();

		int count = 0;
		do {
			count++;
			System.out.println(Arrays.toString(rotator.getPermutations()));
		} while (rotator.nextPermutation() != -1);

		assertEquals((3 * 2 * 1) * (5 * 4 * 3 * 2 * 1) / 2, count);
		
		assertEquals(count, rotator.countPermutations());
	}

	@Test
	void testRemovePermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		StackableItemGroup group1 = new StackableItemGroup();
		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		group1.setItems(products1);
		group1.setId("1");
		groups.add(group1);

		
		StackableItemGroup group2 = new StackableItemGroup();
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		group2.setItems(products2);
		group2.setId("2");
		
		groups.add(group2);
		
		DefaultLoadableItemGroupPermutationRotationIterator rotator = DefaultLoadableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();
		
		rotator.removePermutations(1);

		int count = 0;
		do {
			count++;
			System.out.println(Arrays.toString(rotator.getPermutations()));
		} while (rotator.nextPermutation() != -1);

		assertEquals((2 * 1) * (3 * 2 * 1), count);
		
		assertEquals(count, rotator.countPermutations());

		// remove the rest of the group
		rotator.removePermutations(2);

		assertEquals(3 * 2 * 1, rotator.countPermutations());
	}

	@Test
	void testNexPermutationMaxIndexGroup1() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		StackableItemGroup group1 = new StackableItemGroup();
		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		group1.setItems(products1);
		group1.setId("1");
		groups.add(group1);

		
		StackableItemGroup group2 = new StackableItemGroup();
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		group2.setItems(products2);
		group2.setId("2");
		
		groups.add(group2);
		
		DefaultLoadableItemGroupPermutationRotationIterator iterator = DefaultLoadableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();
		
		int[] permutations = iterator.getPermutations();
		
		int[] clone = new int[permutations.length];
		System.arraycopy(permutations, 0, clone, 0, clone.length);

		// forward to some random permutation within the group2
		for(int i = 0; i < 10; i++) {
			iterator.nextPermutation();
		}
		
		int maxIndex = 3;

		iterator.nextPermutation(maxIndex);

		for(int i = 0; i < maxIndex; i++) {
			assertEquals(clone[i], permutations[i]);
		}
		
		assertNotEquals(clone[maxIndex], permutations[maxIndex]);
		
		// group 2 should be reset
		for(int i = products1.size(); i < permutations.length; i++) {
			assertEquals(clone[i], permutations[i]);
		}
		
		
	}
	

	@Test
	void testNexPermutationMaxIndexGroup2() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		StackableItemGroup group1 = new StackableItemGroup();
		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		group1.setItems(products1);
		group1.setId("1");
		groups.add(group1);

		
		StackableItemGroup group2 = new StackableItemGroup();
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		group2.setItems(products2);
		group2.setId("2");
		
		groups.add(group2);
		
		DefaultLoadableItemGroupPermutationRotationIterator iterator = DefaultLoadableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();

		int[] permutations = iterator.getPermutations();
		
		int[] clone = new int[permutations.length];
		System.arraycopy(permutations, 0, clone, 0, clone.length);
		
		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		int maxIndex = 6;

		iterator.nextPermutation(maxIndex);

		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		for(int i = 0; i < maxIndex; i++) {
			assertEquals(clone[i], permutations[i]);
		}
		
		assertNotEquals(clone[maxIndex], permutations[maxIndex]);
		
		// group 1 should not be touched
		for(int i = 0; i < products1.size(); i++) {
			assertEquals(clone[i], permutations[i]);
		}
		
		
	}
	


	@Test
	void testNexPermutationMaxIndexTransitionGroup() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		StackableItemGroup group1 = new StackableItemGroup();
		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		group1.setItems(products1);
		group1.setId("1");
		groups.add(group1);

		
		StackableItemGroup group2 = new StackableItemGroup();
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		group2.setItems(products2);
		group2.setId("2");
		
		groups.add(group2);
		
		DefaultLoadableItemGroupPermutationRotationIterator iterator = DefaultLoadableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();

		// go to the last permuation of the second group
		for(int i = 0; i < 4 * 3 * 2 * 1 - 1; i++) {
			iterator.nextPermutation();
		}
		
		int[] permutations = iterator.getPermutations();
		
		int[] clone = new int[permutations.length];
		System.arraycopy(permutations, 0, clone, 0, clone.length);
		
		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		int maxIndex = 6;

		int index = iterator.nextPermutation(maxIndex);

		assertTrue(index < products1.size());
		
		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		for(int i = 0; i < index; i++) {
			assertEquals(clone[i], permutations[i]);
		}
		
		assertNotEquals(clone[index], permutations[index]);
	}
}
