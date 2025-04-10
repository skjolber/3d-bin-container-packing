package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;

public class DefaultPoint3D extends SimplePoint3D {

	private static final long serialVersionUID = 1L;

	public DefaultPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public DefaultPoint3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x) {
		return new DefaultPoint3D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveX(int x, StackPlacement yzSupport) {
		return new DefaultYZPlanePoint3D(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public SimplePoint3D moveY(int y) {
		return new DefaultPoint3D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveY(int y, StackPlacement xzSupport) {
		return new DefaultXZPlanePoint3D(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public SimplePoint3D moveZ(int z) {
		return new DefaultPoint3D(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint3D moveZ(int z, StackPlacement xySupport) {
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
		return 0;
	}
	

	public boolean isSupportedXYPlane(int x, int y) { // i.e. z is fixed
		return false;
	}

	public boolean isSupportedYZPlane(int y, int z) { // i.e. x is fixed
		return false;
	}

	public boolean isSupportedXZPlane(int x, int z) { // i.e. y is fixed
		return false;
	}
}
