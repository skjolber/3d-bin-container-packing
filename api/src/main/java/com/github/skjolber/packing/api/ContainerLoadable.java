package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

public class ContainerLoadable {

	protected final ContainerLoadStackValue[] values;
	protected final Stackable stackable;

	protected final long minVolumeLimit;
	protected final long minAreaLimit;

	public ContainerLoadable(Stackable stackable, List<StackValue> stackValues) {
		this.values = new ContainerLoadStackValue[stackValues.size()];
		this.stackable = stackable;

		long minVolumeLimit = Long.MAX_VALUE;
		long minAreaLimit = Long.MAX_VALUE;

		for (int i = 0; i < values.length; i++) {
			StackValue stackValue = stackValues.get(i);

			values[i] = new ContainerLoadStackValue(stackable, stackValue);

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
	
	public ContainerLoadable(ContainerLoadable clone) {
		// clone working object in order to improve performance
		this.values = new ContainerLoadStackValue[clone.values.length];
		this.stackable = clone.stackable.clone();
		for (int i = 0; i < values.length; i++) {
			ContainerLoadStackValue permutationRotation = clone.values[i];
			values[i] = new ContainerLoadStackValue(permutationRotation.getStackable().clone(), permutationRotation.getValue().clone());
			
		}
		this.minAreaLimit = clone.minAreaLimit;
		this.minVolumeLimit = clone.minVolumeLimit;

	}

	public ContainerLoadStackValue[] getBoxes() {
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
	
	public ContainerLoadable clone() {
		return new ContainerLoadable(this);
	}
}
