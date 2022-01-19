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

	protected final IntArrayList swallowed = new IntArrayList();
	protected final IntArrayList moveToXX = new IntArrayList();
	protected final IntArrayList moveToYY = new IntArrayList();
	protected final IntArrayList moveToZZ = new IntArrayList();

	protected final IntArrayList negativeMoveToXX = new IntArrayList();
	protected final IntArrayList negativeMoveToYY = new IntArrayList();
	protected final IntArrayList negativeMoveToZZ = new IntArrayList();

	protected final boolean cloneOnConstrain;

	protected P containerPlacement;
	
	private IntComparator COMPARATOR_Y_THEN_Z_THEN_X = (a, b) -> {
		return Point3D.COMPARATOR_Y_THEN_Z_THEN_X.compare(values.get(a), values.get(b));
	};
	
	private IntComparator COMPARATOR_Z_THEN_X_THEN_Y = (a, b) -> {
		return Point3D.COMPARATOR_Z_THEN_X_THEN_Y.compare(values.get(a), values.get(b));
	};
	
	private IntComparator COMPARATOR_X_THEN_Y_THEN_Z = (a, b) -> {
		return Point3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(values.get(a), values.get(b));
	};
	
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

				swallowed.add(i);
				
				values.flag(i);
				
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

			boolean intersect = !maxMoreThanX || !maxMoreThanY || !maxMoreThanZ;
			
			boolean moveX = canMoveX(point, xx);
			boolean moveY = canMoveY(point, yy);
			boolean moveZ = canMoveZ(point, zz);
			
			if(intersect) {
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
				if(moveX && point.getMaxX() >= xx) {
					// yz plane
					negativeMoveToXX.add(i);
				}
				
				if(moveY && point.getMaxY() >= yy) {
					// xz plane
					negativeMoveToYY.add(i);
				}
				
				if(moveZ && point.getMaxZ() >= zz) {
					// xy plane
					negativeMoveToZZ.add(i);
				}
				continue;
			}
			
			if(moveX && point.getMaxX() >= xx) {
				moveToXX.add(i);
			}
			if(moveY && point.getMaxY() >= yy) {
				moveToYY.add(i);
			}
			if(moveZ && point.getMaxZ() >= zz) {
				moveToZZ.add(i);
			}
		}

		negativeMoveToXX.addAll(moveToXX);
		negativeMoveToYY.addAll(moveToYY);
		negativeMoveToZZ.addAll(moveToZZ);

		for(int i = 0; i < swallowed.size(); i++) {
			int slot = swallowed.get(i);
			Point3D<P> point = values.get(slot);
			
			if(canMoveX(point, xx)) {
				negativeMoveToXX.add(slot);
			}
			if(canMoveY(point, yy)) {
				negativeMoveToYY.add(slot);
			}
			if(canMoveZ(point, zz)) {
				negativeMoveToZZ.add(slot);
			}
		}

		negativeMoveToXX.sortThis(COMPARATOR_Y_THEN_Z_THEN_X);
		negativeMoveToYY.sortThis(COMPARATOR_Z_THEN_X_THEN_Y);
		negativeMoveToZZ.sortThis(COMPARATOR_X_THEN_Y_THEN_Z);
		
		if(!negativeMoveToXX.isEmpty()) {

			addXX.addCapacity(negativeMoveToXX.size());

			add:
			for(int i = 0; i < negativeMoveToXX.size(); i++) {
				Point3D<P> p = values.get(negativeMoveToXX.get(i));
				// add point on the other side
				// with x support
				if(p.getMaxX() >= xx) {
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
		}
		
		if(!negativeMoveToYY.isEmpty()) {
			
			addYY.addCapacity(negativeMoveToYY.size());

			add:
			for(int i = 0; i < negativeMoveToYY.size(); i++) {
				Point3D<P> p = values.get(negativeMoveToYY.get(i));
				
				// add point on the other side
				// with x support
				if(p.getMaxY() >= yy) {
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
		}

		if(!negativeMoveToZZ.isEmpty()) {
			
			addZZ.addCapacity(negativeMoveToZZ.size());
			
			add:
			for(int i = 0; i < negativeMoveToZZ.size(); i++) {
				Point3D<P> p = values.get(negativeMoveToZZ.get(i));
				
				// add point on the other side
				// with x support
				if(p.getMaxZ() >= zz) {
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
		}

		// Constrain max values to the new placement
		constrainFloatingMax(placement);
		
		endIndex -= values.removeFlagged();
		
		placements.add(placement);

		int added = addXX.size() + addYY.size() + addZZ.size();
		// the new points have x coordinate between zero and xx. 
		// insert them at the start of the existing data
		// so that the sorting algorithm does not have to do a full sort
		// rather only sort points with x coordinates from 0 to xx.
		values.addCapacity(added);
		
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

		return !values.isEmpty();
	}
	
	private boolean canMoveZ(Point3D<P> p, int zz) {
		return p.getVolumeAtZ(zz) >= minVolumeLimit;
	}

	private boolean canMoveX(Point3D<P> p, int xx) {
		long areaAtX = p.getAreaAtX(xx);
		if(areaAtX < minAreaLimit) {
			return false;
		}
		return areaAtX * p.getDz() >= minVolumeLimit;
	}

	private boolean canMoveY(Point3D<P> p, int yy) {
		long areaAtY = p.getAreaAtY(yy);
		if(areaAtY < minAreaLimit) {
			return false;
		}
		return areaAtY * p.getDz() >= minVolumeLimit;
	}

	private void filterMinimums() {
		for (int i = 0; i < values.size(); i++) {
			Point3D<P> p = values.get(i);
			
			if(p.getVolume() < minVolumeLimit || p.getArea() < minAreaLimit) {
				values.flag(i);
			}
		}
	}

	private void validate(P target) {
		
		for (P p : placements) {
			for (int i = 0; i < values.size(); i++) {
				Point3D<P> point = values.get(i);
			
				boolean x = point.getMinX() <= p.getAbsoluteEndX() && point.getMaxX() >= p.getAbsoluteX();
				boolean y = point.getMinY() <= p.getAbsoluteEndY() && point.getMaxY() >= p.getAbsoluteY();
				boolean z = point.getMinZ() <= p.getAbsoluteEndZ() && point.getMaxZ() >= p.getAbsoluteZ();
				
				if(x && y && z) {
					throw new IllegalArgumentException();
				}
			}
		}
		
	}

	protected void removeEclipsed(int limit) {

		added:
		for (int i = 0; i < limit; i++) {
			Point3D<P> p1 = values.get(i);

			for(int index = limit; index < values.size(); index++) {
				if(values.isFlag(index) ) {
					continue;
				}
				Point3D<P> value = values.get(index);
				if(value.eclipses(p1)) {
					values.flag(i);
					
					continue added;
				}
			}

			// add value
			
			// do we want to remove any of the existing?
			for(int index = limit; index < values.size(); index++) {
				Point3D<P> value = values.get(index);
				if(values.isFlag(index) ) {
					continue;
				}

				if(value.eclipses(p1)) {
					values.flag(index);
				}
			}

		}
	}
	

	protected void constrainFloatingMax(P placement) {

		// TODO take advantage of sorted values along x axis
		
		addXX.ensureCapacity(addXX.size() + values.size());
		addYY.ensureCapacity(addYY.size() + values.size());
		addZZ.ensureCapacity(addZZ.size() + values.size());
		
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
						
						if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
							values.flag(i);
							
							continue;
						}
						
						// is the point now eclipsed?
						for (int j = 0; j < i - 1; j++) {
							Point3D<P> point3d = values.get(j);
							
							if(point3d.eclipses(point)) {
								values.flag(i);
								
								break;
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

						// is the point now eclipsed?
						for (int j = 0; j < i - 1; j++) {
							Point3D<P> point3d = values.get(j);
							
							if(point3d.eclipses(point)) {
								values.flag(i);
								
								break;
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

						// is the point now eclipsed?
						for (int j = 0; j < i - 1; j++) {
							Point3D<P> point3d = values.get(j);
							
							if(point3d.eclipses(point)) {
								values.flag(i);
								
								break;
							}
						}

					} else {
						values.flag(i);
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
			values.flag(i);
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
