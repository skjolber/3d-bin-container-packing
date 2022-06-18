package com.github.skjolber.packing.iterator;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;

/**
 * 
 * 
 */

public class ParallelPermutationRotationIterator extends AbstractPermutationRotationIterator {

	protected final static int PADDING = 16;
	protected final int[] frequencies;
	protected WorkUnit[] workUnits;
	
	// try to avoid false sharing by using padding
	public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;
	
	public long preventOptmisation(){
		return t0 + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15;
	}
	
	private static class WorkUnit {
		long count;
		int[] permutations;
		int[] rotations;

		int[] lastPermutation;
		int lastPermutationMaxIndex;
		
		// try to avoid false sharing by using padding
		public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;
		
		public long preventOptmisation(){
			return t0 + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15;
		}
	}

	public ParallelPermutationRotationIterator(Dimension bound, List<StackableItem> unconstrained, int parallelizationCount) {
		super(bound, unconstrained);
		
		this.frequencies = new int[unconstrained.size()];
		
		int count = 0;
		for (int i = 0; i < unconstrained.size(); i++) {
			StackableItem permutationRotation = unconstrained.get(i);
			
			frequencies[i] = permutationRotation.getCount();
			
			count += frequencies[i];
		}
		this.reset = new int[count];
		
		workUnits = new WorkUnit[parallelizationCount]; 
		for(int i = 0; i < parallelizationCount; i++) {
			workUnits[i] = new WorkUnit();
			if(workUnits[i].preventOptmisation() != -1L) {
				throw new RuntimeException();
			}
		}
		
		enforceWorkUnit();
	}
	
	public void removePermutations(List<Integer> removed) {
		for (Integer integer : removed) {
			frequencies[integer]--;
		}
		this.reset = new int[reset.length - removed.size()];
		
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
			
			workUnits[i].rotations = new int[PADDING + reset.length];
		}
		
		for(int i = 0; i < workUnits.length - 1; i++) {
			int[] lastPermutation = new int[workUnits[i + 1].permutations.length];
			System.arraycopy(workUnits[i + 1].permutations, 0, lastPermutation, 0, lastPermutation.length);
			
			workUnits[i].lastPermutation = lastPermutation;
			
			// find the first item that differs, so that we do not have to
			// compare items for each iteration (to detect whether we have done enough work)
			for(int k = PADDING; k < lastPermutation.length; k++) {
				if(workUnits[i].permutations[k] != lastPermutation[k]) {
					workUnits[i].lastPermutationMaxIndex = k;
					
					break;
				}
			}
		}
	}
	
	private int getCount() {
		int count = 0;
		for(int f : frequencies) {
			count += f;
		}
		return count;
	}
	
	public int nextWorkUnitRotation(int index) {
		// next rotation
		int[] workUnitRotations = workUnits[index].rotations;
		return nextWorkUnitRotation(index, workUnitRotations.length - 1 - PADDING);
	}
	
	public int nextWorkUnitRotation(int index, int maxIndex) {
		// next rotation
		int[] workUnitRotations = workUnits[index].rotations;
		for(int i = PADDING + maxIndex; i >= PADDING; i--) {
			if(workUnitRotations[i] < matrix[workUnits[index].permutations[i]].getBoxes().length - 1) {
				workUnitRotations[i]++;

				// reset all following counters
				System.arraycopy(reset, 0, workUnitRotations, i + 1, workUnitRotations.length - (i + 1));

				return i - PADDING;
			}
		}

		return -1;
	}

	public int nextWorkUnitPermutation(int index) {
		workUnits[index].count--;
		if(workUnits[index].count > 0) {
			System.arraycopy(reset, 0, workUnits[index].rotations, PADDING, reset.length);

			return nextPermutation(workUnits[index].permutations);
		}
		return -1;
	}
	
	protected int nextPermutation(int[] permutations) {
		// https://www.baeldung.com/cs/array-generate-all-permutations#permutations-in-lexicographic-order
		
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
		int[] result = new int[workUnits[i].permutations.length - PADDING];
		System.arraycopy(workUnits[i].permutations, PADDING, result, 0, result.length);
		return result;
	}

	public PermutationRotationState getState(int index) {
		return new PermutationRotationState(getRotations(index), getPermutations(index));
	}

	private int[] getRotations(int index) {
		int[] result = new int[reset.length];
		System.arraycopy(workUnits[index].rotations, PADDING, result, 0, result.length);
		return result;
	}
	
	public PermutationRotation get(int index, int permutationIndex) {
		return matrix[workUnits[index].permutations[PADDING + permutationIndex]].getBoxes()[workUnits[index].rotations[PADDING + permutationIndex]];
	}

	public int nextWorkUnitPermutation(int index, int maxIndex) {
		workUnits[index].count--;
		if(workUnits[index].count > 0) {
			// reset rotations
			System.arraycopy(reset, 0, workUnits[index].rotations, PADDING, reset.length);

			int resultIndex = nextWorkUnitPermutation(workUnits[index].permutations, maxIndex);
			
			if(index < workUnits.length - 1) {
				if(resultIndex <= workUnits[index].lastPermutationMaxIndex) {
					// TODO initial check for bounds here
					
					// are we still within our designated range?
					// the next permutation must be lexicographically less than the first permutation
					// in the next block
					
					int[] lastPermutation = workUnits[index].lastPermutation;
					int[] currentPermutation = workUnits[index].permutations;
					int i = PADDING;
					while(i <= workUnits[index].lastPermutationMaxIndex) {
						if(currentPermutation[i] < lastPermutation[i]) {
							return resultIndex;
						} else if(currentPermutation[i] > lastPermutation[i]) {
							return -1;
						}
						i++;
					}
				}
			}			
			
			return resultIndex;
		}
		return -1;
	}
	
	public int nextWorkUnitPermutation(int[] permutations, int maxIndex) {
		while(maxIndex >= 0) {
		
			int current = permutations[PADDING + maxIndex];
			
			int minIndex = -1;
			for(int i = PADDING + maxIndex + 1; i < permutations.length; i++) {
				if(current < permutations[i] && (minIndex == -1 || permutations[i] < permutations[minIndex])) {
					minIndex = i;
				}
			}
			
			if(minIndex == -1) {
				maxIndex--;
				
				continue;
			}
			
			// swap indexes
		    permutations[PADDING + maxIndex] = permutations[minIndex];
		    permutations[minIndex] = current;
		    
		    Arrays.sort(permutations, PADDING + maxIndex + 1, permutations.length);
		    
		    return maxIndex;
		}
		return -1;
	}

}
