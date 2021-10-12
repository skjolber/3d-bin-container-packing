package com.github.skjolber.packing.api;

public abstract class StackValue {

	public final static long PRESSURE_REFERENCE = 1000;
	
	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height
	
	protected final long area;
	protected final long volume;

	protected final StackConstraint constraint;
	
	public StackValue(int dx, int dy, int dz, StackConstraint constraint) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.constraint = constraint;
		
		this.area = (long)dx * (long)dy;
		this.volume = area * (long)dz;
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
	
	public long getArea() {
		return area;
	}
	
	public abstract int getWeight();

	public StackConstraint getConstraint() {
		return constraint;
	}
	
	@Override
	public String toString() {
		return "StackValue [dx=" + dx + ", dy=" + dy + ", dz=" + dz + "]";
	}
	
	public abstract Stackable getStackable();
	
}
