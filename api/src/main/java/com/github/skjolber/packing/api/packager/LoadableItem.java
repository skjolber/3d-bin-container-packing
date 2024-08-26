package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class LoadableItem {

	protected final List<StackValue> values;
	protected final Stackable stackable;

	private int count;
	
	protected final long minVolumeLimit;
	protected final long minAreaLimit;

	public LoadableItem(Stackable stackable, List<StackValue> stackValues, int count) {
		this.values = stackValues;
		this.stackable = stackable;
		this.count = count;
		
		long minVolumeLimit = Long.MAX_VALUE;
		long minAreaLimit = Long.MAX_VALUE;

		for (int i = 0; i < values.size(); i++) {
			StackValue stackValue = values.get(i);

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
	
	public List<StackValue> getValues() {
		return values;
	}

	public long getMinAreaLimit() {
		return minAreaLimit;
	}

	public long getMinVolumeLimit() {
		return minVolumeLimit;
	}

	public Stackable getStackable() {
		return stackable;
	}

	public int getCount() {
		return count;
	}
	
	public void decrement() {
		count--;
	}

	public boolean isEmpty() {
		return count == 0;
	}
}
