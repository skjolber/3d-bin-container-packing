package com.github.skjolber.packing.api;

import java.util.List;

public abstract class StackValue {

	public final static long PRESSURE_REFERENCE = 1000;
	
	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height
	
	protected final long area;

	protected final StackConstraint constraint;
	
	protected final List<Surface> sides;
	
	public StackValue(int dx, int dy, int dz, StackConstraint constraint, List<Surface> sides) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.constraint = constraint;
		this.sides = sides;
		
		this.area = (long)dx * (long)dy;
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
	
	public boolean fitsInside2D(int dx, int dy) {
		return dx >= this.dx && dy >= this.dy;
	}
	
	public long getArea() {
		return area;
	}
	
	public StackConstraint getConstraint() {
		return constraint;
	}
	
	public List<Surface> getSides() {
		return sides;
	}
	
	@Override
	public String toString() {
		return "StackValue [" + dx + "x" + dy + "x" + dz + "]";
	}
	
}
