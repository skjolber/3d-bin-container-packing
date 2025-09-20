package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.ep.Point;

public class DefaultXZPlanePoint3D extends SimplePoint3D implements XZPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minY */
	private final Placement xzPlane;

	public DefaultXZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,

			Placement xzPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.xzPlane = xzPlane;
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
	public DefaultXZPlanePoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXZPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				xzPlane);
	}
	
	@Override
	public DefaultXZPlanePoint3D clone() {
		return new DefaultXZPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				xzPlane);
	}

	public Placement getXZPlane() {
		return xzPlane;
	}

	@Override
	public SimplePoint3D moveX(int x) {
		if(x <= xzPlane.getAbsoluteEndX()) {
			return new DefaultXZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xzPlane);
		}
		// xzPlane support is lost
		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x, Placement yzSupport) {
		if(x <= xzPlane.getAbsoluteEndX()) {
			return new DefaultXZPlaneYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, xzPlane, yzSupport);
		}
		// xzPlane support is lost
		return new DefaultYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public SimplePoint3D moveY(int y) {
		// xzPlane support is lost
		return new DefaultPoint3D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveY(int y, Placement xzSupport) {
		// xzPlane support is lost
		return new DefaultXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public SimplePoint3D moveZ(int z) {
		if(z <= xzPlane.getAbsoluteEndZ()) {
			return new DefaultXZPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, xzPlane);
		}
		// all previous plane support is lost
		return new DefaultPoint3D(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveZ(int z, Placement xySupport) {
		if(z <= xzPlane.getAbsoluteEndZ()) {
			return new DefaultXYPlaneXZPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, xzPlane, xySupport);
		}
		// all previous plane support is lost
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
		return 0;
	}

	@Override
	public long calculateYZSupport(int dy, int dz) {
		return 0;
	}

	@Override
	public long calculateXZSupport(int dx, int dz) {
		return Math.min(dx, xzPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dz, xzPlane.getAbsoluteEndZ() - minZ + 1);
	}
	
	public boolean isSupportedXZPlane() { // i.e. y is fixed
		return true;
	}

}
