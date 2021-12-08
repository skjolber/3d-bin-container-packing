package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;

public class DefaultXYPlanePoint3D extends Point3D implements XYPlanePoint3D  {

	/** range constrained to current minZ */
	private final Placement3D xyPlane;
	
	public DefaultXYPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			Placement3D xyPlane
			) {
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
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXYPlanePoint3D(
			minX, minY, minZ,
			maxX, maxY, maxZ,
			xyPlane
		);
	}	
	
	@Override
	public List<Placement3D> getPlacements3D() {
		List<Placement3D> list = new ArrayList<>();
		list.add(xyPlane);
		return list;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(xyPlane);
		return list;
	}

	@Override
	public DefaultXYPlanePoint3D clone() {
		return new DefaultXYPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, xyPlane);
	}
}
