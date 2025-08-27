package com.github.skjolber.packing.api;

public class Dimension {

	public static final Dimension EMPTY = new Dimension(0, 0, 0);

	public static Dimension decode(String size) {
		String[] dimensions = size.split("x");

		return newInstance(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]),
				Integer.parseInt(dimensions[2]));
	}

	public static String encode(Dimension dto) {
		return encode(dto.getDx(), dto.getDy(), dto.getDz());
	}

	public static String encode(int width, int depth, int height) {
		return width + "x" + depth + "x" + height;
	}

	public static Dimension newInstance(int width, int depth, int height) {
		return new Dimension(width, depth, height);
	}

	protected final int dx; // dx
	protected final int dy; // dy
	protected final int dz; // dz

	protected final long area;
	protected final long volume;

	protected final String name;

	public Dimension(String name, int dx, int dy, int dz) {
		this.name = name;

		this.dx = dx;
		this.dy = dy;
		this.dz = dz;

		this.volume = ((long) dy) * ((long) dx) * ((long) dz);
		this.area = ((long) dy) * ((long) dx);
	}

	public Dimension(int dx, int dy, int dz) {
		this(null, dx, dy, dz);
	}

	public int getDx() {
		return dx;
	}

	public int getDz() {
		return dz;
	}

	public int getDy() {
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

	public boolean canHold3D(int w, int d, int h) {
		return (w <= dx && h <= dz && d <= dy) || (h <= dx && d <= dz && w <= dy) || (d <= dx && w <= dz && h <= dy)
				|| (h <= dx && w <= dz && d <= dy) || (d <= dx && h <= dz && w <= dy)
				|| (w <= dx && d <= dz && h <= dy);
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

	public boolean canHold2D(int w, int d, int h) {
		if (h > dz) {
			return false;
		}
		return (w <= dx && d <= dy) || (d <= dx && w <= dy);
	}

	public int getFootprint() {
		return dx * dy;
	}

	public boolean isSquare2D() {
		return dx == dy;
	}

	public boolean isSquare3D() {
		return dx == dy && dx == dz;
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

	public boolean fitsInside3D(int w, int d, int h) {
		return w >= dx && h >= dz && d >= dy;
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

	public long getVolume() {
		return volume;
	}

	public boolean nonEmpty() {
		return dx > 0 && dy > 0 && dz > 0;
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
		result = prime * result + dy;
		result = prime * result + dz;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (volume ^ (volume >>> 32));
		result = prime * result + dx;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dimension other = (Dimension) obj;
		if (dy != other.dy)
			return false;
		if (dz != other.dz)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (volume != other.volume)
			return false;
		if (dx != other.dx)
			return false;
		return true;
	}

}
