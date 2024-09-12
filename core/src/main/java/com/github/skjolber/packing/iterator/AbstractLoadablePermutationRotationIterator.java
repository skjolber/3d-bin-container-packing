package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;

public abstract class AbstractLoadablePermutationRotationIterator implements LoadableItemPermutationRotationIterator {

	protected final IndexedStackableItem[] loadableItems; // by index
	protected int[] reset;
	
	public AbstractLoadablePermutationRotationIterator(IndexedStackableItem[] matrix) {
		this.loadableItems = matrix;
	}

	public IndexedStackableItem[] getMatrix() {
		return loadableItems;
	}
	
	/**
	 * Get number of box items within the constraints.
	 *
	 * @return number between 0 and number of {@linkplain StackableItem}s used in the constructor.
	 */

	public int boxItemLength() {
		return loadableItems.length;
	}

	public long getMinStackableArea(int offset) {
		long minArea = Long.MAX_VALUE;
		for (int i = offset; i < length(); i++) {
			StackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
			}
		}
		return minArea;
	}

	public int getMinStackableAreaIndex(int offset) {
		long minArea = getStackValue(offset).getArea();
		int index = offset;

		for (int i = offset + 1; i < length(); i++) {
			StackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
				index = i;
			}
		}
		return index;
	}

	public List<StackValue> get(PermutationRotationState state, int length) {
		int[] permutations = state.getPermutations();
		int[] rotations = state.getRotations();

		List<StackValue> results = new ArrayList<StackValue>(length);
		for (int i = 0; i < length; i++) {
			results.add(loadableItems[permutations[i]].getStackable().getStackValue(rotations[i]));
		}
		return results;
	}

	public abstract int length();

	public abstract StackValue getStackValue(int index);
}
