package com.github.skjolber.packing.points2d;

public class DefaultHorizontalSupportPoint2D extends Point2D implements HorizontalSupportPoint2D {

	/** range constrained to current minY */
	private final int horizontalSupportMinX;
	private final int horizontalSupportMaxX;
	
	public DefaultHorizontalSupportPoint2D(int minX, int minY, int maxX, int maxY, int horizontalSupportMinX, int horizontalSupportMaxX) {
		super(minX, minY, maxX, maxY);
		this.horizontalSupportMinX = horizontalSupportMinX;
		this.horizontalSupportMaxX = horizontalSupportMaxX;
	}
	
	@Override
	public boolean isHorizontalSupport(int x) {
		return horizontalSupportMinX <= x && x <= horizontalSupportMaxX;
	}
	
	public int getHorizontalSupportMinX() {
		return horizontalSupportMinX;
	}
	
	public int getHorizontalSupportMaxX() {
		return horizontalSupportMaxX;
	}

}
