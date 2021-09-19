package com.github.skjolber.packing.points2d;

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
		
		this.dx = maxX - minX + 1;
		this.dy = maxY - minY + 1;
	}

	public boolean isFixedX(int y) {
		return false;
	}

	public boolean isFixedY(int z) {
		return false;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}
	
	/**
	 * 
	 * Get y constraint (inclusive)
	 * 
	 * @return
	 */

	public int getMaxY() {
		return maxY;
	}

	/**
	 * 
	 * Get x constraint (inclusive)
	 * 
	 * @return
	 */

	public int getMaxX() {
		return maxX;
	}
	
	public void setMaxX(int maxX) {
		this.maxX = maxX;
		
		this.dx = maxX - minX + 1;
	}
	
	public void setMaxY(int maxY) {
		this.maxY = maxY;
		
		this.dy = maxY - minY + 1;
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
