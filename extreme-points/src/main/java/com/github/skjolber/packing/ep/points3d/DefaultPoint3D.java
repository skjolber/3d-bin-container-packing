package com.github.skjolber.packing.ep.points3d;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.Point3D;

public class DefaultPoint3D<P extends Placement3D & Serializable> extends Point3D<P> {

	private static final long serialVersionUID = 1L;

	public DefaultPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D<>(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point2D<P> clone(int maxX, int maxY) {
		return clone(maxX, maxY, this.maxZ);
	}

	@Override
	public List<P> getPlacements3D() {
		return Collections.emptyList();
	}

	@Override
	public List<P> getPlacements2D() {
		return Collections.emptyList();
	}

	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D<>(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ, P yzSupport) {
		return new DefaultYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D<>(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ, P xzSupport) {
		return new DefaultXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D<>(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ, P xySupport) {
		return new DefaultXYPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, xySupport);
	}

	/**
	 * Rotate box, i.e. in 3D
	 *
	 * @return this instance
	 */

	@Override
	public Point3D<P> rotate() {
		return new DefaultPoint3D<>(minY, minZ, minX, maxY, maxZ, maxX);
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
}
