package com.github.skjolber.packing.api;

import java.math.BigDecimal;

public class Dimension {

	public static final Dimension EMPTY = new Dimension(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

	public static Dimension decode(String size) {
		String[] dimensions = size.split("x");

		return newInstance(new BigDecimal(dimensions[0]), new BigDecimal(dimensions[1]), new BigDecimal(dimensions[2]));
	}

	public static String encode(Dimension dto) {
		return encode(dto.getDx(), dto.getDy(), dto.getDz());
	}

	public static String encode(BigDecimal width, BigDecimal depth, BigDecimal height) {
		return width + "x" + depth + "x" + height;
	}

	public static Dimension newInstance(BigDecimal width, BigDecimal depth, BigDecimal height) {
		return new Dimension(width, depth, height);
	}

	protected final BigDecimal dx; // dx
	protected final BigDecimal dy; // dy
	protected final BigDecimal dz; // dz

	protected final BigDecimal area;
	protected final BigDecimal volume;

	protected final String name;

	public Dimension(String name, BigDecimal dx, BigDecimal dy, BigDecimal dz) {
		this.name = name;

		this.dx = dx;
		this.dy = dy;
		this.dz = dz;

		this.volume = dy.multiply(dx).multiply(dz);
		this.area = dy.multiply(dx);
	}

	public Dimension(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
		this(null, dx, dy, dz);
	}

	public BigDecimal getDx() {
		return dx;
	}

	public BigDecimal getDz() {
		return dz;
	}

	public BigDecimal getDy() {
		return dy;
	}

	/**
	 *
	 * Check whether a dimension fits within the current dimensions, rotated in 3D.
	 *
	 * @param dimension the space to fit
	 * @return true if any rotation of the argument can be placed inside this
	 *
	 */
	public boolean canHold3D(Dimension dimension) {
		return canHold3D(dimension.getDx(), dimension.getDy(), dimension.getDz());
	}

	public boolean canHold3D(BigDecimal w, BigDecimal d, BigDecimal h) {
		return (w.compareTo(dx) <= 0 && h.compareTo(dz) <= 0 && d.compareTo(dy) <= 0) ||
				(h.compareTo(dx) <= 0 && d.compareTo(dz) <= 0 && w.compareTo(dy) <= 0) ||
				(d.compareTo(dx) <= 0 && w.compareTo(dz) <= 0 && h.compareTo(dy) <= 0) ||
				(h.compareTo(dx) <= 0 && w.compareTo(dz) <= 0 && d.compareTo(dy) <= 0) ||
				(d.compareTo(dx) <= 0 && h.compareTo(dz) <= 0 && w.compareTo(dy) <= 0) ||
				(w.compareTo(dx) <= 0 && d.compareTo(dz) <= 0 && h.compareTo(dy) <= 0);
	}

	/**
	 *
	 * Check whether a dimension fits within the current object, rotated in 2D.
	 *
	 * @param dimension the dimension to fit
	 * @return true if any rotation of the argument can be placed inside this
	 *
	 */
	public boolean canHold2D(Dimension dimension) {
		return canHold2D(dimension.getDx(), dimension.getDy(), dimension.getDz());
	}

	public boolean canHold2D(BigDecimal w, BigDecimal d, BigDecimal h) {
		if(h.compareTo(dz) > 0) {
			return false;
		}
		return (w.compareTo(dx) <= 0 && d.compareTo(dy) <= 0) || (d.compareTo(dx) <= 0 && w.compareTo(dy) <= 0);
	}

	public BigDecimal getFootprint() {
		return dx.multiply(dy);
	}

	public boolean isSquare2D() {
		return dx.compareTo(dy) == 0;
	}

	public boolean isSquare3D() {
		return dx.compareTo(dy) == 0 && dx.compareTo(dz) == 0;
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

	public boolean fitsInside3D(BigDecimal w, BigDecimal d, BigDecimal h) {
		return w.compareTo(dx) >= 0 && h.compareTo(dz) >= 0 && d.compareTo(dy) >= 0;
	}

	/**
	 * Check whether this object can fit within a dimension, with 3D rotation.
	 *
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space in any rotation
	 *
	 */

	public boolean canFitInside3D(Dimension dimension) {
		return dimension.canHold3D(this);
	}

	/**
	 * Check whether this object can fit within a dimension, with 2D rotation.
	 *
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space in any 2D rotation
	 *
	 */

	public boolean canFitInside2D(Dimension dimension) {
		return dimension.canHold2D(this);
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public boolean nonEmpty() {
		return dx.compareTo(BigDecimal.ZERO) > 0 && dy.compareTo(BigDecimal.ZERO) > 0 && dz.compareTo(BigDecimal.ZERO) > 0;
	}

	@Override
	public String toString() {
		return "Dimension [width=" + dx + ", depth=" + dy + ", height=" + dz + ", volume=" + volume + "]";
	}

	public String encode() {
		return encode(dx, dy, dz);
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dy.intValue();
		result = prime * result + dz.intValue();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (volume.intValue() ^ (volume.intValue() >>> 32));
		result = prime * result + dx.intValue();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Dimension other = (Dimension)obj;
		if(dy != other.dy)
			return false;
		if(dz != other.dz)
			return false;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		if(volume != other.volume)
			return false;
		if(dx != other.dx)
			return false;
		return true;
	}

}
