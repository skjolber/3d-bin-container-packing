package com.github.skjolber.packing.api.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.packer.StackableItem;

public class ParallelPermutationRotationIteratorTest {

	@Test
	void testPermutationsSingleWorkUnit() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("3").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("4").withWeight(1).build()));		
		
		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(container, products);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(container, products, 1);

		assertEquals(iterator.countPermutations(), nthIterator.countPermutations());

		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(0)).isEqualTo(iterator.getPermutations());

			count++;
		} while(nthIterator.nextPermutation(0) != -1 && iterator.nextPermutation() != -1);

		assertEquals( 5 * 4 * 3 * 2 * 1, count);
		assertThat(nthIterator.nextPermutation(0)).isEqualTo(-1);
	}
	
	@Test
	void testPermutationDifference() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("3").withWeight(1).build()));

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(container, products, 1);
		
		int count = 0;
		do {
			count++;
			
			int[] permutations = PermutationRotationIteratorTest.cloneArray(nthIterator.getPermutations(0));
			
			int length = nthIterator.nextPermutation(0);
			
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

		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("0").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("1").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("2").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("3").withWeight(1).build()));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("4").withWeight(1).build()));		

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
		} while(nthIterator.nextPermutation(1) != -1 && iterator.nextPermutation() != -1);

		assertEquals( 5 * 4 * 3, count);
		assertThat(nthIterator.nextPermutation(1)).isEqualTo(-1);
	}

	@Test
	void testPermutationsMultipleWorkUnitsWithRepeatedItems() {
		Dimension container = new Dimension(null, 9, 1, 1);

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("0").withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("1").withWeight(1).build(), 3));
		products.add(new StackableItem(Box.newBuilder().withRotateXYZ(1, 1, 3, 0, 0).withName("2").withWeight(1).build(), 4));		
		
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
		} while(nthIterator.nextPermutation(1) != -1 && iterator.nextPermutation() != -1);

		assertEquals( 8 * 7 * 6 * 5 * 4 * 3 * 2 * 1 / ((3 * 2 * 1) * (4 * 3 * 2 * 1) * 2), count);
		
		assertThat(nthIterator.nextPermutation(1)).isEqualTo(-1);
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
