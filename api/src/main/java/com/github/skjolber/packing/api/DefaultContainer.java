package com.github.skjolber.packing.api;

import java.util.Arrays;

public class DefaultContainer extends Container {

	protected final ContainerStackValue[] stackValues;
	protected final Stack stack;
	
	public DefaultContainer(String name, long volume, int emptyWeight, ContainerStackValue[] stackValues, Stack stack) {
		super(name, volume, emptyWeight, getMaxLoadVolume(stackValues), getMaxLoadWeight(stackValues), calculateMinimumArea(stackValues), getMaximumArea(stackValues));
		
		this.stackValues = stackValues;
		this.stack = stack;
	}
	
	@Override
	public ContainerStackValue[] getStackValues() {
		return stackValues;
	}

	@Override
	public Stack getStack() {
		return stack;
	}

	@Override
	public DefaultContainer clone() {
		return new DefaultContainer(name, volume, emptyWeight, stackValues, new DefaultStack());
	}
	
	@Override
	public boolean canLoad(Stackable stackable) {
		if(stackable.getVolume() > maxLoadVolume) {
			return false;
		}
		if(stackable.getWeight() > maxLoadWeight) {
			return false;
		}
		for(ContainerStackValue stackValue : stackValues) {
			if(stackValue.canLoad(stackable)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public String toString() {
		return "DefaultContainer [stackValues=" + Arrays.toString(stackValues) + "]";
	}


	
}
