package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.Dimension;

public class NthPermutationRotationIterator extends PermutationRotationIterator {

	public NthPermutationRotationIterator(Dimension bound, PermutationRotation[] unconstrained) {
		super(bound, unconstrained);
	}

	public static int[] unrank(int[] frequencies, int rank) {
		// https://stackoverflow.com/questions/22642151/finding-the-ranking-of-a-word-permutations-with-duplicate-letters
		
		int count = 0;
		for(int f : frequencies) {
			count += f;
		}
		
		int permutationCount = 1;
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			for(int i = 0; i < count; i++) {
				permutationCount = permutationCount * (i + 1);
			}
			
			// use classic n-th lexographical permutation algorithm
			throw new RuntimeException("Not implemented");
		} else {
			for(int i = frequencies[first]; i < count; i++) {
				permutationCount = permutationCount * (i + 1);
			}
			for(int i = first + 1; i < frequencies.length; i++) {
				if(frequencies[i] > 1) {
					for(int k = 1; k < frequencies[i]; k++) {
						permutationCount = permutationCount / (k + 1);
					}
				}
			}
			
		    return unrank(frequencies, count, permutationCount, rank);
		}
		
	}
	
	private static int firstDuplicate(int[] frequencies) {
		for(int i = 0; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				return i;
			}
		}
		return -1;
	}	

	private static int[] unrank(int[] frequencies, int elementCount, int permutationCount, int rank) {
		int[] result = new int[elementCount];
	    
	    for(int i = 0; i < elementCount; i++) {
		    for(int k = 0; k < frequencies.length; k++) {
		    	if(frequencies[k] == 0) {
		    		continue;
		    	}
	            int suffixcount = permutationCount * frequencies[k] / (elementCount - i);
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

	public void skipToPermutation(int k) {
		kthPermutation(permutations.length, k);
		
		
	}

	public static int[] kthPermutation(int n, int k) {
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
		
		if(k <= 1){
			return nums;
		}
		if(k >= factorial[n]){
			reverse(nums, 0, n-1);
			return nums;
		}
		
		k -= 1;//0-based 
		for(int i = 0; i < n-1; i++){
			int fact = factorial[n-i-1];
			//index of the element in the rest of the input set
			//to put at i position (note, index is offset by i)
			int index = (k/fact);
			//put the element at index (offset by i) element at position i 
			//and shift the rest on the right of i
			shiftRight(nums, i, i+index);
			//decrement k by fact*index as we can have fact number of 
			//permutations for each element at position less than index
			k = k - fact*index;
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

    private static boolean nextPermutation2(int[] numList)
    {
        /*
         Knuths
         1. Find the largest index j such that a[j] < a[j + 1]. If no such index exists, the permutation is the last permutation.
         2. Find the largest index l such that a[j] < a[l]. Since j + 1 is such an index, l is well defined and satisfies j < l.
         3. Swap a[j] with a[l].
         4. Reverse the sequence from a[j + 1] up to and including the final element a[n].

         */
        int largestIndex = -1;
        for (int i = numList.length - 2; i >= 0; i--)
        {
            if (numList[i] < numList[i + 1]) {
                largestIndex = i;
                break;
            }
        }

        if (largestIndex < 0) return false;

        int largestIndex2 = -1;
        for (int i = numList.length - 1 ; i >= 0; i--) {
            if (numList[largestIndex] < numList[i]) {
                largestIndex2 = i;
                break;
            }
        }

        int tmp = numList[largestIndex];
        numList[largestIndex] = numList[largestIndex2];
        numList[largestIndex2] = tmp;

        for (int i = largestIndex + 1, j = numList.length - 1; i < j; i++, j--) {
            tmp = numList[i];
            numList[i] = numList[j];
            numList[j] = tmp;
        }

        return true;
    }
	
}
