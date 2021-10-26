package com.github.skjolber.packing.points2d;

public class DefaultXSupportPoint2D extends Point2D implements XSupportPoint2D {

	/** range constrained to current minY */
	private final int xSupportMinX;
	private final int xSupportMaxX;
	
	public DefaultXSupportPoint2D(int minX, int minY, int maxX, int maxY, int xSupportMinX, int xSupportMaxX) {
		super(minX, minY, maxX, maxY);
		this.xSupportMinX = xSupportMinX;
		this.xSupportMaxX = xSupportMaxX;
	}
	
	@Override
	public boolean isXSupport(int x) {
		return xSupportMinX <= x && x <= xSupportMaxX;
	}
	
	public int getXSupportMinX() {
		return xSupportMinX;
	}
	
	public int getXSupportMaxX() {
		return xSupportMaxX;
	}
	
	@Override
	public boolean isXEdge(int x) {
		return xSupportMaxX == x - 1;
	}

	@Override
	public String toString() {
		return "DefaultXSupportPoint2D [minX=" + minX + ", minY=" + minY + ", maxY=" + maxY + ", maxX=" + maxX
				+ ", xSupportMinX=" + xSupportMinX + ", xSupportMaxX=" + xSupportMaxX + "]";
	}

	public Point2D clone(int maxX, int maxY) {
		return new DefaultXSupportPoint2D(minX, minY, maxX, maxY, xSupportMinX, Math.min(maxX, xSupportMaxX));
	}

	
}
