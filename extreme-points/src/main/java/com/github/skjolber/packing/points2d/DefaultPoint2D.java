package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

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

}
