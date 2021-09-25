package com.github.skjolber.packing.points2d;

public class DefaultFixedXYPoint2D extends Point2D implements FixedYPoint2D, FixedXPoint2D {

	/** range constrained to current minY */
	private final int fixedMinX;
	private final int fixedMaxX;
	
	/** range constrained to current minX */
	private int fixedMinY;
	private int fixedMaxY;
	
	public DefaultFixedXYPoint2D(int minX, int minY, int maxX, int maxY, int fixedX, int fixedXx, int fixedY, int fixedYy) {
		super(minX, minY, maxX, maxY);
		
		if(minX < 0) {
			throw new RuntimeException();
		}
		if(minY < 0) {
			throw new RuntimeException();
		}
		if(maxX < 0) {
			throw new RuntimeException();
		}
		if(maxY < 0) {
			throw new RuntimeException();
		}
		if(fixedX < 0) {
			throw new RuntimeException();
		}
		if(fixedXx < 0) {
			throw new RuntimeException();
		}
		if(fixedY < 0) {
			throw new RuntimeException();
		}
		if(fixedYy < 0) {
			throw new RuntimeException();
		}
		
		this.fixedMinX = fixedX;
		this.fixedMaxX = fixedXx;
		this.fixedMinY = fixedY;
		this.fixedMaxY = fixedYy;
	}

	@Override
	public boolean isFixedX(int y) {
		return y <= fixedMaxY;
	}

	@Override
	public boolean isFixedY(int x) {
		return x <= fixedMaxX;
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
