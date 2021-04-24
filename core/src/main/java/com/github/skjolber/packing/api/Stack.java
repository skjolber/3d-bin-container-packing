package com.github.skjolber.packing.api;

import java.util.List;

public abstract class Stack {

	protected ContainerStackValue containerStackValue;
	
	public ContainerStackValue getContainerStackValue() {
		return containerStackValue;
	}
	
	public abstract List<StackPlacement> getPlacements();

	public abstract void add(StackPlacement e);

	public long getFreeVolumeLoad() {
		return containerStackValue.getMaxLoadVolume() - getVolume();
	}

	public int getFreeWeightLoad() {
		return containerStackValue.getMaxLoadWeight() - getWeight();
	}
	
	public abstract int getWeight();

	public abstract int getDz();

	public abstract long getVolume();

	public abstract void clear();

	public abstract boolean isEmpty();

}
