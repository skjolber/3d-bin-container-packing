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

public class ParallelStackableItemGroupPermutationRotationIteratorListTest {

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
		
		ParallelStackableItemGroupPermutationRotationIteratorList rotator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.withParallelizationCount(5)
				.build();

		ParallelStackableItemGroupPermutationRotationIterator rotator2 = ParallelStackableItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();
		
		int count = 0;
		do {
			count++;
			System.out.println(Arrays.toString(rotator.getPermutations()) + " " + Arrays.toString(rotator2.getPermutations()));
		} while (rotator.nextPermutation() != -1 && rotator2.nextPermutation() != -1);

		assertEquals((3 * 2 * 1) * (3 * 2 * 1), count);
		
		assertEquals(count, rotator.countPermutations());
	}

	@Test
	void testPermutationsRepeatedItems() {
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
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build(), 2));
		groups.add(new StackableItemGroup("2", products2));
		
		ParallelStackableItemGroupPermutationRotationIteratorList rotator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.withParallelizationCount(2)
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
		
		ParallelStackableItemGroupPermutationRotationIteratorList rotator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.withParallelizationCount(2)
				.build();
		
		rotator.removePermutations(1);

		int count = 0;
		do {
			count++;
			System.out.println(Arrays.toString(rotator.getPermutations()));
		} while (rotator.nextPermutation() != -1);

		assertEquals((2 * 1) * (3 * 2 * 1), count);
		
		assertEquals(count, rotator.countPermutations());
	}
	
	@Test
	void testRemoveWholeGroup() {
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
		
		ParallelStackableItemGroupPermutationRotationIteratorList rotator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.withParallelizationCount(2)
				.build();
		
		rotator.removePermutations(1);
		// remove the rest of the first group
		rotator.removePermutations(2);
		
		int count = 0;
		do {
			count++;
		} while (rotator.nextPermutation() != -1);

		assertEquals((3 * 2 * 1), count);
		
		assertEquals(count, rotator.countPermutations());
	}
	

	@Test
	void testNexPermutationMaxIndexGroup1() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		groups.add(new StackableItemGroup("1", products1));

		ParallelStackableItemGroupPermutationRotationIteratorList iterator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products1.size())
				.withParallelizationCount(1)
				.build();

		int[] before = iterator.getPermutations();
		
		int maxIndex = 3;

		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		iterator.nextPermutation(maxIndex);

		System.out.println(Arrays.toString(iterator.getPermutations()));

		int[] after = iterator.getPermutations();
		for(int i = 0; i < maxIndex; i++) {
			assertEquals(before[i], after[i]);
		}
		
		assertNotEquals(before[maxIndex], after[maxIndex]);
		
		// group 2 should be reset
		for(int i = products1.size(); i < after.length - 1; i++) {
			assertTrue(before[i] <= after[i + 1]);
		}
	}

	@Test
	void testNexPermutationMaxIndexGroup2() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		groups.add(new StackableItemGroup("1", products1));
		
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		groups.add(new StackableItemGroup("2", products2));
		
		ParallelStackableItemGroupPermutationRotationIteratorList iterator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.withParallelizationCount(2)
				.build();

		int[] before = iterator.getPermutations();
		
		System.out.println(Arrays.toString(before));
		
		int maxIndex = 6;

		iterator.nextPermutation(maxIndex);
		
		int[] after = iterator.getPermutations();

		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		for(int i = 0; i < maxIndex; i++) {
			assertEquals(before[i], after[i]);
		}
		
		assertNotEquals(before[maxIndex], after[maxIndex]);
		
		// group 1 should not be touched
		for(int i = 0; i < products1.size(); i++) {
			assertEquals(before[i], after[i]);
		}
	}

	@Test
	void testNexPermutationMaxIndexTransitionGroup() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItemGroup> groups = new ArrayList<>();

		List<StackableItem> products1 = new ArrayList<>();
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		groups.add(new StackableItemGroup("1", products1));

		
		List<StackableItem> products2 = new ArrayList<>();
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		groups.add(new StackableItemGroup("2", products2));
		
		ParallelStackableItemGroupPermutationRotationIteratorList iterator = ParallelStackableItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container)
				.withStackableItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.withParallelizationCount(2)
				.build();

		// go to the last permuation of the second group
		for(int i = 0; i < 4 * 3 * 2 * 1 - 1; i++) {
			iterator.nextPermutation();
		}
		
		int[] before = iterator.getPermutations();
		
		System.out.println(Arrays.toString(iterator.getPermutations()));
		
		int maxIndex = 6;

		int index = iterator.nextPermutation(maxIndex);

		assertTrue(index < products1.size());

		int[] after = iterator.getPermutations();
		
		System.out.println(Arrays.toString(after));
		
		for(int i = 0; i < index; i++) {
			assertEquals(before[i], after[i]);
		}
		
		assertNotEquals(before[index], after[index]);
	}
}
