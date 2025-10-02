package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

public class DefaultPoint2D extends SimplePoint2D {

	private static final long serialVersionUID = 1L;

	public DefaultPoint2D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public boolean isXSupport(int x) {
		return false;
	}

	@Override
	public boolean isYSupport(int y) {
		return false;
	}

	public SimplePoint2D clone(int maxX, int maxY) {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D moveX(int x) {
		return new DefaultPoint2D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D moveY(int y) {
		return new DefaultPoint2D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D moveX(int x, Placement ySupport) {
		return new DefaultYSupportPoint2D(x, minY, minZ, maxY, maxY, maxZ, ySupport);
	}

	@Override
	public SimplePoint2D moveY(int y, Placement xSupport) {
		return new DefaultXSupportPoint2D(minX, y, minZ, maxX, maxX, maxZ, xSupport);
	}

	@Override
	public Point clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D clone() {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ);
	}

}
