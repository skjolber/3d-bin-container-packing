package com.github.skjolber.packing.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.*;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;

public class DefaultPermutationRotationIterator implements PermutationRotationIterator {

	protected final PermutationStackableValue[] matrix;
	protected final Dimension dimension;
	protected int[] reset;
	protected int[] rotations; // 2^n or 6^n
	
	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	public DefaultPermutationRotationIterator(Dimension bound, List<StackableItem> unconstrained) {
		List<Integer> types = new ArrayList<>(unconstrained.size() * 2);

		List<PermutationStackableValue> matrix = new ArrayList<>(unconstrained.size());
		for(int i = 0; i < unconstrained.size(); i++) {
			StackableItem item = unconstrained.get(i);
			Stackable rotations = item.getStackable().rotations(bound);
			
			// create PermutationRotation even if this box does not fit at all, 
			// so that permutation indexes are directly comparable between parallel instances of this class
			matrix.add(new PermutationStackableValue(item.getCount(), rotations));

			if(rotations.getStackValues().length > 0) {
				for(int k = 0; k < item.getCount(); k++) {
					types.add(i);
				}
			}
		}

		this.matrix = matrix.toArray(new PermutationStackableValue[matrix.size()]);

		this.dimension = bound;
		this.reset = new int[types.size()];
		this.rotations = new int[types.size()];
		
		// permutations is a 'pointer' list
		// keep the the number of permutations tight;
		// identical boxes need not be interchanged
		permutations = new int[types.size()];

		for(int i = 0; i < permutations.length; i++) {
			permutations[i] = types.get(i);
		}
	}

	@Override
	public void removePermutations(int count) {
		this.rotations = new int[rotations.length - count];
		this.reset = new int[rotations.length];
		
		// discard a number of items
		int newLength = permutations.length - count;

		int[] permutations = new int[this.permutations.length - count];
		System.arraycopy(this.permutations, count, permutations, 0, newLength);
		Arrays.sort(permutations); // ascending order to make the permutation logic work

		this.permutations = permutations;
	}
	
	/**
	 * Remove permutations, if present.
	 */

	@Override
	public void removePermutations(List<Integer> removed) {
		int left = permutations.length;
		permutations:
		for (Integer remove : removed) {
			for (int k = 0; k < permutations.length; k++) {
				if(remove == permutations[k]) {
					permutations[k] = -1; // mark as removed
					left--;
					continue permutations;
				}
			}
		}
		
		int[] effectivePermutations = new int[left];
		int destinationIndex = 0;
		
		for(int i = 0; i < permutations.length; i++) {
			if(permutations[i] != -1) {
				effectivePermutations[destinationIndex] = permutations[i];
				destinationIndex++;
			}
		}
		
		Arrays.sort(effectivePermutations); // ascending order to make the permutation logic work
		
		this.permutations = effectivePermutations;
		
		this.rotations = new int[effectivePermutations.length];
		this.reset = new int[effectivePermutations.length];
	}

	@Override
	public int nextRotation() {
		// next rotation
		for(int i = 0; i < rotations.length; i++) {
			if(rotations[i] < matrix[permutations[i]].getBoxes().length - 1) {
				rotations[i]++;

				// reset all previous counters
				System.arraycopy(reset, 0, rotations, 0, i);

				return i;
			}
		}

		return -1;
	}

	@Override
	public int[] getPermutations() {
		return permutations;
	}

	private void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	public long countRotations() {
		long n = 1;
		for (final int rotation : rotations) {
			if (Long.MAX_VALUE / matrix[rotation].getBoxes().length <= n) {
				return -1L;
			}

			n = n * matrix[rotation].getBoxes().length;
		}
		return n;
	}

	/**
	 * Return number of permutations for boxes which fit within this container.
	 * 
	 * @return permutation count
	 */
	
	long countPermutations() {
		// reduce permutations for boxes which are duplicated
		
		// could be further bounded by looking at how many boxes (i.e. n x the smallest) which actually
		// fit within the container volume

		int maxCount = 0;
		for (final PermutationStackableValue aMatrix1 : matrix) {
			if (maxCount < aMatrix1.getCount()) {
				maxCount = aMatrix1.getCount();
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (final PermutationStackableValue aMatrix : matrix) {
				for (int k = 0; k < aMatrix.getCount(); k++) {
					factors[k]++;
				}
			}

			for(long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}

				n = n * (i + 1);

				for(int k = 1; k < maxCount; k++) {
					while(factors[k] > 0 && n % (k + 1) == 0) {
						n = n / (k + 1);

						factors[k]--;
					}
				}
			}

			for(int k = 1; k < maxCount; k++) {
				while(factors[k] > 0) {
					n = n / (k + 1);

					factors[k]--;
				}
			}
		} else {
			for(long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}
				n = n * (i + 1);
			}
		}
		return n;
	}


	@Override
	public PermutationRotation get(int index) {
		return matrix[permutations[index]].getBoxes()[rotations[index]];
	}

	@Override
	public int nextPermutation() {
		resetRotations();

	    // Find longest non-increasing suffix
	    int i = permutations.length - 1;
	    while (i > 0 && permutations[i - 1] >= permutations[i])
	        i--;
	    // Now i is the head index of the suffix

	    // Are we at the last permutation already?
	    if (i <= 0) {
	        return -1;
	    }
	    

	    // Let array[i - 1] be the pivot
	    // Find rightmost element that exceeds the pivot
	    int j = permutations.length - 1;
	    while (permutations[j] <= permutations[i - 1])
	        j--;
	    // Now the value array[j] will become the new pivot
	    // Assertion: j >= i

	    int head = i - 1;

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
	public int length() {
		return permutations.length;
	}

	@Override
	public PermutationRotationState getState() {
		return new PermutationRotationState(rotations, permutations);
	}

	@Override
	public void setState(PermutationRotationState state) {
		this.rotations = state.getRotations();
		this.permutations = state.getPermutations();
	}

	/**
	 * Get number of box items within the constraints.
	 *
	 * @return number between 0 and number of {@linkplain BoxItem}s used in the constructor.
	 */

	int boxItemLength() {
		return matrix.length;
	}

}
