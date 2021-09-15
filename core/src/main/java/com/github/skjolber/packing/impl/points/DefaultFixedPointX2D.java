package com.github.skjolber.packing.impl.points;

public class DefaultFixedPointX2D extends Point2D implements FixedPointX2D  {

	/** range constrained to current minX */
	private final int fixedMinY;
	private final int fixedMaxY;
	
	public DefaultFixedPointX2D(int minX, int minY, int maxX, int maxY, int fixedY, int fixedYy) {
		super(minX, minY, maxX, maxY);
		this.fixedMinY = fixedY;
		this.fixedMaxY = fixedYy;
	}
	@Override
	public boolean isFixedY() {
		return false;
	}
	@Override
	public boolean isFixedX() {
		return true;
	}

	public int getFixedMinY() {
		return fixedMinY;
	}
	
	public int getFixedMaxY() {
		return fixedMaxY;
	}
}
