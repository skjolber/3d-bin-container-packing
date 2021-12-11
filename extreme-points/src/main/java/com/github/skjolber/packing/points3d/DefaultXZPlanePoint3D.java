package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;

public class DefaultXZPlanePoint3D extends Point3D implements XZPlanePoint3D {

	/** range constrained to current minY */
	private final Placement3D xzPlane;
	
	public DefaultXZPlanePoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ, 
			
			Placement3D xzPlane
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
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXZPlanePoint3D(
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
	public List<Placement3D> getPlacements3D() {
		List<Placement3D> list = new ArrayList<>();
		list.add(xzPlane);
		return list;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(xzPlane);
		return list;
	}	
	
	@Override
	public DefaultXZPlanePoint3D clone() {
		return new DefaultXZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, xzPlane);
	}
}
