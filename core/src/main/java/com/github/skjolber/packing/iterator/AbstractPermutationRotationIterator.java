package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;

public abstract class AbstractPermutationRotationIterator {

	protected final PermutationStackableValue[] matrix;
	protected int[] reset;
	
	public AbstractPermutationRotationIterator(Dimension bound, List<StackableItem> unconstrained) {
		List<PermutationStackableValue> matrix = new ArrayList<>(unconstrained.size());
		for(int i = 0; i < unconstrained.size(); i++) {
			StackableItem item = unconstrained.get(i);
			
			Stackable stackable = item.getStackable();
			List<StackValue> boundRotations = stackable.rotations(bound);
			
			// create PermutationRotation even if this box does not fit at all, 
			// so that permutation indexes are directly comparable between parallel instances of this class
			matrix.add(new PermutationStackableValue(item.getCount(), stackable, boundRotations));
		}
		
		this.matrix = matrix.toArray(new PermutationStackableValue[matrix.size()]);
	}
	
	/**
	 * Get number of box items within the constraints.
	 *
	 * @return number between 0 and number of {@linkplain StackableItem}s used in the constructor.
	 */

	public int boxItemLength() {
		return matrix.length;
	}
	
	public long getMinStackableVolume() {
		long minVolume = Integer.MAX_VALUE;
		for (PermutationStackableValue permutationStackableValue : matrix) {
			if(permutationStackableValue.getMinVolumeLimit() < minVolume) {
				minVolume = permutationStackableValue.getMinVolumeLimit();
			}
		}
		return minVolume;
	}

	public long getMinStackableArea() {
		long minArea = Integer.MAX_VALUE;
		for (PermutationStackableValue permutationStackableValue : matrix) {
			if(permutationStackableValue.getMinAreaLimit() < minArea) {
				minArea = permutationStackableValue.getMinAreaLimit();
			}
		}
		return minArea;
	}
	
	
	public List<PermutationRotation> get(PermutationRotationState state, int length) {
		int[] permutations = state.getPermutations();
		int[] rotations = state.getRotations();
		
		List<PermutationRotation> results = new ArrayList<PermutationRotation>(length);
		for(int i = 0; i < length; i++) {
			results.add(matrix[permutations[i]].getBoxes()[rotations[i]]);
		}
		return results;
	}

	public int length() {
		return reset.length;
	}

}
