package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.List;

/**
 * Basic build block for stacking multiple boxes or containers. 
 * Denotes a size (i.e. actual dimensions and downwards surface) and stacking constraints. 
 * A box which can be rotated typically contain 2 (2D rotation) or 6 (3D rotation) of this class.   
 * 
 */

public abstract class StackValue implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height
	
	protected final long area;

	protected final StackConstraint constraint;
	
	protected final List<Surface> surfaces;
	protected long volume;
	
	public StackValue(int dx, int dy, int dz, StackConstraint constraint, List<Surface> surfaces) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.constraint = constraint;
		this.surfaces = surfaces;
		
		this.area = (long)dx * (long)dy;
		this.volume = (long)dx * (long)dy * (long)dz;
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
		return fitsInside3D(dimension.getDx(), dimension.getDy(), dimension.getDz());
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
	
	public long getVolume() {
		return volume;
	}
	
	public StackConstraint getConstraint() {
		return constraint;
	}
	
	public List<Surface> getSurfaces() {
		return surfaces;
	}
	
	@Override
	public String toString() {
		return "StackValue[" + surfaces + " " + dx + "x" + dy + "x" + dz + "]";
	}
	
}
