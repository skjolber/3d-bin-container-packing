package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;

public class PermutationStackableValue {

	protected final int index;
	protected final int count;
	protected final PermutationRotation[] values;
	protected final Box box;

	protected final long minVolumeLimit;
	protected final long minAreaLimit;

	public PermutationStackableValue(int index, int count, Box box, List<BoxStackValue> stackValues) {
		this.index = index;
		this.count = count;
		this.values = new PermutationRotation[stackValues.size()];
		this.box = box;

		long minVolumeLimit = Long.MAX_VALUE;
		long minAreaLimit = Long.MAX_VALUE;

		for (int i = 0; i < values.length; i++) {
			BoxStackValue stackValue = stackValues.get(i);

			values[i] = new PermutationRotation(box, stackValue);

			if(minVolumeLimit > stackValue.getVolume()) {
				minVolumeLimit = stackValue.getVolume();
			}

			if(minAreaLimit > stackValue.getArea()) {
				minAreaLimit = stackValue.getArea();
			}
		}

		this.minAreaLimit = minAreaLimit;
		this.minVolumeLimit = minVolumeLimit;
	}
	
	public PermutationStackableValue(PermutationStackableValue clone) {
		// clone working object in order to improve performance
		this.index = clone.index;
		this.count = clone.count;
		this.values = new PermutationRotation[clone.values.length];
		this.box = clone.box.clone();
		for (int i = 0; i < values.length; i++) {
			PermutationRotation permutationRotation = clone.values[i];
			values[i] = new PermutationRotation(permutationRotation.getBox().clone(), permutationRotation.getBoxStackValue().clone());
			
		}
		this.minAreaLimit = clone.minAreaLimit;
		this.minVolumeLimit = clone.minVolumeLimit;

	}

	public PermutationRotation[] getBoxes() {
		return values;
	}

	public int getCount() {
		return count;
	}

	public long getMinAreaLimit() {
		return minAreaLimit;
	}

	public long getMinVolumeLimit() {
		return minVolumeLimit;
	}

	public int getIndex() {
		return index;
	}

	public Box getStackable() {
		return box;
	}
	
	public PermutationStackableValue clone() {
		return new PermutationStackableValue(this);
	}
}
