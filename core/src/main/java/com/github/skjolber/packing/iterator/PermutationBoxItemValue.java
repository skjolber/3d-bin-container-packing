package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

public class PermutationBoxItemValue {

	protected final int index;
	protected final int count;
	protected final PermutationRotation[] values;
	protected final BoxItem boxItem;

	protected final long minVolumeLimit;
	protected final long minAreaLimit;

	public PermutationBoxItemValue(int index, int count, BoxItem boxItem, List<BoxStackValue> stackValues) {
		this.index = index;
		this.count = count;
		this.values = new PermutationRotation[stackValues.size()];
		this.boxItem = boxItem;

		long minVolumeLimit = Long.MAX_VALUE;
		long minAreaLimit = Long.MAX_VALUE;

		for (int i = 0; i < values.length; i++) {
			BoxStackValue stackValue = stackValues.get(i);

			values[i] = new PermutationRotation(boxItem, stackValue);

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
	
	public PermutationBoxItemValue(PermutationBoxItemValue copy) {
		// copy working object in order to improve performance
		this.index = copy.index;
		this.count = copy.count;
		this.values = new PermutationRotation[copy.values.length];
		this.boxItem = copy.boxItem.copy();
		for (int i = 0; i < values.length; i++) {
			PermutationRotation permutationRotation = copy.values[i];
			values[i] = new PermutationRotation(permutationRotation.getBoxItem().copy(), permutationRotation.getBoxStackValue().copy());
			
		}
		this.minAreaLimit = copy.minAreaLimit;
		this.minVolumeLimit = copy.minVolumeLimit;

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

	public BoxItem getStackable() {
		return boxItem;
	}
	
	public PermutationBoxItemValue copy() {
		return new PermutationBoxItemValue(this);
	}
}
