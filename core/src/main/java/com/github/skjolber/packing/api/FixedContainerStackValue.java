package com.github.skjolber.packing.api;

public class FixedContainerStackValue extends ContainerStackValue {

	protected final int weight;
	protected final long pressure;

	public FixedContainerStackValue(
			int dx, int dy, int dz, 
			int maxSupportedWeight, int maxSupportedCount,
			int stackWeight,
			int pressureReference, int loadDx, int loadDy, int loadDz, int emptyWeight, int maxLoadWeight) {
		super(dx, dy, dz, maxSupportedWeight, maxSupportedCount, pressureReference, loadDx, loadDy, loadDz, emptyWeight, maxLoadWeight);
					
		this.weight = stackWeight + emptyWeight;
		this.pressure = ((long)weight * (long)pressureReference) / area;
	}

	public long getPressure() {
		return pressure;
	}
	
	public int getWeight() {
		return weight;
	}
	
}
