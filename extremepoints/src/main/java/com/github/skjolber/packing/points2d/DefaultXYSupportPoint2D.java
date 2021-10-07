package com.github.skjolber.packing.points2d;

public class DefaultXYSupportPoint2D extends Point2D implements XSupportPoint2D, YSupportPoint2D {

	/** range constrained to current minY */
	private final int xSupportMinX;
	private final int xSupportMaxX;
	
	/** range constrained to current minX */
	private int ySupportMinY;
	private int ySupportMaxY;
	
	public DefaultXYSupportPoint2D(int minX, int minY, int maxX, int maxY, int xSupportMinX, int xSupportMaxX, int ySupportMinY, int ySupportMaxY) {
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
		if(xSupportMinX < 0) {
			throw new RuntimeException();
		}
		if(xSupportMaxX < 0) {
			throw new RuntimeException();
		}
		if(ySupportMinY < 0) {
			throw new RuntimeException();
		}
		if(ySupportMaxY < 0) {
			throw new RuntimeException();
		}
		
		this.xSupportMinX = xSupportMinX;
		this.xSupportMaxX = xSupportMaxX;
		this.ySupportMinY = ySupportMinY;
		this.ySupportMaxY = ySupportMaxY;
	}

	@Override
	public boolean isYSupport(int y) {
		return ySupportMinY <= y && y <= ySupportMaxY;
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

	public int getYSupportMinY() {
		return ySupportMinY;
	}
	
	public int getYSupportMaxY() {
		return ySupportMaxY;
	}

}
