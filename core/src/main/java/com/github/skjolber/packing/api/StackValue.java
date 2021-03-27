package com.github.skjolber.packing.api;

public class StackValue {

	protected final int maxSupportedCount;
	protected final int maxSupportedWeight;
	protected final long maxSupportedPressure;
	
	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height
	
	protected final long pressure;
	protected final long area;
	
	protected final long volume;
	
	protected final int weight;
	
	protected final Direction direction;
	
	protected final int pressureReference;
	
	public StackValue(int dx, int dy, int dz, int weight, int maxSupportedWeight, int maxSupportedCount, int pressureReference, Direction direction) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.weight = weight;
		
		this.maxSupportedWeight = maxSupportedWeight;
		this.maxSupportedCount = maxSupportedCount;
		this.pressureReference = pressureReference;
		this.direction = direction;
				
		this.pressure = ((long)weight * (long)pressureReference) / ((long)dx * (long)dy);
		this.area = dx * dy;
		
		this.volume = dx * dy * dz;
		
		this.maxSupportedPressure = ( (long)maxSupportedWeight * (long)pressureReference) / ((long)dx * (long)dy);
	}
	
	private StackValue next3d;
	private StackValue previous3d;
	
	private StackValue next2d;
	private StackValue previous2d;

	public int getDx() {
		return dx;
	}
	
	public int getDy() {
		return dy;
	}
	
	public int getDz() {
		return dz;
	}

	public int getWeight() {
		return weight;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
	public int getPressureReference() {
		return pressureReference;
	}
	
	public StackValue getNext2d() {
		return next2d;
	}
	
	public StackValue getNext3d() {
		return next3d;
	}
	
	public StackValue getPrevious2d() {
		return previous2d;
	}
	
	public StackValue getPrevious3d() {
		return previous3d;
	}
}
