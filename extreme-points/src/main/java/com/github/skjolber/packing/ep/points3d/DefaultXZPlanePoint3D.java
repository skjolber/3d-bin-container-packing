package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.api.ep.XZPlanePoint3D;

public class DefaultXZPlanePoint3D<P extends Placement3D> extends Point3D<P> implements XZPlanePoint3D {

	/** range constrained to current minY */
	private final P xzPlane;
	
	public DefaultXZPlanePoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ, 
			
			P xzPlane
			) {
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
	public Point3D<P> clone(int maxX, int maxY, int maxZ) {
		return new DefaultXZPlanePoint3D<>(
				minX, minY, minZ, 
				maxX, maxY, maxZ, 
				xzPlane
				);
	}

	@Override
	public Placement3D getXZPlane() {
		return xzPlane;
	}
	
	@Override
	public List<P> getPlacements3D() {
		List<P> list = new ArrayList<>(2);
		list.add(xzPlane);
		return list;
	}

	@Override
	public List<P> getPlacements2D() {
		List<P> list = new ArrayList<>(2);
		list.add(xzPlane);
		return list;
	}	
	
	@Override
	public DefaultXZPlanePoint3D<P> clone() {
		return new DefaultXZPlanePoint3D<>(minX, minY, minZ, maxX, maxY, maxZ, xzPlane);
	}
	
	@Override
	public Point3D<P> moveX(int x, int maxX, int maxY, int maxZ) {
		if(x <= xzPlane.getAbsoluteEndX()) {
			return new DefaultXZPlanePoint3D<>(x, minY, minZ, maxX, maxY, maxZ, xzPlane);
		}
		// xzPlane support is lost
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
		// xzPlane support is lost
		return new DefaultPoint3D<>(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveY(int y, int maxX, int maxY, int maxZ, P xzSupport) {
		// xzPlane support is lost
		return new DefaultXZPlanePoint3D<>(minX, y, minZ, maxX, maxY, maxZ, xzSupport);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ) {
		if(z <= xzPlane.getAbsoluteEndZ()) {
			return new DefaultXZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, xzPlane);
		}
		// all previous plane support is lost
		return new DefaultPoint3D<>(minX, minY, z, maxX, maxY, maxZ);
	}

	@Override
	public Point3D<P> moveZ(int z, int maxX, int maxY, int maxZ, P xySupport) {
		if(z <= xzPlane.getAbsoluteEndZ()) {
			return new DefaultXYPlaneXZPlanePoint3D<>(minX, minY, z, maxX, maxY, maxZ, xzPlane, xySupport);
		}
		// all previous plane support is lost
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
		return 0;
	}

	@Override
	public long calculateXZSupport(int dx, int dz) {
		return Math.min(dx, xzPlane.getAbsoluteEndX() - minX + 1) * (long)Math.min(dz, xzPlane.getAbsoluteEndZ() - minZ + 1);
	}

}
