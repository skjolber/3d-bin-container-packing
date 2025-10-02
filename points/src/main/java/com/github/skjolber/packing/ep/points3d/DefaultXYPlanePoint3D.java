package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

public class DefaultXYPlanePoint3D extends SimplePoint3D implements XYPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minZ */
	private final Placement xyPlane;

	public DefaultXYPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			Placement xyPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		this.xyPlane = xyPlane;
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
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlane.getAbsoluteX() <= x && x <= xyPlane.getAbsoluteEndX() && xyPlane.getAbsoluteY() <= y && y <= xyPlane.getAbsoluteEndY();
	}

	public boolean isXYPlaneEdgeX(int x) {
		return xyPlane.getAbsoluteEndX() == x - 1;
	}

	public boolean isXYPlaneEdgeY(int y) {
		return xyPlane.getAbsoluteEndY() == y - 1;
	}
	
	public Placement getXYPlane() {
		return xyPlane;
	}

	@Override
	public DefaultXYPlanePoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXYPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				xyPlane);
	}
	

	@Override
	public DefaultXYPlanePoint3D clone() {
		return new DefaultXYPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				xyPlane);
	}	

	@Override
	public SimplePoint3D moveX(int x) {
		if(x <= xyPlane.getAbsoluteEndX()) {
			return new DefaultXYPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x, Placement yzSupport) {
		if(x <= xyPlane.getAbsoluteEndX()) {
			return new DefaultXYPlaneYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public SimplePoint3D moveY(int y) {
		if(y <= xyPlane.getAbsoluteEndY()) {
			return new DefaultXYPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultPoint3D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveY(int y, Placement xzSupport) {
		if(y <= xyPlane.getAbsoluteEndY()) {
			return new DefaultXYPlaneXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public SimplePoint3D moveZ(int z) {
		// xyPlane support is lost
		return new DefaultPoint3D(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveZ(int z, Placement xySupport) {
		// xyPlane support is lost
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
	public long calculateXZSupport(int dx, int dz) {
		return 0;
	}

	@Override
	public long calculateYZSupport(int dy, int dz) {
		return 0;
	}

	public boolean isSupportedXYPlane() { // i.e. z is fixed
		return true;
	}

}
