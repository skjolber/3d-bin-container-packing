package com.github.skjolber.packing.impl.points3d;

public class DefaultPoint extends Point {

	public DefaultPoint(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public boolean isFixedY() {
		return false;
	}

	@Override
	public boolean isFixedX() {
		return false;
	}

	@Override
	public boolean isFixedZ() {
		return false;
	}
}
