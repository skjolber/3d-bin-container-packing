package com.github.skjolber.packing.iterator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.BoxItem;

class PermutationRotationIteratorTest extends AbstractPermutationRotationIteratorTest {

	@Test
	void testCount() {
		for (int i = 1; i <= 8; i++) {
			Dimension container = new Dimension(null, 3 * (i + 1), 1, 1);

			List<BoxItem> products1 = new ArrayList<>();

			for (int k = 0; k < i; k++) {
				Box box = Box.newBuilder().withSize(3, 1, 1).withRotate3D().withDescription(Integer.toString(k)).withWeight(1).build();

				BoxItem item = new BoxItem(box);

				products1.add(item);
			}

			DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container)
					.withStackableItems(products1)
					.withMaxLoadWeight(products1.size())
					.build();

			long count = rotator.countRotations();

			int rotate = 0;
			do {
				rotate++;
			} while (rotator.nextRotation() != -1);

			assertEquals(count, rotate);
		}
	}

	@Test
	void testNumberOfUnconstrainedRotations() {
		Dimension container = new Dimension(null, 3, 3, 3);

		List<BoxItem> products = new ArrayList<>();

		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withDescription("0").withWeight(1).build();
		products.add(new BoxItem(box));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(6, rotator.countRotations());
	}

	@Test
	void testNumberOfConstrainedRotations() {
		Dimension container = new Dimension(null, 1, 2, 3);

		List<BoxItem> products = new ArrayList<>();

		Box box = Box.newBuilder().withRotate3D().withSize(1, 2, 3).withDescription("0").withWeight(1).build();

		products.add(new BoxItem(box));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(1, rotator.countRotations());
	}

	@Test
	void testNumberOfConstrainedRotationsWithOutOfScopeBox() {
		Dimension container = new Dimension(null, 4, 4, 4);

		List<BoxItem> products = new ArrayList<>();

		Box box1 = Box.newBuilder().withRotate3D().withSize(1, 2, 3).withDescription("0").withWeight(1).build();
		Box box2 = Box.newBuilder().withRotate3D().withSize(5, 2, 2).withDescription("0").withWeight(1).build();

		products.add(new BoxItem(box1));
		products.add(new BoxItem(box2));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(6, rotator.countRotations());
	}

	
	@Test
	void testNumberOfRotationsForSquare2D() {
		Dimension container = new Dimension(null, 3, 3, 3);

		List<BoxItem> products = new ArrayList<>();

		Box box = Box.newBuilder().withSize(3, 1, 1).withRotate2D().withDescription("0").withWeight(1).build();
		products.add(new BoxItem(box));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(2, rotator.countRotations());
	}

	@Test
	void testNumberOfConstrainedRotationsForSquare2D() {
		Dimension container = new Dimension(null, 3, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		Box box = Box.newBuilder().withSize(3, 1, 1).withRotate2D().withDescription("0").withWeight(1).build();
		products.add(new BoxItem(box));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(1, rotator.countRotations());
	}

	@Test
	void testNumberOfRotationsForSquare3D() {
		Dimension container = new Dimension(null, 3, 3, 3);

		List<BoxItem> products = new ArrayList<>();

		Box box = Box.newBuilder().withRotate3D().withSize(1, 1, 1).withDescription("0").withWeight(1).build();
		products.add(new BoxItem(box));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(1, rotator.countRotations());
	}

	@Test
	void testRotation() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(1, rotator.countRotations());

		do {
			// check order unchanged
			for (int i = 0; i < products.size(); i++) {
				assertEquals(Integer.toString(i), rotator.get(i).getStackable().getDescription());
			}

			// all rotations can fit
			for (int i = 0; i < products.size(); i++) {
				assertTrue(rotator.get(i).getValue().fitsInside3D(container));
			}

			assertMinStackableVolumeValid(rotator);
		} while (rotator.nextRotation() != -1);

	}

	@Test
	void testPermutations() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		int count = 0;
		do {
			assertMinStackableVolumeValid(rotator);

			count++;
		} while (rotator.nextPermutation() != -1);

		assertEquals(5 * 4 * 3 * 2 * 1, count);
	}

	@Test
	void testPermutationsForMaxIndex() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		int count = 0;
		do {
			count++;
		} while (rotator.nextPermutation(rotator.length() - 1) != -1);

		assertEquals(5 * 4 * 3 * 2 * 1, count);
	}

	@Test
	void testPermutationsForMaxIndexInRightOrder() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 4).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 5).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 6).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 7).withDescription("4").withWeight(1).build()));

		DefaultPermutationRotationIterator rotator1 = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		DefaultPermutationRotationIterator rotator2 = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		int count = 0;
		do {
			assertMinStackableVolumeValid(rotator1);
			assertMinStackableVolumeValid(rotator2);

			int[] permutations1 = rotator1.getPermutations();
			int[] permutations2 = rotator2.getPermutations();
			assertArrayEquals(permutations1, permutations2);

			count++;
		} while (rotator1.nextPermutation(rotator1.length() - 1) != -1 && rotator2.nextPermutation() != -1);

		assertEquals(5 * 4 * 3 * 2 * 1, count);
	}

	@Test
	void testPermutationCorrectIndexReturned() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 4).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 5).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 6).withDescription("3").withWeight(1).build()));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		int count = 0;
		do {
			count++;

			int[] permutations = cloneArray(rotator.getPermutations());

			int length = rotator.nextPermutation();

			if(length == -1) {
				break;
			}
			assertThat(firstDiffIndex(permutations, rotator.getPermutations())).isEqualTo(length);

		} while (true);

		assertEquals(4 * 3 * 2 * 1, count);
	}

	public static int firstDiffIndex(int[] a, int[] b) {
		for (int i = 0; i < a.length; i++) {
			if(a[i] != b[i]) {
				return i;
			}
		}
		return -1;
	}

	public static int[] cloneArray(int[] permutations) {
		int[] clone = new int[permutations.length];
		System.arraycopy(permutations, 0, clone, 0, permutations.length);
		return clone;
	}

	@Test
	void testPermutationsWithMultipleBoxes() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build(), 2));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build(), 4));

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		int count = 0;
		do {
			count++;
		} while (rotator.nextPermutation() != -1);

		assertEquals((6 * 5 * 4 * 3 * 2 * 1) / ((4 * 3 * 2 * 1) * (2 * 1)), count);
	}

	@Test
	void testCounts() {
		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(5, 10, 10).withDescription("0").withWeight(1).build(), 2));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(5, 10, 10).withDescription("1").withWeight(1).build(), 2));

		int n = 4;

		Dimension container = new Dimension(null, 5 * n, 10, 10);

		DefaultPermutationRotationIterator rotator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		int length = rotator.length();

		assertEquals(4, length);

	}

	@Test
	void testCountPermutations1() {
		int n = 25;

		Dimension container = new Dimension(null, 5 * n, 10, 10);

		List<BoxItem> products = new ArrayList<>();
		for (int k = 0; k < n; k++) {
			products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(5, 10, 10).withWeight(1).build(), 1));
		}

		DefaultPermutationRotationIterator iterator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(-1L, iterator.countPermutations());
	}

	@Test
	void testCountPermutations2() {
		int n = 25;

		Dimension container = new Dimension(null, 5 * n, 10, 10);

		List<BoxItem> products = new ArrayList<>();
		for (int k = 0; k < n; k++) {
			products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(5, 10, 10).withWeight(1).build(), 1));
		}
		DefaultPermutationRotationIterator iterator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		assertEquals(-1L, iterator.countPermutations());
	}

	@Test
	void testRemovePermutations1() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		DefaultPermutationRotationIterator iterator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		iterator.removePermutations(3);

		int[] permutations = iterator.getPermutations();

		assertEquals(permutations.length, 2);
		assertEquals(3, permutations[0]);
		assertEquals(4, permutations[1]);

		int nextPermutation = iterator.nextPermutation();
		assertEquals(0, nextPermutation);

		// no more rotations
		assertEquals(-1, iterator.nextPermutation());
	}

	@Test
	void testRemovePermutations2() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		DefaultPermutationRotationIterator iterator = DefaultPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container)
				.withStackableItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		List<Integer> remove = new ArrayList<>();
		remove.add(2);
		remove.add(4);
		iterator.removePermutations(remove);

		int[] permutations = iterator.getPermutations();
		assertEquals(0, permutations[0]);
		assertEquals(1, permutations[1]);
		assertEquals(3, permutations[2]);
	}

}
