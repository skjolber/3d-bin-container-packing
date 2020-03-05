package com.github.skjolber.packing.impl;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.Dimension;

/**
 * 
 * 
 */

public class ParallelPermutationRotationIterator extends DefaultPermutationRotationIterator {

	protected final static int PADDING = 16;
	
	protected final int[] frequencies;
	
	protected WorkUnit[] workUnits;
	
	private static class WorkUnit {
		long count;
		int[] permutations;
		int[] rotations;
		
		// try to avoid false sharing by using padding
		public long t1, t2, t3, t4, t5, t6, t7 = -1L;
		
		public long preventOptmisation(){
			return t1 + t2 + t3 + t4 + t5 + t6 + t7;
		}
	}

	public ParallelPermutationRotationIterator(List<BoxItem> list, Dimension bound, boolean rotate3D, int parallelizationCount) {
		this(bound, toRotationMatrix(list, rotate3D), parallelizationCount);
	}
	
	public ParallelPermutationRotationIterator(Dimension bound, PermutationRotation[] unconstrained, int parallelizationCount) {
		super(bound, unconstrained);
		
		this.frequencies = new int[unconstrained.length];
		
		for (int i = 0; i < unconstrained.length; i++) {
			PermutationRotation permutationRotation = unconstrained[i];
			
			frequencies[i] = permutationRotation.getCount();
		}
		
		workUnits = new WorkUnit[parallelizationCount]; 
		for(int i = 0; i < parallelizationCount; i++) {
			workUnits[i] = new WorkUnit();
			if(workUnits[i].preventOptmisation() != -1L) {
				throw new RuntimeException();
			}
		}
		enforceWorkUnit();
	}
	
	public void removePermutations(int count) {
		for(int i = 0; i < count; i++) {
			frequencies[permutations[PADDING + i]]--;
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

		int[] copyOfFrequencies = new int[frequencies.length];
		for(int i = 0; i < workUnits.length; i++) {
			long rank = (countPermutations * i) / workUnits.length;
			
			if(first == -1) {
				// use classic n-th lexographical permutation algorithm
				workUnits[i].permutations = kthPermutation(frequencies.length, rank);			
				if(workUnits[i].permutations.length < PADDING) {
					throw new RuntimeException("Expected size >= " + PADDING + ", found " + workUnits[i].permutations.length);
				}
			} else {
				// use more complex n-th lexographical permutation algorithm
				System.arraycopy(frequencies, 0, copyOfFrequencies, 0, frequencies.length);
				workUnits[i].permutations = kthPermutation(copyOfFrequencies, count, countPermutations, rank);
				if(workUnits[i].permutations.length < PADDING) {
					throw new RuntimeException("Expected size >= " + PADDING + ", found " + workUnits[i].permutations.length);
				}
			}

			workUnits[i].count = countPermutations / workUnits.length;
			if(countPermutations % workUnits.length > i) {
				workUnits[i].count++;
			}
			
			workUnits[i].rotations = new int[PADDING + rotations.length];
		}
	}
	
	private int getCount() {
		int count = 0;
		for(int f : frequencies) {
			count += f;
		}
		return count;
	}
	
	public int nextRotation(int index) {
		// next rotation
		for(int i = 0; i < workUnits[index].rotations.length - PADDING; i++) {
			if(workUnits[index].rotations[PADDING + i] < matrix[workUnits[index].permutations[PADDING + i]].getBoxes().length - 1) {
				workUnits[index].rotations[PADDING + i]++;

				// reset all previous counters
				System.arraycopy(reset, 0, workUnits[index].rotations, PADDING, i);

				return i;
			}
		}

		return -1;
	}
	

	public int nextPermutation(int index) {
		workUnits[index].count--;
		if(workUnits[index].count > 0) {
			System.arraycopy(reset, 0, workUnits[index].rotations, PADDING, reset.length);

			return nextPermutation(workUnits[index].permutations);
		}
		return -1;
	}
	
	protected int nextPermutation(int[] permutations) {

	    // Find longest non-increasing suffix

	    int i = permutations.length - 1;
	    while (i > PADDING && permutations[i - 1] >= permutations[i])
	        i--;
	    // Now i is the head index of the suffix

	    // Are we at the last permutation already?
	    if (i <= PADDING) {
	        return -1;
	    }

	    // Let array[i - 1] be the pivot
	    // Find rightmost element that exceeds the pivot
	    int j = permutations.length - 1;
	    while (permutations[j] <= permutations[i - 1])
	        j--;
	    // Now the value array[j] will become the new pivot
	    // Assertion: j >= i

	    int head = i - 1 - PADDING;

	    // Swap the pivot with j
	    int temp = permutations[i - 1];
	    permutations[i - 1] = permutations[j];
	    permutations[j] = temp;

	    // Reverse the suffix
	    j = permutations.length - 1;
	    while (i < j) {
	        temp = permutations[i];
	        permutations[i] = permutations[j];
	        permutations[j] = temp;
	        i++;
	        j--;
	    }

	    // Successfully computed the next permutation
	    return head;
	}
	

	@Override
	public long countPermutations() {
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
		int[] result = new int[PADDING + elementCount];
	    
	    for(int i = 0; i < elementCount; i++) {
		    for(int k = 0; k < frequencies.length; k++) {
		    	if(frequencies[k] == 0) {
		    		continue;
		    	}
	            long suffixcount = permutationCount * frequencies[k] / (elementCount - i);
	            if (rank <= suffixcount) {
	            	result[PADDING + i] = k;

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
		final int[] nums = new int[n + PADDING];
		if(n <= 1) {
			return nums;
		}
		
		final int[] factorial = new int[n+1];

		factorial[0] = 1;
		factorial[1] = 1;
		nums[PADDING + 1] = 1;
		
		for (int i = 2; i <= n; i++) {
			nums[PADDING + i-1] = i - 1;
			factorial[i] = i*factorial[i - 1];
		}
		
		if(rank <= 1){
			return nums;
		}
		if(rank >= factorial[n]){
			reverse(nums, PADDING, PADDING + n-1);
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
			shiftRight(nums, PADDING + i, PADDING + i+index);
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

	public int[] getPermutations(int i) {
		int[] result = new int[permutations.length];
		System.arraycopy(workUnits[i].permutations, PADDING, result, 0, result.length);
		return result;
	}

	public PermutationRotationState getState(int index) {
		return new PermutationRotationState(getRotations(index), getPermutations(index));
	}

	private int[] getRotations(int index) {
		int[] result = new int[rotations.length];
		System.arraycopy(workUnits[index].rotations, PADDING, result, 0, result.length);
		return result;
	}
	
	public Box get(int permutationIndex, int index) {
		return matrix[workUnits[index].permutations[PADDING + permutationIndex]].getBoxes()[workUnits[index].rotations[PADDING + permutationIndex]];
	}
}
