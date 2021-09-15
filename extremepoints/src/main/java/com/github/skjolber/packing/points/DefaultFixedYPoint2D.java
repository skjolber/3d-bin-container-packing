package com.github.skjolber.packing.points;

public class DefaultFixedYPoint2D extends Point2D implements FixedYPoint2D {

	/** range constrained to current minY */
	private final int fixedMinX;
	private final int fixedMaxX;
	
	public DefaultFixedYPoint2D(int minX, int minY, int maxX, int maxY, int fixedX, int fixedXx) {
		super(minX, minY, maxX, maxY);
		this.fixedMinX = fixedX;
		this.fixedMaxX = fixedXx;
	}
	
	@Override
	public boolean isFixedY() {
		return true;
	}
	@Override
	public boolean isFixedX() {
		return false;
	}
	
	public int getFixedMinX() {
		return fixedMinX;
	}
	
	public int getFixedMaxX() {
		return fixedMaxX;
	}

}
