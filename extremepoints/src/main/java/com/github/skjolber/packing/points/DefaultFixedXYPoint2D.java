package com.github.skjolber.packing.points;

public class DefaultFixedXYPoint2D extends Point2D implements FixedYPoint2D, FixedXPoint2D {

	/** range constrained to current minY */
	private final int fixedMinX;
	private final int fixedMaxX;
	
	/** range constrained to current minX */
	private int fixedMinY;
	private int fixedMaxY;
	
	public DefaultFixedXYPoint2D(int minX, int minY, int maxX, int maxY, int fixedX, int fixedXx, int fixedY, int fixedYy) {
		super(minX, minY, maxX, maxY);
		this.fixedMinX = fixedX;
		this.fixedMaxX = fixedXx;
		this.fixedMinY = fixedY;
		this.fixedMaxY = fixedYy;
	}


	@Override
	public boolean isFixedY() {
		return true;
	}
	
	@Override
	public boolean isFixedX() {
		return true;
	}
	
	public int getFixedMinX() {
		return fixedMinX;
	}

	public int getFixedMaxX() {
		return fixedMaxX;
	}

	public int getFixedMinY() {
		return fixedMinY;
	}
	
	public int getFixedMaxY() {
		return fixedMaxY;
	}

}
