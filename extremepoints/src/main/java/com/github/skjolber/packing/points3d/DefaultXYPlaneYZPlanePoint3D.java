package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;

public class DefaultXYPlaneYZPlanePoint3D extends Point3D implements XYPlanePoint3D, YZPlanePoint3D {

	/** range constrained to current minX */
	private final Placement3D yzPlane;
	
	/** range constrained to current minZ */
	private final Placement3D xyPlane;

	public DefaultXYPlaneYZPlanePoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, 
			Placement3D yzPlane,
			Placement3D xyPlane
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.yzPlane = yzPlane;
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

	@Override
	public Placement3D getYZPlane() {
		return yzPlane;
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
		return new DefaultXYPlaneYZPlanePoint3D(
				minX, minY, minZ, 
				maxX, maxY, maxZ,
				yzPlane, xyPlane				
				);
	}

	@Override
	public List<Placement3D> getPlacements3D() {
		List<Placement3D> list = new ArrayList<>();
		list.add(xyPlane);
		list.add(yzPlane);
		return list;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(xyPlane);
		list.add(yzPlane);
		return list;
	}	
	
	@Override
	public DefaultXYPlaneYZPlanePoint3D clone() {
		return new DefaultXYPlaneYZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane, xyPlane);
	}
	
}
