package com.github.skjolber.packing.api.ep;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.StackValue;

public abstract class Point2D<P extends Placement2D> implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final Comparator<Point2D<?>> COMPARATOR_X_THEN_Y = new Comparator<Point2D<?>>() {

		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			int x = o1.minX.compareTo(o2.minX);

			if(x == 0) {
				x = o1.minY.compareTo(o2.minY);
			}

			if(x == 0) {
				x = o2.maxX.compareTo(o1.maxX);
			}
			if(x == 0) {
				x = o2.maxY.compareTo(o1.maxY);
			}

			return x;
		}
	};

	public static final Comparator<Point2D<?>> COMPARATOR_MOVE_YY = new Comparator<Point2D<?>>() {

		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			int x = o1.minX.compareTo(o2.minX);

			if(x == 0) {
				x = o2.maxX.compareTo(o1.maxX);
			}

			if(x == 0) {
				x = o2.maxY.compareTo(o1.maxY);
			}

			return x;
		}
	};

	public static final Comparator<Point2D<?>> COMPARATOR_MOVE_XX = new Comparator<Point2D<?>>() {

		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			int x = o1.minY.compareTo(o2.minY);

			if(x == 0) {
				x = o2.maxY.compareTo(o1.maxY);
			}
			if(x == 0) {
				x = o2.maxX.compareTo(o1.maxX);
			}

			return x;
		}
	};

	public static final Comparator<Point2D<?>> X_COMPARATOR = new Comparator<Point2D<?>>() {

		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			int compare = o1.minX.compareTo(o2.minX);
			if(compare == 0) {
				compare = o2.maxX.compareTo(o1.maxX);
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
			int compare = o1.minY.compareTo(o2.minY);
			if(compare == 0) {
				compare = o2.maxY.compareTo(o1.maxY);
				if(compare == 0) {
					boolean o1YSupportPoint2D = o1 instanceof YSupportPoint2D;
					boolean o2YSupportPoint2D = o2 instanceof YSupportPoint2D;

					return Boolean.compare(o1YSupportPoint2D, o2YSupportPoint2D);
				}
			}
			return compare;
		}
	};

	protected final BigDecimal minX;
	protected final BigDecimal minY;

	protected BigDecimal maxY;
	protected BigDecimal maxX;

	protected BigDecimal dx;
	protected BigDecimal dy;

	protected BigDecimal area;
	protected BigDecimal volume;

	public Point2D(BigDecimal minX, BigDecimal minY, BigDecimal maxX, BigDecimal maxY) {
		super();

		/*
		if(maxX < minX) {
			throw new IllegalArgumentException("MaxX " + maxX + " is less than minX " + minX);
		}
		
		if(maxY < minY) {
			throw new IllegalArgumentException("MaxY " + maxY + " is less than minY " + minY);
		}
		*/

		this.minX = minX;
		this.minY = minY;
		this.maxY = maxY;
		this.maxX = maxX;

		this.dx = maxX.subtract(minX).add(BigDecimal.ONE);
		this.dy = maxY.subtract(minY).add(BigDecimal.ONE);

		calculateArea();
	}

	private void calculateArea() {
		this.area = dx.multiply(dy);
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

	public boolean isXSupport(BigDecimal x) {
		return false;
	}

	public BigDecimal getMinX() {
		return minX;
	}

	public BigDecimal getMinY() {
		return minY;
	}

	/**
	 * 
	 * Get y constraint (inclusive)
	 * 
	 * @return max y
	 */

	public BigDecimal getMaxY() {
		return maxY;
	}

	/**
	 * 
	 * Get x constraint (inclusive)
	 * 
	 * @return max x
	 */

	public BigDecimal getMaxX() {
		return maxX;
	}

	public void setMaxX(BigDecimal maxX) {
		if(maxX.compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("Cannot set max x to " + maxX + " for " + minX + "x" + minY);
		}
		this.maxX = maxX;

		this.dx = maxX.subtract(minX).add(BigDecimal.ONE);

		calculateArea();
	}

	public void setMaxY(BigDecimal maxY) {
		if(maxY.compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("Cannot set max y to " + maxY + " for " + minX + "x" + minY);
		}
		this.maxY = maxY;

		this.dy = maxY.subtract(minY).add(BigDecimal.ONE);

		calculateArea();
	}

	public BigDecimal getDy() {
		return dy;
	}

	public BigDecimal getDx() {
		return dx;
	}

	public boolean intersects(Point2D<P> point) {
		return !(point.getMaxX().compareTo(minX) < 0 || point.getMinX().compareTo(maxX) > 0 || point.getMaxY().compareTo(minY) < 0 || point.getMinY().compareTo(maxY) > 0);
	}

	public boolean crossesX(BigDecimal x) {
		// not including limits
		return minX.compareTo(x) < 0 && maxX.compareTo(x) > 0;
	}

	public boolean crossesY(BigDecimal y) {
		// not including limits
		return minY.compareTo(y) < 0 && y.compareTo(maxY) < 0;
	}

	public boolean strictlyInsideX(BigDecimal x1, BigDecimal x2) {
		// not including limits
		return x1.compareTo(minX) < 0 && minX.compareTo(x2) < 0;
	}

	public boolean strictlyInsideY(BigDecimal y1, BigDecimal y2) {
		// not including limits
		return y1.compareTo(minY) < 0 && minY.compareTo(y2) < 0;
	}

	public boolean isShadowedByX(BigDecimal min, BigDecimal max) {
		return minX.compareTo(min) < 0 && maxX.compareTo(max) > 0;
	}

	public boolean isShadowedByY(BigDecimal min, BigDecimal max) {
		return minY.compareTo(min) < 0 && maxY.compareTo(max) > 0;
	}

	public boolean shadowsOrSwallowsX(BigDecimal min, BigDecimal max) {
		return minX.compareTo(max) <= 0 && maxX.compareTo(min) >= 0;
	}

	public boolean isShadowedOrSwallowedByY(BigDecimal min, BigDecimal max) {
		return minY.compareTo(max) < 0 && maxY.compareTo(min) > 0;
	}

	public boolean swallowsMinY(BigDecimal min, BigDecimal max) {
		return min.compareTo(minY) <= 0 && minY.compareTo(max) <= 0;
	}

	public boolean swallowsMinX(BigDecimal min, BigDecimal max) {
		return min.compareTo(minX) <= 0 && minX.compareTo(max) <= 0;
	}

	public boolean swallowsMaxY(BigDecimal min, BigDecimal max) {
		return min.compareTo(maxY) <= 0 && maxY.compareTo(max) <= 0;
	}

	public boolean swallowsMaxX(BigDecimal min, BigDecimal max) {
		return min.compareTo(maxX) <= 0 && maxX.compareTo(max) <= 0;
	}

	@Override
	public String toString() {
		return "Point2D [" + minX + "x" + minY + " " + maxX + "x" + maxY + "]";
	}

	public abstract Point2D<P> clone(BigDecimal maxX, BigDecimal maxY);

	public boolean eclipses(Point2D<P> point) {
		return minY.compareTo(point.getMinY()) <= 0 &&
				minX.compareTo(point.getMinX()) <= 0 &&
				point.getMaxX().compareTo(maxX) <= 0 &&
				point.getMaxY().compareTo(maxY) <= 0;
	}

	public boolean eclipsesMovedX(Point2D<P> point, BigDecimal x) {
		return minX.compareTo(x) <= 0 && point.getMaxX().compareTo(maxX) <= 0 && eclipsesY(point);
	}

	public boolean eclipsesMovedY(Point2D<P> point, BigDecimal y) {
		return minY.compareTo(y) <= 0 && point.getMaxY().compareTo(maxY) <= 0 && eclipsesX(point);
	}

	public boolean eclipsesX(Point2D<P> point) {
		return minX.compareTo(point.getMinX()) <= 0 && point.getMaxX().compareTo(maxX) <= 0;
	}

	public boolean eclipsesY(Point2D<P> point) {
		return minY.compareTo(point.getMinY()) <= 0 && point.getMaxY().compareTo(maxY) <= 0;
	}

	public BigDecimal getArea() {
		return area;
	}

	public boolean fits2D(StackValue stackValue) {
		return !(stackValue.getDx().compareTo(dx) > 0 || stackValue.getDy().compareTo(dy) > 0);
	}

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

	public abstract Point2D<P> moveX(BigDecimal x);

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

	public abstract Point2D<P> moveX(BigDecimal x, P ySupport);

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

	public abstract Point2D<P> moveY(BigDecimal y);

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

	public abstract Point2D<P> moveY(BigDecimal y, P xSupport);

	public boolean isInsideY(BigDecimal yy) {
		return minY.compareTo(yy) <= 0 && yy.compareTo(maxY) <= 0;
	}

	public boolean isInsideX(BigDecimal xx) {
		return minX.compareTo(xx) <= 0 && xx.compareTo(maxX) <= 0;
	}

	public BigDecimal getAreaAtX(BigDecimal xx) {
		return dy.multiply(maxX.subtract(xx).add(BigDecimal.ONE));
	}

	public BigDecimal getAreaAtY(BigDecimal yy) {
		return dx.multiply(maxY.subtract(yy).add(BigDecimal.ONE)) ;
	}

	public BigDecimal getAreaAtMaxX(BigDecimal maxX) {
		return dy.multiply((maxX.subtract(minX).add(BigDecimal.ONE)));
	}

	public BigDecimal getAreaAtMaxY(BigDecimal maxY) {
		return dx.multiply(maxY.subtract(minY).add(BigDecimal.ONE));
	}

}
