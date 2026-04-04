package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class ContainerItemLoadCalculation {

	// how much of the current boxes will fit into the container 
	private long loadableVolume;
	private long loadableWeight;
	
	private ControlledContainerItem containerItem;
	private int index;

	public ContainerItemLoadCalculation(long loadableVolume, long loadableWeight, ControlledContainerItem containerItem, int index) {
		super();
		this.loadableVolume = loadableVolume;
		this.loadableWeight = loadableWeight;
		this.containerItem = containerItem;
		this.index = index;
	}

	public ContainerItem getContainer() {
		return containerItem;
	}
	
	public ContainerCostCalculator getCostCalculator() {
		return containerItem.getCostCalculator();
	}

	public long getLoadableVolume() {
		return loadableVolume;
	}

	public long getLoadableWeight() {
		return loadableWeight;
	}

	public int getIndex() {
		return index;
	}
}
