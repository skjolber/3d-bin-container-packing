package com.github.skjolber.packing.api;

public class BoxStackValue extends StackValue {

	protected final int weight;
	protected final long pressure;

	public BoxStackValue(int dx, int dy, int dz, int weight, int maxSupportedWeight, int maxSupportedCount,
			int pressureReference) {
		super(dx, dy, dz,  maxSupportedWeight, maxSupportedCount, pressureReference);
		
		this.weight = weight;
		this.pressure = ((long)weight * (long)pressureReference) / ((long)dx * (long)dy);
	}

	public long getPressure() {
		return pressure;
	}
	
	public int getWeight() {
		return weight;
	}
}
