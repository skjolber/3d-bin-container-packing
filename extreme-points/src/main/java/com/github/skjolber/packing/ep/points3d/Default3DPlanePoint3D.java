package com.github.skjolber.packing.ep.points3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.api.ep.XYPlanePoint3D;
import com.github.skjolber.packing.api.ep.XZPlanePoint3D;
import com.github.skjolber.packing.api.ep.YZPlanePoint3D;

public class Default3DPlanePoint3D<P extends Placement3D & Serializable> extends Point3D<P> implements XZPlanePoint3D, YZPlanePoint3D, XYPlanePoint3D {

	private static final long serialVersionUID = 1L;

	/** range constrained to current minX */
	private final P yzPlane;

	/** range constrained to current minY */
	private final P xzPlane;

	/** range constrained to current minZ */
	private final P xyPlane;

	public Default3DPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			P yzPlane,
			P xzPlane,
			P xyPlane) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.yzPlane = yzPlane;
		this.xzPlane = xzPlane;
		this.xyPlane = xyPlane;
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

	public int getSupportedXZPlaneMinX() {
		return xzPlane.getAbsoluteX();
	}

	public int getSupportedXZPlaneMaxX() {
		return xzPlane.getAbsoluteEndX();
	}

	@Override
	public int getSupportedXZPlaneMaxZ() {
		return xzPlane.getAbsoluteEndZ();
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
	public Point3D<P> clone(int maxX, int maxY, int maxZ) {
		return new Default3DPlanePoint3D<>(minX, minY, minZ,
				maxX, maxY, maxZ,
				yzPlane, xzPlane, xyPlane);
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
	public Placement3D getXYPlane() {
		return xyPlane;
	}

	@Override
	public List<P> getPlacements3D() {
		List<P> list = new ArrayList<>(3);
		list.add(xyPlane);
		list.add(xzPlane);
		list.add(yzPlane);
		return list;
	}

	@Override
	public List<P> getPlacements2D() {
		List<P> list = new ArrayList<>(3);
		list.add(xyPlane);
		list.add(xzPlane);
		list.add(yzPlane);
		return list;
	}

	@Override
	public Default3DPlanePoint3D<P> clone() {
		return new Default3DPlanePoint3D<>(minX, minY, minZ, maxX, maxY, maxZ, yzPlane, xzPlane, xyPlane);
	}

	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ) {
		boolean withinXYPlane = x <= xyPlane.getAbsoluteEndX();
		boolean withinXZPlane = x <= xzPlane.getAbsoluteEndX();

		// yz plane support lost
		if(withinXYPlane && withinXZPlane) {
			return new DefaultXYPlaneXZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xzPlane, xyPlane);
		} else if(withinXYPlane) {
			return new DefaultXYPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xyPlane);
		} else if(withinXZPlane) {
			return new DefaultXZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xzPlane);
		}

		return new DefaultPoint3D<>(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ, P yzSupport) {
		boolean withinXYPlane = x <= xyPlane.getAbsoluteEndX();
		boolean withinXZPlane = x <= xzPlane.getAbsoluteEndX();

		// yz plane support lost
		if(withinXYPlane && withinXZPlane) {
			return new Default3DPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xzPlane, xyPlane);
		} else if(withinXYPlane) {
			return new DefaultXYPlaneYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xyPlane);
		} else if(withinXZPlane) {
			return new DefaultXZPlaneYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport, xzPlane);
		}

		return new DefaultYZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, yzSupport);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ) {
		boolean withinXYPlane = y <= xyPlane.getAbsoluteEndY();
		boolean withinYZPlane = y <= yzPlane.getAbsoluteEndY();

		// xz plane support lost
		if(withinXYPlane && withinYZPlane) {
			return new DefaultXYPlaneYZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, yzPlane, xyPlane);
		} else if(withinXYPlane) {
			return new DefaultXYPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xyPlane);
		} else if(withinYZPlane) {
			return new DefaultYZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, yzPlane);
		}

		return new DefaultPoint3D<>(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ, P xzSupport) {
		boolean withinXYPlane = y <= xyPlane.getAbsoluteEndY();
		boolean withinYZPlane = y <= yzPlane.getAbsoluteEndY();

		if(withinXYPlane && withinYZPlane) {
			return new Default3DPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, yzPlane, xzSupport, xyPlane);
		} else if(withinXYPlane) {
			return new DefaultXYPlaneXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport, xyPlane);
		} else if(withinYZPlane) {
			return new DefaultXZPlaneYZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport, yzPlane);
		}

		return new DefaultXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ) {
		boolean withinXZPlane = z <= xzPlane.getAbsoluteEndZ();
		boolean withinYZPlane = z <= yzPlane.getAbsoluteEndZ();

		// xy plane support lost
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
	public String toString() {
		return getClass().getSimpleName() + " [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY
				+ "x" + maxZ + " " + "(" + dx + "x" + dy + "x" + dz + ") xy=" + xyPlane + " xz=" + xzPlane + " yz=" + yzPlane + "]";
	}

	@Override
	public long calculateXYSupport(int dx, int dy) {
		return Math.min(dx, xyPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dy, xyPlane.getAbsoluteEndY() - minY + 1);
	}

	@Override
	public long calculateXZSupport(int dx, int dz) {
		return Math.min(dx, xzPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dz, xzPlane.getAbsoluteEndZ() - minZ + 1);
	}

	@Override
	public long calculateYZSupport(int dy, int dz) {
		return (long)Math.min(dy, yzPlane.getAbsoluteEndY() - minY + 1) * Math.min(dz, yzPlane.getAbsoluteEndZ() - minZ + 1);
	}

}
