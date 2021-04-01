package com.github.skjolber.packing.api;

import com.github.skjolber.packing.Dimension;

public abstract class StackValue {

	// structural integrity for this oritentation
	protected final int maxSupportedCount;
	protected final int maxSupportedWeight;
	
	protected final long maxSupportedPressure;
	
	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height
	
	protected final long area;
	protected final long volume;

	protected final int pressureReference;
	
	public StackValue(int dx, int dy, int dz, int maxSupportedWeight, int maxSupportedCount, int pressureReference) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		
		this.maxSupportedWeight = maxSupportedWeight;
		this.maxSupportedCount = maxSupportedCount;
		this.pressureReference = pressureReference;
				
		this.area = (long)dx * (long)dy;
		this.volume = area * (long)dz;
		this.maxSupportedPressure = ( (long)maxSupportedWeight * (long)pressureReference) / area;
	}

	public int getDx() {
		return dx;
	}
	
	public int getDy() {
		return dy;
	}
	
	public int getDz() {
		return dz;
	}

	public int getPressureReference() {
		return pressureReference;
	}
	
	public long getVolume() {
		return volume;
	}

	/**
	 * Check whether this object fits within a dimension (without rotation).
	 *
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space
	 */

	public boolean fitsInside3D(Dimension dimension) {
		return fitsInside3D(dimension.getWidth(), dimension.getDepth(), dimension.getHeight());
	}

	public boolean fitsInside3D(int dx, int dy, int dz) {
		return dx >= this.dx && dy >= this.dy && dz >= this.dz;
	}
	
	public int getMaxSupportedCount() {
		return maxSupportedCount;
	}
	
	public long getMaxSupportedPressure() {
		return maxSupportedPressure;
	}
	
	public int getMaxSupportedWeight() {
		return maxSupportedWeight;
	}
	
	public long getArea() {
		return area;
	}
	
	public abstract long getPressure();
	
	public abstract int getWeight();
}
