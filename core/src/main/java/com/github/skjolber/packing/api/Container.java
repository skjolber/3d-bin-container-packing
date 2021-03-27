package com.github.skjolber.packing.api;

public abstract class Container extends Stackable {

	protected final int emptyWeight;
	
	protected final long maxLoadVolume;
	protected final int maxLoadWeight;

	public Container(String name, int emptyWeight, long volumeCapacity, int weightCapacity) {
		super(name);
		
		this.emptyWeight = emptyWeight;
		this.maxLoadVolume = volumeCapacity;
		this.maxLoadWeight = weightCapacity;
	}
	
	@Override
	public int getWeight() {
		int weight = emptyWeight;
		
		for (StackEntry stackEntry : getStack().getEntries()) {
			weight += stackEntry.getStackable().getWeight();
		}
		
		return weight;
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}
	
	public long getMaxLoadWeight() {
		return maxLoadWeight;
	}
	
	@Override
	public abstract ContainerStackValue[] getStackValues();

	public abstract Stack getStack();
	
	public int getEmptyWeight() {
		return emptyWeight;
	}
	
	public int getMaxWeight() {
		return emptyWeight + maxLoadWeight;
	}
}
