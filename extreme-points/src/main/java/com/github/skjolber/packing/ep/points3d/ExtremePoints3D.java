package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point3D;

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
	
	protected long minVolume = -1L;
	
	protected List<Point3D<P>> values = new ArrayList<>();
	protected List<P> placements = new ArrayList<>();

	// reuse working variables
	protected final List<Point3D<P>> addXX = new ArrayList<>();
	protected final List<Point3D<P>> addYY = new ArrayList<>();
	protected List<Point3D<P>> addZZ = new ArrayList<>();

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
	
	protected void setSize(int dx, int dy, int dz) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;

		this.containerPlacement = createContainerPlacement();
	}

	private P createContainerPlacement() {
		return (P) new DefaultPlacement3D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ);
	}

	protected void addFirstPoint() {
		values.add(new Default3DPlanePoint3D<>(
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
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		int zz = source.getMinZ() + boxDz;
			
		boolean supportedXYPlane = source.isSupportedXYPlane(placement.getAbsoluteEndX(), placement.getAbsoluteEndY());
		boolean supportedXZPlane = source.isSupportedXZPlane(placement.getAbsoluteEndX(), placement.getAbsoluteEndZ());
		boolean supportedYZPlane = source.isSupportedYZPlane(placement.getAbsoluteEndY(), placement.getAbsoluteEndZ());
		
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
		
		int endIndex = binarySearchPlusMinX(placement.getAbsoluteEndX());
		
		for(int i = 0; i < endIndex; i++) {		
			Point3D<P> point = values.get(i);
			
			boolean lessThanXX = point.getMinX() <= placement.getAbsoluteEndX();
			boolean lessThanYY = point.getMinY() <= placement.getAbsoluteEndY();
			boolean lessThanZZ = point.getMinZ() <= placement.getAbsoluteEndZ();
			
			if(!lessThanXX || !lessThanYY || !lessThanZZ) {
				
				// 
				// |
				// |
				// |  *           *        *
				// |          
				// |          |------|
				// |          |      |     *
				// |          |------|
				// |                       
				// |                       *
				// |                       
				// ---------------------------
				//

				continue;
			}

			// Points within (xx, yy, zz)
			// 
			// |
			// |
			// |          
			// | *  *     |------|
			// |          |  *  *|
			// |   *      |------|
			// |              *        
			// |     *   *       *
			// | *         *            
			// ---------------------------
			//

			boolean lessThanX = point.getMinX() < source.getMinX();
			boolean lessThanY = point.getMinY() < source.getMinY();
			boolean lessThanZ = point.getMinZ() < source.getMinZ();
				
			if(!lessThanX && !lessThanY && !lessThanZ) {
				
				// 
				// |
				// |
				// |
				// |          
				// |          |------|
				// |          | *  * |     
				// |          |------|
				// |                       
				// |                       
				// |                       
				// ---------------------------
				//

				swallowed.add(point);
				
				continue;
			}
			
			if(supportedXYPlane && supportedXZPlane && supportedYZPlane) {
				
				// 
				// |
				// |          ║
				// |          ║
				// |          ║------|
				// |          ║      |
				// |          ║══════════
				// |                       
				// |                  
				// |                       
				// ---------------------------
				// 
				
				continue;
			}
			
			// Points within (xx, yy, zz), excluding the placement itself
			// 
			// |
			// |
			// |          
			// | *  *     |------|
			// |          |      |
			// |   *      |------|
			// |              *        
			// |     *   *       *
			// | *         *            
			// ---------------------------
			//
			
			boolean maxMoreThanX = point.getMaxX() >= source.getMinX();
			boolean maxMoreThanY = point.getMaxY() >= source.getMinY();
			boolean maxMoreThanZ = point.getMaxZ() >= source.getMinZ();

			if(!maxMoreThanX || !maxMoreThanY || !maxMoreThanZ) {
				// point does not intersect placement
				// 
				// 
				// |          
				// |          |------|
				// |          | *  * |     
				// |          |------|
				// |                       
				// |             |--------|          
				// |             |        |  
				// |             |--------|          
				// ---------------------------
				//				

				// 
				// | |----|   
				// | |    |   |------|
				// | |----|   | *  * |     
				// |          |------|
				// |                       
				// |                       
				// |                       
				// |                       
				// ---------------------------
				//				

				// 
				// |          
				// |          |------|
				// |          | *  * |     
				// |          |------|
				// |                       
				// |  |----|                     
				// |  |    |                 
				// |  |----|                     
				// ---------------------------
				//

				// does any point intersect the xx, yy or zz planes?
				if(point.getMaxX() >= xx) {
					// yz plane

					negativeMoveToXX.add(point);

					/*
					if(!supportedXYPlane && !supportedXZPlane) {
						negativeMoveToXX.add(point);
					} else if(!supportedXYPlane) {

						// minZ to maxZ
						// 0 to maxY
						if(point.getMaxZ() >= source.getMinZ()) {
							negativeMoveToXX.add(point);
						}
					} else if(!supportedXZPlane) {
						
						// 0 to maxZ
						// minY to maxY
						
						negativeMoveToXX.add(point);
					}*/
				}
				
				if(point.getMaxY() >= yy) {
					// xz plane
					negativeMoveToYY.add(point);
				}
				
				if(point.getMaxZ() >= zz) {
					// xy plane
					negativeMoveToZZ.add(point);
					
					/*
					if(!supportedXZPlane && !supportedYZPlane) {
						// 0 to maxX
						// 0 to maxY

						negativeMoveToZZ.add(point);
					} else if(!supportedXZPlane) {
						// minX to maxX
						// 0 to maxY
						
						negativeMoveToZZ.add(point);
					} else if(!supportedYZPlane) {
						// 0 to maxX
						// minY to maxY
						
						negativeMoveToZZ.add(point);
					}
					*/
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
		}
		
		negativeMoveToXX.addAll(moveToXX);
		negativeMoveToXX.addAll(swallowed);
		Collections.sort(negativeMoveToXX, Point3D.COMPARATOR_Y_THEN_Z_THEN_X);
		
		negativeMoveToYY.addAll(moveToYY);
		negativeMoveToYY.addAll(swallowed);
		Collections.sort(negativeMoveToYY, Point3D.COMPARATOR_Z_THEN_X_THEN_Y);
		
		negativeMoveToZZ.addAll(moveToZZ);
		negativeMoveToZZ.addAll(swallowed);
		Collections.sort(negativeMoveToZZ, Point3D.COMPARATOR_X_THEN_Y_THEN_Z);

		deleted.addAll(swallowed);
		
		if(!negativeMoveToXX.isEmpty()) {

			add:
			for(Point3D<P> p : negativeMoveToXX) {
				// add point on the other side
				// with x support
				if(p.getMaxX() >= xx) {
					for(Point3D<P> add : addXX) {
						if(add.eclipsesMovedX(p, xx)) {
							continue add;
						}
					}
					if(p.getMinY() < placement.getAbsoluteY() || p.getMinZ() < placement.getAbsoluteZ()) {
						// too low, no support
						addXX.add(p.moveX(xx, p.getMaxX(), p.getMaxY(), p.getMaxZ()));
					} else {
						addXX.add(p.moveX(xx, p.getMaxX(), p.getMaxY(), p.getMaxZ(), placement));
					}
				}
			}			
		}
		
		if(!negativeMoveToYY.isEmpty()) {
			
			add:
			for(Point3D<P> p : negativeMoveToYY) {
				// add point on the other side
				// with x support
				if(p.getMaxY() >= yy) {
					for(Point3D<P> add : addYY) {
						if(add.eclipsesMovedY(p, yy)) {
							continue add;
						}
					}
					if(p.getMinX() < placement.getAbsoluteX() || p.getMinZ() < placement.getAbsoluteZ()) {
						// too low, no support
						addYY.add(p.moveY(yy, p.getMaxX(), p.getMaxY(), p.getMaxZ()));
					} else {
						addYY.add(p.moveY(yy, p.getMaxX(), p.getMaxY(), p.getMaxZ(), placement));
					}
				}
			}			
		}

		if(!negativeMoveToZZ.isEmpty()) {
			
			add:
			for(Point3D<P> p : negativeMoveToZZ) {
				// add point on the other side
				// with x support
				if(p.getMaxZ() >= zz) {
					for(Point3D<P> add : addZZ) {
						if(add.eclipsesMovedZ(p, zz)) {
							continue add;
						}
					}
					if(p.getMinX() < placement.getAbsoluteX() || p.getMinY() < placement.getAbsoluteY()) {
						// too low, no support
						addZZ.add(p.moveZ(zz, p.getMaxX(), p.getMaxY(), p.getMaxZ()));
					} else {
						addZZ.add(p.moveZ(zz, p.getMaxX(), p.getMaxY(), p.getMaxZ(), placement));
					}
				}
			}			
		}

		// Constrain max values to the new placement
		constrainFloatingMax(placement);
		
		values.removeAll(deleted);
		
		placements.add(placement);
		
		if(minVolume != -1L) {
			filterMinVolume();
		}
		
		removeEclipsed(values, addXX);
		removeEclipsed(values, addYY);
		removeEclipsed(values, addZZ);
		
		values.addAll(addXX);
		values.addAll(addYY);
		values.addAll(addZZ);

		Collections.sort(values, Point3D.COMPARATOR_X_THEN_Y_THEN_Z);

		swallowed.clear();
		
		moveToXX.clear();
		moveToYY.clear();
		moveToZZ.clear();
		
		negativeMoveToXX.clear();
		negativeMoveToYY.clear();
		negativeMoveToZZ.clear();
		
		addXX.clear();
		addYY.clear();
		addZZ.clear();
		
		// validate(placement);
		
		deleted.clear();

		return !values.isEmpty();
	}
	
	private void filterMinVolume() {
		
		filterMinVolume(addXX);
		filterMinVolume(addYY);
		filterMinVolume(addZZ);
	}

	private void filterMinVolume(List<Point3D<P>> addXX) {
		for (int i = 0; i < addXX.size(); i++) {
			Point3D<P> p = addXX.get(i);
			
			if(p.getVolume() < minVolume) {
				addXX.remove(i);
				i--;
			}
		}
	}

	private void validate(P target) {
		
		for (P p : placements) {
			for (Point3D<P> point : values) {
			
				boolean x = point.getMinX() <= p.getAbsoluteEndX() && point.getMaxX() >= p.getAbsoluteX();
				boolean y = point.getMinY() <= p.getAbsoluteEndY() && point.getMaxY() >= p.getAbsoluteY();
				boolean z = point.getMinZ() <= p.getAbsoluteEndZ() && point.getMaxZ() >= p.getAbsoluteZ();
				
				if(x && y && z) {
					throw new IllegalArgumentException();
				}
			}
		}
		
	}

	protected void removeEclipsed(List<Point3D<P>> values, List<Point3D<P>> added) {
		for(int index = 0; index < values.size(); index++) {
			Point3D<P> value = values.get(index);
			
			for (int i = 0; i < added.size(); i++) {
				Point3D<P> p1 = added.get(i);

				if(value.eclipses(p1)) {
					added.remove(i);
					i--;
				} else if(p1.eclipses(value)) {
					values.remove(index);
					index--;
					
					break;
				}
			}
		}
	}

	protected void constrainFloatingMax(P placement) {

		for (int i = 0; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			
			if(placement.getAbsoluteEndX() < point.getMinX()) {
				continue;
			}

			if(placement.getAbsoluteEndY() < point.getMinY()) {
				continue;
			}

			if(placement.getAbsoluteEndZ() < point.getMinZ()) {
				continue;
			}

			if(placement.getAbsoluteX() > point.getMaxX()) {
				continue;
			}

			if(placement.getAbsoluteY() > point.getMaxY()) {
				continue;
			}	

			if(placement.getAbsoluteZ() > point.getMaxZ()) {
				continue;
			}	

			boolean x = placement.getAbsoluteX() <= point.getMaxX();
			boolean y = placement.getAbsoluteY() <= point.getMaxY();
			boolean z = placement.getAbsoluteZ() <= point.getMaxZ();

			boolean constrain = x && y && z;
			if(!constrain) {
				continue;
			}
			
			if(!cloneOnConstrain) {
				if(point.getMinY() >= placement.getAbsoluteY() && point.getMinZ() >= placement.getAbsoluteZ() ) {
					// adjusting x is sufficient
					if(point.getMinX() < placement.getAbsoluteX()) {
						point.setMaxX(placement.getAbsoluteX() - 1);
						
						// is the point now eclipsed?
						for (int j = 0; j < i - 1; j++) {
							Point3D<P> point3d = values.get(j);
							
							if(point3d.eclipses(point)) {
								deleted.add(point);
								
								break;
							}
						}
					} else {
						deleted.add(point);
					}
					continue;
				}
				if(point.getMinX() >= placement.getAbsoluteX() && point.getMinZ() >= placement.getAbsoluteZ() ) {
					// adjusting y is sufficient
					if(point.getMinY() < placement.getAbsoluteY()) {
						point.setMaxY(placement.getAbsoluteY() - 1);
						
						// is the point now eclipsed?
						for (int j = 0; j < i - 1; j++) {
							Point3D<P> point3d = values.get(j);
							
							if(point3d.eclipses(point)) {
								deleted.add(point);
								
								break;
							}
						}
					} else {
						deleted.add(point);
					}
					continue;
				}
				if(point.getMinY() >= placement.getAbsoluteY() && point.getMinX() >= placement.getAbsoluteX() ) {
					// adjusting z is sufficient
					if(point.getMinZ() < placement.getAbsoluteZ()) {
						point.setMaxZ(placement.getAbsoluteZ() - 1);
						
						// is the point now eclipsed?
						for (int j = 0; j < i - 1; j++) {
							Point3D<P> point3d = values.get(j);
							
							if(point3d.eclipses(point)) {
								deleted.add(point);
								
								break;
							}
						}

					} else {
						deleted.add(point);
					}
					continue;
				}
			}
			
			if(x) {
				if(point.getMinX() < placement.getAbsoluteX()) {
					addXX.add(point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ()));
				}
			}
			
			if(y) {
				if(point.getMinY() < placement.getAbsoluteY()) {
					addYY.add(point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ()));
				}
			}
			
			if(z) {
				if(point.getMinZ() < placement.getAbsoluteZ()) {
					addZZ.add(point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1));
				}
			}

			deleted.add(point);	
		}
		
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
	
	public int getHeight() {
		return containerMaxZ + 1;
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

	public int getMinZ() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point3D<P> point2d = values.get(i);
			
			if(point2d.getMinZ() < values.get(min).getMinZ()) {
				min = i;
			}
		}
		return min;
	}
	
	public int get(int x, int y, int z) {
		for (int i = 0; i < values.size(); i++) {
			Point3D<P> point2d = values.get(i);
			
			if(point2d.getMinY() == y && point2d.getMinX() == x && point2d.getMinZ() == z) {
				return i;
			}
		}
		return -1;
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
		setSize(dx, dy, dz);
		
		redo();
	}

	public int findPoint(int x, int y, int z) {
		for(int i = 0; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			if(point.getMinX() == x && point.getMinY() == y && point.getMinZ() == z) {
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

	public long getMaxVolume() {
		long maxPointVolume = -1L;
		for (Point3D<P> point : values) {
			if(maxPointVolume < point.getArea()) {
				maxPointVolume = point.getVolume(); 
			}
		}
		return maxPointVolume;
	}

	public void setMinVolume(long minVolume) {
		this.minVolume = minVolume;
	}
}
