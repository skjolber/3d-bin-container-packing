package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Basic build block for stacking multiple boxes or containers.
 * Denotes a size (i.e. actual dimensions and downwards surface) and stacking constraints.
 * A box which can be rotated typically contain 2 (2D rotation) or 6 (3D rotation) of this class.
 * 
 */

public abstract class StackValue implements Serializable {

	private static final long serialVersionUID = 1L;

	protected final BigDecimal dx; // width
	protected final BigDecimal dy; // depth
	protected final BigDecimal dz; // height

	protected final BigDecimal area;

	protected final StackConstraint constraint;

	protected final List<Surface> surfaces;
	protected BigDecimal volume;

	public StackValue(BigDecimal dx, BigDecimal dy, BigDecimal dz, StackConstraint constraint, List<Surface> surfaces) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.constraint = constraint;
		this.surfaces = surfaces;

		this.area = dx.multiply(dy);
		this.volume = dx.multiply(dy).multiply(dz);
	}

	public BigDecimal getDx() {
		return dx;
	}

	public BigDecimal getDy() {
		return dy;
	}

	public BigDecimal getDz() {
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

	public boolean fitsInside3D(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
		return dx.compareTo(this.dx) >= 0 && dy.compareTo(this.dy) >= 0 && dz.compareTo(this.dz) >= 0;
	}

	public boolean fitsInside2D(BigDecimal dx, BigDecimal dy) {
		return dx.compareTo(this.dx) >= 0 && dy.compareTo(this.dy) >= 0;
	}

	public BigDecimal getArea() {
		return area;
	}

	public BigDecimal getVolume() {
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
