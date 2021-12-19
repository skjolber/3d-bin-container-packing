package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.points3d.Point3D;

public class DefaultPoint2D extends Point2D {

	public DefaultPoint2D(int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY);
	}

	@Override
	public boolean isXSupport(int x) {
		return false;
	}

	@Override
	public boolean isYSupport(int y) {
		return false;
	}

	public Point2D clone(int maxX, int maxY) {
		return new DefaultPoint2D(minX, minY, maxX, maxY);
	}
	
	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		return list;
	}

	@Override
	public Point2D moveX(int x, int y, int maxX, int maxY) {
		return new DefaultPoint2D(x, y, maxX, maxY);
	}

	@Override
	public Point2D moveY(int x, int y, int maxX, int maxY) {
		return new DefaultPoint2D(x, y, maxX, maxY);
	}

	@Override
	public Point2D moveX(int x, int y, int maxX, int maxY, Placement2D ySupport) {
		return new DefaultYSupportPoint2D(x, y, maxY, maxY, ySupport);
	}

	@Override
	public Point2D moveY(int x, int y, int maxX, int maxY, Placement2D xSupport) {
		return new DefaultXSupportPoint2D(x, y, maxX, maxX, xSupport);
	}
	

}
