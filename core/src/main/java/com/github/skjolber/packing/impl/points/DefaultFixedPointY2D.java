package com.github.skjolber.packing.impl.points;

public class DefaultFixedPointY2D extends Point2D implements FixedPointY2D {

	/** range constrained to current minY */
	private final int fixedMinX;
	private final int fixedMaxX;
	
	public DefaultFixedPointY2D(int minX, int minY, int maxX, int maxY, int fixedX, int fixedXx) {
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
