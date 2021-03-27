package com.github.skjolber.packing.api;

import java.util.List;

public abstract class Stack {

	protected ContainerStackValue containerStackValue;
	
	public ContainerStackValue getContainerStackValue() {
		return containerStackValue;
	}
	
	public abstract List<StackEntry> getEntries();

	public abstract void add(StackEntry e);

	public long getFreeVolumeLoad() {
		return containerStackValue.getMaxLoadVolume() - getVolume();
	}

	public int getFreeWeightLoad() {
		return containerStackValue.getMaxLoadWeight() - getWeight();
	}
	
	public abstract void clear();

	public abstract int getWeight();

	public abstract int getDz();

	public abstract long getVolume();

}
