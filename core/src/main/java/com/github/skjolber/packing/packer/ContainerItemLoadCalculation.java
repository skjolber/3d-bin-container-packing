package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;
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
	

	public long getCostByWeight() {
		Container container1 = containerItem.getContainer();

		int maxLoadWeight = container1.getMaxLoadWeight();
		long maxLoadVolume = container1.getMaxLoadVolume();

		long weight1;
		if(loadableVolume <= maxLoadVolume && loadableWeight <= maxLoadWeight) {
			// can hold all the volume and weight
			weight1 = loadableWeight;
		} else {
			// cannot hold all the weight or the volume
			long maxVolume = Math.min(maxLoadVolume, loadableVolume);
			
			long calculatedWeight = (loadableWeight * maxVolume) / loadableVolume;
			
			weight1 = Math.min(calculatedWeight, maxLoadWeight);
		}

		return containerItem.getCostCalculator().getCostPerWeight(weight1);
	}

	public long getCostByVolume() {
		Container container1 = containerItem.getContainer();

		int maxLoadWeight = container1.getMaxLoadWeight();
		long maxLoadVolume = container1.getMaxLoadVolume();

		long volume;
		if(loadableVolume <= maxLoadVolume && loadableWeight <= maxLoadWeight) {
			// can hold all the volume and weight
			volume = loadableVolume;
		} else {
			// cannot hold all the weight or the volume
			long maxWeight = Math.min(maxLoadWeight, loadableWeight);
			
			long calculatedVolume = (loadableVolume * maxWeight) / loadableWeight;
			
			volume = Math.min(calculatedVolume, maxLoadVolume);
		}

		return containerItem.getCostCalculator().getCostPerVolume(volume);
	}
}
