package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.points2d.Point2D;

public class DefaultPoint3D extends Point3D {

	public DefaultPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point2D clone(int maxX, int maxY) {
		return clone(maxX, maxY, this.maxZ);
	}
	
	@Override
	public List<Placement3D> getPlacements3D() {
		List<Placement3D> list = new ArrayList<>();
		return list;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		return list;
	}	

}
