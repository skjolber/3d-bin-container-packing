package com.github.skjolber.packing.points2d;

import java.awt.Point;
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

public class ExtremePoints2D<P extends Placement2D> implements ExtremePoints<P, Point2D> {
	
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

	public ExtremePoints2D(int dx, int dy) {
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

		} else {
			System.out.println("Y support");
		}
		
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
			
		} else {
			System.out.println("X support");
		}
		
		
		System.out.println("Got " + moveToYY.size() + " shadowed below");

		negativeMoveToYY.addAll(moveToYY);
		negativeMoveToYY.addAll(swallowed);
		Collections.sort(negativeMoveToYY, Point2D.COMPARATOR_X_THEN_Y);
		
		negativeMoveToXX.addAll(moveToXX);
		negativeMoveToXX.addAll(swallowed);
		Collections.sort(negativeMoveToXX, Point2D.COMPARATOR_Y_THEN_X);
		
		deleted.addAll(swallowed);
		
		if(!negativeMoveToYY.isEmpty()) {
			Point2D previousYY = null;

			for(Point2D p : negativeMoveToYY) {
				// add point on the other side
				// with x support
				if(p.getMaxY() >= yy) {
					
					if(previousYY == null || previousYY.getMaxY() != p.getMaxY()) {
						boolean split = maxYForMaxX < p.getMaxY() && p.getMaxX() < maxX;
						if(p.getMinX() < placement.getAbsoluteX()) {
							if(split) {
								Point2D moveY1 = p.moveY(p.getMinX(), yy, maxX, maxYForMaxX);
								addYY.add(moveY1);

								Point2D moveY2 = p.moveY(p.getMinX(), yy, p.getMaxX(), p.getMaxY());
								addYY.add(moveY2);

							} else {
								Point2D moveY = p.moveY(p.getMinX(), yy, maxX, p.getMaxY());
								addYY.add(moveY);
							}
						} else {
							if(split) {
								Point2D moveY1 = p.moveY(p.getMinX(), yy, maxX, maxYForMaxX, placement);
								addYY.add(moveY1);

								Point2D moveY2 = p.moveY(p.getMinX(), yy, p.getMaxX(), p.getMaxY(), placement);
								addYY.add(moveY2);

							} else {
								Point2D moveY = p.moveY(p.getMinX(), yy, maxX, p.getMaxY(), placement);
								addYY.add(moveY);
							}
						}
						

						previousYY = p;
					}
				}
			}			
		}

		if(!negativeMoveToXX.isEmpty()) {
			Point2D previousXX = null;
			
			for (Point2D p : negativeMoveToXX) {
				if(p.getMaxX() >= xx) {
					if(previousXX == null || previousXX.getMaxX() != p.getMaxX()) {
						boolean split = maxXForMaxY < p.getMaxX() && p.getMaxY() < maxY;
						
						if(p.getMinY() < placement.getAbsoluteY()) {
							if(split) {
								Point2D moveX1 = p.moveX(xx, p.getMinY(), maxXForMaxY, maxY);
								addXX.add(moveX1);

								Point2D moveX2 = p.moveX(xx, p.getMinY(), p.getMaxX(), p.getMaxY());
								addXX.add(moveX2);
							} else {
								Point2D moveX = p.moveX(xx, p.getMinY(), p.getMaxX(), maxY);
								addXX.add(moveX);
								
							}
						} else {
							if(split) {
								Point2D moveX1 = p.moveX(xx, p.getMinY(), maxXForMaxY, maxY, placement);
								addXX.add(moveX1);

								Point2D moveX2 = p.moveX(xx, p.getMinY(), p.getMaxX(), p.getMaxY(), placement);
								addXX.add(moveX2);
							} else {
								Point2D moveX = p.moveX(xx, p.getMinY(), p.getMaxX(), maxY, placement);	
								addXX.add(moveX);
							}
						}

						previousXX = p;
					}
				}
			}
		}
		
		// project swallowed or shadowed to yy
		if(!moveToYY.isEmpty()) {
			// shadowedX
			for(Point2D point : moveToYY) {
				if(point.getMinY() < placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);
				} else {
					deleted.add(point);
				}
			}
		}
		
		if(!moveToXX.isEmpty()) {
			for (Point2D point : moveToXX) {
				if(point.getMinX() < placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);
				} else {
					deleted.add(point);
				}
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
	
	protected boolean withinX(int x, P placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}	

	protected boolean withinY(int y, P placement) {
		return placement.getAbsoluteY() <= y && y <= placement.getAbsoluteEndY();
	}
	
	public int getDepth() {
		return containerMaxY + 1;
	}
	
	public int getWidth() {
		return containerMaxX + 1;
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
