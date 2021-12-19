package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.points3d.Default3DPlanePoint3D;
import com.github.skjolber.packing.points3d.DefaultPlacement3D;

/**
 * 
 * Implementation of so-called extreme points in 2D.
 *
 */

public class ExtremePoints2D3<P extends Placement2D> implements ExtremePoints<P, Point2D> {
	
	protected int containerMaxX;
	protected int containerMaxY;

	protected final List<Point2D> values = new ArrayList<>();
	protected final List<P> placements = new ArrayList<>();

	// reuse working variables
	protected final List<Point2D> deleted = new ArrayList<>();
	protected final List<Point2D> addYY = new ArrayList<>();
	protected final List<Point2D> addXX = new ArrayList<>();

	protected final List<Point2D> swallowed = new ArrayList<>();
	protected final List<Point2D> moveToXX = new ArrayList<>();
	protected final List<Point2D> moveToYY = new ArrayList<>();

	protected final List<Point2D> negativeMoveToYY = new ArrayList<>();
	protected final List<Point2D> negativeMoveToXX = new ArrayList<>();

	protected Placement2D containerPlacement;

	public ExtremePoints2D3(int dx, int dy) {
		setSize(dx, dy);
		addFirstPoint();
	}
	
	private void setSize(int dx, int dy) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;

		this.containerPlacement = new DefaultPlacement2D(0, 0, containerMaxX, containerMaxY);
	}

	private void addFirstPoint() {
		values.add(new DefaultXYSupportPoint2D(0, 0, containerMaxX, containerMaxY, containerPlacement, containerPlacement));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {

		// overall approach:
		//
		// find the points swallowed by the new placement
		//  - if supported, swallowed points give the new max limits
		//  - if unsupported, find also the shadowed points
		
		// project points points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side
		// add points shadowed by the two new points (if they could be moved in the negative direction)
		// remove points which are eclipsed by others
		
		// keep track of placement borders, where possible
		Point2D source = values.get(index);
		
		boolean hasSupport = source instanceof XSupportPoint2D || source instanceof YSupportPoint2D;

		System.out.println();
		System.out.println("Add " + index + " at " + placement.getAbsoluteX() + "x" + placement.getAbsoluteY());
		for(Point2D p : values) {
			System.out.println(" " + p);
		}

		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		
		//       |
		//       |
		//       |        |---------------| 
		//       |       
		//       |        |               |
		//       |    
		//  minY |        x════════════════         <---- support for a range of x (at minY)
		//       |                     
		//       |                    
		//       |--------------------------
		//               minX             maxX
		
		boolean xSupport = source.isXSupport(xx); // i.e. is source minY also minY at XX?

		//
		// vmaxY |                    
		//       |          
		//  yy   |        ║ - - - |
		//       |        ║
		//       |        ║       |
		//       |        ║              <-- support for a range of Y (at minX)
		//       |        ║       |
		//       |        ║  
		//  minY |        x - - - |
		//       |
		//       |-----------------------------
		//               minX    maxX
		//   
		
		boolean ySupport = source.isYSupport(yy); // i.e. is source minX also minX at YY?

		int maxX = -1;
		int maxYForMaxX = -1;
		
		int maxY = -1;
		int maxXForMaxY = -1;
		
		//    y
		//    |                          |
		//    |                          |
		//    |                          |
		// yy |         |-------|        |
		//    | a       |     b |        |
		//    |         |  b    |        |
		//    | a       |       |        |
		//    |     a   | b     |        |
		//    a---------b-------|        |
		//    |         |   c            |
		//    |         |     c          |
		//    |         |  c             |
		//    |         |      c         |
		//    |---------c----------------|--- x
		//		               xx 
		//
		// a - shadowed x
		// b - swallowed
		// c - shadowed y
		//
		// Copy maxX and maxY from existing points:
		// a & b used to determine maxX at yy
		// b & c used to determine maxY at xx
		
		for(Point2D point : values) {
			boolean swallowsMinX = point.swallowsMinX(placement.getAbsoluteX(), placement.getAbsoluteEndX());
			boolean swallowsMinY = point.swallowsMinY(placement.getAbsoluteY(), placement.getAbsoluteEndY());
			
			if(swallowsMinX && swallowsMinY) { // b
				System.out.println("Swallowed: " + point);
				swallowed.add(point);
				
				if(point.getMaxX() >= xx) {
					if(maxY == -1 || point.getMaxY() > maxY || (point.getMaxY() == maxY && point.getMaxX() > maxXForMaxY)) {
						maxY = point.getMaxY();
						maxXForMaxY = point.getMaxX();
					}
				}
				
				if(point.getMaxY() >= yy) {
					if(maxX == -1 || point.getMaxX() > maxX || (point.getMaxX() == maxX && point.getMaxY() > maxYForMaxX)) {
						maxX = point.getMaxX();
						maxYForMaxX = point.getMaxY();
					}
				}

			} else if(swallowsMinX && !xSupport && point.getMinY() < placement.getAbsoluteY() && placement.getAbsoluteY() <= point.getMaxY()) { // c
				// shadowed (at negative) y
				moveToYY.add(point);
				
				if(point.getMaxX() >= xx) {
					if(maxY == -1 || point.getMaxY() > maxY || (point.getMaxY() == maxY && point.getMaxX() > maxXForMaxY)) {
						maxY = point.getMaxY();
						maxXForMaxY = point.getMaxX();
					}
				}

			} else if(swallowsMinY && !ySupport && point.getMinX() < placement.getAbsoluteX() &&  placement.getAbsoluteX() <= point.getMaxX()) { // a
				// shadowed (at negative) x
				moveToXX.add(point);
				
				if(point.getMaxY() >= yy) {
					if(maxX == -1 || point.getMaxX() > maxX || (point.getMaxX() == maxX && point.getMaxY() > maxYForMaxX)) {
						maxX = point.getMaxX();
						maxYForMaxX = point.getMaxY();
					}
				}
			}
		}
		
		System.out.println("MaxX " + maxX + "x" + maxYForMaxX);
		System.out.println("MaxY " + maxXForMaxY + "x" + maxY);
		
		// project swallowed or shadowed to xx
		Point2D previousYY = null;
		
		if(!ySupport) {
			System.out.println("No y support");

			// not enough y support
			// 
			//       |  
			//       |  
			// yy    *←------|--------|
			//       |       |        |
			//       |       |        |
			//       |       |        |
			//       |-------|        | <- maximum y support (not high enough)
			//       |       |        |
			//       |       |        |
			//       |  box  |        |
			//       |       |        |
			//       |       |        |
			//       |-------x-----------------------------------
			//
			// or no y support
			//
			//       |  
			//       |  
			// yy    *←------|--------|
			//       |       |        |
			//       |       |        |
			//       |       |        |
			//       | empty |        | 
			//       |       |        |
			//       |       |        |
			//       |       |        |
			//       |       |        |
			//       |       |        |
			//       |-------x----------------
			
			// project negative x at yy
			
			Placement2D projectNegativeX = projectNegativeX(source.getMinX(), yy);
			if(projectNegativeX == null) {
				System.out.println("Negative x for yy limit at container border");
				for(Point2D point : values) {
					if(point.getMinX() < source.getMinX() && point.getMinY() <= yy && yy <= point.getMaxY() ) {
						negativeMoveToYY.add(point);
						System.out.println("Negative x for yy found point " + point);
					}
				}
				
			} else {
				System.out.println("Negative x for yy limit at " + projectNegativeX);
				for(Point2D point : values) {
					if(projectNegativeX.getAbsoluteEndX() < point.getMinX() && point.getMinX() < source.getMinX() && point.getMinY() <= yy && yy <= point.getMaxY() ) {
						negativeMoveToYY.add(point);
						System.out.println("Negative x for yy found point " + point);
					}
				}
			}
			
			if(!negativeMoveToYY.isEmpty()) {
				Collections.sort(negativeMoveToYY, Point2D.COMPARATOR_X_THEN_Y);
				for(Point2D p : negativeMoveToYY) {
					if(p.getMaxY() >= yy) {
						if(previousYY == null || previousYY.getMaxY() != p.getMaxY()) {
							Point2D moveY = p.moveY(p.getMinX(), yy, maxX, p.getMaxY());
							System.out.println("Negative x for yy move point " + p + " to " + moveY);

							addYY.add(moveY);
							
							previousYY = p;
						}
						
						if(p.getMaxY() != maxYForMaxX) {
							// another point is possible
							addYY.add(p.moveY(p.getMinX(), yy, p.getMaxX(), p.getMaxY(), placement));
						}						
					}
				}
			}
		} else {
			System.out.println("Y support");
		}
		
		System.out.println("Got " + moveToYY.size() + " shadowed below");

		if(!moveToYY.isEmpty()) {
			Collections.sort(moveToYY, Point2D.COMPARATOR_X_THEN_Y);

			// shadowedX
			for(Point2D point : moveToYY) {
				
				// add point on the other side
				// with x support
				if(point.getMaxY() >= yy) {
					if(previousYY == null || previousYY.getMaxY() != point.getMaxY()) {
						//addYY.add(point.moveY(point.getMinX(), yy, maxX, maxYForMaxX, placement));
						addYY.add(point.moveY(point.getMinX(), yy, maxX, point.getMaxY(), placement));
						
						previousYY = point;
					} else {
						System.out.println("Do not move " + point + " to yy, already at " + previousYY);
					}
					
					if(point.getMaxY() != maxYForMaxX) {
						// another point is possible
						addYY.add(point.moveY(point.getMinX(), yy, point.getMaxX(), point.getMaxY(), placement));
					}
				}

				if(point.getMinY() < placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);
				} else {
					deleted.add(point);
				}
			}
		}

		if(!swallowed.isEmpty()) {
			for(Point2D point : swallowed) {
				// add point on the other side
				// with x support
				System.out.println("Previous y at " + previousYY);

				if(point.getMaxY() >= yy) {
					
					if(previousYY == null || previousYY.getMaxY() != point.getMaxY()) {
						addYY.add(point.moveY(point.getMinX(), yy, maxX, point.getMaxY(), placement));

						System.out.println("Add swallowed at yy: " + point);

						previousYY = point;
					}
					
					if(point.getMaxY() != maxYForMaxX) {
						// another point is possible
						Point2D moveY = point.moveY(point.getMinX(), yy, point.getMaxX(), point.getMaxY(), placement);
						
						System.out.println("Additional swallowed at yy: " + point + " -> " + moveY);

						addYY.add(moveY);
					}
					
				}
				
				deleted.add(point);
			}			
		}
		
		// project swallowed or shadowed to yy
		
		Point2D previousXX = null;

		if(!xSupport) {
			System.out.println("No X support");
			// not enough x support
			//
			//      |
			//      |
			//      |-------------------|
			//      |                   |
			//      |                   |
			//      |                   |
			// minY x--------------------
			//      |           |       |
			//      |           |       |
			//      |   box     |       |
			//      |           |       ↓
			//      |-----------|-------*-----
			//     minX       max x     xx
			//                support
			//           (not wide enough)
			//
			//
			// or no x support
			//
			//      |
			//      |
			//  yy  |-------------------|
			//      |                   |
			//      |                   |
			//      |                   |
			// minY x--------------------
			//      |                   |
			//      |                   |
			//      |      empty        |
			//      |                   ↓
			//      |-------------------*-----
			//     minX                 xx
			//
			
			// project negative yy at x
			
			// find the most negative x that swallows xx
			
			Placement2D projectNegativeY = projectNegativeY(xx, source.getMinY());
			if(projectNegativeY == null) {
				System.out.println("Negative y for xx limit at container border");
				for(Point2D point : values) {
					if(point.getMinY() < source.getMinY() && point.getMinX() <= xx && xx <= point.getMaxX() ) {
						negativeMoveToXX.add(point);
					}
				}
			} else {
				System.out.println("Negative y for xx limit at " + projectNegativeY);
				for(Point2D point : values) {
					if(projectNegativeY.getAbsoluteEndY() < point.getMinY() && point.getMinY() < source.getMinY() && point.getMinX() <= xx && xx <= point.getMaxX() ) {
						negativeMoveToXX.add(point);
					}
				}
			}
			
			if(!negativeMoveToXX.isEmpty()) {
				Collections.sort(negativeMoveToXX, Point2D.COMPARATOR_Y_THEN_X);

				for (Point2D p : negativeMoveToXX) {
					if(p.getMaxX() >= xx) {
						if(previousXX == null || previousXX.getMaxX() != p.getMaxX()) {
							Point2D moveX = p.moveX(xx, p.getMinY(), p.getMaxX(), maxY);

							System.out.println("Negative y for xx move point " + p + " to " + moveX);

							addXX.add(moveX);

							previousXX = p;
						}
						
						
						if(p.getMaxX() != maxXForMaxY) {
							// another point is possible
							addXX.add(p.moveX(xx, p.getMinY(), p.getMaxX(), p.getMaxY(), placement));
						}						
					}
				}
			}
		} else {
			System.out.println("X support");
		}

		if(!moveToXX.isEmpty()) {
			System.out.println("Got " + moveToXX.size() + " shadowed left");
			
			Collections.sort(moveToXX, Point2D.COMPARATOR_Y_THEN_X);

			for (Point2D point : moveToXX) {
				if(point.getMaxX() >= xx) {
					if(previousXX == null || previousXX.getMaxX() != point.getMaxX()) {
						// add point on the other side
						// with x support
						//Point2D moveX = point.moveX(xx, point.getMinY(), maxXForMaxY, maxY, placement);
						Point2D moveX = point.moveX(xx, point.getMinY(), point.getMaxX(), maxY, placement);
						addXX.add(moveX);
						
						previousXX = point;
					} else {
						System.out.println("Do not move " + point + " to xx, already at " + previousXX);
					}
					
					if(point.getMaxX() != maxXForMaxY) {
						// another point is possible
						addXX.add(point.moveX(xx, point.getMinY(), point.getMaxX(), point.getMaxY(), placement));
					}

				}

				if(point.getMinX() < placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);
				} else {
					deleted.add(point);
				}
			}
		}
		
		if(!swallowed.isEmpty()) {
			System.out.println("Got " + swallowed.size() + " swallowed y: " + swallowed);
			Collections.sort(swallowed, Point2D.COMPARATOR_Y_THEN_X);
			System.out.println("Previous xx at " + previousXX);
			for (Point2D point : swallowed) {
				// add point on the other side
				// with x support
				if(point.getMaxX() >= xx) {
					if(previousXX == null || previousXX.getMaxX() != point.getMaxX()) {
						Point2D moveX = point.moveX(xx, point.getMinY(), point.getMaxX(), maxY, placement);
						addXX.add(moveX);
						
						System.out.println("Add swallowed at xx: " + point + " -> " + moveX);

						previousXX = point;
					}
					
					if(point.getMaxX() != maxXForMaxY) {
						// another point is possible
						Point2D moveX = point.moveX(xx, point.getMinY(), point.getMaxX(), point.getMaxY(), placement);
						System.out.println("Additional swallowed at xx: " + point + " -> " + moveX);
						addXX.add(moveX);
					}					
				}

				deleted.add(point);
			}
		}
		System.out.println("Add x was " + addXX);
		
		for(int i = 0; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			
			boolean remove;
			if(hasSupport) {
				remove = !constrainPositiveMax(point2d, placement);
			} else {
				System.out.println("Constrain floating max for " + point2d);
				remove = !constrainFloatingMax(point2d, placement);
			}
			if(remove) {
				values.remove(i);
				i--;
			}
		}
		
		values.removeAll(deleted);
		
		placements.add(placement);
		
		values.addAll(addXX);
		values.addAll(addYY);

		Collections.sort(values, Point2D.COMPARATOR_X_THEN_Y);
		removeEclipsed();
		Collections.sort(values, Point2D.COMPARATOR_Y_THEN_X);
		removeEclipsed();

		swallowed.clear();
		moveToXX.clear();
		moveToYY.clear();
		
		negativeMoveToYY.clear();
		negativeMoveToXX.clear();
		
		addXX.clear();
		addYY.clear();
		
		deleted.clear();

		Collections.sort(values, Point2D.COMPARATOR_X_THEN_Y);

		System.out.println("");
		System.out.println("Finally " + values.size() + " points");
		for(Point2D p : values) {
			System.out.println(" " + p);
		}

		return !values.isEmpty();
	}

	protected void addX() {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			for (int i = 0; i < addXX.size(); i++) {
				Point2D p1 = addXX.get(i);

				if(existing.eclipses(p1)) {
					addXX.remove(i);
					i--;
				} else if(p1.eclipses(existing)) {
					values.set(index, p1);
					existing = p1;
					
					index--;
					break;
				}
			}
			index++;
		}
		
		values.addAll(addXX);	
	}
	
	protected void addY() {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			for (int i = 0; i < addYY.size(); i++) {
				Point2D p1 = addYY.get(i);
				
				if(existing.eclipses(p1)) {
					addYY.remove(i);
					i--;
				} else if(p1.eclipses(existing)) {
					values.set(index, p1);
					existing = p1;
					
					index--;
					break;
				}
			}
			index++;
		}
		
		values.addAll(addYY);	
	}

	private void appendFirstNegativeShadowXX(Point2D source, int xx, Point2D dx) {
		if(dx.getMinY() < source.getMinY()) {
			for (int i = 0; i < values.size(); i++) {
				Point2D point = values.get(i);
			
				// Add point shadowed by the new point (without constraining them)
					
				//
				//      |         |-------------------|
				//      |         |                   |
				//      |         |                   |
				//      |         |                   |
				//      |         |--------------------         
				//      |                                       ▲ 
				//      |     ◄----------------------------►    |           
				//      |                                       ▼ 
				//      |-----------------------------*-----    
					
				if(point.strictlyInsideY(dx.getMinY(), source.getMinY())) { // vertical constraint
					if(point.crossesX(xx)) { // horizontal constraint (crosses xx)'
						addXX.add(point);
					}
				}
			}
		}
	}
	
	protected void appendFirstNegativeShadowYY(Point2D source, int yy, Point2D dy) {
		// using dy
		if(dy.getMinX() < source.getMinX()) {
		
			// supported one way (by container border)
			//
			//       |  
			//       |  
			// yy    |-----------------|
			//       |        |        |
			//       |        |        |
			//       |   *    |        |
			//		         minX      xx
			//
			//       |  
			//       |  
			// yy    |---*-------------|  <--- move up
			//       |        |        |
			//       |        |        |
			//       |        |        |
			//		         minX      xx
			
			for (int i = 0; i < values.size(); i++) {
				Point2D point = values.get(i);

				// Add shadowed points
				if(point.strictlyInsideX(dy.getMinX(), source.getMinX())) { // vertical constraint
					if(point.crossesY(yy)) { // horizontal constraint (crosses xx)
						addYY.add(point);
					}
				}							
			}
		}
	}

	protected void appendShadowedAtXX(P placement, Point2D source, int xx, int yy, boolean includeSource) {
		
		// Move points shadowed by the placement
		//
		//    shadowed:
		//
		//    |                          |
		//    |                          |
		//    |                          |
		//    |         |-------|        |
		//    |         |       |        |
		//    *         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|-------|--------|---
		//			
		//    |                          |
		//    |                          |
		//    |                          |
		//    |         |-------|        |
		//    |         |       |        |
		//    *         |       *        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|-------|--------|---
		//    			
		//  point     source   xx      point
		//  minX       minX            maxX
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			if(point == source && !includeSource) {
				continue;
			}

			// Move points swallowed or shadowed by the placement
			if(point.isShadowedByX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support

					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), point.getMaxY(),
							placement
							);
					
					addXX.add(next);
				}
				
				// constrain current point
			}
		}
	}

	private void raiseToXX(P placement, int xx, int yy, int maxY) {
		// complete adding
		// if some were only shadowed, add a new point
		for (int i = 0; i < addXX.size(); i++) {
			Point2D point = addXX.get(i);					
			
			if(point.getMinX() < xx) {
				Point2D p;
				if(point.getMinY() < placement.getAbsoluteY()) {
					p = new DefaultPoint2D(xx, point.getMinY(), point.getMaxX(), maxY);
				} else {
					p = new DefaultYSupportPoint2D(xx, point.getMinY(), point.getMaxX(), maxY, placement);
				}

				addXX.set(i, p);
			}
		}
	}

	protected DefaultXYSupportPoint2D getSupportedAtXX(Point2D source, Placement2D p, int xx, int yy, int maxY) {
		// in other words there is a placement in negative y direction at XX 
		// so the y coordinate is minY

		//       |
		//       |
		// yy    |    |--------|
		//       |    |        |
		// smaxY |----|        | 	
		//       |    |        | 
		//       |    |        |
		//       |    |        |
		//       |    |        | 
		//       |    |        |
		//  minY |    |---------------|   <---- Y-support
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//           minX      xx    smaxX

		// or
		
		//
		// smaxY |----|
		//       |    |   dx
		// yy    |    |--------|
		//       |    |        |
		//       |    |        | dy
		//       |    |        |
		// minY  |    |---------------|  <---- Y-support
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//          minX      xx    smaxX

		// using dx
		//
		// yy   |             |
		//      |             |
		//      |             |
		// minY |             *-------     <---- Y-support
		//      |                    
		//      |                    
		//      |--------------------------
		//                    xx   fmaxX
		
		if(source.getMaxX() >= xx) {
			XSupportPoint2D fixedPointY = (XSupportPoint2D)source;
			
			if(maxY >= source.getMinY()) {
				return new DefaultXYSupportPoint2D(xx, source.getMinY(), source.getMaxX(), maxY, fixedPointY.getXSupport(), p);
			}
		}
		return null;
	}
	
	protected void appendSwallowedAtXX(P placement, Point2D source, int xx, int yy) {
		//    swallowed:
		//
		//    y
		//    |
		//    |
		//    |---------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|--------------- x
		//              xx
		//    y
		//    |
		//    |
		//    |---------|
		//    |         |
		//    |         *
		//    |         | 
		//    |---------|--------------- x
		//              xx
		
		for (int i = 0; i < swallowed.size(); i++) {
			Point2D point = swallowed.get(i);
			// Move points swallowed by the placement
			if(point.getMaxX() >= xx) {
				// add point on the other side
				// vertical support
				DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
						xx, point.getMinY(),
						point.getMaxX(), point.getMaxY(),
						placement
						);

				addXX.add(next);
			} else {
				// delete current point (which was swallowed)
				deleted.add(point);
			}
		}
	}
	
	
	
	protected void appendSwallowedAtYY(P placement, Point2D source, int xx, int yy, boolean includeSource) {
		//    swallowed:
		//
		//    y
		//    |
		//    |
		// yy |-*-------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|--------------- x
		//
		//    y
		//    |
		//    |
		// yy |-*-------|
		//    |         |
		//    |         |
		//    |         | 
		//    |---------|--------------- x
		//
		//

		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			if(point == source && !includeSource) {
				continue;
			}

			// Move points swallowed by the placement
			if(point.swallowsMinY(source.getMinY(), yy) && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() >= yy) {
					// add point
					// horizontal support
					DefaultXSupportPoint2D next = new DefaultXSupportPoint2D(
							point.getMinX(), yy, 
							point.getMaxX(), point.getMaxY(), 
							placement
							);
					
					addYY.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}

		// removeIdenticalMaxAtYY(added);
	}
	
	protected void removeEclipsedYY() {
		Collections.sort(addYY, Point2D.X_COMPARATOR);

		for(int index = 0; index < addYY.size(); index++) {
			Point2D lowest = addYY.get(index);
			for (int i = index + 1; i < addYY.size(); i++) {
				Point2D p1 = addYY.get(i);

				if(lowest.eclipses(p1)) {
					addYY.remove(i);
					i--;
				}
			}
		}
	}

	protected void removeEclipsedXX() {
		Collections.sort(addXX, Point2D.Y_COMPARATOR);

		for(int index = 0; index < addXX.size(); index++) {
			Point2D lowest = addXX.get(index);
			for (int i = index + 1; i < addXX.size(); i++) {
				Point2D p1 = addXX.get(i);

				if(lowest.eclipses(p1)) {
					addXX.remove(i);
					i--;
				}
			}
		}
	}
	
	protected void removeEclipsed() {
		
		System.out.println("");
		System.out.println("Before eclipses");
		for(Point2D p : values) {
			System.out.println(" " + p);
		}
		
		for(int index = 0; index < values.size(); index++) {
			Point2D lowest = values.get(index);
			for (int i = index + 1; i < values.size(); i++) {
				Point2D p1 = values.get(i);

				if(lowest.eclipses(p1)) {
					System.out.println("Remove eclipsed " + p1 + " by " + lowest);
					values.remove(i);
					i--;
				}
			}
		}
	}

	protected void removeIdentialMaxAtXX() {
		// x coordinate fixed
		Collections.sort(addXX, Point2D.Y_COMPARATOR);
		
		removeIdenticalMax(addXX);
	}

	protected void removeIdenticalMax(List<Point2D> added) {
		// remove those points which have the same extreme points (they share one coordinate)
		for (int j = 0; j < added.size(); j++) {
			Point2D point2d = added.get(j);
			
			for (int i = j + 1; i < added.size(); i++) {
				Point2D p = added.get(i);

				if(point2d.getMaxX() == p.getMaxX() && point2d.getMaxY() == p.getMaxY()) {
					added.remove(i);
					i--;
				}
			}
		}
	}

	protected int constrainIfNotMaxY(Point2D source, int x) {
		if(isMax(source)) {
			return containerMaxY;
		}
		return constrainY(x, source.getMinY());
	}

	private boolean isMax(Point2D source) {
		return source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY;
	}

	protected void appendSwallowedOrShadowedAtYY(P placement, Point2D source, int xx, int yy, boolean includeSource) {
		// Move points swallowed or shadowed by the placement
		
		//    swallowed:
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|----
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    |         |
		//    |         | 
		//    |---------|-----	
		//
		//    shadowed:
		//
		// point maxY  |--------------------------
		//             |
		//             |
		//             |
		//          yy |---------------------|
		//             |                     |
		// source minY |---------|-----------|
		//             |         | 
		// point minY  |---------|----*----------
		//
		//
		//
		// point maxY  |--------------------------
		//             |
		//             |
		//             |
		//          yy |--------------*------|    <-- added
		//             |                     |
		// source minY |---------|-----------|
		//             |         | 
		// point minY  |---------|----*---------- <-- shadowed
		//
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			if(point == source && !includeSource) {
				continue;
			}

			// Move points swallowed or shadowed by the placement
			if(point.isShadowedOrSwallowedByY(source.getMinY(), yy) && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() >= yy) {
					// add point
					// horizontal support

					DefaultXSupportPoint2D next = new DefaultXSupportPoint2D(
							point.getMinX(), yy, 
							point.getMaxX(), point.getMaxY(), 
							placement
							);

					addYY.add(next);
				}
				
				if(point.getMinY() < source.getMinY()) {
					// constrain current point
					point.setMaxY(source.getMinY() - 1);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
	}

	private void raiseToYY(P placement, int xx, int yy, int maxX) {
		for (int i = 0; i < addYY.size(); i++) {
			Point2D point = addYY.get(i);					
			
			if(point.getMinY() < yy) {
				Point2D p;
				if(point.getMinX() < placement.getAbsoluteX()) {
					p = new DefaultPoint2D(point.getMinX(), yy, maxX, point.getMaxY());
				} else {
					p = new DefaultXSupportPoint2D(point.getMinX(), yy, maxX, point.getMaxY(), placement);
				}

				addYY.set(i, p);
			}
		}
	}
	
	protected DefaultXYSupportPoint2D getSupportedAtYY(Point2D source, Placement2D placement, int xx, int yy, int maxX) {
		// in other words there is a placement in negative x direction at YY
		// so the x coordinate is minX
		
		//
		// vmaxY |----|                      <-- x-support max y
		//       |    |          
		//  yy   |    *-------------------|
		//       |    |                   |
		//       |    |                   | 
		//       |    |                   |
		//  minY |    |--------------------  <-- x-support min y
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//           minX            maxX
		
		// or
		
		//
		// vmaxY |----|                      <-- x-support max y
		//       |    |   
		// yy    |    *--------|
		//       |    |        |
		//       |    |        | 
		//       |    |        |
		// minY  |    |---------------|      <-- x-support min y
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//          minX      xx    fmaxX
		
		// using dy
		//
		//
		// vmaxY |    |                      <-- x-support max y
		//       |    |   
		// yy    |    *---------
		//       |
		//       |
		//       |
		//       |
		//       |
		//       |
		//       |--------------------------
		//           minX      xx    		
		//
		
		if(source.getMaxY() >= yy) {
			YSupportPoint2D fixedPointX = (YSupportPoint2D)source;
			
			if(maxX >= source.getMinX()) {
				return new DefaultXYSupportPoint2D(source.getMinX(), yy, maxX, source.getMaxY(), placement, fixedPointX.getYSupport());
			}
		}
		
		return null;
	}

	protected void removeIdenticalMaxAtYY() {
		// y coordinate fixed
		Collections.sort(addYY, Point2D.X_COMPARATOR);

		removeIdenticalMax(addYY);
	}

	protected int constrainIfNotMaxX(Point2D source, int yy) {
		if(isMax(source)) {
			return containerMaxX;
		} 
		return constrainX(source.getMinX(), yy);
	}

	protected int constrainX(int x, int y) {
		// constrain up
		P closestX = closestPositiveX(x, y);
		if(closestX != null) {
			return closestX.getAbsoluteX() - 1;
		} else {
			return containerMaxX;
		}
	}

	protected int constrainY(int x, int y) {
		// constrain up
		P closestY = closestPositiveY(x, y);
		if(closestY != null) {
			return closestY.getAbsoluteY() - 1;
		} else {
			return containerMaxY;
		}
	}
	
	protected Point2D projectNegativeYAtXX(Point2D source, Placement2D placement, int xx, int yy, int maxY) {
		if(xx >= containerMaxX) {
			return null;
		}
		P moveY = projectNegativeY(xx, yy);
		if(moveY == null) {
			
			// supported one way (by container border)
			//
			//      |    |-------------------|
			//      |    |                   |
			//      |    |                   |
			//      |    |                   |
			// minY |    |--------------------
			//      |    |               |   |
			//      |    |               |   ↓
			//      |----|---------------|---*-----
			//          minX            maxX
			
			int x = xx;
			int y = 0;
			
			int maxX = constrainX(x, y);
			if(x <= maxX) {
				return new DefaultXSupportPoint2D(x, y, maxX, maxY, containerPlacement);
			}
		} else if(moveY.getAbsoluteEndY() < source.getMinY()) {
			
			// supported one way
			//
			//      |    |-------------------|
			//      |    |                   |
			//      |    |                   |
			//      |    |                   |
			// minY |    |--------------------
			//      |    |               |   ↓
			//      |    |               |   *--------|
			//      |    |               |   |        |
			//      |----|---------------|---|--------|--------
			//          minX            maxX
			int x = xx;
			int y = moveY.getAbsoluteEndY() + 1;
			
			int maxX = constrainX(x, y);
			if(x <= maxX) {
				return new DefaultXSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, maxX, maxY, moveY);
			}
		} else if(moveY.getAbsoluteEndY() + 1 < yy) {
			
			// supported both ways
			// 
			//      |    |-------------------|
			//      |    |                   |
			//      |    |                   |
			//      |    |                   |
			//      |    |                   ↓
			//      |    |                   *--------|
			//      |    |                   |        |
			// minY |    |-------------------|        |
			//      |    |               |   |        |
			//      |    |               |   |        |
			//      |    |               |   |        |
			//      |----|---------------|---|--------|--------
			//          minX            maxX

			int x = xx;
			int y = moveY.getAbsoluteEndY() + 1;
			
			int maxX = constrainX(x, y);
			if(x <= maxX) {
				return new DefaultXYSupportPoint2D(x, y, maxX, maxY, placement, moveY);
			}
		}
		
		// no space to move
		// 
		//      |    |-------------------*---------
		//      |    |                   |        |
		//      |    |                   |        | 
		//      |    |                   |        |
		//      |    |                   |        |
		//      |    |                   |        |
		//      |    |                   |        |
		// minY |    |-------------------|        |
		//      |    |               |   |        |
		//      |    |               |   |        |
		//      |    |               |   |        |
		//      |----|---------------|---|--------|--------
		//          minX            maxX
		
		return null;
	}

	protected Point2D projectNegativeXAtYY(Point2D source, Placement2D placement, int xx, int yy, int maxX) {
		if(yy >= containerMaxY) {
			return null;
		}
		P moveX = projectNegativeX(xx, yy);
		if(moveX == null) {
			
			// supported one way (by container border)
			//
			//       |  
			//       |  
			// yy    |←------------|
			//       |    |        |
			//       |    |        |
			//       |    |        |
			// fmaxY |----|        |
			//
			
			int x = 0;
			int y = yy;

			int maxY = constrainY(x, y);
			if(y <= maxY) {
				return new DefaultYSupportPoint2D(x, y, maxX, maxY, containerPlacement);
			}
		} else if(moveX.getAbsoluteEndX() < source.getMinX()) {
			
			// supported one way
			//
			// aendy |-|
			//       | |
			// yy    | |←----------|
			//       |    |        |
			//       |    |        |
			//       | |  |        |
			// fmaxY |----|        |
			//
			//       aendx
			
			int x = moveX.getAbsoluteEndX() + 1;
			int y = yy;

			int maxY = constrainY(x, y);
			if(y <= maxY) {
				return new DefaultYSupportPoint2D(x, y, maxX, maxY, moveX);
			}
		} else if(moveX.getAbsoluteEndX() + 1 < xx) {
			
			// supported both ways
			//
			//
			// aendy |-------|
			//       |       |
			//       |       |
			// yy    |    |--*←----|
			//       |    |        |
			//       |    |        |
			//       |    |        |
			// fmaxY |----|        |
			//
			//             aendx
			int x = moveX.getAbsoluteEndX() + 1;
			int y = yy;

			int maxY = constrainY(x, y);
			if(y <= maxY) {
				return new DefaultXYSupportPoint2D(moveX.getAbsoluteX() + 1, yy, maxX, maxY, moveX, placement);
			}
		}
		
		// no space to move
		//
		//
		// aendy |-------------|
		//       |             |
		//       |             |
		// yy    |    |--------*
		//       |    |        |
		//       |    |        |
		//       |    |        |
		// fmaxY |----|        |
		//
		//  
		
		return null;
	}

	protected boolean constrainFloatingMax(Point2D point, P placement) {

		if(placement.getAbsoluteEndX() < point.getMinX()) {
			return true;
		}

		if(placement.getAbsoluteEndY() < point.getMinY()) {
			return true;
		}
		
		if(placement.getAbsoluteX() > point.getMaxX()) {
			return true;
		}

		if(placement.getAbsoluteY() > point.getMaxY()) {
			return true;
		}	

		boolean x = placement.getAbsoluteX() > point.getMinX();
		boolean y = placement.getAbsoluteY() > point.getMinY();

		if(x) {
			Point2D clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY());
			addXX.add(clone);
			System.out.println("Add floating clone x " + clone);
		}
		
		if(y) {
			Point2D clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1);
			addYY.add(clone);
			System.out.println("Add floating clone y " + clone);
		}

		return !(x || y);
	}	
	
	protected boolean constrainPositiveMax(Point2D point, P placement) {
		if(placement.getAbsoluteX() >= point.getMinX()) {
			if(withinY(point.getMinY(), placement)) {
				int limit = placement.getAbsoluteX() - 1;
				if(limit < point.getMinX()) {
					return false;
				}
				if(point.getMaxX() > limit) {
					point.setMaxX(limit);
				}
			}
		}
		
		if(placement.getAbsoluteY() >= point.getMinY()) {
			if(withinX(point.getMinX(), placement)) {
				int limit = placement.getAbsoluteY() - 1;
				if(limit < point.getMinY()) {
					return false;
				}
				if(point.getMaxY() > limit) {
					point.setMaxY(limit);
				}
			}
		}
		
		return true;
	}	
	
	protected P projectNegativeX(int x, int y) {
		
		// excluded:
		//
		//         |
		// absEndy-|-----|
		//         |     |
		//         |     |
		//         |-----|
		//         |
		//         |        *
		//         |                
		//         |--------------------------
		//               |
		//            absEndX
		//
		//         |
		// absEndy-|-----------|
		//         |           |
		//         |           |
		//   absY  |-----------|
		//         |
		//         |        *
		//         |                
		//         |--------------------------
		//         |           |
		//        absX       absEndX
		//
		//
		// included:
		//
		//         |
		//         |
		//         |
		// absEndy-|-----|
		//         |     |
		//         |     |  *
		//         |     |          
		//         |-----|--------------------
		//               |
		//            absEndX
		
		P rightmost = null;
		for (P placement : placements) {
			if(placement.getAbsoluteEndX() <= x && withinY(y, placement)) {
				// most to the right
				if(rightmost == null || placement.getAbsoluteEndX() > rightmost.getAbsoluteEndX()) {
					rightmost = placement;
				}
			}
		}
		
		return rightmost;
	}	

	protected P projectNegativeY(int x, int y) {

		// excluded:
		//
		// |
		// |
		// |                    |-----| absEndY
		// |                 *  |     |
		// |                    |     |
		// |                    |     |
		// |--------------------|-----|  absY
		//                      |     |
		//                    absX absEndX
		//
		// |
		// |                  
		// |                 *
		// |                    |-----| absEndY
		// |                    |     |
		// |--------------------|-----|- absY
		//                      |     |
		//                    absX absEndX
		//
		// included:
		//
		// |                  
		// |                 *
		// |              |------------| absEndY
		// |              |            |
		// |--------------|------------|  absY
		//                |            |
		//               absX       absEndX
		//
		
		// TODO what if one or more placements start and begin within minY and maxY?
		
		P mostPositive = null;
		for (P placement : placements) {
			if(placement.getAbsoluteEndY() <= y && withinX(x, placement)) {
				
				// the highest
				if(mostPositive == null || placement.getAbsoluteEndY() > mostPositive.getAbsoluteEndY()) {
					mostPositive = placement;
				}
			}
		}
		
		return mostPositive;
	}
	
	protected P closestPositiveY(int x, int y) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteY() >= y) {
				if(withinX(x, placement)) {
					if(closest == null || placement.getAbsoluteY() < closest.getAbsoluteY() || (placement.getAbsoluteY() == closest.getAbsoluteY() && placement.getAbsoluteX() < closest.getAbsoluteX())) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
	}

	protected P closestPositiveX(int x, int y) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteX() >= x) {
				if(withinY(y, placement)) {
					if(closest == null || placement.getAbsoluteX() < closest.getAbsoluteX() || (placement.getAbsoluteX() == closest.getAbsoluteX() && placement.getAbsoluteY() < closest.getAbsoluteY())) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
	}
	
	protected boolean withinX(int x, P placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}	

	protected boolean withinY(int y, P placement) {
		return placement.getAbsoluteY() <= y && y <= placement.getAbsoluteEndY();
	}
	
	public int getDepth() {
		return containerMaxY;
	}
	
	public int getWidth() {
		return containerMaxX;
	}

	@Override
	public String toString() {
		return "ExtremePoints2D [width=" + containerMaxX + ", depth=" + containerMaxY + ", values=" + values + "]";
	}
	
	public List<P> getPlacements() {
		return placements;
	}

	public Point2D getValue(int i) {
		return values.get(i);
	}
	
	public List<Point2D> getValues() {
		return values;
	}
	
	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(point.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}	

	public long getMaxArea() {
		long maxPointArea = -1L;
		for (Point2D point2d : values) {
			if(maxPointArea < point2d.getArea()) {
				maxPointArea = point2d.getArea(); 
			}
		}
		return maxPointArea;
	}

	public void redo() {
		values.clear();
		placements.clear();
		
		addFirstPoint();
	}

	@Override
	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy);
		
		redo();
	}
	
	public int findPoint(int x, int y) {
		for(int i = 0; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			if(point2d.getMinX() == x && point2d.getMinY() == y) {
				return i;
			}
		}
		return -1;
	}
}
