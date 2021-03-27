package com.github.skjolber.packing.api;

public class DefaultContainer extends Container {

	protected final ContainerStackValue[] stackValues;
	protected Stack stack;
	protected final long volume;
	
	public DefaultContainer(String name, long volume, int emptyWeight, long volumeCapacity, int weightCapacity, ContainerStackValue[] stackValues) {
		super(name, emptyWeight, volumeCapacity, weightCapacity);
		this.volume = volume;
		this.stackValues = stackValues;
	}

	@Override
	public ContainerStackValue[] getStackValues() {
		return stackValues;
	}

	@Override
	public Stack getStack() {
		return stack;
	}

	public long getVolume() {
		return volume;
	}
	
	public void setStack(Stack stack) {
		this.stack = stack;
	}
}
