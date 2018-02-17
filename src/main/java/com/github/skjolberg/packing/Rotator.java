package com.github.skjolberg.packing;

import java.util.List;

public class Rotator {

	private Box[][] matrix;
	private int[] reset;
	private int[] rotations;

	public Rotator(List<Box> list, Dimension bound, boolean rotate3D) {
		this(bound, toRotationMatrix(list, rotate3D));
	}

	public Rotator(Dimension bound, Box[][] matrix) {
		this.matrix = matrix;
		
		// constrain matrix
		for(int i = 0; i < matrix.length; i++) {
			for(int k = 0; k < matrix[i].length; k++) {
				if(!matrix[i][k].fitsInside3D(bound)) {
					matrix[i][k] = null;
				}
			}
		}
		
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
				boxes[i][1] = boxes[i][0].clone().rotate3D();
				boxes[i][2] = boxes[i][1].clone().rotate3D();
				boxes[i][3] = boxes[i][2].clone().rotate2D3D();
				boxes[i][4] = boxes[i][3].clone().rotate3D();
				boxes[i][5] = boxes[i][4].clone().rotate3D();
			} else {
				boxes[i][0] = box.clone();
				boxes[i][1] = box.clone().rotate2D();
			}
		}
		return boxes;
	}
	
	public int rotate(int height) {
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
				
				return i;
			}
		}
		
		return -1;
	}
	
	public boolean isConstrained(int fromIndex, int height) {
		for(int i = fromIndex; i < matrix.length; i++) {
			if(matrix[i][rotations[i]].getHeight() > height) {
				return true;
			}
		}
		return false;
	}
	
	public Box getBox(int index) {
		return matrix[index][rotations[index]];
	}

	public void reset() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}
	
	public long count() {
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
}
