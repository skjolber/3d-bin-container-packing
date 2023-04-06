package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.Arrays;

public class DefaultContainer extends Container {

	private static final long serialVersionUID = 1L;

	protected final ContainerStackValue[] stackValues;
	protected final Stack stack;

	public DefaultContainer(String id, String description, BigDecimal volume, BigDecimal emptyWeight, ContainerStackValue[] stackValues, Stack stack) {
		super(id, description, volume, emptyWeight, getMaxLoadVolume(stackValues), getMaxLoadWeight(stackValues), calculateMinimumArea(stackValues), getMaximumArea(stackValues));

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
		return new DefaultContainer(id, description, volume, emptyWeight, stackValues, new DefaultStack());
	}

	@Override
	public boolean canLoad(Stackable stackable) {
		if(stackable.getVolume().compareTo(maxLoadVolume) > 0) {
			return false;
		}
		if(stackable.getWeight().compareTo(maxLoadWeight) > 0) {
			return false;
		}
		for (ContainerStackValue stackValue : stackValues) {
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
