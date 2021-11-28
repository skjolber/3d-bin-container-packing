package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;

public class DefaultYZPlanePoint3D extends Point3D implements YZPlanePoint3D  {

	/** range constrained to current minX */
	private final Placement3D yzPlane;

	public DefaultYZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			Placement3D yzPlane
			) {
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

	@Override
	public Placement3D getYZPlane() {
		return yzPlane;
	}

	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultYZPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				yzPlane
			);
	}
	
	@Override
	public List<Placement3D> getPlacements3D() {
		List<Placement3D> list = new ArrayList<>();
		list.add(yzPlane);
		return list;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(yzPlane);
		return list;
	}		
	
	@Override
	public DefaultYZPlanePoint3D clone() {
		return new DefaultYZPlanePoint3D(minX, minY, minZ, maxX, maxY, maxZ, yzPlane);
	}
}
