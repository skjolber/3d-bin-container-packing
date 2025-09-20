package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.ep.Point;

public class DefaultYSupportPoint2D extends SimplePoint2D implements YSupportPoint2D {

	private static final long serialVersionUID = 1L;
	/** range constrained to current minX */
	private final Placement ySupport;

	public DefaultYSupportPoint2D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Placement ySupport) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		this.ySupport = ySupport;
	}

	@Override
	public boolean isYSupport(int y) {
		return ySupport.getAbsoluteY() <= y && y <= ySupport.getAbsoluteEndY();
	}

	public int getYSupportMinY() {
		return ySupport.getAbsoluteY();
	}

	public int getYSupportMaxY() {
		return ySupport.getAbsoluteEndY();
	}

	@Override
	public String toString() {
		return "DefaultYSupportPoint2D [" + minX + "x" + minY + " " + maxX + "x" + maxY
				+ ", ySupportMinY=" + getYSupportMinY() + ", ySupportMaxY=" + getYSupportMaxY() + "]";
	}

	public Point2D clone(int maxX, int maxY) {
		return new DefaultYSupportPoint2D(minX, minY, minZ, maxX, maxY, maxZ, ySupport);
	}
	
	@Override
	public Point clone(int maxX, int maxY, int maxZ) {
		return new DefaultYSupportPoint2D(minX, minY, minZ, maxX, maxY, maxZ, ySupport);
	}

	@Override
	public int getSupportedMaxY() {
		return ySupport.getAbsoluteEndY();
	}
	
	@Override
	public int getSupportedMinY() {
		return ySupport.getAbsoluteY();
	}

	@Override
	public SimplePoint2D moveY(int y) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D(minX, y, minZ, maxX, maxY, maxZ, ySupport);
		}
		return new DefaultPoint2D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D moveY(int y, Placement xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D(minX, y, minZ, maxX, maxY, maxZ, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D(minX, y, minZ, maxX, maxY, maxZ, xSupport);
	}

	@Override
	public SimplePoint2D moveX(int x, Placement ySupport) {
		return new DefaultYSupportPoint2D(x, minY, minZ, maxX, maxY, maxZ, ySupport);
	}

	@Override
	public SimplePoint2D moveX(int x) {
		return new DefaultPoint2D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D clone() {
		return new DefaultPoint2D(minX, minY, minZ, maxX, maxY, maxZ);
	}

}
