package com.github.skjolber.packing.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Dimension;

@SuppressWarnings("unchecked")
public abstract class AbstractBoxItemGroupsPermutationRotationIteratorTest<T extends AbstractBoxItemGroupIteratorBuilder>{

	public abstract T newBuilder();
	
	@Test
	void testPermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));

		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
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

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));

		
		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build(), 2));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
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

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));

		
		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
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
	}
	
	@Test
	void testRemoveWholeGroup() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));

		
		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator rotator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
				.withMaxLoadWeight(products2.size())
				.build();
		
		rotator.removePermutations(1);
		// remove the rest of the first group
		rotator.removePermutations(2);
		
		int count = 0;
		do {
			count++;
		} while (rotator.nextPermutation() != -1);

		assertEquals(3 * 2 * 1, rotator.countPermutations());
		assertEquals(count, rotator.countPermutations());
	}


	@Test
	void testNextPermutationMaxIndexGroup1() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));
		
		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator iterator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
				.withMaxLoadWeight(products2.size())
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
	void testNextPermutationMaxIndexGroup2() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));
		
		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator iterator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
				.withMaxLoadWeight(products2.size())
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
	void testNextPermutationMaxIndexTransitionGroup() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItemGroup> groups = new ArrayList<>();

		List<BoxItem> products1 = new ArrayList<>();
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products1.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));
		groups.add(new BoxItemGroup("1", products1));

		
		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("5").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("6").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("7").withWeight(1).build()));
		products2.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("8").withWeight(1).build()));
		groups.add(new BoxItemGroup("2", products2));
		
		BoxItemPermutationRotationIterator iterator = newBuilder()
				.withLoadSize(container)
				.withBoxItemGroups(groups)
				.withMaxLoadWeight(products2.size())
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
