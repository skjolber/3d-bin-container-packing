package com.github.skjolber.packing.api;

public class DefaultContainerStackValue extends ContainerStackValue {

	protected final Stack stack;

	public DefaultContainerStackValue(
			int dx, int dy, int dz, 
			int maxSupportedWeight, int maxSupportedCount,
			int pressureReference, int loadDx, int loadDy, int loadDz, 
			int emptyWeight, int maxLoadWeight, 
			Stack stack) {
		super(dx, dy, dz, maxSupportedWeight, maxSupportedCount, pressureReference, loadDx, loadDy, loadDz, emptyWeight, maxLoadWeight);
		
		this.stack = stack;
	}
	
	@Override
	public long getPressure() {
		return ((long)getWeight() * (long)pressureReference) / area;
	}

	@Override
	public int getWeight() {
		return emptyWeight + stack.getWeight();
	}
	
}
