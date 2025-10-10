package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

public class DefaultYZPlanePoint3D extends SimplePoint3D implements YZPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minX */
	private final Placement yzPlane;

	public DefaultYZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			Placement yzPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.yzPlane = yzPlane;
	}

	public int getSupportedYZPlaneMinY() {
		return yzPlane.getAbsoluteY();
	}

	public int getSupportedYZPlaneMaxY() {
		return yzPlane.getAbsoluteEndY();
	}

	@Override
	public int getSupportedYZPlaneMinZ() {
		return yzPlane.getAbsoluteZ();
	}

	@Override
	public int getSupportedYZPlaneMaxZ() {
		return yzPlane.getAbsoluteEndZ();
	}

	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlane.getAbsoluteY() <= y && y <= yzPlane.getAbsoluteEndY() && yzPlane.getAbsoluteZ() <= z && z <= yzPlane.getAbsoluteEndZ();
	}

	public boolean isYZPlaneEdgeZ(int z) {
		return yzPlane.getAbsoluteEndZ() == z - 1;
	}

	public boolean isYZPlaneEdgeY(int y) {
		return yzPlane.getAbsoluteEndY() == y - 1;
	}

	public Placement getYZPlane() {
		return yzPlane;
	}

	@Override
	public DefaultYZPlanePoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultYZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane);
	}
	
	@Override
	public DefaultYZPlanePoint3D clone() {
		return new DefaultYZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane);
	}

	@Override
	public SimplePoint3D moveX(int x) {
		// xzPlane support is lost
		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x, Placement yzSupport) {
		// xzPlane support is lost
		return new DefaultYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public SimplePoint3D moveY(int y) {
		// xzPlane support is lost
		if(y <= yzPlane.getAbsoluteEndY()) {
			return new DefaultYZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, yzPlane);
		}
		return new DefaultPoint3D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveY(int y, Placement xzSupport) {
		// xzPlane support is lost
		if(y <= yzPlane.getAbsoluteEndY()) {
			return new DefaultXZPlaneYZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport, yzPlane);
		}
		return new DefaultXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public SimplePoint3D moveZ(int z) {
		if(z <= yzPlane.getAbsoluteEndZ()) {
			return new DefaultYZPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, yzPlane);
		}
		// all previous plane support is lost
		return new DefaultPoint3D(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveZ(int z, Placement xySupport) {
		if(z <= yzPlane.getAbsoluteEndZ()) {
			return new DefaultXYPlaneYZPlanePoint3D(minX, minY, z, maxX, maxY, maxZ, yzPlane, xySupport);
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
	public long calculateXZSupport(int dx, int dz) {
		return 0;
	}

	@Override
	public long calculateYZSupport(int dy, int dz) {
		return (long)Math.min(dy, yzPlane.getAbsoluteEndY() - minY + 1) * Math.min(dz, yzPlane.getAbsoluteEndZ() - minZ + 1);
	}

	public boolean isSupportedYZPlane() {
		return true;
	}
}
