package com.github.skjolber.packing.points2d;

public class DefaultVerticalSupportPoint2D extends Point2D implements VerticalSupportPoint2D  {

	/** range constrained to current minX */
	private final int verticalSupportMinY;
	private final int verticalSupportMaxY;
	
	public DefaultVerticalSupportPoint2D(int minX, int minY, int maxX, int maxY, int verticalSupportMinY, int verticalSupportMaxY) {
		super(minX, minY, maxX, maxY);
		this.verticalSupportMinY = verticalSupportMinY;
		this.verticalSupportMaxY = verticalSupportMaxY;
	}
	
	@Override
	public boolean isVerticalSupport(int y) {
		return verticalSupportMinY <= y && y <= verticalSupportMaxY;
	}

	public int getVerticalSupportMinY() {
		return verticalSupportMinY;
	}
	
	public int getVeriftalSupportMaxY() {
		return verticalSupportMaxY;
	}
}
