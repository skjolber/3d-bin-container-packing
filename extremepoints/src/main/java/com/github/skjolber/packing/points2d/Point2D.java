package com.github.skjolber.packing.points2d;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.points3d.Point3D;

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
			int compare = Integer.compare(o1.minX, o2.minX);
			if(compare == 0) {
				compare = Integer.compare(o2.maxX, o1.maxX);
				if(compare == 0) {
					boolean o1XSupportPoint2D = o1 instanceof XSupportPoint2D;
					boolean o2XSupportPoint2D = o2 instanceof XSupportPoint2D;
					
					return Boolean.compare(o1XSupportPoint2D, o2XSupportPoint2D);
				}
			}
			return compare;
		}
	};

	public static final Comparator<Point2D> Y_COMPARATOR = new Comparator<Point2D>() {
		
		@Override
		public int compare(Point2D o1, Point2D o2) {
			int compare = Integer.compare(o1.minY, o2.minY);
			if(compare == 0) {
				compare = Integer.compare(o2.maxY, o1.maxY);
				if(compare == 0) {
					boolean o1YSupportPoint2D = o1 instanceof YSupportPoint2D;
					boolean o2YSupportPoint2D = o2 instanceof YSupportPoint2D;
					
					return Boolean.compare(o1YSupportPoint2D, o2YSupportPoint2D);
				}
			}
			return compare;
		}
	};
	
	protected final int minX;
	protected final int minY;
	
	protected int maxY;
	protected int maxX;
	
	protected int dx;
	protected int dy;
	
	protected long area;
	
	public Point2D(int minX, int minY, int maxX, int maxY) {
		super();
		
		if(maxX < minX) {
			throw new IllegalArgumentException("X: "+ maxX + " < " + minX);
		}

		if(maxY < minY) {
			throw new IllegalArgumentException("Y: "+ maxY + " < " + minY);
		}

		this.minX = minX;
		this.minY = minY;
		this.maxY = maxY;
		this.maxX = maxX;
		
		this.dx = maxX - minX + 1;
		this.dy = maxY - minY + 1;
		
		calculateArea();
	}
	
	private void calculateArea() {
		this.area = (long)dx * (long)dy;
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
		
		calculateArea();
	}
	
	public void setMaxY(int maxY) {
		if(maxY < 0) {
			throw new RuntimeException("Cannot set max y to " + maxY + " for " + minX + "x" + minY);
		}
		this.maxY = maxY;
		
		this.dy = maxY - minY + 1;
		
		calculateArea();
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
	
	public boolean strictlyInsideX(int x1, int x2) {
		// not including limits
		return x1 < minX && minX < x2;
	}

	public boolean strictlyInsideY(int y1, int y2) {
		// not including limits
		return y1 < minY && minY < y2;
	}
	
	public boolean isShadowedOrSwallowedByX(int min, int max) {
		return minX < max && maxX > min;
	}

	public boolean isShadowedOrSwallowedByY(int min, int max) {
		return minY < max && maxY > min;
	}

	public boolean swallowsMinY(int min, int max) {
		return min <= minY && minY <= max;
	}

	public boolean swallowsMinX(int min, int max) {
		return min <= minX && minX <= max;
	}

	@Override
	public String toString() {
		return "Point2D [" + minX + "x" + minY + " " + maxX + "x" + maxY + "]";
	}

	public abstract Point2D clone(int maxX, int maxY);

	public boolean containsInPlane(Point2D point) {
		return point.swallowsMinY(minY, maxY) && point.swallowsMinX(minX, maxX);
	}
	public boolean eclipses(Point2D point) {
		return eclipsesX(point) && eclipsesY(point);
	}

	public boolean eclipsesX(Point2D point) {
		return minX <= point.getMinX() && point.getMaxX() <= maxX;
	}

	public boolean eclipsesY(Point2D point) {
		return minY <= point.getMinY() && point.getMaxY() <= maxY;
	}

	public long getArea() {
		return area;
	}

	public boolean fits2D(StackValue stackValue) {
		return !(stackValue.getDx() > dx || stackValue.getDy() > dy);
	}

	public abstract List<Placement2D> getPlacements2D();
	
}
