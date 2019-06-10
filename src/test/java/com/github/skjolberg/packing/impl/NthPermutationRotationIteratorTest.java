package com.github.skjolberg.packing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

public class NthPermutationRotationIteratorTest {

	@Test
	public void testUnranking() {
		
		int[] frequencies = new int[2];
		
		int count = (7 * 6 * 5 * 4 * 3 * 2 * 1) / ((4 * 3 * 2 * 1) * (3 * 2 * 1));
		for(int i = 0; i < count; i++) {
			frequencies[0] = 3;
			frequencies[1] = 4;

			System.out.println(Arrays.asList(NthPermutationRotationIterator.unrank(frequencies, i + 1)));
		}
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
	
	@Test
	public void testOriginal() {
		int count = (7 * 6 * 5 * 4 * 3 * 2 * 1) / ((4 * 3 * 2 * 1) * (3 * 2 * 1));
		System.out.println("Count " + count);
		for(int i = 0; i < count; i++) {
			System.out.println(unrankperm("0001111", i + 1));
		}
	}
}
