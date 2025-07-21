package com.github.skjolber.packing.ep.points2d;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.Point;

public abstract class Point2D extends Point {

	private static final long serialVersionUID = 1L;

	public static final Comparator<Point2D> COMPARATOR_X_THEN_Y = new Comparator<Point2D>() {

		@Override
		public int compare(Point2D o1, Point2D o2) {
			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}
			
			if(o1.maxX < o2.maxX) {
				return 1;
			} else if(o1.maxX != o2.maxX) {
				return -1;
			}
			
			return -Integer.compare(o1.maxY, o2.maxY);
		}
	};

	public static final Comparator<Point2D> COMPARATOR_MOVE_YY = new Comparator<Point2D>() {

		@Override
		public int compare(Point2D o1, Point2D o2) {
			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.maxX < o2.maxX) {
				return 1;
			} else if(o1.maxX != o2.maxX) {
				return -1;
			}
			
			return -Integer.compare(o1.maxY, o2.maxY);
		}
	};

	public static final Comparator<Point2D> COMPARATOR_MOVE_XX = new Comparator<Point2D>() {

		@Override
		public int compare(Point2D o1, Point2D o2) {
			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}
			
			if(o1.maxY < o2.maxY) {
				return 1;
			} else if(o1.maxY != o2.maxY) {
				return -1;
			}

			return -Integer.compare(o1.maxX, o2.maxX);
		}
	};

	public Point2D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		/*
		if(maxX < minX) {
			throw new IllegalArgumentException("MaxX " + maxX + " is less than minX " + minX);
		}
		
		if(maxY < minY) {
			throw new IllegalArgumentException("MaxY " + maxY + " is less than minY " + minY);
		}
		*/
	}

	//
	// vmaxY |                    
	//       |          
	//  yy   |        ║ 
	//       |        ║
	//       |        ║ 
	//       |        ║   <-- support for a range of Y (at minX)
	//       |        ║
	//       |        ║
	//  minY |        x
	//       |
	//       |-----------------------------
	//                minX  

	public boolean isYSupport(int y) {
		return false;
	}

	//       |
	//       |
	//       |    
	//  minY |    x════════════════         <---- support for a range of x (at minY)
	//       |                     
	//       |                    
	//       |--------------------------
	//           minX             smaxX

	public boolean isXSupport(int x) {
		return false;
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

	@Override
	public String toString() {
		return "Point2D [" + minX + "x" + minY + " " + maxX + "x" + maxY + "]";
	}

	public abstract Point2D clone(int maxX, int maxY);

	public boolean eclipses(Point2D point) {
		return minY <= point.getMinY() &&
				minX <= point.getMinX() &&
				point.getMaxX() <= maxX &&
				point.getMaxY() <= maxY;
	}

	public boolean eclipsesMovedX(Point2D point, int x) {
		return minX <= x && point.getMaxX() <= maxX && eclipsesY(point);
	}

	public boolean eclipsesMovedY(Point2D point, int y) {
		return minY <= y && point.getMaxY() <= maxY && eclipsesX(point);
	}

	public long getArea() {
		return area;
	}

	public boolean fits2D(BoxStackValue stackValue) {
		return !(stackValue.getDx() > dx || stackValue.getDy() > dy);
	}

	public boolean isInsideY(int yy) {
		return minY <= yy && yy <= maxY;
	}

	public boolean isInsideX(int xx) {
		return minX <= xx && xx <= maxX;
	}

	public long getAreaAtX(int xx) {
		return dy * (long)(maxX - xx + 1);
	}

	public long getAreaAtY(int yy) {
		return dx * (long)(maxY - yy + 1);
	}

	public long getAreaAtMaxX(int maxX) {
		return dy * (long)(maxX - minX + 1);
	}

	public long getAreaAtMaxY(int maxY) {
		return dx * (long)(maxY - minY + 1);
	}

}
