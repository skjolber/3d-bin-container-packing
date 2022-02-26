package com.github.skjolber.packing.test.bouwkamp;

public class BouwkampBox {
	
	protected final int minX;
	protected final int minY;
	
	protected final int maxY;
	protected final int maxX;

	protected final int number;

	public BouwkampBox(int number, int minX, int minY, int maxY, int maxX) {
		super();
		this.number = number;
		this.minX = minX;
		this.minY = minY;
		this.maxY = maxY;
		this.maxX = maxX;
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMaxX() {
		return maxX;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}
	
	public int getNumber() {
		return number;
	}
	
	public boolean intersects(BouwkampBox point) {
		return !(point.getMaxX() < minX || point.getMinX() > maxX || point.getMaxY() < minY || point.getMinY() > maxY);
	}
	
}
