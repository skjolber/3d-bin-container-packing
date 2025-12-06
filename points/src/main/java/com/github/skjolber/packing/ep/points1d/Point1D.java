package com.github.skjolber.packing.ep.points1d;

import com.github.skjolber.packing.api.point.Point;

public class Point1D extends Point {

	public Point1D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public String toString() {
		return "Point1D [" + minX + " " + maxX + "]";
	}

	@Override
	public Point clone(int maxX, int maxY, int maxZ) {
		return new Point1D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point1D clone() {
		return new Point1D(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public Point1D moveX(int x) {
		return new Point1D(x, minY, minZ, maxX, maxY, maxZ);
	}

	public Point1D moveY(int y) {
		return new Point1D(minX, y, minZ, maxX, maxY, maxZ);
	}

	public Point1D moveZ(int z) {
		return new Point1D(minX, minY, z, maxX, maxY, maxZ);
	}

}
