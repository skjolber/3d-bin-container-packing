package com.github.skjolber.packing.ep.points3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.api.ep.XZPlanePoint3D;
import com.github.skjolber.packing.api.ep.YZPlanePoint3D;

public class DefaultXZPlaneYZPlanePoint3D<P extends Placement3D & Serializable> extends Point3D<P> implements XZPlanePoint3D, YZPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minY */
	private final P xzPlane;

	/** range constrained to current minX */
	private final P yzPlane;

	public DefaultXZPlaneYZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			P xzPlane,
			P yzPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.xzPlane = xzPlane;
		this.yzPlane = yzPlane;
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

	@Override
	public Point3D<P> clone(int maxX, int maxY, int maxZ) {
		return new DefaultXZPlaneYZPlanePoint3D<>(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				xzPlane, yzPlane);
	}

	@Override
	public Placement3D getXZPlane() {
		return xzPlane;
	}

	@Override
	public Placement3D getYZPlane() {
		return yzPlane;
	}

	@Override
	public List<P> getPlacements3D() {
		List<P> list = new ArrayList<>(2);
		list.add(xzPlane);
		list.add(yzPlane);
		return list;
	}

	@Override
	public List<P> getPlacements2D() {
		List<P> list = new ArrayList<>(2);
		list.add(xzPlane);
		list.add(yzPlane);
		return list;
	}

	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ) {
		// yzPlane support is lost
		if(x <= xzPlane.getAbsoluteEndX()) {
			return new DefaultXZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xzPlane);
		}
		// all previous support is lost
		return new DefaultPoint3D<>(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ, P yzSupport) {
		if(x <= xzPlane.getAbsoluteEndX()) {
			return new DefaultXZPlaneYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xzPlane, yzSupport);
		}
		// xzPlane support is lost
		return new DefaultYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ) {
		if(y <= yzPlane.getAbsoluteEndY()) {
			return new DefaultYZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, yzPlane);
		}
		// all previous support is lost
		return new DefaultPoint3D<>(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ, P xzSupport) {
		if(y <= yzPlane.getAbsoluteEndY()) {
			return new DefaultXZPlaneYZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport, yzPlane);
		}
		// yz plane support is lost
		return new DefaultXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ) {
		boolean withinXZPlane = z <= xzPlane.getAbsoluteEndZ();
		boolean withinYZPlane = z <= yzPlane.getAbsoluteEndZ();

		if(withinXZPlane && withinYZPlane) {
			return new DefaultXZPlaneYZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, yzPlane, xzPlane);
		} else if(withinXZPlane) {
			return new DefaultXZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, xzPlane);
		} else if(withinYZPlane) {
			return new DefaultYZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, yzPlane);
		}

		// all previous support is lost
		return new DefaultPoint3D<>(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ, P xySupport) {
		boolean withinXZPlane = z <= xzPlane.getAbsoluteEndZ();
		boolean withinYZPlane = z <= yzPlane.getAbsoluteEndZ();

		if(withinXZPlane && withinYZPlane) {
			return new Default3DPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, yzPlane, xzPlane, xySupport);
		} else if(withinXZPlane) {
			return new DefaultXYPlaneXZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, xzPlane, xySupport);
		} else if(withinYZPlane) {
			return new DefaultXYPlaneYZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, yzPlane, xySupport);
		}

		// all previous support is lost
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
	public long calculateYZSupport(int dy, int dz) {
		return (long)Math.min(dy, yzPlane.getAbsoluteEndY() - minY + 1) * Math.min(dz, yzPlane.getAbsoluteEndZ() - minZ + 1);
	}

	@Override
	public long calculateXZSupport(int dx, int dz) {
		return Math.min(dx, xzPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dz, xzPlane.getAbsoluteEndZ() - minZ + 1);
	}

}
