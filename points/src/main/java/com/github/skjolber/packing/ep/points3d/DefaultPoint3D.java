package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement;

/**
 * The only point implementation: a free space with optional supporting planes.
 * <ul>
 *   <li>xy plane: support below, constraining the point's minimum z</li>
 *   <li>xz plane: support in front, constraining the point's minimum y</li>
 *   <li>yz plane: support to the left, constraining the point's minimum x</li>
 * </ul>
 * A missing plane is {@code null}. A single final class keeps the calculator's calls on points
 * monomorphic, so that they can be inlined.
 * <p>
 * Moving a point along an axis loses the plane perpendicular to that axis, unless a new support is
 * given, and keeps each other plane only while the new coordinate is still within it.
 */
public final class DefaultPoint3D extends SimplePoint3D {

	/** range constrained to current minX */
	private final Placement yzPlane;

	/** range constrained to current minY */
	private final Placement xzPlane;

	/** range constrained to current minZ */
	private final Placement xyPlane;

	public DefaultPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this(minX, minY, minZ, maxX, maxY, maxZ, null, null, null);
	}

	/**
	 * @param yzPlane support to the left, or null
	 * @param xzPlane support in front, or null
	 * @param xyPlane support below, or null
	 */
	public DefaultPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Placement yzPlane, Placement xzPlane, Placement xyPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		this.yzPlane = yzPlane;
		this.xzPlane = xzPlane;
		this.xyPlane = xyPlane;
	}

	public Placement getYZPlane() {
		return yzPlane;
	}

	public Placement getXZPlane() {
		return xzPlane;
	}

	public Placement getXYPlane() {
		return xyPlane;
	}

	@Override
	public boolean isSupportedXYPlane() {
		return xyPlane != null;
	}

	@Override
	public boolean isSupportedXZPlane() {
		return xzPlane != null;
	}

	@Override
	public boolean isSupportedYZPlane() {
		return yzPlane != null;
	}

	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlane != null && xyPlane.getAbsoluteX() <= x && x <= xyPlane.getAbsoluteEndX() && xyPlane.getAbsoluteY() <= y && y <= xyPlane.getAbsoluteEndY();
	}

	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlane != null && xzPlane.getAbsoluteX() <= x && x <= xzPlane.getAbsoluteEndX() && xzPlane.getAbsoluteZ() <= z && z <= xzPlane.getAbsoluteEndZ();
	}

	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlane != null && yzPlane.getAbsoluteY() <= y && y <= yzPlane.getAbsoluteEndY() && yzPlane.getAbsoluteZ() <= z && z <= yzPlane.getAbsoluteEndZ();
	}

	@Override
	public DefaultPoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane, xzPlane, xyPlane);
	}

	@Override
	public DefaultPoint3D clone() {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane, xzPlane, xyPlane);
	}

	@Override
	public SimplePoint3D moveX(int x) {
		return moveX(x, null);
	}

	@Override
	public SimplePoint3D moveX(int x, Placement yzSupport) {
		// yz plane support lost, unless replaced
		Placement xz = xzPlane != null && x <= xzPlane.getAbsoluteEndX() ? xzPlane : null;
		Placement xy = xyPlane != null && x <= xyPlane.getAbsoluteEndX() ? xyPlane : null;
		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xz, xy);
	}

	@Override
	public SimplePoint3D moveY(int y) {
		return moveY(y, null);
	}

	@Override
	public SimplePoint3D moveY(int y, Placement xzSupport) {
		// xz plane support lost, unless replaced
		Placement yz = yzPlane != null && y <= yzPlane.getAbsoluteEndY() ? yzPlane : null;
		Placement xy = xyPlane != null && y <= xyPlane.getAbsoluteEndY() ? xyPlane : null;
		return new DefaultPoint3D(minX, y, minZ, maxX, maxY, maxZ, yz, xzSupport, xy);
	}

	@Override
	public SimplePoint3D moveZ(int z) {
		return moveZ(z, null);
	}

	@Override
	public SimplePoint3D moveZ(int z, Placement xySupport) {
		// xy plane support lost, unless replaced
		Placement yz = yzPlane != null && z <= yzPlane.getAbsoluteEndZ() ? yzPlane : null;
		Placement xz = xzPlane != null && z <= xzPlane.getAbsoluteEndZ() ? xzPlane : null;
		return new DefaultPoint3D(minX, minY, z, maxX, maxY, maxZ, yz, xz, xySupport);
	}

	/** Equal coordinates and the same supporting planes present (previously: the same point class). */
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj)) {
			return false;
		}
		DefaultPoint3D other = (DefaultPoint3D)obj;
		return (xyPlane == null) == (other.xyPlane == null) && (xzPlane == null) == (other.xzPlane == null) && (yzPlane == null) == (other.yzPlane == null);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "DefaultPoint3D [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY + "x" + maxZ + " (" + dx + "x" + dy + "x" + dz + ")"
				+ (xyPlane != null ? " xy=" + xyPlane : "") + (xzPlane != null ? " xz=" + xzPlane : "") + (yzPlane != null ? " yz=" + yzPlane : "") + "]";
	}
}
