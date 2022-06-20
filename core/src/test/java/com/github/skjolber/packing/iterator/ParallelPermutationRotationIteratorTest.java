package com.github.skjolber.packing.iterator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;

public class ParallelPermutationRotationIteratorTest {

	@Test
	void testPermutationsSingleWorkUnit() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));		
		
		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(container, products);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(container, products, 1);

		assertEquals(iterator.countPermutations(), nthIterator.countPermutations());

		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(0)).isEqualTo(iterator.getPermutations());
			count++;
		} while(nthIterator.nextWorkUnitPermutation(0) != -1 && iterator.nextPermutation() != -1);

		assertEquals( 5 * 4 * 3 * 2 * 1, count);
		assertThat(nthIterator.nextWorkUnitPermutation(0)).isEqualTo(-1);
	}
	
	@Test
	void testPermutationsForMaxIndexInRightOrder() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		long max = 5 * 4 * 3 * 2 * 1;
		
		for(int i = 0; i < 3; i++) {
			int l = i + 1;
			
			DefaultPermutationRotationIterator rotator1 = new DefaultPermutationRotationIterator(container, products);
			ParallelPermutationRotationIterator rotator2 = new ParallelPermutationRotationIterator(container, products, l);
	
			long limit = rotator1.countPermutations() / l;
			long count = 0;
			do {
				int[] permutations1 = rotator1.getPermutations();
				int[] permutations2 = rotator2.getPermutations(0);
				
				assertArrayEquals(permutations1, permutations2);
				
				count++;
			} while(count < limit && rotator1.nextPermutation(rotator1.length() - 1) != -1 && rotator2.nextWorkUnitPermutation(0) != -1);
	
			assertEquals(max / l, limit);
		}
	}
	
	@Test
	void testPermutationsSkip() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));

		for(int i = 0; i < 3; i++) {
			int l = i + 1;
			
			DefaultPermutationRotationIterator rotator1 = new DefaultPermutationRotationIterator(container, products);
			ParallelPermutationRotationIterator rotator2 = new ParallelPermutationRotationIterator(container, products, l);

			int permutationIndex1;
			do {
				permutationIndex1 = rotator1.nextPermutation();
			} while(permutationIndex1 > 2);
			
			int permutationIndex2 = rotator2.nextWorkUnitPermutation(0, 2);
			assertArrayEquals(rotator1.getPermutations(), rotator2.getPermutations(0));
		}
	}


	
	@Test
	void testPermutationCorrectIndexReturned() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(container, products, 1);
		
		int count = 0;
		do {
			count++;
			
			int[] permutations = PermutationRotationIteratorTest.cloneArray(nthIterator.getPermutations(0));
			
			int length = nthIterator.nextWorkUnitPermutation(0);
			
			if(length == -1) {
				break;
			}
			assertThat(PermutationRotationIteratorTest.firstDiffIndex(permutations, nthIterator.getPermutations(0))).isEqualTo(length);
			
		} while(true);

		assertEquals(4 * 3 * 2 * 1, count);
	}	

	@Test
	void testPermutationsMultipleWorkUnits() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("3").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("4").withWeight(1).build()));		

		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(container, products);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(container, products, 2);

		long countPermutations = nthIterator.countPermutations();
		assertEquals(iterator.countPermutations(), countPermutations);

		for(int i = 0; i < countPermutations / 2  - 1; i++) { // -1 because we're starting at the first permutation
			iterator.nextPermutation();
		}
		
		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(1)).isEqualTo(iterator.getPermutations());
			count++;
		} while(nthIterator.nextWorkUnitPermutation(1) != -1 && iterator.nextPermutation() != -1);

		assertEquals( 5 * 4 * 3, count);
		assertThat(nthIterator.nextWorkUnitPermutation(1)).isEqualTo(-1);
	}

	@Test
	void testPermutationsMultipleWorkUnitsWithRepeatedItems() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("0").withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("1").withWeight(1).build(), 3));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1, 1, 3).withDescription("2").withWeight(1).build(), 4));		
		
		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(container, products);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(container, products, 2);

		long countPermutations = nthIterator.countPermutations();
		assertEquals(iterator.countPermutations(), countPermutations);

		for(int i = 0; i < countPermutations / 2  - 1; i++) { // -1 because we're starting at the first permutation
			iterator.nextPermutation();
		}
		
		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(1)).isEqualTo(iterator.getPermutations());
			count++;
		} while(nthIterator.nextWorkUnitPermutation(1) != -1 && iterator.nextPermutation() != -1);

		assertEquals( 8 * 7 * 6 * 5 * 4 * 3 * 2 * 1 / ((3 * 2 * 1) * (4 * 3 * 2 * 1) * 2), count);
		
		assertThat(nthIterator.nextWorkUnitPermutation(1)).isEqualTo(-1);
	}
	
	public static long rankPerm(String perm) {
	    long rank = 1;
	    long suffixPermCount = 1;
	    java.util.Map<Character, Integer> charCounts =
	        new java.util.HashMap<Character, Integer>();
	    for (int i = perm.length() - 1; i > -1; i--) {
	        char x = perm.charAt(i);
	        int xCount = charCounts.containsKey(x) ? charCounts.get(x) + 1 : 1;
	        charCounts.put(x, xCount);
	        for (java.util.Map.Entry<Character, Integer> e : charCounts.entrySet()) {
	            if (e.getKey() < x) {
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
	    for(int i = 0; i < letters.length(); i++) {
	        char x = letters.charAt(i);
	        int xCount = charCounts.containsKey(x) ? charCounts.get(x) + 1 : 1;
	        charCounts.put(x, xCount);

	        permcount = (permcount * (i + 1)) / xCount;
	    }
	    // ctr is the histogram of letters
	    // permcount is the number of distinct perms of letters
	    StringBuilder perm = new StringBuilder();
	    
	    for(int i = 0; i < letters.length(); i++) {
		    List<Character> sorted = new ArrayList<>(charCounts.keySet());
		    Collections.sort(sorted);
		    
	    	for(Character x : sorted) {
	            // suffixcount is the number of distinct perms that begin with x
	    		Integer frequency = charCounts.get(x);
	            int suffixcount = permcount * frequency / (letters.length() - i); 
	            
	            if (rank <= suffixcount) {
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

}
