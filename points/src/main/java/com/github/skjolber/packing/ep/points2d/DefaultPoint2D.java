package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.Placement;

/**
 * The only 2D point implementation: a free area with optional supporting lines.
 * <ul>
 *   <li>x support: a line in the x direction, i.e. support below, fixing the minimum y</li>
 *   <li>y support: a line in the y direction, i.e. support to the left, fixing the minimum x</li>
 * </ul>
 * A missing support is {@code null}. A single final class keeps the calculator's calls on points
 * monomorphic, so that they can be inlined.
 * <p>
 * Moving a point along x loses the y support (unless a new one is given) and keeps the x support
 * while the new x is within it; moving along y is symmetric.
 */
public final class DefaultPoint2D extends SimplePoint2D {

	private final Placement xSupport;
	private final Placement ySupport;

	public DefaultPoint2D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this(minX, minY, minZ, maxX, maxY, maxZ, null, null);
	}

	/**
	 * @param xSupport support below, or null
	 * @param ySupport support to the left, or null
	 */
	public DefaultPoint2D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Placement xSupport, Placement ySupport) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		this.xSupport = xSupport;
		this.ySupport = ySupport;
	}

	public Placement getXSupport() {
		return xSupport;
	}

	public Placement getYSupport() {
		return ySupport;
	}

	public boolean hasXSupport() {
		return xSupport != null;
	}

	public boolean hasYSupport() {
		return ySupport != null;
	}

	@Override
	public boolean isXSupport(int x) {
		return xSupport != null && xSupport.getAbsoluteX() <= x && x <= xSupport.getAbsoluteEndX();
	}

	@Override
	public boolean isYSupport(int y) {
		return ySupport != null && ySupport.getAbsoluteY() <= y && y <= ySupport.getAbsoluteEndY();
	}

	public int getSupportedMinX() {
		return xSupport.getAbsoluteX();
	}

	public int getSupportedMaxX() {
		return xSupport.getAbsoluteEndX();
	}

	public int getSupportedMinY() {
		return ySupport.getAbsoluteY();
	}

	public int getSupportedMaxY() {
		return ySupport.getAbsoluteEndY();
	}

	@Override
	public SimplePoint2D moveX(int x) {
		return moveX(x, null);
	}

	@Override
	public SimplePoint2D moveX(int x, Placement ySupport) {
		// y support lost, unless replaced
		Placement keptX = xSupport != null && x <= xSupport.getAbsoluteEndX() ? xSupport : null;
		return new DefaultPoint2D(x, minY, minZ, maxX, maxY, maxZ, keptX, ySupport);
	}

	@Override
	public SimplePoint2D moveY(int y) {
		return moveY(y, null);
	}

	@Override
	public SimplePoint2D moveY(int y, Placement xSupport) {
		// x support lost, unless replaced
		Placement keptY = ySupport != null && y <= ySupport.getAbsoluteEndY() ? ySupport : null;
		return new DefaultPoint2D(minX, y, minZ, maxX, maxY, maxZ, xSupport, keptY);
	}

	@Override
	public SimplePoint2D clone(int maxX, int maxY) {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ, xSupport, ySupport);
	}

	@Override
	public DefaultPoint2D clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ, xSupport, ySupport);
	}

	@Override
	public SimplePoint2D clone() {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ, xSupport, ySupport);
	}

	/** Equal coordinates and the same supports present (previously: the same point class). */
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj)) {
			return false;
		}
		DefaultPoint2D other = (DefaultPoint2D)obj;
		return (xSupport == null) == (other.xSupport == null) && (ySupport == null) == (other.ySupport == null);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "DefaultPoint2D [" + minX + "x" + minY + " " + maxX + "x" + maxY
				+ (xSupport != null ? ", xSupport=" + xSupport.getAbsoluteX() + "-" + xSupport.getAbsoluteEndX() : "")
				+ (ySupport != null ? ", ySupport=" + ySupport.getAbsoluteY() + "-" + ySupport.getAbsoluteEndY() : "") + "]";
	}
}
