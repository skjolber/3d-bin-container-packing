package com.github.skjolber.packing.points;

public class DefaultPoint2D extends Point2D {

	public DefaultPoint2D(int minX, int minY, int maxY, int maxX) {
		super(minX, minY, maxX, maxY);
	}

	@Override
	public boolean isFixedY() {
		return false;
	}

	@Override
	public boolean isFixedX() {
		return false;
	}

}
