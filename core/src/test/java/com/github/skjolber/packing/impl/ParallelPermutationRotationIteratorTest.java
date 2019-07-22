package com.github.skjolber.packing.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.impl.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.impl.ParallelPermutationRotationIterator;

public class ParallelPermutationRotationIteratorTest {

	@Test
	void testPermutationsSingleWorkUnit() {
		Box container = new Box(9, 1, 1, 0);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("0", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("1", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("2", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("3", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("4", 1, 1, 3, 0), 1));

		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(products, container, true);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(products, container, true, 1);

		assertEquals(iterator.countPermutations(), nthIterator.countPermutations());

		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(0)).isEqualTo(iterator.getPermutations());

			count++;
		} while(nthIterator.nextPermutation(0) && iterator.nextPermutation());

		assertEquals( 5 * 4 * 3 * 2 * 1, count);
		assertFalse(nthIterator.nextPermutation(0));
	}

	@Test
	void testPermutationsMultipleWorkUnits() {
		Box container = new Box(9, 1, 1, 0);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("0", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("1", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("2", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("3", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("4", 1, 1, 3, 0), 1));

		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(products, container, true);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(products, container, true, 2);

		long countPermutations = nthIterator.countPermutations();
		assertEquals(iterator.countPermutations(), countPermutations);

		for(int i = 0; i < countPermutations / 2  - 1; i++) { // -1 because we're starting at the first permutation
			iterator.nextPermutation();
		}
		
		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(1)).isEqualTo(iterator.getPermutations());
			count++;
		} while(nthIterator.nextPermutation(1) && iterator.nextPermutation());

		assertEquals( 5 * 4 * 3, count);
		assertFalse(nthIterator.nextPermutation(1));
	}

	@Test
	void testPermutationsMultipleWorkUnitsWithRepeatedItems() {
		Box container = new Box(9, 1, 1, 0);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("0", 1, 1, 3, 0), 1));
		products.add(new BoxItem(new Box("1", 1, 1, 3, 0), 3));
		products.add(new BoxItem(new Box("2", 1, 1, 3, 0), 4));

		DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(products, container, true);

		ParallelPermutationRotationIterator nthIterator = new ParallelPermutationRotationIterator(products, container, true, 2);

		long countPermutations = nthIterator.countPermutations();
		assertEquals(iterator.countPermutations(), countPermutations);

		for(int i = 0; i < countPermutations / 2  - 1; i++) { // -1 because we're starting at the first permutation
			iterator.nextPermutation();
		}
		
		int count = 0;
		do {
			assertThat(nthIterator.getPermutations(1)).isEqualTo(iterator.getPermutations());
			count++;
		} while(nthIterator.nextPermutation(1) && iterator.nextPermutation());

		assertEquals( 8 * 7 * 6 * 5 * 4 * 3 * 2 * 1 / ((3 * 2 * 1) * (4 * 3 * 2 * 1) * 2), count);
		assertFalse(nthIterator.nextPermutation(1));
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
