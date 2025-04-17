package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;

public class DefaultXYPlaneXZPlanePoint3D extends SimplePoint3D implements XYPlanePoint3D, XZPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minY */
	private final StackPlacement xzPlane;

	/** range constrained to current minZ */
	private final StackPlacement xyPlane;

	public DefaultXYPlaneXZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			StackPlacement xzPlane,
			StackPlacement xyPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.xzPlane = xzPlane;
		this.xyPlane = xyPlane;
	}

	public int getSupportedXZPlaneMinX() {
		return xzPlane.getAbsoluteX();
	}

	public int getSupportedXZPlaneMaxX() {
		return xzPlane.getAbsoluteEndX();
	}

	@Override
	public int getSupportedXZPlaneMaxZ() {
		return xzPlane.getAbsoluteZ();
	}

	@Override
	public int getSupportedXZPlaneMinZ() {
		return xzPlane.getAbsoluteEndZ();
	}

	public int getSupportedXYPlaneMinY() {
		return xyPlane.getAbsoluteY();
	}

	public int getSupportedXYPlaneMaxY() {
		return xyPlane.getAbsoluteEndY();
	}

	@Override
	public int getSupportedXYPlaneMinX() {
		return xyPlane.getAbsoluteX();
	}

	@Override
	public int getSupportedXYPlaneMaxX() {
		return xyPlane.getAbsoluteEndX();
	}

	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlane.getAbsoluteX() <= x && x <= xzPlane.getAbsoluteEndX() && xzPlane.getAbsoluteZ() <= z && z <= xzPlane.getAbsoluteEndZ();
	}

	public boolean isXZPlaneEdgeX(int x) {
		return xzPlane.getAbsoluteX() == x - 1;
	}

	public boolean isXZPlaneEdgeZ(int z) {
		return xzPlane.getAbsoluteEndZ() == z - 1;
	}

	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlane.getAbsoluteX() <= x && x <= xyPlane.getAbsoluteEndX() && xyPlane.getAbsoluteY() <= y && y <= xyPlane.getAbsoluteEndY();
	}

	public boolean isXYPlaneEdgeX(int x) {
		return xyPlane.getAbsoluteEndX() == x - 1;
	}

	public boolean isXYPlaneEdgeY(int y) {
		return xyPlane.getAbsoluteEndY() == y - 1;
	}

	@Override
	public DefaultXYPlaneXZPlanePoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXYPlaneXZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, xzPlane, xyPlane);
	}

	@Override
	public DefaultXYPlaneXZPlanePoint3D clone() {
		return new DefaultXYPlaneXZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, xzPlane, xyPlane);
	}
	
	public StackPlacement getXZPlane() {
		return xzPlane;
	}

	public StackPlacement getXYPlane() {
		return xyPlane;
	}

	@Override
	public SimplePoint3D moveX(int x) {
		boolean withinXYPlane = x <= xyPlane.getAbsoluteEndX();
		boolean withinXZPlane = x <= xzPlane.getAbsoluteEndX();

		if(withinXYPlane && withinXZPlane) {
			return new DefaultXYPlaneXZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xzPlane, xyPlane);
		} else if(withinXYPlane) {
			return new DefaultXYPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xyPlane);
		} else if(withinXZPlane) {
			return new DefaultXZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xzPlane);
		}

		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x, StackPlacement yzSupport) {
		boolean withinXYPlane = x <= xyPlane.getAbsoluteEndX();
		boolean withinXZPlane = x <= xzPlane.getAbsoluteEndX();

		if(withinXYPlane && withinXZPlane) {
			return new Default3DPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xzPlane, xyPlane);
		} else if(withinXYPlane) {
			return new DefaultXYPlaneYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xyPlane);
		} else if(withinXZPlane) {
			return new DefaultXZPlaneYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xzPlane, yzSupport);
		}

		return new DefaultYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public SimplePoint3D moveY(int y) {
		// xz plane support is lost
		if(y <= xyPlane.getAbsoluteEndY()) {
			return new DefaultXYPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xyPlane);
		}
		// all previous support is lost
		return new DefaultPoint3D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveY(int y, StackPlacement xzSupport) {
		// xz plane support is lost
		if(y <= xyPlane.getAbsoluteEndY()) {
			return new DefaultXYPlaneXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport, xyPlane);
		}
		// all previous support is lost
		return new DefaultXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public SimplePoint3D moveZ(int z) {
		// xy plane support is lost
		if(z <= xzPlane.getAbsoluteEndZ()) {
			return new DefaultXZPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, xzPlane);
		}
		// all previous support is lost
		return new DefaultPoint3D(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveZ(int z, StackPlacement xySupport) {
		if(z <= xzPlane.getAbsoluteEndZ()) {
			return new DefaultXYPlaneXZPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, xzPlane, xySupport);
		}
		// all previous support is lost
		return new DefaultXYPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, xySupport);
	}

	/**
	 * Rotate box, i.e. in 3D
	 *
	 * @return this instance
	 */

	@Override
	public Point rotate() {
		return new DefaultPoint3D(minY, minZ, minX, maxY, maxZ, maxX);
	}

	@Override
	public long calculateXYSupport(int dx, int dy) {
		return Math.min(dx, xyPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dy, xyPlane.getAbsoluteEndY() - minY + 1);
	}

	@Override
	public long calculateYZSupport(int dy, int dz) {
		return 0;
	}

	@Override
	public long calculateXZSupport(int dx, int dz) {
		return Math.min(dx, xzPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dz, xzPlane.getAbsoluteEndZ() - minZ + 1);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY
				+ "x" + maxZ + " " + "(" + dx + "x" + dy + "x" + dz + ") xy=" + xyPlane + " xz=" + xzPlane + "]";
	}
	
	public boolean isSupportedXZPlane() { // i.e. y is fixed
		return true;
	}
	
	public boolean isSupportedXYPlane() { // i.e. z is fixed
		return true;
	}

}
