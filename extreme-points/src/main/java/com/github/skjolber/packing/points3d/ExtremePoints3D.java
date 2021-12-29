package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.ExtremePoints;
import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.Point3D;

/**
 * 
 * Implementation of so-called extreme points in 2D.
 *
 */

public class ExtremePoints3D<P extends Placement3D> implements ExtremePoints<P, Point3D<P>> {
	
	public static final Comparator<Point3D<?>> COMPARATOR_X = new Comparator<Point3D<?>>() {
		
		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			return Integer.compare(o1.getMinX(), o2.getMinX());
		}
	};
	
	protected int containerMaxX;
	protected int containerMaxY;
	protected int containerMaxZ;
	
	protected final List<Point3D<P>> values = new ArrayList<>();
	protected final List<P> placements = new ArrayList<>();

	// reuse working variables
	protected final List<Point3D<P>> addXX = new ArrayList<>();
	protected final List<Point3D<P>> addYY = new ArrayList<>();
	protected List<Point3D<P>> addedZ = new ArrayList<>();

	protected final List<Point3D<P>> swallowed = new ArrayList<>();
	protected final List<Point3D<P>> moveToXX = new ArrayList<>();
	protected final List<Point3D<P>> moveToYY = new ArrayList<>();
	protected final List<Point3D<P>> moveToZZ = new ArrayList<>();

	protected final List<Point3D<P>> negativeMoveToXX = new ArrayList<>();
	protected final List<Point3D<P>> negativeMoveToYY = new ArrayList<>();
	protected final List<Point3D<P>> negativeMoveToZZ = new ArrayList<>();

	protected final boolean cloneOnConstrain;

	protected final List<Point3D<P>> deleted = new ArrayList<>();

	protected P containerPlacement;

	public ExtremePoints3D(int dx, int dy, int dz) {
		this(dx, dy, dz, false);
	}
	
	public ExtremePoints3D(int dx, int dy, int dz, boolean cloneOnConstrain) {
		setSize(dx, dy, dz);
		this.cloneOnConstrain = cloneOnConstrain;
		addFirstPoint();
	}
	
	private void setSize(int dx, int dy, int dz) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;

		this.containerPlacement = (P) new DefaultPlacement3D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ);
	}

	protected void addFirstPoint() {
		values.add(new Default3DPlanePoint3D(
				0, 0, 0, 
				containerMaxX, containerMaxY, containerMaxZ, 
				containerPlacement,
				containerPlacement,
				containerPlacement
				));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1, placement.getAbsoluteEndZ() - placement.getAbsoluteZ() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy, int boxDz) {	

		// overall approach:
		// Do not iterate over placements to find point max / mins, rather
		// project existing points. 
		//  
		// project points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side
		// add points shadowed by the two new points (if they could be moved in the negative direction)
		// remove points which are eclipsed by others
		
		// keep track of placement borders, where possible
		Point3D<P> source = values.get(index);
		
		boolean hasXYSupport = source instanceof XYPlanePoint3D;
		boolean hasXZSupport = source instanceof XZPlanePoint3D;
		boolean hasYZSupport = source instanceof YZPlanePoint3D;

		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		int zz = source.getMinZ() + boxDz;
		
		boolean supportedXYPlane = source.isSupportedXYPlane(xx, yy);
		boolean supportedXZPlane = source.isSupportedXYPlane(xx, zz);
		boolean supportedYZPlane = source.isSupportedXYPlane(yy, zz);
		/*
		boolean xSupporteXYPlaneXX = source.isSupportedXYPlane(xx, source.getMinY());
		boolean xSupporteXYPlaneYY = source.isSupportedXYPlane(source.getMinX(), yy);
		
		boolean supporteXZPlaneXX = source.isSupportedXZPlane(xx, source.getMinZ());
		boolean supporteXZPlaneYY = source.isSupportedXZPlane(source.getMinX(), zz);
		
		boolean supporteYZPlaneYY = source.isSupportedYZPlane(yy, source.getMinZ());
		boolean supporteYZPlaneZZ = source.isSupportedYZPlane(source.getMinY(), zz);
		 */
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
		//
		// determine start and end index based on previous sort (in x direction)
		//
		
		for(int i = 0; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			
			boolean lessThanXX = point.getMinX() < xx;
			boolean lessThanYY = point.getMinY() < yy;
			boolean lessThanZZ = point.getMinZ() < zz;
			
			if(!lessThanXX && !lessThanYY && !lessThanZZ) {
				continue;
			}
			boolean lessThanX = point.getMinX() < source.getMinX();
			boolean lessThanY = point.getMinY() < source.getMinY();
			boolean lessThanZ = point.getMinZ() < source.getMinZ();
				
			if(!lessThanX && !lessThanY && !lessThanZ) {
				swallowed.add(point);
				
				continue;
			}
			
			if(supportedXYPlane && supportedXZPlane && supportedYZPlane) {
				continue;
			}
			
			boolean maxMoreThanX = point.getMaxX() >= source.getMinX();
			boolean maxMoreThanY = point.getMaxY() >= source.getMinY();
			boolean maxMoreThanZ = point.getMaxZ() >= source.getMinZ();
			
			if(!maxMoreThanX || !maxMoreThanY || !maxMoreThanZ) {
				// does not touch the new point
				// shadowed point?
				if(point.getMaxX() >= xx) {
					negativeMoveToXX.add(point);
				}
				if(point.getMaxY() >= yy) {
					negativeMoveToYY.add(point);
				}
				if(point.getMaxZ() >= zz) {
					negativeMoveToZZ.add(point);
				}
				continue;
			}
			
			if(point.getMaxX() >= xx) {
				moveToXX.add(point);
			}
			if(point.getMaxY() >= yy) {
				moveToYY.add(point);
			}
			if(point.getMaxZ() >= zz) {
				moveToZZ.add(point);
			}

			/*
			if(!hasXYSupport && !hasXZSupport && !supportedYZPlane) {
				moveToXX.add(point);
				moveToYY.add(point);
				moveToZZ.add(point);
			} else if(!hasXZSupport && !supportedYZPlane) {
				// xy plane support
				
				moveToXX.add(point);
				moveToYY.add(point);
				
			} else if(!hasXYSupport && !supportedYZPlane) {
				// xz plane support
				
				moveToXX.add(point);
				moveToZZ.add(point);

			} else if(!hasXYSupport && !hasXZSupport) {
				// yz plane support

				moveToYY.add(point);
				moveToZZ.add(point);

			} else if(!hasXYSupport) {
				// xz + yz
				moveToZZ.add(point);
				
			} else if(!hasXZSupport) {
				// xy + yz
				moveToYY.add(point);
				
			} else if(!supportedYZPlane) {
				// xz + xy
				moveToXX.add(point);
			
			}
			*/
		}
		
		// project swallowed or shadowed to xx
		if(!supportedXYPlane) {
			
			// extend XY plane to zero 
			
			Placement2D projectNegativeX = projectNegativeX(source.getMinX(), yy);
			if(projectNegativeX == null) {
				for(Point3D<P> point : values) {
					if(point.getMinX() < source.getMinX() && point.getMinY() <= yy && yy <= point.getMaxY() ) {
						negativeMoveToYY.add(point);
					}
				}
				
			} else {
				for(Point3D<P> point : values) {
					if(projectNegativeX.getAbsoluteEndX() < point.getMinX() && point.getMinX() < source.getMinX() && point.getMinY() <= yy && yy <= point.getMaxY() ) {
						negativeMoveToYY.add(point);
					}
				}
			}
		}
		
		if(!xSupport) {
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
				for(Point3D<P> point : values) {
					if(point.getMinY() < source.getMinY() && point.getMinX() <= xx && xx <= point.getMaxX() ) {
						negativeMoveToXX.add(point);
					}
				}
			} else {
				for(Point3D<P> point : values) {
					if(projectNegativeY.getAbsoluteEndY() < point.getMinY() && point.getMinY() < source.getMinY() && point.getMinX() <= xx && xx <= point.getMaxX() ) {
						negativeMoveToXX.add(point);
					}
				}
			}
		}
		
		negativeMoveToYY.addAll(moveToYY);
		negativeMoveToYY.addAll(swallowed);
		Collections.sort(negativeMoveToYY, Point3D.COMPARATOR_X_THEN_Y);
		
		negativeMoveToXX.addAll(moveToXX);
		negativeMoveToXX.addAll(swallowed);
		Collections.sort(negativeMoveToXX, Point3D.COMPARATOR_Y_THEN_X);
		
		deleted.addAll(swallowed);
		
		if(!negativeMoveToYY.isEmpty()) {
			
			int maxX = -1;
			int maxYForMaxX = -1;
			
			for(Point3D<P> point : negativeMoveToYY) {
				if(point.getMaxY() >= yy) {
					if(point.getMaxX() < maxX) {
						continue;
					}
					if(point.getMaxX() != maxX || point.getMaxY() > maxYForMaxX) {
						maxX = point.getMaxX();
						maxYForMaxX = point.getMaxY();
					}
				}
			}
			
			Point3D<P> previousYY = null;

			for(Point3D<P> p : negativeMoveToYY) {
				// add point on the other side
				// with x support
				if(p.getMaxY() >= yy) {
					
					// TODO if not floating elements yet, this could be simplified
					if(previousYY == null || previousYY.getMaxY() != p.getMaxY()) {
						boolean split = maxYForMaxX < p.getMaxY() && p.getMaxX() < maxX;
						if(p.getMinX() < placement.getAbsoluteX()) {
							if(split) {
								Point3D<P> moveY1 = p.moveY(yy, maxX, maxYForMaxX);
								addYY.add(moveY1);

								Point3D<P> moveY2 = p.moveY(yy, p.getMaxX(), p.getMaxY());
								addYY.add(moveY2);

							} else {
								Point3D<P> moveY = p.moveY(yy, maxX, p.getMaxY());
								addYY.add(moveY);
							}
						} else {
							if(split) {
								Point3D<P> moveY1 = p.moveY(yy, maxX, maxYForMaxX, placement);
								addYY.add(moveY1);

								Point3D<P> moveY2 = p.moveY(yy, p.getMaxX(), p.getMaxY(), placement);
								addYY.add(moveY2);

							} else {
								Point3D<P> moveY = p.moveY(yy, maxX, p.getMaxY(), placement);
								addYY.add(moveY);
							}
						}
						previousYY = p;
					}
				}
			}			
		}

		if(!negativeMoveToXX.isEmpty()) {
			int maxY = -1;
			int maxXForMaxY = -1;

			for (Point3D<P> point : negativeMoveToXX) {
				if(point.getMaxX() >= xx) {
					if(point.getMaxY() < maxY) {
						continue;
					}
					if(point.getMaxY() != maxY || point.getMaxX() > maxXForMaxY) {
						maxY = point.getMaxY();
						maxXForMaxY = point.getMaxX();
					}
				}
			}

			Point3D<P> previousXX = null;
			for (Point3D<P> p : negativeMoveToXX) {
				if(p.getMaxX() >= xx) {
					if(previousXX == null || previousXX.getMaxX() != p.getMaxX()) {
						boolean split = maxXForMaxY < p.getMaxX() && p.getMaxY() < maxY;
						
						if(p.getMinY() < placement.getAbsoluteY()) {
							if(split) {
								Point3D<P> moveX1 = p.moveX(xx, maxXForMaxY, maxY);
								addXX.add(moveX1);

								Point3D<P> moveX2 = p.moveX(xx, p.getMaxX(), p.getMaxY());
								addXX.add(moveX2);
							} else {
								Point3D<P> moveX = p.moveX(xx, p.getMaxX(), maxY);
								addXX.add(moveX);
							}
						} else {
							if(split) {
								Point3D<P> moveX1 = p.moveX(xx, maxXForMaxY, maxY, placement);
								addXX.add(moveX1);

								Point3D<P> moveX2 = p.moveX(xx, p.getMaxX(), p.getMaxY(), placement);
								addXX.add(moveX2);
							} else {
								Point3D<P> moveX = p.moveX(xx, p.getMaxX(), maxY, placement);	
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
			for(Point3D<P> point : moveToYY) {
				if(point.getMinY() < placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);
				} else {
					deleted.add(point);
				}
			}
		}
		
		if(!moveToXX.isEmpty()) {
			for (Point3D<P> point : moveToXX) {
				if(point.getMinX() < placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);
				} else {
					deleted.add(point);
				}
			}
		}

		for(Point3D<P> point : moveToYY) {
			if(!constraintPositiveMaxY(point, placement)) {
				deleted.add(point);
			}
		}
		for(Point3D<P> point : moveToXX) {
			if(!constrainPositiveMaxX(point, placement)) {
				deleted.add(point);
			}
		}
		if(!hasSupport) {
			
			if(!ySupport) {
				// search has not been performed yet
				pointIndex = binarySearchMinusMinX(placement.getAbsoluteX());
			}
			
			for(int i = 0; i <= pointIndex; i++) {
				Point3D<P> Point3D = values.get(i);
		
				if(!constrainFloatingMax(Point3D, placement)) {
					deleted.add(Point3D);
				}
			}
		}
		
		values.removeAll(deleted);
		
		placements.add(placement);
		
		values.addAll(addXX);
		values.addAll(addYY);

		Collections.sort(values, Point3D.COMPARATOR_Y_THEN_X);
		removeEclipsed(binarySearchPlusMinY(placement.getAbsoluteEndY()));

		Collections.sort(values, Point3D.COMPARATOR_X_THEN_Y);
		removeEclipsed(binarySearchPlusMinX(placement.getAbsoluteEndX()));

		swallowed.clear();
		moveToXX.clear();
		moveToYY.clear();
		
		negativeMoveToYY.clear();
		negativeMoveToXX.clear();
		
		addXX.clear();
		addYY.clear();
		
		deleted.clear();

		return !values.isEmpty();
	}
	
	protected void removeEclipsed(int limit) {
		for(int index = 0; index < limit; index++) {
			Point3D<P> lowest = values.get(index);
			for (int i = index + 1; i < limit; i++) {
				Point3D<P> p1 = values.get(i);

				if(lowest.eclipses(p1)) {
					values.remove(i);
					i--;
					limit--;
				}
			}
		}
	}

	protected boolean constrainFloatingMax(Point3D<P> point, P placement) {

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
			Point3D<P> clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY());
			addXX.add(clone);
		}
		
		if(y) {
			Point3D<P> clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1);
			addYY.add(clone);
		}

		return !(x || y);
	}	

	private boolean constraintPositiveMaxY(Point3D<P> point, P placement) {
		int limit = placement.getAbsoluteY() - 1;
		if(limit < point.getMinY()) {
			return false;
		}
		if(point.getMaxY() > limit) {
			point.setMaxY(limit);
		}
		return true;

	}

	private boolean constrainPositiveMaxX(Point3D<P> point, P placement) {
		int limit = placement.getAbsoluteX() - 1;
		if(limit < point.getMinX()) {
			return false;
		}
		if(point.getMaxX() > limit) {
			point.setMaxX(limit);
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

	public Point3D<P> getValue(int i) {
		return values.get(i);
	}
	
	public List<Point3D<P>> getValues() {
		return values;
	}
	
	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			
			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			
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
		for (Point3D<P> Point3D : values) {
			if(maxPointArea < Point3D.getArea()) {
				maxPointArea = Point3D.getArea(); 
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
			Point3D<P> Point3D = values.get(i);
			if(Point3D.getMinX() == x && Point3D.getMinY() == y) {
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

}
