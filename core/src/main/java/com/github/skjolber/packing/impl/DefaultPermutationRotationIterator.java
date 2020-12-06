package com.github.skjolber.packing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.*;

public class DefaultPermutationRotationIterator implements PermutationRotationIterator {

	public static PermutationRotation[] toRotationMatrix(List<BoxItem> list, boolean rotate3D) {
		PermutationRotation[] boxes = new PermutationRotation[list.size()];
		for(int i = 0; i < list.size(); i++) {
			Box box = list.get(i).getBox();

			// make sure not to rotate the original box, so that it stays in its original orentation.

			List<Box> result = new ArrayList<>();
			if(rotate3D) {
				Box box0 = box.clone();
				boolean square0 = box.isSquare2D();

				result.add(box0);

				if(!box.isSquare3D()) {

					box = box.clone().rotate3D();
					boolean square1 = box.isSquare2D();

					result.add(box);

					box = box.clone().rotate3D();
					boolean square2 = box.isSquare2D();

					result.add(box);

					if(!square0 && !square1 && !square2) {
						box = box.clone().rotate2D3D();
						result.add(box);

						box = box.clone().rotate3D();
						result.add(box);

						box = box.clone().rotate3D();
						result.add(box);
					}
				}
			} else {
				Box box0 = box.clone();
				result.add(box0);
				
				// do not rotate 2d if square
				if(!box0.isSquare2D()) {
					result.add(box0.clone().rotate2D());
				}
			}

			boxes[i] = new PermutationRotation(list.get(i).getCount(), result.toArray(new Box[result.size()]));
		}
		return boxes;
	}

	protected final PermutationRotation[] matrix;
	protected final Dimension dimension;
	protected int[] reset;
	protected int[] rotations; // 2^n or 6^n
	
	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	public DefaultPermutationRotationIterator(List<BoxItem> list, Dimension bound, boolean rotate3D) {
		this(bound, toRotationMatrix(list, rotate3D));
	}

	public DefaultPermutationRotationIterator(Dimension bound, PermutationRotation[] unconstrained) {
		List<Integer> types = new ArrayList<>(unconstrained.length * 2);

		List<PermutationRotation> matrix = new ArrayList<>(unconstrained.length);
		for(int i = 0; i < unconstrained.length; i++) {
			List<Box> result = new ArrayList<>();

			Box[] boxes = unconstrained[i].getBoxes();
			for (final Box box : boxes) {
				if (box != null && box.fitsInside3D(bound)) {
					result.add(box);
				}
			}

			// create PermutationRotation even if this box does not fit at all, 
			// so that permutation indexes are directly comparable between parallel instances of this class
			matrix.add(new PermutationRotation(unconstrained[i].getCount(), result.toArray(new Box[result.size()])));

			if(!result.isEmpty()) {
				for(int k = 0; k < unconstrained[i].getCount(); k++) {
					types.add(i);
				}
			}
		}

		this.matrix = matrix.toArray(new PermutationRotation[matrix.size()]);

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
		for (final PermutationRotation aMatrix1 : matrix) {
			if (maxCount < aMatrix1.getCount()) {
				maxCount = aMatrix1.getCount();
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (final PermutationRotation aMatrix : matrix) {
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
	public Box get(int index) {
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
