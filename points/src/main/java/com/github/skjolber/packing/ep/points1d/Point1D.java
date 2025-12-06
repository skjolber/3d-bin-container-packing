package com.github.skjolber.packing.ep.points1d;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.point.Point;

public class Point1D extends Point {

	public Point1D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public boolean intersects(Point1D point) {
		return !(point.getMaxX() < minX || point.getMinX() > maxX);
	}

	public boolean crossesX(int x) {
		// not including limits
		return minX < x && maxX > x;
	}

	public boolean crossesY(int y) {
		// not including limits
		return minY < y && y < maxY;
	}

	public boolean strictlyInsideX(int x1, int x2) {
		// not including limits
		return x1 < minX && minX < x2;
	}

	public boolean strictlyInsideY(int y1, int y2) {
		// not including limits
		return y1 < minY && minY < y2;
	}

	@Override
	public String toString() {
		return "Point1D [" + minX + " " + maxX + "]";
	}

	public boolean eclipses(Point1D point) {
		return minX <= point.getMinX() && point.getMaxX() <= maxX;
	}

	public boolean eclipsesMovedX(Point1D point, int x) {
		return minX <= x && point.getMaxX() <= maxX;
	}

	public long getArea() {
		return area;
	}

	public boolean fits1D(BoxStackValue stackValue) {
		return !(stackValue.getDx() > dx);
	}

	public boolean isInsideX(int xx) {
		return minX <= xx && xx <= maxX;
	}

	public long getAreaAtX(int xx) {
		return dy * (long)(maxX - xx + 1);
	}

	public long getAreaAtY(int yy) {
		return dx * (long)(maxY - yy + 1);
	}

	public long getAreaAtMaxX(int maxX) {
		return dy * (long)(maxX - minX + 1);
	}

	public long getAreaAtMaxY(int maxY) {
		return dx * (long)(maxY - minY + 1);
	}

	@Override
	public Point clone(int maxX, int maxY, int maxZ) {
		return new Point1D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Point1D clone() {
		return new Point1D(minX, minY, minZ, maxX, maxY, maxZ);
	}

}
