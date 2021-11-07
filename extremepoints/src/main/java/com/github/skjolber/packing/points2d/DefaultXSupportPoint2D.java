package com.github.skjolber.packing.points2d;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultXSupportPoint2D extends Point2D implements XSupportPoint2D {

	/** range constrained to current minY */
	private final Placement2D xSupport;
	
	public DefaultXSupportPoint2D(int minX, int minY, int maxX, int maxY, Placement2D xSupport) {
		super(minX, minY, maxX, maxY);
		this.xSupport = xSupport;
	}
	
	@Override
	public boolean isXSupport(int x) {
		return getXSupportMinX() <= x && x <= getXSupportMaxX();
	}
	
	public int getXSupportMinX() {
		return xSupport.getAbsoluteX();
	}
	
	public int getXSupportMaxX() {
		return xSupport.getAbsoluteEndX();
	}
	
	@Override
	public boolean isXEdge(int x) {
		return getXSupportMaxX() == x - 1;
	}

	@Override
	public String toString() {
		return "DefaultXSupportPoint2D [" +  + minX + "x" + minY + " " + maxX + "x" + maxY 
				+ ", xSupportMinX=" + getXSupportMinX() + ", xSupportMaxX=" + getXSupportMaxX() + "]";
	}

	public Point2D clone(int maxX, int maxY) {
		return new DefaultXSupportPoint2D(minX, minY, maxX, maxY, xSupport);
	}

	@Override
	public Placement2D getXSupport() {
		return xSupport;
	}
}
