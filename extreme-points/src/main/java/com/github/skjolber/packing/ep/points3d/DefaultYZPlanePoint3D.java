package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point3D;

public class DefaultYZPlanePoint3D extends SimplePoint3D implements YZPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minX */
	private final StackPlacement yzPlane;

	public DefaultYZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			StackPlacement yzPlane) {
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

	public StackPlacement getYZPlane() {
		return yzPlane;
	}

	@Override
	public DefaultYZPlanePoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultYZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane);
	}

	@Override
	public SimplePoint3D moveX(int x) {
		// xzPlane support is lost
		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x, StackPlacement yzSupport) {
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
	public SimplePoint3D moveY(int y, StackPlacement xzSupport) {
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
	public SimplePoint3D moveZ(int z, StackPlacement xySupport) {
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
	public Point3D rotate() {
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
