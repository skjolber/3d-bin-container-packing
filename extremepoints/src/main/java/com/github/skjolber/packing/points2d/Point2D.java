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
	
	public static final Comparator<Point2D> X_COMPARATOR = new Comparator<Point2D>() {
		
		@Override
		public int compare(Point2D o1, Point2D o2) {
			return Integer.compare(o1.minX, o2.minX);
		}
	};

	public static final Comparator<Point2D> Y_COMPARATOR = new Comparator<Point2D>() {
		
		@Override
		public int compare(Point2D o1, Point2D o2) {
			return Integer.compare(o1.minY, o2.minY);
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

	public boolean isYSupport(int y) {
		return false;
	}

	public boolean isXSupport(int x) {
		return false;
	}

	public boolean isYEdge(int y) {
		return false;
	}

	public boolean isXEdge(int x) {
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
		if(maxX < 0) {
			throw new RuntimeException("Cannot set max x to " + maxX + " for " + minX + "x" + minY);
		}
		this.maxX = maxX;
		
		this.dx = maxX - minX + 1;
	}
	
	public void setMaxY(int maxY) {
		if(maxY < 0) {
			throw new RuntimeException("Cannot set max y to " + maxY + " for " + minX + "x" + minY);
		}
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
	
	public boolean crossesX(int x) {
		// not including limits
		return minX < x && maxX > x;
	}
	
	public boolean crossesY(int y) {
		// not including limits
		return minY < y && y < maxY; 
	}
	
	public boolean innerX(int x1, int x2) {
		// not including limits
		return x1 < minX && minX < x2;
	}

	public boolean innerY(int y1, int y2) {
		// not including limits
		return y1 < minY && minY < y2;
	}
	
	public boolean shadowedOrSwallowedX(int min, int max) {
		return minX < max && maxX > min;
	}

	public boolean shadowedOrSwallowedY(int min, int max) {
		return minY < max && maxY > min;
	}

	public boolean swallowedY(int min, int max) {
		return min <= minY && minY <= max;
	}

	public boolean swallowedX(int min, int max) {
		return min <= minX && minX <= max;
	}

	@Override
	public String toString() {
		return "Point2D [minX=" + minX + ", minY=" + minY + ", maxY=" + maxY + ", maxX=" + maxX + "]";
	}

	


}
