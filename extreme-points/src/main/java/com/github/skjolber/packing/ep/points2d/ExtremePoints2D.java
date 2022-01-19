package com.github.skjolber.packing.ep.points2d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point2D;

/**
 * 
 * Implementation of so-called extreme points in 2D.
 *
 */

public class ExtremePoints2D<P extends Placement2D> implements ExtremePoints<P, Point2D<P>> {
	
	public static final Comparator<Point2D<?>> COMPARATOR_X = new Comparator<Point2D<?>>() {
		
		@Override
		public int compare(Point2D<?> o1, Point2D<?> o2) {
			return Integer.compare(o1.getMinX(), o2.getMinX());
		}
	};
	
	protected int containerMaxX;
	protected int containerMaxY;
	
	protected final Point2DFlagList<P> values = new Point2DFlagList<>();
	protected final List<P> placements = new ArrayList<>();

	// reuse working variables
	protected final Point2DList<P> addXX = new Point2DList<>();
	protected final Point2DList<P> addYY = new Point2DList<>();

	protected final IntArrayList swallowed = new IntArrayList();
	protected final IntArrayList moveToXX = new IntArrayList();
	protected final IntArrayList moveToYY = new IntArrayList();

	protected final IntArrayList negativeMoveToYY = new IntArrayList();
	protected final IntArrayList negativeMoveToXX = new IntArrayList();

	protected P containerPlacement;

	private long minArea = 0;

	private IntComparator COMPARATOR_X_THEN_Y = (a, b) -> {
		return Point2D.COMPARATOR_X_THEN_Y.compare(values.get(a), values.get(b));
	};

	private IntComparator COMPARATOR_Y_THEN_X = (a, b) -> {
		return Point2D.COMPARATOR_Y_THEN_X.compare(values.get(a), values.get(b));
	};

	public ExtremePoints2D(int dx, int dy) {
		setSize(dx, dy);
		addFirstPoint();
	}
	
	private void setSize(int dx, int dy) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;

		this.containerPlacement = (P) new DefaultPlacement2D(0, 0, containerMaxX, containerMaxY);
	}

	private void addFirstPoint() {
		values.add(new DefaultXYSupportPoint2D<P>(0, 0, containerMaxX, containerMaxY, containerPlacement, containerPlacement));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {

		// overall approach:
		// Do not iterate over placements to find point max / mins, rather
		// project existing points. 
		//  
		// project points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side
		// add points shadowed by the two new points (if they could be moved in the negative direction)
		// remove points which are eclipsed by others
		
		// keep track of placement borders, where possible
		Point2D<P> source = values.get(index);
		
		boolean xSupport = source.isXSupport(source.getMinX());
		boolean ySupport = source.isYSupport(source.getMinY());
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		
		values.flag(index);
		
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
		
		boolean xxSupport = xSupport && source.isXSupport(xx); // i.e. is source minY also minY at XX?

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
		
		boolean yySupport = ySupport && source.isYSupport(yy); // i.e. is source minX also minX at YY?

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
		
		// determine start and end index based on previous sort (in x direction)
		
		int pointIndex;
		if(yySupport) {
			pointIndex = binarySearchMinusMinX(placement.getAbsoluteX());
		} else {
			pointIndex = 0;
		}
		int endIndex = binarySearchPlusMinX(placement.getAbsoluteEndX());
		
		for(int i = pointIndex; i < endIndex; i++) {
			Point2D<P> point = values.get(i);
			
			boolean swallowsMinX = point.swallowsMinX(placement.getAbsoluteX(), placement.getAbsoluteEndX());
			boolean swallowsMinY = point.swallowsMinY(placement.getAbsoluteY(), placement.getAbsoluteEndY());
			
			if(swallowsMinX && swallowsMinY) { // b
				swallowed.add(i);
				
				values.flag(i);
			} else if(swallowsMinX && !xxSupport && point.getMinY() < placement.getAbsoluteY() && placement.getAbsoluteY() <= point.getMaxY()) { // c
				// shadowed (at negative) y
				moveToYY.add(i);
			} else if(swallowsMinY && !yySupport && point.getMinX() < placement.getAbsoluteX() &&  placement.getAbsoluteX() <= point.getMaxX()) { // a
				// shadowed (at negative) x
				moveToXX.add(i);
			} else {
				// TODO if floating, add to own list as an optimization, 
			}
		}
		
		// project swallowed or shadowed to xx
		if(!yySupport) {

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
				for(int i = 0; i < values.size(); i++) {
					Point2D<P> point = values.get(i);					
					if(point.getMinX() < source.getMinX() && point.getMinY() <= yy && yy <= point.getMaxY() ) {
						negativeMoveToYY.add(i);
					}
				}
			} else {
				for(int i = 0; i < values.size(); i++) {
					Point2D<P> point = values.get(i);
					if(projectNegativeX.getAbsoluteEndX() < point.getMinX() && point.getMinX() < source.getMinX() && point.getMinY() <= yy && yy <= point.getMaxY() ) {
						negativeMoveToYY.add(i);
					}
				}
			}
		}
		
		if(!xxSupport) {
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
				for(int i = 0; i < values.size(); i++) {
					Point2D<P> point = values.get(i);
					if(point.getMinY() < source.getMinY() && point.getMinX() <= xx && xx <= point.getMaxX() ) {
						negativeMoveToXX.add(i);
					}
				}
			} else {
				for(int i = 0; i < values.size(); i++) {
					Point2D<P> point = values.get(i);
					if(projectNegativeY.getAbsoluteEndY() < point.getMinY() && point.getMinY() < source.getMinY() && point.getMinX() <= xx && xx <= point.getMaxX() ) {
						negativeMoveToXX.add(i);
					}
				}
			}
		}
		
		negativeMoveToYY.addAll(moveToYY);
		negativeMoveToYY.addAll(swallowed);
		negativeMoveToYY.sortThis(COMPARATOR_X_THEN_Y);
		
		negativeMoveToXX.addAll(moveToXX);
		negativeMoveToXX.addAll(swallowed);
		negativeMoveToXX.sortThis(COMPARATOR_Y_THEN_X);
		
		if(!negativeMoveToYY.isEmpty()) {
			
			add:
			for(int i = 0; i < negativeMoveToYY.size(); i++) {
				Point2D<P> p = values.get(negativeMoveToYY.get(i));
				// add point on the other side
				// with x support
				if(p.getMaxY() >= yy) {
					for(int k = 0; k < addYY.size(); k++) {
						Point2D<P> add = addYY.get(k);
						if(add.eclipsesMovedY(p, yy)) {
							continue add;
						}
					}
					// intentionally do not check if
					// p.getMinX() < placement.getAbsoluteX()
					// nor
					// p.getMaxX() < placement.getAbsoluteX()
					
					addYY.add(p.moveY(yy, p.getMaxX(), p.getMaxY(), placement));
				}
			}			
		}

		if(!negativeMoveToXX.isEmpty()) {
			addXX.ensureCapacity(negativeMoveToXX.size());

			add:
			for(int i = 0; i < negativeMoveToXX.size(); i++) {
				Point2D<P> p = values.get(negativeMoveToXX.get(i));
				// add point on the other side
				// with x support
				if(p.getMaxX() >= xx) {
					for(int k = 0; k < addXX.size(); k++) {
						Point2D<P> add = addXX.get(k);
						if(add.eclipsesMovedX(p, xx)) {
							continue add;
						}
					}
					// intentionally do not check if
					// p.getMinY() < placement.getAbsoluteY()
					// nor
					// p.getMaxY() < placement.getAbsoluteY()
					addXX.add(p.moveX(xx, p.getMaxX(), p.getMaxY(), placement));
				}
			}			
		}
		
		// project swallowed or shadowed to yy
		if(!moveToYY.isEmpty()) {
			// shadowedX
			for(int i = 0; i < moveToYY.size(); i++) {
				int slot = moveToYY.get(i);
				Point2D<P> point = values.get(slot);
				if(!constraintPositiveMaxY(point, placement)) {
					values.flag(slot);
				}
			}
		}
		
		if(!moveToXX.isEmpty()) {
			for(int i = 0; i < moveToXX.size(); i++) {
				int slot = moveToXX.get(i);
				Point2D<P> point = values.get(slot);
				if(!constrainPositiveMaxX(point, placement)) {
					values.flag(slot);
				}
			}
		}

		if(!(xSupport || ySupport)) {
			
			if(!yySupport) {
				// search has not been performed yet
				pointIndex = binarySearchMinusMinX(placement.getAbsoluteX());
			}
			
			for(int i = 0; i <= pointIndex; i++) {
				Point2D<P> point2d = values.get(i);
		
				if(!constrainFloatingMax(point2d, placement)) {
					values.flag(i);
				}
			}
		}
		
		values.compress();
		
		placements.add(placement);
		
		values.ensureCapacity(values.size() + addXX.size() + addYY.size());
		
		for (int i = 0; i < addXX.size(); i++) {
			Point2D<P> p = addXX.get(i);
			
			if(p.getArea() >= minArea) {
				values.add(p);
			}
		}
		for (int i = 0; i < addYY.size(); i++) {
			Point2D<P> p = addYY.get(i);
			
			if(p.getArea() >= minArea) {
				values.add(p);
			}
		}
		
		values.sort(Point2D.COMPARATOR_Y_THEN_X);
		removeEclipsed(binarySearchPlusMinY(placement.getAbsoluteEndY()));
		values.compress();

		values.sort(Point2D.COMPARATOR_X_THEN_Y);
		removeEclipsed(binarySearchPlusMinX(placement.getAbsoluteEndX()));
		values.compress();

		swallowed.clear();
		moveToXX.clear();
		moveToYY.clear();
		
		negativeMoveToYY.clear();
		negativeMoveToXX.clear();
		
		addXX.clear();
		addYY.clear();

		return !values.isEmpty();
	}
	
	protected void removeEclipsed(int limit) {
		for(int index = 0; index < limit; index++) {
			Point2D<P> lowest = values.get(index);
			for (int i = index + 1; i < limit; i++) {
				Point2D<P> p1 = values.get(i);

				if(lowest.eclipses(p1)) {
					values.flag(i);
				}
			}
		}
	}

	protected boolean constrainFloatingMax(Point2D<P> point, P placement) {

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
			Point2D<P> clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY());
			addXX.add(clone);
		}
		
		if(y) {
			Point2D<P> clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1);
			addYY.add(clone);
		}

		return !(x || y);
	}	

	private boolean constraintPositiveMaxY(Point2D<P> point, P placement) {
		int limit = placement.getAbsoluteY() - 1;
		if(limit < point.getMinY()) {
			return false;
		}
		if(point.getMaxY() > limit) {
			point.setMaxY(limit);
		}
		if(point.getArea() < minArea) {
			return false;
		}
		return true;
	}

	private boolean constrainPositiveMaxX(Point2D<P> point, P placement) {
		int limit = placement.getAbsoluteX() - 1;
		if(limit < point.getMinX()) {
			return false;
		}
		if(point.getMaxX() > limit) {
			point.setMaxX(limit);
		}
		if(point.getArea() < minArea) {
			return false;
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

	public Point2D<P> getValue(int i) {
		return values.get(i);
	}
	
	public List<Point2D<P>> getValues() {
		return values.toList();
	}
	
	@Override
	public int getValueCount() {
		return values.size();
	}

	
	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D<P> point = values.get(i);
			
			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D<P> point = values.get(i);
			
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
		for(int i = 0; i < values.size(); i++) {
			Point2D<P> point = values.get(i);
			if(maxPointArea < point.getArea()) {
				maxPointArea = point.getArea(); 
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
			Point2D<P> point2d = values.get(i);
			if(point2d.getMinX() == x && point2d.getMinY() == y) {
				return i;
			}
		}
		return -1;
	}
	
	
    public int binarySearchPlusMinY(int key) {
    	// return exclusive result
    	
        int low = 0;
        int high = values.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
		
	     	// 0 if x == y
	     	// -1 if x < y
	     	// 1 if x > y
		
            int midVal = values.get(mid).getMinY(); 
           
            int cmp = Integer.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
            	// key found
            	do {
            		mid++;
            	} while(mid < values.size() && values.get(mid).getMinY() == key);
            	
                return mid; 
            }
        }
        // key not found
        return low;  
    }
    
    public int binarySearchPlusMinX(int key) {
    	// return exclusive result
    	
        int low = 0;
        int high = values.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
		
	     	// 0 if x == y
	     	// -1 if x < y
	     	// 1 if x > y
		
            int midVal = values.get(mid).getMinX(); 
           
            int cmp = Integer.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
            	// key found
            	do {
            		mid++;
            	} while(mid < values.size() && values.get(mid).getMinX() == key);
            	
                return mid; 
            }
        }
        // key not found
        return low;  
    }

    public int binarySearchMinusMinX(int key) {
    	// return inclusive result
    	
        int low = 0;
        int high = values.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
		
	     	// 0 if x == y
	     	// -1 if x < y
	     	// 1 if x > y
		
            int midVal = values.get(mid).getMinX(); 
           
            int cmp = Integer.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
            	// key found
            	while(mid > 0 && values.get(mid - 1).getMinX() == key) {
            		mid--;
            	}
            	
                return mid; 
            }
        }
        // key not found
        return low;
    }

	public void setMinArea(long minArea) {
		this.minArea = minArea;
	}

}
