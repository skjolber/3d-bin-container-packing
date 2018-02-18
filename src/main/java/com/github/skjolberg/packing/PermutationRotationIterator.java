package com.github.skjolberg.packing;

import java.util.ArrayList;
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


	public static Box[][] toRotationMatrix(List<Box> list, boolean rotate3D) {
		Box[][] boxes = new Box[list.size()][];
		for(int i = 0; i < list.size(); i++) {
			boxes[i] = new Box[rotate3D ? 6 : 2];
			
			Box box = list.get(i);
			
			List<Box> result = new ArrayList<>();
			if(rotate3D) {
				Box box0 = box.clone();
				boolean square0 = box.isSquare2D();
				
				result.add(box0);
				
				if(!box.isSquare3D()) {
					
					box.rotate3D();
					boolean square1 = box.isSquare2D();
					
					result.add(box.clone());
	
					box.rotate3D();
					boolean square2 = box.isSquare2D();
	
					result.add(box.clone());
	
					if(!square0 && !square1 && !square2) {
						box.rotate2D3D();
						
						result.add(box.clone());
						
						box.rotate3D();
		
						result.add(box.clone());
		
						box.rotate3D();
		
						result.add(box.clone());
					}
				}
			} else {
				result.add(box.clone());
				
				// do not rotate 2d if square
				if(!box.isSquare2D()) {
					result.add(box.clone().rotate2D());
				}
			}

			boxes[i] = result.toArray(new Box[result.size()]);
		}
		return boxes;
	}
	
	private Box[][] matrix;
	private int[] reset;
	private int[] rotations; // 2^n or 6^n
	private int[] permutations; // n!

	public PermutationRotationIterator(List<Box> list, Dimension bound, boolean rotate3D) {
		this(bound, toRotationMatrix(list, rotate3D));
	}

	public PermutationRotationIterator(Dimension bound, Box[][] unconstrained) {
		Box[][] matrix = new Box[unconstrained.length][];
		for(int i = 0; i < unconstrained.length; i++) {
			List<Box> result = new ArrayList<>();
			
			for(int k = 0; k < unconstrained[i].length; k++) {
				if(unconstrained[i][k] != null && unconstrained[i][k].fitsInside3D(bound)) {
					result.add(unconstrained[i][k]);
				}
			}
			matrix[i] = result.toArray(new Box[result.size()]);
		}

		this.matrix = matrix;

		// permutations is a 'pointer' list
		permutations = new int[matrix.length];
		for(int i = 0; i < matrix.length; i++) {
			permutations[i] = i;
		}
		
		reset = new int[matrix.length];
		rotations = new int[reset.length];
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	
	public boolean nextRotation() {
		// next rotation
		for(int i = 0; i < rotations.length; i++) {
			while(rotations[i] < matrix[i].length - 1) {
				rotations[i]++;
				
				// reset all previous counters 
				System.arraycopy(reset, 0, rotations, 0, i);
				
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
			n = n * matrix[i].length;
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
