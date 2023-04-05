package com.github.skjolber.packing.ep.points3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.api.ep.XYPlanePoint3D;

public class DefaultXYPlanePoint3D<P extends Placement3D & Serializable> extends Point3D<P> implements XYPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minZ */
	private final P xyPlane;

	public DefaultXYPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			P xyPlane) {
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

	@Override
	public Placement3D getXYPlane() {
		return xyPlane;
	}

	@Override
	public Point3D<P> clone(int maxX, int maxY, int maxZ) {
		return new DefaultXYPlanePoint3D<>(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				xyPlane);
	}

	@Override
	public Point3D<P> moveX(int x) {
		if(x <= xyPlane.getAbsoluteEndX()) {
			return new DefaultXYPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultPoint3D<>(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveX(int x, P yzSupport) {
		if(x <= xyPlane.getAbsoluteEndX()) {
			return new DefaultXYPlaneYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public Point3D<P> moveY(int y) {
		if(y <= xyPlane.getAbsoluteEndY()) {
			return new DefaultXYPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultPoint3D<>(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveY(int y, P xzSupport) {
		if(y <= xyPlane.getAbsoluteEndY()) {
			return new DefaultXYPlaneXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport, xyPlane);
		}
		// xyPlane support is lost
		return new DefaultXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public Point3D<P> moveZ(int z) {
		// xyPlane support is lost
		return new DefaultPoint3D<>(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveZ(int z, P xySupport) {
		// xyPlane support is lost
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

}
