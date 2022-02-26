package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point3D;

/**
 * 
 * Implementation of so-called extreme points in 3D.
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
	
	// TODO should there be a min area constraint on min z, min y and min x too?
	protected long minVolumeLimit = 0;
	protected long minAreaLimit = 0;
	
	protected Point3DFlagList<P> values = new Point3DFlagList<>();
	protected List<P> placements = new ArrayList<>();

	// reuse working variables
	protected final Point3DList<P> addXX = new Point3DList<>();
	protected final Point3DList<P> addYY = new Point3DList<>();
	protected final Point3DList<P> addZZ = new Point3DList<>();

	protected final IntArrayList moveToXX = new IntArrayList();
	protected final IntArrayList moveToYY = new IntArrayList();
	protected final IntArrayList moveToZZ = new IntArrayList();

	protected final boolean cloneOnConstrain;

	protected P containerPlacement;
	
	private IntComparator COMPARATOR_Y_THEN_Z_THEN_X = (a, b) -> Point3D.COMPARATOR_Y_THEN_Z_THEN_X.compare(values.get(a), values.get(b));
	private IntComparator COMPARATOR_Z_THEN_X_THEN_Y = (a, b) -> Point3D.COMPARATOR_Z_THEN_X_THEN_Y.compare(values.get(a), values.get(b));
	private IntComparator COMPARATOR_X_THEN_Y_THEN_Z = (a, b) -> Point3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(values.get(a), values.get(b));
	
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
		
		int xx = placement.getAbsoluteEndX() + 1;
		int yy = placement.getAbsoluteEndY() + 1;
		int zz = placement.getAbsoluteEndZ() + 1;
			
		boolean supportedXYPlane = source.isSupportedXYPlane(placement.getAbsoluteEndX(), placement.getAbsoluteEndY()); 
		boolean supportedXZPlane = source.isSupportedXZPlane(placement.getAbsoluteEndX(), placement.getAbsoluteEndZ());
		boolean supportedYZPlane = source.isSupportedYZPlane(placement.getAbsoluteEndY(), placement.getAbsoluteEndZ());
		
		boolean supported = supportedXYPlane && supportedXZPlane && supportedYZPlane;
		
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
		
		moveToXX.ensureCapacity(endIndex);
		moveToYY.ensureCapacity(endIndex);
		moveToZZ.ensureCapacity(endIndex);
		
		addXX.ensureCapacity(values.size());
		addYY.ensureCapacity(values.size());
		addZZ.ensureCapacity(values.size());
		
		int pointIndex;
		if(supportedYZPlane) {
			// b and c only
			
			// already have index for point at absoluteX
			pointIndex = index;
        	while(pointIndex > 0 && values.get(pointIndex - 1).getMinX() == placement.getAbsoluteX()) {
        		pointIndex--;
        	}
		} else {
			pointIndex = 0;
		}
		
		for(int i = pointIndex; i < endIndex; i++) {		
			Point3D<P> point = values.get(i);
			
			if(point.getMinY() > placement.getAbsoluteEndY() || point.getMinZ() > placement.getAbsoluteEndZ()) {
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

			if(point.getMinX() >= source.getMinX() && point.getMinY() >= source.getMinY() && point.getMinZ() >= source.getMinZ()) {
				
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

				if(canMoveX(point, xx)) {
					moveToXX.add(i);
				}
				if(canMoveY(point, yy)) {
					moveToYY.add(i);
				}
				if(canMoveZ(point, zz)) {
					moveToZZ.add(i);
				}
				
				values.flag(i);
				
				continue;
			}
			
			if(supported) {
				
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
			
			// does any point intersect the xx, yy or zz planes?
			if(canMoveX(point, xx)) {
				// yz plane
				moveToXX.add(i);
			}
			
			if(canMoveY(point, yy)) {
				// xz plane
				moveToYY.add(i);
			}
			
			if(canMoveZ(point, zz)) {
				// xy plane
				moveToZZ.add(i);
			}
		}

		if(!moveToXX.isEmpty()) {
			moveToXX.sortThis(COMPARATOR_Y_THEN_Z_THEN_X);
			
			add:
			for(int i = 0; i < moveToXX.size(); i++) {
				Point3D<P> p = values.get(moveToXX.get(i));
				// add point on the other side
				// with x support
				for(int k = 0; k < addXX.size(); k++) {
					Point3D<P> add = addXX.get(k);
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
		
		if(!moveToYY.isEmpty()) {
			moveToYY.sortThis(COMPARATOR_Z_THEN_X_THEN_Y);
			
			add:
			for(int i = 0; i < moveToYY.size(); i++) {
				Point3D<P> p = values.get(moveToYY.get(i));
				
				// add point on the other side
				// with x support
				for(int k = 0; k < addYY.size(); k++) {
					Point3D<P> add = addYY.get(k);
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

		if(!moveToZZ.isEmpty()) {
			moveToZZ.sortThis(COMPARATOR_X_THEN_Y_THEN_Z);
			
			add:
			for(int i = 0; i < moveToZZ.size(); i++) {
				Point3D<P> p = values.get(moveToZZ.get(i));
				
				// add point on the other side
				// with x support
				for(int k = 0; k < addZZ.size(); k++) {
					Point3D<P> add = addZZ.get(k);
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

		// Constrain max values to the new placement

		if(supported) {
			// not necessary
		} else if( 
				(supportedXYPlane && supportedXZPlane) ||
				(supportedXYPlane && supportedYZPlane) ||
				(supportedXZPlane && supportedYZPlane)
				) {
			if(cloneOnConstrain) {
				constrainMaxWithClone(placement, endIndex);
			} else  {
				constrainMax(placement, endIndex);
			}
		} else {
			// Constrain max values to the new placement
			if(cloneOnConstrain) {
				constrainFloatingMaxWithClone(placement, endIndex);
			} else {
				constrainFloatingMax(placement, endIndex);
			}
		}
		
		endIndex -= values.removeFlagged();
		
		placements.add(placement);

		int added = addXX.size() + addYY.size() + addZZ.size();
		// the new points have x coordinate between zero and xx. 
		// insert them at the start of the existing data
		// so that the sorting algorithm does not have to do a full sort
		// rather only sort points with x coordinates from 0 to xx.
		values.ensureAdditionalCapacity(added);
		
		// insert xx last, because it has the highest x coordinate
		values.move(added);
		values.setAll(addZZ, 0);
		values.setAll(addYY, addZZ.size());
		values.setAll(addXX, addZZ.size() + addYY.size());
		
		removeEclipsed(added);
		
		endIndex += added - values.removeFlagged();

		// make sure to capture all point <= xx
		while(endIndex < values.size() && values.get(endIndex).getMinX() <= xx) {
			endIndex++;
		}
		
		values.sort(Point3D.COMPARATOR_X_THEN_Y_THEN_Z, endIndex);

		moveToXX.clear();
		moveToYY.clear();
		moveToZZ.clear();
		
		addXX.clear();
		addYY.clear();
		addZZ.clear();
		
		return !values.isEmpty();
	}

	private void constrainMax(P placement, int endIndex) {
		for (int i = 0; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}
			
			Point3D<P> point = values.get(i);
			if(!withinX(point.getMinX(), placement)) {
				if(withinZ(point.getMinZ(), placement) && withinY(point.getMinY(), placement)) {
					if(point.getMinX() < placement.getAbsoluteX()) {
						if(point.getMaxX() >= placement.getAbsoluteX()) {
							point.setMaxX(placement.getAbsoluteX() - 1);
							if(point.getArea() < minAreaLimit) {
								values.flag(i);
							}
						}
					}
				}				
			} else if(!withinY(point.getMinY(), placement)) {
				// already within x
				if(withinZ(point.getMinZ(), placement)) {
					if(point.getMinY() < placement.getAbsoluteY()) {
						if(point.getMaxY() >= placement.getAbsoluteY()) {
							point.setMaxY(placement.getAbsoluteY() - 1);
							if(point.getArea() < minAreaLimit) {
								values.flag(i);
							}
						}
					}
				}
			} else if(point.getMinZ() < placement.getAbsoluteZ()) { // i.e. not within z
				// already within x and y
				if(point.getMaxZ() >= placement.getAbsoluteZ()) {
					if(point.getMaxZ() >= placement.getAbsoluteZ()) {
						point.setMaxZ(placement.getAbsoluteZ() - 1);
						if(point.getArea() < minAreaLimit) {
							values.flag(i);
						}
					}
				}
			}				
			
		}
	}

	private void constrainMaxWithClone(P placement, int endIndex) {
		addXX.ensureAdditionalCapacity(endIndex);
		addYY.ensureAdditionalCapacity(endIndex);
		addZZ.ensureAdditionalCapacity(endIndex);

		for (int i = 0; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}
			
			Point3D<P> point = values.get(i);
			if(!withinX(point.getMinX(), placement)) {
				if(withinZ(point.getMinZ(), placement) && withinY(point.getMinY(), placement)) {
					if(point.getMinX() < placement.getAbsoluteX()) {
						if(point.getMaxX() >= placement.getAbsoluteX()) {
							Point3D<P> clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ());
							if(clone.getArea() >= minAreaLimit) {
								addXX.add(clone);
							}
							values.flag(i);
						}
					}
				}				
			} else if(!withinY(point.getMinY(), placement)) {
				if(withinZ(point.getMinZ(), placement)) {
					if(point.getMinY() < placement.getAbsoluteY()) {
						if(point.getMaxY() >= placement.getAbsoluteY()) {
							Point3D<P> clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ());
							if(clone.getArea() >= minAreaLimit) {
								addYY.add(clone);
							}
							values.flag(i);
						}
					}
				}
			} else if(point.getMinZ() < placement.getAbsoluteZ()) { // i.e. if(!withinZ(point.getMinZ(), placement)) {
				
				if(point.getMaxZ() >= placement.getAbsoluteZ()) {
					Point3D<P> clone = point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1);
					if(clone.getArea() >= minAreaLimit) {
						addZZ.add(clone);
					}
					values.flag(i);
				}
			}				
		}
	}
	
	private boolean canMoveZ(Point3D<P> p, int zz) {
		if(p.getMaxZ() < zz) {
			return false;
		}
		return !isConstrainedAtZ(p, zz);
	}

	private boolean isConstrainedAtZ(Point3D<P> p, int zz) {
		return p.getVolumeAtZ(zz) < minVolumeLimit;
	}

	private boolean canMoveX(Point3D<P> p, int xx) {
		if(p.getMaxX() < xx) {
			return false;
		}
		return !isConstrainedAtX(p, xx);
	}

	private boolean isConstrainedAtX(Point3D<P> p, int xx) {
		long areaAtX = p.getAreaAtX(xx);
		if(areaAtX >= minAreaLimit) {
			return false;
		}
		return areaAtX * p.getDz() < minVolumeLimit;
	}
	
	private boolean isConstrainedAtMaxX(Point3D<P> p, int maxX) {
		long areaAtMaxX = p.getAreaAtMaxX(maxX);
		if(areaAtMaxX >= minAreaLimit) {
			return false;
		}
		return areaAtMaxX * p.getDz() < minVolumeLimit;
	}
	
	private boolean isConstrainedAtMaxY(Point3D<P> p, int maxY) {
		long areaAtMaxY = p.getAreaAtMaxY(maxY);
		if(areaAtMaxY >= minAreaLimit) {
			return false;
		}
		return areaAtMaxY * p.getDz() < minVolumeLimit;
	}
	
	private boolean isConstrainedAtMaxZ(Point3D<P> p, int maxZ) {
		return p.getVolumeAtMaxZ(maxZ) < minVolumeLimit;
	}
	

	private boolean canMoveY(Point3D<P> p, int yy) {
		if(p.getMaxY() < yy) {
			return false;
		}
		return !isConstraintedAtY(p, yy);
	}

	private boolean isConstraintedAtY(Point3D<P> p, int yy) {
		long areaAtY = p.getAreaAtY(yy);
		if(areaAtY >= minAreaLimit) {
			return false;
		}
		return areaAtY * p.getDz() < minVolumeLimit;
	}

	private void filterMinimums() {
		for (int i = 0; i < values.size(); i++) {
			Point3D<P> p = values.get(i);
			
			if(p.getVolume() < minVolumeLimit || p.getArea() < minAreaLimit) {
				values.flag(i);
			}
		}
	}

	protected void removeEclipsed(int limit) {

		//   unsorted        sorted
		// |   new    |   existing current   |
		// |----------|----------------------|--> x

		Point3DFlagList<P> values = this.values;
		
		int size = values.size();
		
		added:
		for (int i = 0; i < limit; i++) {
			Point3D<P> unsorted = values.get(i);

			// check if one of the existing values contains the new value
			for(int index = limit; index < size; index++) {
				if(values.isFlag(index) ) {
					continue;
				}
				
				Point3D<P> sorted = values.get(index);
				if(sorted.getMinX() > unsorted.getMinX()) {
					// so sorted cannot contain unsorted
					// at this index or later
					break;
				}
				if(unsorted.getVolume() <= sorted.getVolume() && unsorted.getArea() <= sorted.getArea()) {
					if(sorted.eclipses(unsorted)) {
						// discard unsorted
						values.flag(i);
						
						continue added;
					}
				}
			}

			// all new points are the result of moving or constraining
			// existing points, so none of the new points 
			// can contain the old, less the previous points would
			// already have contained them.
		}
	}
	

	protected void constrainFloatingMaxWithClone(P placement, int limit) {
		addXX.ensureAdditionalCapacity(limit);
		addYY.ensureAdditionalCapacity(limit);
		addZZ.ensureAdditionalCapacity(limit);

		for (int i = 0; i < limit; i++) {
			Point3D<P> point = values.get(i);
			
			if(
					placement.getAbsoluteEndX() < point.getMinX() ||
					placement.getAbsoluteEndY() < point.getMinY() ||
					placement.getAbsoluteEndZ() < point.getMinZ() ||
					placement.getAbsoluteX() > point.getMaxX() || 
					placement.getAbsoluteY() > point.getMaxY() ||
					placement.getAbsoluteZ() > point.getMaxZ()
					) {
					continue;
			}
	

			// before add
			//    
			//    |
			//    |--------|
			//    |        |
			//    |--------| 
			//    |
			//    |
			//    |
			// a  *        *      |---------|
			//    |               |         |
			//    |               |         |
			//    *--------*------|---------|-----
			//    c        b

			//  after add
			//             
			//    |        |---------|
			//    |--------|         | 
			//    |        |         |
			//    |--------|         |
			//    |        |         |
			//    |        |         |
			// a  *        |------|--|------|
			//    |               |         |
			//    |               |         |
			//    *--------*------|---------|-----
			//    c        b
			
			//
			// Point c is split in three, each of which eclipse a or b
			//
			// So that we end up with
			//             
			//    |        |---------|
			//    |--------|         | 
			//    |        |         |
			//    |--------|         |
			//    |        |         |
			//    |        |         |
			//    |        |------|--|------|
			//    |               |         |
			//    |               |         |
			//    *---------------|---------|-----
			//    c         
			
			// i.e. with c
			//             
			//    |--------|         
			//    |        |         
			//    |        |         
			//    |        |
			//    |        |          
			//    |        |                
			//    *--------|----------------------
			//
			// and
			//
			//    |         
			//    |                 
			//    |                 
			//    |---------------|
			//    |               |         
			//    |               |         
			//    *---------------|--------------
			//             

			
			addX: 
			if(point.getMinX() < placement.getAbsoluteX()) {
				if(!isConstrainedAtMaxX(point, placement.getAbsoluteX() - 1)) {
					Point3D<P> clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ());
					
					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point3D<P> point3d = values.get(j);
						if(point3d.getDx() > clone.getMinX()) {
							break;
						}

						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addX;
							}
						}
					}
					
					// is the point now eclipsed by new points?
					for (int j = 0; j < addXX.size(); j++) {
						Point3D<P> point3d = addXX.get(j);
						
						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addX;
							}
						}
					}
					
					addXX.add(clone);
				}
			}
			
			addY:
			if(point.getMinY() < placement.getAbsoluteY()) {
				if(!isConstrainedAtMaxY(point, placement.getAbsoluteY() - 1)) {
					Point3D<P> clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ());
					
					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point3D<P> point3d = values.get(j);
						if(point3d.getDx() > clone.getMinX()) {
							break;
						}
						
						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addY;
							}
						}
					}
					
					// is the point now eclipsed by new points?
					for (int j = 0; j < addYY.size(); j++) {
						Point3D<P> point3d = addYY.get(j);
						
						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addY;
							}
						}
					}						
					
					addYY.add(clone);
				}
			}
			
			addZ:
			if(point.getMinZ() < placement.getAbsoluteZ()) {
				if(!isConstrainedAtMaxZ(point, placement.getAbsoluteZ() - 1)) {
					Point3D<P> clone = point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1);
					
					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point3D<P> point3d = values.get(j);
						if(point3d.getDx() > clone.getMinX()) {
							break;
						}

						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addZ;
							}
						}
					}
					
					// is the point now eclipsed by new points?
					for (int j = 0; j < addZZ.size(); j++) {
						Point3D<P> point3d = addZZ.get(j);
						
						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addZ;
							}
						}
					}						
					
					
					addZZ.add(clone);
				}
			}
			values.flag(i);
		}
		
	}		

	protected void constrainFloatingMax(P placement, int limit) {

		Point3DFlagList<P> values = this.values;
		Point3DList<P> addXX = this.addXX;
		Point3DList<P> addYY = this.addYY;
		Point3DList<P> addZZ = this.addZZ;
		
		long minAreaLimit = this.minAreaLimit;
		long minVolumeLimit = this.minVolumeLimit;
		
		addXX.ensureAdditionalCapacity(limit);
		addYY.ensureAdditionalCapacity(limit);
		addZZ.ensureAdditionalCapacity(limit);
		
		int startAddXX = addXX.size();
		int startAddYY = addYY.size();
		int startAddZZ = addZZ.size();
		
		boolean splitXX = false;
		boolean splitYY = false;
		boolean splitZZ = false;

		limitLoop:
		for (int i = 0; i < limit; i++) {
			Point3D<P> point = values.get(i);

			if(
					placement.getAbsoluteEndZ() < point.getMinZ() ||
					placement.getAbsoluteEndY() < point.getMinY() ||
					placement.getAbsoluteEndX() < point.getMinX() ||
					placement.getAbsoluteX() > point.getMaxX() || 
					placement.getAbsoluteY() > point.getMaxY() ||
					placement.getAbsoluteZ() > point.getMaxZ()
					) {
					continue;
			}

			if(point.getMinY() >= placement.getAbsoluteY() && point.getMinZ() >= placement.getAbsoluteZ() ) {
				// adjusting x is sufficient
				if(point.getMinX() < placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);
					
					if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
						values.flag(i);
						
						continue;
					}
					
					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						Point3D<P> point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}							
						if(point3d.getVolume() >= point.getVolume()) {
							if(point3d.eclipses(point)) {
								values.flag(i);
								
								continue limitLoop;
							}
						}
					}
					
					if(splitXX) {
						// is the point now eclipsed by new points?
						for (int j = startAddXX; j < addXX.size(); j++) {
							Point3D<P> point3d = addXX.get(j);
							
							if(point3d.getVolume() >= point.getVolume()) {
								if(point3d.eclipses(point)) {
									values.flag(i);
									
									break;
								}
							}
						}
					}						
				} else {
					values.flag(i);
				}
				continue;
			}
			if(point.getMinX() >= placement.getAbsoluteX() && point.getMinZ() >= placement.getAbsoluteZ() ) {
				// adjusting y is sufficient
				if(point.getMinY() < placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);
					
					if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
						values.flag(i);
						
						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						Point3D<P> point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}							
						if(point3d.getVolume() >= point.getVolume()) {
							if(point3d.eclipses(point)) {
								values.flag(i);
								
								continue limitLoop;
							}
						}
					}
					
					if(splitYY) {
						// is the point now eclipsed by new points?
						for (int j = startAddYY; j < addYY.size(); j++) {
							Point3D<P> point3d = addYY.get(j);
							
							if(point3d.getVolume() >= point.getVolume()) {
								if(point3d.eclipses(point)) {
									values.flag(i);
									
									break;
								}
							}
						}
					}
					
				} else {
					values.flag(i);
				}
				continue;
			}
			if(point.getMinY() >= placement.getAbsoluteY() && point.getMinX() >= placement.getAbsoluteX() ) {
				// adjusting z is sufficient
				if(point.getMinZ() < placement.getAbsoluteZ()) {
					point.setMaxZ(placement.getAbsoluteZ() - 1);
					
					if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
						values.flag(i);
						
						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						Point3D<P> point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}
						if(point3d.getVolume() >= point.getVolume()) {
							if(point3d.eclipses(point)) {
								values.flag(i);
								
								continue limitLoop;
							}
						}
					}

					if(splitZZ) {
						// is the point now eclipsed by new points?
						for (int j = startAddZZ; j < addZZ.size(); j++) {
							Point3D<P> point3d = addZZ.get(j);
							
							if(point3d.getVolume() >= point.getVolume()) {
								if(point3d.eclipses(point)) {
									values.flag(i);
									
									break;
								}
							}
						}
					}

				} else {
					values.flag(i);
				}
				continue;
			}
			
			// fall through: must add multiple points
			
			// Points eclipsed by others:
			// before add
			//    
			//    |
			//    |--------|
			//    |        |
			//    |--------| 
			//    |
			//    |
			//    |
			// a  *        *      |---------|
			//    |               |         |
			//    |               |         |
			//    *--------*------|---------|-----
			//    c        b

			//  after add
			//             
			//    |        |---------|
			//    |--------|         | 
			//    |        |         |
			//    |--------|         |
			//    |        |         |
			//    |        |         |
			// a  *        |------|--|------|
			//    |               |         |
			//    |               |         |
			//    *--------*------|---------|-----
			//    c        b
			
			//
			// Point c is split in two, each of which eclipse a or b
			//
			// So that we end up with
			//             
			//    |        |---------|
			//    |--------|         | 
			//    |        |         |
			//    |--------|         |
			//    |        |         |
			//    |        |         |
			//    |        |------|--|------|
			//    |               |         |
			//    |               |         |
			//    *---------------|---------|-----
			//    c         
			
			
			if(!isConstrainedAtMaxX(point, placement.getAbsoluteX() - 1)) {
				addXX.add(point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ()));
				
				splitXX = true;
			}
			if(!isConstrainedAtMaxY(point, placement.getAbsoluteY() - 1)) {
				addYY.add(point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ()));
				splitYY = true;
			}
			
			if(!isConstrainedAtMaxZ(point, placement.getAbsoluteZ() - 1)) {
				addZZ.add(point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1));
				
				splitZZ = true;
			}
			values.flag(i);
		}
		
	}
	
	protected boolean withinX(int x, P placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}	

	protected boolean withinY(int y, P placement) {
		return placement.getAbsoluteY() <= y && y <= placement.getAbsoluteEndY();
	}
	
	protected boolean withinZ(int z, P placement) {
		return placement.getAbsoluteZ() <= z && z <= placement.getAbsoluteEndZ();
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
		return "ExtremePoints3D [width=" + containerMaxX + ", depth=" + containerMaxY + ", values=" + values + "]";
	}
	
	public List<P> getPlacements() {
		return placements;
	}

	public Point3D<P> getValue(int i) {
		return values.get(i);
	}
	
	public List<Point3D<P>> getValues() {
		return values.toList();
	}
	
	@Override
	public int getValueCount() {
		return values.size();
	}
	
	public List<Point3D<P>> getValuesAsList() {
		return values.toList();
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
		for(int i = 0; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
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
		for(int i = 0; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			if(maxPointVolume < point.getArea()) {
				maxPointVolume = point.getVolume(); 
			}
		}
		return maxPointVolume;
	}
	
	public void setMinimumAreaAndVolumeLimit(long area, long volume) {
		if(minAreaLimit != area || minVolumeLimit != volume) {
			this.minAreaLimit = area;
			this.minVolumeLimit = volume;			
			filterMinimums();
		}
	}
	
	public void setMinimumAreaLimit(long min) {
		if(minAreaLimit != min) {
			this.minAreaLimit = min;
			filterMinimums();
		}
	}

	public void setMinimumVolumeLimit(long min) {
		if(minVolumeLimit != min) {
			this.minVolumeLimit = min;			
			filterMinimums();
		}
	}
	
	public long getMinAreaLimit() {
		return minAreaLimit;
	}
	
	public long getMinVolumeLimit() {
		return minVolumeLimit;
	}
}
