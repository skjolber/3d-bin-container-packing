package com.github.skjolber.packing.iterator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.Dimension;

public class ParallelBoxItemPermutationRotationIteratorTest extends AbstractPermutationRotationIteratorTest {

	@Test
	void testPermutationsSingleWorkUnit() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		ParallelBoxItemPermutationRotationIteratorList calculator = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.withParallelizationCount(1)
				.build();

		assertEquals(iterator.countPermutations(), calculator.countPermutations());

		ParallelBoxItemPermutationRotationIterator nthIterator = calculator.getIterator(0);

		int count = 0;
		do {
			assertMinStackableVolumeValid(iterator);
			assertMinStackableVolumeValid(nthIterator);

			assertThat(nthIterator.getPermutations()).isEqualTo(iterator.getPermutations());
			count++;
		} while (nthIterator.nextPermutation() != -1 && iterator.nextPermutation() != -1);

		assertEquals(5 * 4 * 3 * 2 * 1, count);
		assertThat(nthIterator.nextPermutation()).isEqualTo(-1);
	}

	@Test
	void testPermutationsForMaxIndexInRightOrder() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		long max = 5 * 4 * 3 * 2 * 1;

		for (int i = 0; i < 3; i++) {
			int l = i + 1;

			DefaultBoxItemPermutationRotationIterator rotator1 = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getDx(), container.getDy(), container.getDz())
					.withBoxItems(products)
					.withMaxLoadWeight(products.size())
					.build();

			ParallelBoxItemPermutationRotationIteratorList calculator = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
					.withLoadSize(container.getDx(), container.getDy(), container.getDz())
					.withBoxItems(products)
					.withMaxLoadWeight(products.size())
					.withParallelizationCount(1)
					.build();

			long limit = rotator1.countPermutations() / l;
			long count = 0;
			do {
				assertMinStackableVolumeValid(rotator1);
				assertMinStackableVolumeValid(calculator.getIterator(0));

				int[] permutations1 = rotator1.getPermutations();
				int[] permutations2 = calculator.getIterator(0).getPermutations();

				assertArrayEquals(permutations1, permutations2);

				count++;
			} while (count < limit && rotator1.nextPermutation(rotator1.length() - 1) != -1 && calculator.getIterator(0).nextPermutation() != -1);

			assertEquals(max / l, limit);
		}
	}

	@Test
	void testPermutationsSkip() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		for (int i = 0; i < 3; i++) {
			DefaultBoxItemPermutationRotationIterator rotator1 = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getDx(), container.getDy(), container.getDz())
					.withBoxItems(products)
					.withMaxLoadWeight(products.size())
					.build();

			ParallelBoxItemPermutationRotationIteratorList calculator = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
					.withLoadSize(container.getDx(), container.getDy(), container.getDz())
					.withBoxItems(products)
					.withMaxLoadWeight(products.size())
					.withParallelizationCount(1)
					.build();

			int permutationIndex1;
			do {
				assertMinStackableVolumeValid(rotator1);

				permutationIndex1 = rotator1.nextPermutation();

			} while (permutationIndex1 > 2);

			calculator.getIterator(0).nextPermutation(2);
			assertArrayEquals(rotator1.getPermutations(), calculator.getIterator(0).getPermutations());
		}
	}

	@Test
	void testPermutationCorrectIndexReturned() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));

		ParallelPermutationRotationIteratorList calculator = new ParallelPermutationRotationIteratorListBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.withParallelizationCount(1)
				.build();

		int count = 0;
		do {
			count++;

			int[] permutations = PermutationRotationIteratorTest.cloneArray(calculator.getIterator(0).getPermutations());

			int length = calculator.getIterator(0).nextPermutation();

			if(length == -1) {
				break;
			}
			assertThat(PermutationRotationIteratorTest.firstDiffIndex(permutations, calculator.getIterator(0).getPermutations())).isEqualTo(length);

		} while (true);

		assertEquals(4 * 3 * 2 * 1, count);
	}

	@Test
	void testPermutationsMultipleWorkUnits() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		ParallelBoxItemPermutationRotationIteratorList calculator = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.withParallelizationCount(2)
				.build();

		long countPermutations = calculator.getIterator(0).countPermutations();
		assertEquals(iterator.countPermutations(), countPermutations);

		ParallelBoxItemPermutationRotationIterator iterator0 = calculator.getIterator(0);

		int count0 = 0;
		do {
			assertThat(iterator0.getPermutations()).isEqualTo(iterator.getPermutations());

			count0++;

			int index0 = iterator0.nextPermutation();
			if(index0 == -1) {
				break;
			}
			int index = iterator.nextPermutation();
			if(index == -1) {
				Assertions.fail();
			}
		} while (true);

		assertEquals(5 * 4 * 3, count0);
		assertThat(iterator0.nextPermutation()).isEqualTo(-1);

		iterator.nextPermutation(); // jump to first in next iterator

		ParallelBoxItemPermutationRotationIterator iterator1 = calculator.getIterator(1);

		int count1 = 0;
		do {
			assertMinStackableVolumeValid(iterator1);
			assertMinStackableVolumeValid(iterator);

			assertThat(iterator1.getPermutations()).isEqualTo(iterator.getPermutations());
			count1++;

			int index1 = iterator1.nextPermutation();
			int index = iterator.nextPermutation();
			if(index1 == -1 || index == -1) {
				break;
			}
		} while (true);

		assertEquals(5 * 4 * 3, count1);
		assertThat(iterator1.nextPermutation()).isEqualTo(-1);

		assertThat(iterator.nextPermutation()).isEqualTo(-1);
	}

	@Test
	void testPermutationsMultipleWorkUnitsWithRepeatedItems() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build(), 3));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build(), 4));

		DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator
				.newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.build();

		ParallelBoxItemPermutationRotationIteratorList calculator = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
				.withLoadSize(container.getDx(), container.getDy(), container.getDz())
				.withBoxItems(products)
				.withMaxLoadWeight(products.size())
				.withParallelizationCount(2)
				.build();

		long countPermutations = calculator.countPermutations();
		assertEquals(iterator.countPermutations(), countPermutations);

		for (int i = 0; i < countPermutations / 2; i++) {
			iterator.nextPermutation();
		}

		ParallelBoxItemPermutationRotationIterator iterator1 = calculator.getIterator(1);

		int count = 0;
		do {
			assertMinStackableVolumeValid(iterator1);
			assertMinStackableVolumeValid(iterator);

			assertThat(iterator1.getPermutations()).isEqualTo(iterator.getPermutations());
			count++;

			int index1 = iterator1.nextPermutation();
			int index = iterator.nextPermutation();
			if(index1 == -1 || index == -1) {
				break;
			}
		} while (true);

		assertEquals(8 * 7 * 6 * 5 * 4 * 3 * 2 * 1 / ((3 * 2 * 1) * (4 * 3 * 2 * 1) * 2), count);

		assertThat(calculator.getIterator(1).nextPermutation()).isEqualTo(-1);
	}

	public static long rankPerm(String perm) {
		long rank = 1;
		long suffixPermCount = 1;
		java.util.Map<Character, Integer> charCounts = new java.util.HashMap<Character, Integer>();
		for (int i = perm.length() - 1; i > -1; i--) {
			char x = perm.charAt(i);
			int xCount = charCounts.containsKey(x) ? charCounts.get(x) + 1 : 1;
			charCounts.put(x, xCount);
			for (java.util.Map.Entry<Character, Integer> e : charCounts.entrySet()) {
				if(e.getKey() < x) {
					rank += suffixPermCount * e.getValue() / xCount;
				}
			}
			suffixPermCount *= perm.length() - i;
			suffixPermCount /= xCount;
		}
		return rank;
	}

	// https://stackoverflow.com/questions/22642151/finding-the-ranking-of-a-word-permutations-with-duplicate-letters
	public static String unrankperm(String letters, int rank) {
		java.util.Map<Character, Integer> charCounts = new java.util.HashMap<Character, Integer>();

		int permcount = 1;
		for (int i = 0; i < letters.length(); i++) {
			char x = letters.charAt(i);
			int xCount = charCounts.containsKey(x) ? charCounts.get(x) + 1 : 1;
			charCounts.put(x, xCount);

			permcount = (permcount * (i + 1)) / xCount;
		}
		// ctr is the histogram of letters
		// permcount is the number of distinct perms of letters
		StringBuilder perm = new StringBuilder();

		for (int i = 0; i < letters.length(); i++) {
			List<Character> sorted = new ArrayList<>(charCounts.keySet());
			Collections.sort(sorted);

			for (Character x : sorted) {
				// suffixcount is the number of distinct perms that begin with x
				Integer frequency = charCounts.get(x);
				int suffixcount = permcount * frequency / (letters.length() - i);

				if(rank <= suffixcount) {
					perm.append(x);

					permcount = suffixcount;

					if(frequency == 1) {
						charCounts.remove(x);
					} else {
						charCounts.put(x, frequency - 1);
					}
					break;
				}
				rank -= suffixcount;
			}
		}
		return perm.toString();
	}

	@Test
	void testRemovePermutations() {

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new BoxItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		List<List<Integer>> removes = Arrays.asList(
				Arrays.asList(0, 1),
				Arrays.asList(1, 3),
				Arrays.asList(3, 4));

		for (List<Integer> remove : removes) {

			Dimension container = new Dimension(null, 9, 1, 1);

			ParallelBoxItemPermutationRotationIteratorList calculator = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
					.withLoadSize(container.getDx(), container.getDy(), container.getDz())
					.withBoxItems(products)
					.withMaxLoadWeight(products.size())
					.withParallelizationCount(2)
					.build();

			DefaultBoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getDx(), container.getDy(), container.getDz())
					.withBoxItems(products)
					.withMaxLoadWeight(products.size())
					.build();

			calculator.removePermutations(remove);
			iterator.removePermutations(remove);

			assertEquals(iterator.length(), products.size() - remove.size());

			ParallelBoxItemPermutationRotationIterator iterator0 = calculator.getIterator(0);

			long countPermutations = calculator.getIterator(0).countPermutations();
			assertEquals(iterator.countPermutations(), countPermutations);

			int count0 = 0;
			do {
				assertMinStackableVolumeValid(iterator0);
				assertMinStackableVolumeValid(iterator);

				assertThat(iterator0.getPermutations()).isEqualTo(iterator.getPermutations());

				count0++;

				int index0 = iterator0.nextPermutation();
				if(index0 == -1) {
					break;
				}
				int index = iterator.nextPermutation();
				if(index == -1) {
					Assertions.fail();
				}
			} while (true);

			assertEquals(3, count0);
			assertThat(iterator0.nextPermutation()).isEqualTo(-1);

			iterator.nextPermutation(); // jump to first in next iterator

			ParallelBoxItemPermutationRotationIterator iterator1 = calculator.getIterator(1);

			int count1 = 0;
			do {
				assertThat(iterator1.getPermutations()).isEqualTo(iterator.getPermutations());
				count1++;

				int index1 = iterator1.nextPermutation();
				int index = iterator.nextPermutation();
				if(index1 == -1 || index == -1) {
					break;
				}
			} while (true);

			assertEquals(3, count1);
			assertThat(iterator1.nextPermutation()).isEqualTo(-1);

			assertThat(iterator.nextPermutation()).isEqualTo(-1);
		}
	}

}
