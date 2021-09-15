package com.github.skjolber.packing.impl.points;

public abstract class Point2D implements Comparable<Point2D>{

	protected final int minX;
	protected final int minY;
	
	protected int maxY;
	protected int maxX;
	
	public Point2D(int minX, int minY, int maxX, int maxY) {
		super();
		this.minX = minX;
		this.minY = minY;
		this.maxY = maxY;
		this.maxX = maxX;
	}

	public abstract boolean isFixedY();
	
	public abstract boolean isFixedX();

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMaxX() {
		return maxX;
	}
	
	@Override
	public int compareTo(Point2D o) {
		
		int x = Integer.compare(this.minX, o.minX);

		if(x == 0) {
			return Integer.compare(this.minY, o.minY);
		}
		return x;
	}
	
	public void setMaxX(int maxX) {
		this.maxX = maxX;
	}
	
	public void setMaxY(int maxY) {
		this.maxY = maxY;
	}

}
