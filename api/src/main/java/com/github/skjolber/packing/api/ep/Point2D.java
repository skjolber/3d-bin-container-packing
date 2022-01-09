package com.github.skjolber.packing.api.ep;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.StackValue;

public abstract class Point2D<P extends Placement2D> {
	
	public static final Comparator<Point2D<?>> COMPARATOR_X_THEN_Y = new Comparator<Point2D<?>>() {
		
		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			int x = Integer.compare(o1.minX, o2.minX);

			if(x == 0) {
				x = Integer.compare(o1.minY, o2.minY);
			}
			
			if(x == 0) {
				x = -Integer.compare(o1.maxX, o2.maxX);
			}
			if(x == 0) {
				x = -Integer.compare(o1.maxY, o2.maxY);
			}
			
			return x;
		}
	};

	public static final Comparator<Point2D<?>> COMPARATOR_Y_THEN_X = new Comparator<Point2D<?>>() {
		
		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			int x = Integer.compare(o1.minY, o2.minY);

			if(x == 0) {
				x = Integer.compare(o1.minX, o2.minX);
			}
			if(x == 0) {
				x = -Integer.compare(o1.maxY, o2.maxY);
			}
			if(x == 0) {
				x = -Integer.compare(o1.maxX, o2.maxX);
			}
			return x;
		}
	};
	
	public static final Comparator<Point2D<?>> X_COMPARATOR = new Comparator<Point2D<?>>() {
		
		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
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

	public static final Comparator<Point2D<?>> Y_COMPARATOR = new Comparator<Point2D<?>>() {
		
		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
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
			throw new IllegalArgumentException("MaxX " + maxX + " is less than minX " + minX);
		}

		if(maxY < minY) {
			throw new IllegalArgumentException("MaxY " + maxY + " is less than minY " + minY);
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
	 * @return max y
	 */

	public int getMaxY() {
		return maxY;
	}

	/**
	 * 
	 * Get x constraint (inclusive)
	 * 
	 * @return max x
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
	
	public boolean intersects(Point2D<P> point) {
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

	public boolean isShadowedByX(int min, int max) {
		return minX < min && maxX > max;
	}

	public boolean isShadowedByY(int min, int max) {
		return minY < min && maxY > max;
	}

	public boolean shadowsOrSwallowsX(int min, int max) {
		return minX <= max && maxX >= min;
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

	public boolean swallowsMaxY(int min, int max) {
		return min <= maxY && maxY <= max;
	}

	public boolean swallowsMaxX(int min, int max) {
		return min <= maxX && maxX <= max;
	}

	@Override
	public String toString() {
		return "Point2D [" + minX + "x" + minY + " " + maxX + "x" + maxY + "]";
	}

	public abstract Point2D<P> clone(int maxX, int maxY);

	public boolean eclipses(Point2D<P> point) {
		return eclipsesX(point) && eclipsesY(point);
	}
	
	public boolean eclipsesMovedX(Point2D<P> point, int x) {
		return minX <= x && point.getMaxX() <= maxX && eclipsesY(point);
	}

	public boolean eclipsesMovedY(Point2D<P> point, int y) {
		return minY <= y && point.getMaxY() <= maxY && eclipsesX(point);
	}

	public boolean eclipsesX(Point2D<P> point) {
		return minX <= point.getMinX() && point.getMaxX() <= maxX;
	}

	public boolean eclipsesY(Point2D<P> point) {
		return minY <= point.getMinY() && point.getMaxY() <= maxY;
	}

	public long getArea() {
		return area;
	}

	public boolean fits2D(StackValue stackValue) {
		return !(stackValue.getDx() > dx || stackValue.getDy() > dy);
	}

	public abstract List<P> getPlacements2D();

	//       |                  
	//       |                  
	//       |                  
	//       |   |-------|      
	//       |   |       |      
	//       |   |       |      
	//       |---x=========================
	//
	//       |                  
	//       |                  
	//       |         |-------|
	//       |         |       |
	//       |         |       |
	//       |         |       |
	//       |---------x===================
	//
	
	public abstract Point2D<P> moveX(int x, int maxX, int maxY);
	
	//       |                  
	//       |                  
	//       |                  
	//       |   |-------|      
	//       |   |       |      
	//       |   |       |      
	//       |---x=========================
	//
	//       |      added y support        
	//       |         |         
	//       |---------║         
	//       |         ║         
	//       |         ║-------|
	//       |         ║       |
	//       |         ║       |
	//       |         ║       |
	//       |---------x===================
	//

	public abstract Point2D<P> moveX(int x, int maxX, int maxY, P ySupport);

	
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║-------|      
	//       |   ║       |      
	//       |   ║       |      
	//       |---x-------|----------------
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║---------|      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   x---------|      
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract Point2D<P> moveY(int y, int maxX, int maxY);

	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║-------|      
	//       |   ║       |      
	//       |   ║       |      
	//       |---x-------|----------------
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║---------|      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   x===================  <-- added x support
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract Point2D<P> moveY(int y, int maxX, int maxY, P xSupport);

	public boolean isInsideY(int yy) {
		 return minY <= yy && yy <= maxY;
	}
	
	public boolean isInsideX(int xx) {
		 return minX <= xx && xx <= maxX;
	}
	
	
}
