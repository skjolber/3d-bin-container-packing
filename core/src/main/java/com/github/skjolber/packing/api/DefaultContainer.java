package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.Dimension;

public class DefaultContainer extends Container {

	protected final ContainerStackValue[] stackValues;
	protected final Stack stack;
	protected final long volume;
	
	public DefaultContainer(String name, long volume, int emptyWeight, long maxLoadVolume, int maxLoadWeight, ContainerStackValue[] stackValues, Stack stack) {
		super(name, emptyWeight, maxLoadVolume, maxLoadWeight);
		this.volume = volume;
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

	public long getVolume() {
		return volume;
	}

	@Override
	public DefaultContainer rotations(Dimension bound) {
		// TODO optimize if max is above min bounds 
		for (int i = 0; i < stackValues.length; i++) {
			ContainerStackValue stackValue = stackValues[i];
			if(stackValue.fitsInside3D(bound)) {
				List<ContainerStackValue> fitsInside = new ArrayList<>(stackValues.length);
				fitsInside.add(stackValue);
				
				while(++i < stackValues.length) {
					if(stackValues[i].fitsInside3D(bound)) {
						fitsInside.add(stackValues[i]);
					}
				}
				return new DefaultContainer(name, volume, emptyWeight, maxLoadVolume, maxLoadWeight, fitsInside.toArray(new ContainerStackValue[fitsInside.size()]), stack);
			}
		}
		return null;
	}
}
