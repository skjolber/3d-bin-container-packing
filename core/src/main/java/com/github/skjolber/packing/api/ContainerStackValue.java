package com.github.skjolber.packing.api;

public class ContainerStackValue extends StackValue {

	public ContainerStackValue(int dx, int dy, int dz, int weight, int maxSupportedWeight, int maxSupportedCount,
			int pressureReference, int loadDx, int loadDy, int loadDz, Direction direction) {
		super(dx, dy, dz, weight, maxSupportedWeight, maxSupportedCount, pressureReference, direction);
		
		this.loadDx = loadDx;
		this.loadDy = loadDy;
		this.loadDz = loadDz;
		
		this.loadVolume = loadDx * loadDy * loadDz;
	}

	protected int maxLoadWeight;

	protected final int loadDx; // x
	protected final int loadDy; // y
	protected final int loadDz; // z
	
	protected final int loadVolume;
	
	public int getMaxLoadVolume() {
		return loadVolume;
	}
	
	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public int getLoadDx() {
		return loadDx;
	}
	
	public int getLoadDy() {
		return loadDy;
	}
	
	public int getLoadDz() {
		return loadDz;
	}
}
