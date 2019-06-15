package com.github.skjolberg.packing.impl;

import java.util.List;

import com.github.skjolberg.packing.BoxItem;
import com.github.skjolberg.packing.Dimension;

/**
 * 
 * 
 */

public class NthPermutationRotationIterator extends PermutationRotationIterator {

	private int[] frequencies;
	private int workUnit;
	private int workUnitCount;
	private long workUnitPermutations;
	
	public NthPermutationRotationIterator(List<BoxItem> list, Dimension bound, boolean rotate3D, int workUnitIndex, int workUnitCount) {
		this(bound, toRotationMatrix(list, rotate3D), workUnitIndex, workUnitCount);
	}
	
	public NthPermutationRotationIterator(Dimension bound, PermutationRotation[] unconstrained, int workUnitIndex, int workUnitCount) {
		super(bound, unconstrained);
		
		this.workUnit = workUnitIndex;
		this.workUnitCount = workUnitCount;
		
		this.frequencies = new int[unconstrained.length];
		
		for (int i = 0; i < unconstrained.length; i++) {
			PermutationRotation permutationRotation = unconstrained[i];
			
			frequencies[i] = permutationRotation.getCount();
		}
		
		enforceWorkUnit();
	}
	
	public void removePermutations(int count) {
		for(int i = 0; i < count; i++) {
			frequencies[permutations[i]]--;
		}
		super.removePermutations(count);
		
		enforceWorkUnit();
	}
	
	public void removePermutations(List<Integer> removed) {
		for (Integer integer : removed) {
			frequencies[integer]--;
		}
		super.removePermutations(removed);
		
		enforceWorkUnit();
	}

	private void enforceWorkUnit() {
		int count = getCount();
		
		long countPermutations;
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			countPermutations = getPermutationCount(count);
			
		} else {
			countPermutations = getPermutationCountWithRepeatedItems(count, first);
		}
		
		if(countPermutations == -1L) {
			throw new IllegalArgumentException();
		}
		
		long rank = (countPermutations * workUnit) / workUnitCount;
		
		if(first == -1) {
			// use classic n-th lexographical permutation algorithm
			this.permutations = kthPermutation(frequencies.length, rank);			
		} else {
			// use more complex n-th lexographical permutation algorithm
			int[] copyOfFrequencies = new int[frequencies.length];
			System.arraycopy(frequencies, 0, copyOfFrequencies, 0, frequencies.length);
			this.permutations = kthPermutation(copyOfFrequencies, count, countPermutations, rank);
		}
		
		this.workUnitPermutations = countPermutations / workUnitCount;
		if(countPermutations % workUnitCount > workUnit) {
			workUnitPermutations++;
		}
	}
	
	private int getCount() {
		int count = 0;
		for(int f : frequencies) {
			count += f;
		}
		return count;
	}

	@Override
	public boolean nextPermutation() {
		workUnitPermutations--;
		if(workUnitPermutations > 0) {
			return super.nextPermutation();
		}
		return false;
	}

	@Override
	long countPermutations() {
		return countPermutations(getCount());
	}

	long countPermutations(int count) {
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			return getPermutationCount(count);
		} else {
			return getPermutationCountWithRepeatedItems(count, first);
		}
	}

	private long getPermutationCount(int count) {
		long permutationCount = 1;
		for(int i = 0; i < count; i++) {
			if(Long.MAX_VALUE / (i + 1) <= permutationCount) {
				return -1L;
			}
			permutationCount = permutationCount * (i + 1);
		}
		return permutationCount;
	}

	private long getPermutationCountWithRepeatedItems(int count, int first) {
		long permutationCount = 1;
		// cancel out the first set of factors
		// 
		// For [3, 4] this would look like:
		//
		// 1 * 2 * 3 * 4 * 5 * 6 * 7
		// -----------------------------
		// (1 * 2 * 3) (1 * 2 * 3 * 4)
		//
		// which is equal to
		//
		// 4 * 5 * 6 * 7
		// -----------------------------
		// (1 * 2 * 3 * 4)
		//
		// above the line:
		for(int i = frequencies[first]; i < count; i++) {
			if(Long.MAX_VALUE / (i + 1) <= permutationCount) {
				return -1L;
			}
			permutationCount = permutationCount * (i + 1);
		}
		// below the line:
		for(int i = first + 1; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				for(int k = 1; k < frequencies[i]; k++) {
					permutationCount = permutationCount / (k + 1);
				}
			}
		}
		// future improvement: cancel out more
		return permutationCount;
	}
	
	private static int firstDuplicate(int[] frequencies) {
		for(int i = 0; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				return i;
			}
		}
		return -1;
	}	

	static int[] kthPermutation(int[] frequencies, int elementCount, long permutationCount, long rank) {
		int[] result = new int[elementCount];
	    
	    for(int i = 0; i < elementCount; i++) {
		    for(int k = 0; k < frequencies.length; k++) {
		    	if(frequencies[k] == 0) {
		    		continue;
		    	}
	            long suffixcount = permutationCount * frequencies[k] / (elementCount - i);
	            if (rank <= suffixcount) {
	            	result[i] = k;

	            	permutationCount = suffixcount;

	            	frequencies[k]--;
	            	break;
	            }
	            rank -= suffixcount;
		    }
	    }
		return result;
	}

	static int[] kthPermutation(int n, long rank) {
		// http://www.zrzahid.com/k-th-permutation-sequence/
		if(n > 13) {
			throw new IllegalArgumentException();
		}
		
		final int[] nums = new int[n];
		final int[] factorial = new int[n+1];

		factorial[0] = 1;
		factorial[1] = 1;
		nums[1] = 1;
		
		for (int i = 2; i <= n; i++) {
			nums[i-1] = i - 1;
			factorial[i] = i*factorial[i - 1];
		}
		
		if(rank <= 1){
			return nums;
		}
		if(rank >= factorial[n]){
			reverse(nums, 0, n-1);
			return nums;
		}
		
		rank -= 1;//0-based 
		for(int i = 0; i < n-1; i++){
			int fact = factorial[n-i-1];
			//index of the element in the rest of the input set
			//to put at i position (note, index is offset by i)
			int index = (int) (rank/fact);
			//put the element at index (offset by i) element at position i 
			//and shift the rest on the right of i
			shiftRight(nums, i, i+index);
			//decrement k by fact*index as we can have fact number of 
			//permutations for each element at position less than index
			rank = rank - fact*index;
		}
		
		return nums;
	}

	private static void shiftRight(int[] a, int s, int e){
		int temp = a[e];
		for(int i = e; i > s; i--){
			a[i] = a[i-1];
		}
		a[s] = temp;
	}

	public static void reverse(int A[], int i, int j){
		while(i < j){
			swap(A, i, j);
			i++;
			j--;
		}
	}

	private static void swap(int[] a, int i, int j) {
		int spare = a[i];
		a[i] = a[j];
		a[j] = spare;
	}
	
	public static boolean nextPermutation(int[] nums) {
	    int k = -1;
	    for (int i = nums.length - 2; i >= 0; i--) {
	        if (nums[i] < nums[i + 1]) {
	            k = i;
	            break;
	        }
	    } 
	    if (k == -1) {
	        reverse(nums, 0, nums.length-1);
	        return false;
	    }
	    int l = -1;
	    for (int i = nums.length - 1; i > k; i--) {
	        if (nums[i] > nums[k]) {
	            l = i;
	            break;
	        } 
	    }
	    swap(nums, k, l);
	    reverse(nums, k + 1, nums.length-1); 
	    
	    return true;
	}

	
}
