package com.github.skjolber.packing.points2d;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultYSupportPoint2D extends Point2D implements YSupportPoint2D  {

	/** range constrained to current minX */
	private final Placement2D ySupport;
	
	public DefaultYSupportPoint2D(int minX, int minY, int maxX, int maxY, Placement2D ySupport) {
		super(minX, minY, maxX, maxY);
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
	
	@Override
	public boolean isYEdge(int y) {
		return ySupport.getAbsoluteEndY() == y - 1;
	}
	
	public Point2D clone(int maxX, int maxY) {
		return new DefaultYSupportPoint2D(minX, minY, maxX, maxY, ySupport);
	}

	@Override
	public Placement2D getYSupport() {
		return ySupport;
	}
	
}
