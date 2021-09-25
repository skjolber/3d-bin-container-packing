package com.github.skjolber.packing.points2d;

public class DefaultFixedXPoint2D extends Point2D implements FixedXPoint2D  {

	/** range constrained to current minX */
	private final int fixedMinY;
	private final int fixedMaxY;
	
	public DefaultFixedXPoint2D(int minX, int minY, int maxX, int maxY, int fixedY, int fixedYy) {
		super(minX, minY, maxX, maxY);
		this.fixedMinY = fixedY;
		this.fixedMaxY = fixedYy;
	}
	
	@Override
	public boolean isFixedX(int y) {
		return y <= fixedMaxY;
	}

	public int getFixedMinY() {
		return fixedMinY;
	}
	
	public int getFixedMaxY() {
		return fixedMaxY;
	}
}
