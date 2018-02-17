package com.github.skjolberg.packing;

import java.util.List;

/**
 * 
 * Rotation and permutations built into the same class. Minimizes the number of rotations.
 * <br><br>
 * The maximum number of combinations is n! * 6^n, however after accounting for bounds and 
 * sides with equal lengths the number can be a lot lower (and this number can 
 * be obtained before starting the calculation).
 * 
 * @see <a href="https://www.nayuki.io/page/next-lexicographical-permutation-algorithm" target="_top">next-lexicographical-permutation-algorithm</a>
 */

public class PermutationRotationIterator {

	private Box[][] matrix;
	private int[] reset;
	private int[] rotations; // 6^n
	private int[] permutations; // n!

	public PermutationRotationIterator(List<Box> list, Dimension bound, boolean rotate3D) {
		this(bound, toRotationMatrix(list, rotate3D));
	}

	public PermutationRotationIterator(Dimension bound, Box[][] unconstrained) {
		Box[][] matrix = new Box[unconstrained.length][];
		for(int i = 0; i < unconstrained.length; i++) {
			matrix[i] = new Box[unconstrained[i].length];
			
			for(int k = 0; k < unconstrained[i].length; k++) {
				if(unconstrained[i][k] != null && unconstrained[i][k].fitsInside3D(bound)) {
					matrix[i][k] = unconstrained[i][k];
				}
			}
		}

		this.matrix = matrix;

		// permutations is a 'pointer' list
		permutations = new int[matrix.length];
		for(int i = 0; i < matrix.length; i++) {
			permutations[i] = i;
		}
		
		// rotation baseline - make sure the rotation resets to rotations actually present
		reset = new int[matrix.length];
		for(int i = 0; i < reset.length; i++) {
			int index = 0;
			while(matrix[i][index] == null) {
				index++;
			}
			reset[i] = index;
		}
		rotations = new int[reset.length];
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	public static Box[][] toRotationMatrix(List<Box> list, boolean rotate3D) {
		Box[][] boxes = new Box[list.size()][];
		for(int i = 0; i < list.size(); i++) {
			boxes[i] = new Box[rotate3D ? 6 : 2];
			
			Box box = list.get(i);
			
			if(rotate3D) {
				boxes[i][0] = box.clone();
				
				if(box.isSquare3D()) {
					continue;
				}
				
				boxes[i][1] = boxes[i][0].clone().rotate3D();
				boxes[i][2] = boxes[i][1].clone().rotate3D();

				// do not rotate 2d if square
				if(!boxes[i][0].isSquare2D()) {
					boxes[i][3] = boxes[i][0].clone().rotate2D();
				}

				if(!boxes[i][1].isSquare2D()) {
					boxes[i][4] = boxes[i][1].clone().rotate2D();
				}

				if(!boxes[i][2].isSquare2D()) {
					boxes[i][5] = boxes[i][2].clone().rotate2D();
				}
			} else {
				boxes[i][0] = box.clone();
				
				if(boxes[i][0].isSquare2D()) {
					continue;
				}
				boxes[i][1] = boxes[i][0].clone().rotate2D();
			}
		}
		return boxes;
	}
	
	public boolean nextRotation() {
		// next rotation
		for(int i = 0; i < rotations.length; i++) {
			while(rotations[i] < matrix[i].length - 1) {
				rotations[i]++;
				
				if(matrix[i][rotations[i]] == null) {
					continue;
				}

				// reset all previous counter to minimal
				for(int k = 0; k < i; k++) {
					rotations[k] = reset[k];
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isWithinHeight(int fromIndex, int height) {
		for(int i = fromIndex; i < matrix.length; i++) {
			if(matrix[permutations[i]][rotations[permutations[i]]].getHeight() > height) {
				return false;
			}
		}
		return true;
	}
	
	protected void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}
	
	public long countRotations() {
		long n = 1;
		for(int i = 0; i < rotations.length; i++) {
			int factor = 0;
			for(int k = 0; k < matrix[i].length; k++) {
				if(matrix[i][k] != null) {
					factor++;
				}
			}
			n = n * factor;
		}
		return n;
	}
	
	public long countPermutations() {
		long n = 1;
		for(int i = 0; i < matrix.length; i++) {
			n = n * (i + 1);
		}
		return n;
	}

	
	public Box get(int index) {
		return matrix[permutations[index]][rotations[permutations[index]]];
	}
	
	public boolean nextPermutation() {
		resetRotations();
		
	    // Find longest non-increasing suffix
		
	    int i = permutations.length - 1;
	    while (i > 0 && permutations[i - 1] >= permutations[i])
	        i--;
	    // Now i is the head index of the suffix
	    
	    // Are we at the last permutation already?
	    if (i <= 0) {
	        return false;
	    }
	    
	    // Let array[i - 1] be the pivot
	    // Find rightmost element that exceeds the pivot
	    int j = permutations.length - 1;
	    while (permutations[j] <= permutations[i - 1])
	        j--;
	    // Now the value array[j] will become the new pivot
	    // Assertion: j >= i
	    
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
	    return true;
	}
	
	public int length() {
		return rotations.length;
	}

}
