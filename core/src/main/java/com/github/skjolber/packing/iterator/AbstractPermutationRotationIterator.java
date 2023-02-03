package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackableItem;

public abstract class AbstractPermutationRotationIterator implements PermutationRotationIterator {

	protected final PermutationStackableValue[] matrix;
	protected int[] reset;

	public AbstractPermutationRotationIterator(PermutationStackableValue[] matrix) {
		this.matrix = matrix;
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
		long minVolume = Long.MAX_VALUE;
		for (PermutationStackableValue permutationStackableValue : matrix) {
			if(permutationStackableValue.getMinVolumeLimit() < minVolume) {
				minVolume = permutationStackableValue.getMinVolumeLimit();
			}
		}
		return minVolume;
	}

	public long getMinStackableVolume(int offset) {
		long minVolume = Long.MAX_VALUE;
		for (int i = offset; i < length(); i++) {
			PermutationRotation permutationRotation = get(i);
			long volume = permutationRotation.getValue().getVolume();
			if(volume < minVolume) {
				minVolume = volume;
			}
		}
		return minVolume;
	}

	public long getMinStackableArea() {
		long minArea = Long.MAX_VALUE;
		for (PermutationStackableValue permutationStackableValue : matrix) {
			if(permutationStackableValue.getMinAreaLimit() < minArea) {
				minArea = permutationStackableValue.getMinAreaLimit();
			}
		}
		return minArea;
	}

	public long getMinStackableArea(int offset) {
		long minArea = Long.MAX_VALUE;
		for (int i = offset; i < length(); i++) {
			PermutationRotation permutationRotation = get(i);
			long area = permutationRotation.getValue().getArea();
			if(area < minArea) {
				minArea = area;
			}
		}
		return minArea;
	}

	public int getMinStackableAreaIndex(int offset) {
		long minArea = get(offset).getValue().getArea();
		int index = offset;

		for (int i = offset + 1; i < length(); i++) {
			PermutationRotation permutationRotation = get(i);
			long area = permutationRotation.getValue().getArea();
			if(area < minArea) {
				minArea = area;
				index = i;
			}
		}
		return index;
	}

	public int getMinStackableVolumeIndex(int offset) {
		long minVolume = get(offset).getValue().getVolume();
		int index = offset;

		for (int i = offset + 1; i < length(); i++) {
			PermutationRotation permutationRotation = get(i);
			long volume = permutationRotation.getValue().getVolume();
			if(volume < minVolume) {
				minVolume = volume;
				index = i;
			}
		}
		return index;
	}

	public List<PermutationRotation> get(PermutationRotationState state, int length) {
		int[] permutations = state.getPermutations();
		int[] rotations = state.getRotations();

		List<PermutationRotation> results = new ArrayList<PermutationRotation>(length);
		for (int i = 0; i < length; i++) {
			results.add(matrix[permutations[i]].getBoxes()[rotations[i]]);
		}
		return results;
	}

	public int length() {
		return reset.length;
	}

}
