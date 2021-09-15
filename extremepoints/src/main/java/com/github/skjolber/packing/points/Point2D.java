package com.github.skjolber.packing.points;

import java.util.Comparator;

public abstract class Point2D {

	public static final Comparator<Point2D> COMPARATOR = new Comparator<Point2D>() {
		
		@Override
		public int compare(Point2D o1, Point2D o2) {
			int x = Integer.compare(o1.minX, o2.minX);

			if(x == 0) {
				return Integer.compare(o1.minY, o2.minY);
			}
			return x;
		}
	};
	
	protected final int minX;
	protected final int minY;
	
	protected int maxY;
	protected int maxX;
	
	protected int dx;
	protected int dy;
	
	public Point2D(int minX, int minY, int maxX, int maxY) {
		super();
		this.minX = minX;
		this.minY = minY;
		this.maxY = maxY;
		this.maxX = maxX;
		
		this.dx = maxX - minX;
		this.dy = maxY - minY;
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
	
	public void setMaxX(int maxX) {
		this.maxX = maxX;
		
		this.dx = maxX - minX;
	}
	
	public void setMaxY(int maxY) {
		this.maxY = maxY;
		
		this.dy = maxY - minY;
	}
	
	public int getDy() {
		return dy;
	}

	public int getDx() {
		return dx;
	}
	
	public boolean intersects(Point2D point) {
		return !(point.getMaxX() < minX || point.getMinX() > maxX || point.getMaxY() < minY || point.getMinY() > maxY);
	}
}
