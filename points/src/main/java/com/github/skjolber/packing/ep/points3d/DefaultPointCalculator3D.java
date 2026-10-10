package com.github.skjolber.packing.ep.points3d;

import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.PlacementList;

/**
 * 
 * Implementation of so-called extreme points in 3D.
 * <p>
 * The points which are moved past a placement are processed in a canonical total order (see {@linkplain CustomIntXComparator}, {@linkplain CustomIntYComparator} and
 * {@linkplain CustomIntZComparator}), so the free points after a placement only depend on the free points before it, not on the sorting algorithm. Each move adds only the
 * maximal moved points, and of moved points which are geometrically identical the one with the richest supports.
 *
 */

public class DefaultPointCalculator3D implements PointCalculator {

	protected int containerMaxX;
	protected int containerMaxY;
	protected int containerMaxZ;

	// TODO should there be a min area constraint on min z, min y and min x too?
	protected long minVolumeLimit = 0;
	protected long minAreaLimit = 0;

	protected Point3DFlagList values = new Point3DFlagList(); // i.e. current (input) values
	protected Point3DFlagList otherValues = new Point3DFlagList(); // i.e. next (output) values

	protected PlacementList placements;

	// reuse working variables
	protected final Point3DListArray addXX = new Point3DListArray();
	protected final Point3DListArray addYY = new Point3DListArray();
	protected final Point3DListArray addZZ = new Point3DListArray();

	protected final Point3DArray constrainXX = new Point3DArray();
	protected final Point3DArray constrainYY = new Point3DArray();
	protected final Point3DArray constrainZZ = new Point3DArray();

	// note: int array because we also want to keep track of the index
	// TODO could be point index
	protected final CustomIntArrayList moveToXX = new CustomIntArrayList();
	protected final CustomIntArrayList moveToYY = new CustomIntArrayList();
	protected final CustomIntArrayList moveToZZ = new CustomIntArrayList();

	protected final Point3DList addedXX = new Point3DList(values.getCapacity());
	protected final Point3DList addedYY = new Point3DList(values.getCapacity());
	protected final Point3DList addedZZ = new Point3DList(values.getCapacity());

	protected final boolean immutablePoints;

	protected Placement containerPlacement;

	protected CustomIntXComparator xxComparator = new CustomIntXComparator();
	protected CustomIntYComparator yyComparator = new CustomIntYComparator();
	protected CustomIntZComparator zzComparator = new CustomIntZComparator();
	
	protected Point3DList initialPoints;

	public DefaultPointCalculator3D(boolean immutablePoints, BoxItemSource boxItemSource) {
		this.immutablePoints = immutablePoints;
		
		int count = 0;
		for(int i = 0; i < boxItemSource.size(); i++) {
			count += boxItemSource.get(i).getCount();
		}
		
		this.placements = new PlacementList(count);
	}
	
	public DefaultPointCalculator3D(boolean immutablePoints, int capacity) {
		this.immutablePoints = immutablePoints;
		this.placements = new PlacementList(capacity);
	}

	public void setSize(int dx, int dy, int dz) {
		int containerMaxX = dx - 1;
		int containerMaxY = dy - 1;
		int containerMaxZ = dz - 1;

		if(containerPlacement != null &&
				this.containerMaxX == containerMaxX &&
				this.containerMaxY == containerMaxY &&
				this.containerMaxZ == containerMaxZ) {
			return;
		}

		this.containerMaxX = containerMaxX;
		this.containerMaxY = containerMaxY;
		this.containerMaxZ = containerMaxZ;

		this.containerPlacement = createContainerPlacement();
	}

	private Placement createContainerPlacement() {
		BoxStackValue value = new BoxStackValue(containerMaxX + 1, containerMaxY + 1, containerMaxZ + 1, null, -1);
		
		return new Placement(value, new DefaultPoint3D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ), false);
	}
	
	public boolean add(Point point, Placement placement) {
		return add(indexOf(point), placement);
	}
	
	public boolean add(Point point, Placement placement, int filteredIndex, int filteredSize) {
		if(point.getIndex() == -1) {
			if(filteredSize == size()) {
				// i.e. no filtering was performed
				return add(filteredIndex, placement);
			}
			if(point == values.get(filteredIndex)) {
				// i.e. filtering only after index
				return add(filteredIndex, placement);
			}
			
			// TODO point index is probably close to filtered index if no too many items have been filtered
			
			return add(values.getIndex(point, filteredIndex), placement);
		} 
		return add(indexOf(point), placement);
	}

	public boolean add(int index, Placement placement) {
		return insert(values.get(index), index, placement);
	}

	/**
	 * Resolve the current index of a free point. Points are shared between stack levels,
	 * so a stored index is only trusted if it still refers to the same point. Points which
	 * are not current free points are located by their coordinates.
	 */
	protected int indexOf(Point point) {
		int index = point.getIndex();
		if(index >= 0 && index < values.size() && values.get(index) == point) {
			return index;
		}
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i) == point) {
				return i;
			}
		}
		return values.getIndex(point, 0);
	}

	/** Insert at a point which contains the placement, deriving the supported planes from the point. */
	protected boolean insert(SimplePoint3D point, int index, Placement placement) {
		// check supported planes when placement is not placed at point
		boolean supportedXYPlane = point.getMinZ() == placement.getAbsoluteZ() && point.isSupportedXYPlane(placement.getAbsoluteEndX(), placement.getAbsoluteEndY());
		boolean supportedXZPlane = point.getMinY() == placement.getAbsoluteY() && point.isSupportedXZPlane(placement.getAbsoluteEndX(), placement.getAbsoluteEndZ());
		boolean supportedYZPlane = point.getMinX() == placement.getAbsoluteX() && point.isSupportedYZPlane(placement.getAbsoluteEndY(), placement.getAbsoluteEndZ());
		
		return add(point, index, placement, supportedXYPlane, supportedXZPlane, supportedYZPlane);
	}

	/** See {@link #add(int, List, long, long)}. Resolves the selected point before filtering. */
	public boolean add(Point point, List<Placement> batch, long remainingMinimumArea, long remainingMinimumVolume) {
		return add(indexOf(point), batch, remainingMinimumArea, remainingMinimumVolume);
	}

	/**
	 * Insert an arrangement into one initially selected free-space point. Placements
	 * use absolute coordinates, must not overlap and must all fit the selected point.
	 * They are inserted in list order and retained directly, without copying or moving
	 * them. Callers must not mutate them while the calculator uses them.
	 *
	 * During insertion each limit is the smaller of the batch minimum and the
	 * corresponding remaining-item minimum, so the batch never prunes space which
	 * smaller remaining items need. After insertion, the remaining-item limits are
	 * installed. Use zero to disable a limit, or {@link Long#MAX_VALUE} for both limits
	 * when no items remain. Limits cannot recover previously pruned space: earlier
	 * insertions must also use safe remaining-item limits.
	 *
	 * Children are inserted with {@link #add(SimplePoint3D, int, Placement, boolean, boolean, boolean)},
	 * not {@link #add(int, Placement)}; subclasses which hook single insertions must also
	 * override {@link #addBatch(int, List, long, long)}.
	 *
	 * No overlap preflight or rollback snapshot is made. Invalid overlapping input can
	 * fail after partial insertion; callers requiring rollback must use a checkpoint.
	 * A false return means no usable free points remain, not that insertion failed.
	 *
	 * @param index live point index before filtering
	 * @param batch non-empty, prevalidated arrangement
	 * @param remainingMinimumArea smallest area of the remaining items
	 * @param remainingMinimumVolume smallest volume of the remaining items
	 * @return whether free points remain after all placements have been inserted
	 * @throws IllegalArgumentException if the batch is empty, a placement does not fit or a limit is negative
	 */
	public boolean add(int index, List<Placement> batch, long remainingMinimumArea, long remainingMinimumVolume) {
		if(remainingMinimumArea < 0 || remainingMinimumVolume < 0) {
			throw new IllegalArgumentException("Expected non-negative remaining minima");
		}
		if(batch.isEmpty()) {
			throw new IllegalArgumentException("Expected at least one placement");
		}
		return addBatch(index, batch, remainingMinimumArea, remainingMinimumVolume);
	}

	/** Shared batch entry point so stack calculators checkpoint exactly once per batch. */
	protected boolean addBatch(int index, List<Placement> batch, long remainingMinimumArea, long remainingMinimumVolume) {
		SimplePoint3D source = values.get(index);
		long minimumArea = remainingMinimumArea;
		long minimumVolume = remainingMinimumVolume;
		int batchSize = batch.size();
		for(int i = 0; i < batchSize; i++) {
			Placement placement = batch.get(i);
			if(!source.fits3D(placement)) {
				throw new IllegalArgumentException("Batch placement is outside the selected free space");
			}
			BoxStackValue value = placement.getStackValue();
			minimumArea = Math.min(minimumArea, value.getArea());
			minimumVolume = Math.min(minimumVolume, value.getVolume());
		}
		placements.ensureAdditionalCapacity(batchSize);
		int currentIndex = index;
		if(minimumArea != minAreaLimit || minimumVolume != minVolumeLimit) {
			setMinimumAreaAndVolumeLimit(minimumArea, minimumVolume);
			// The source contains every child, so it survives minimum filtering, but
			// earlier points may have been removed: its index can only decrease.
			currentIndex = indexOf(source, Math.min(index, values.size() - 1));
		}
		for(int i = 0; i < batchSize; i++) {
			Placement placement = batch.get(i);
			if(source == null) {
				currentIndex = findContainingPoint(placement);
				if(currentIndex == -1) {
					throw new IllegalArgumentException("No free space contains the next batch placement");
				}
				source = values.get(currentIndex);
			}
			// Bypass single-placement entry points: stack subclasses record a batch
			// once, not one search step per physical placement.
			insert(source, currentIndex, placement);
			source = null;
		}
		setMinimumAreaAndVolumeLimit(remainingMinimumArea, remainingMinimumVolume);
		return !values.isEmpty();
	}

	/** Index of the given point, searching downwards from {@code fromIndex}. */
	protected int indexOf(SimplePoint3D point, int fromIndex) {
		for(int i = fromIndex; i >= 0; i--) {
			if(values.get(i) == point) {
				return i;
			}
		}
		throw new IllegalStateException("Cannot locate point");
	}

	/**
	 * Find a point which contains the placement, preferring a point whose minimum corner
	 * is the placement's corner so that its supported planes are known. Current points
	 * are sorted by minimum X.
	 */
	protected int findContainingPoint(Placement placement) {
		int containing = -1;
		for(int i = 0; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);
			if(point.getMinX() > placement.getAbsoluteX()) {
				break;
			}
			if(point.fits3D(placement)) {
				if(point.getMinX() == placement.getAbsoluteX() && point.getMinY() == placement.getAbsoluteY() && point.getMinZ() == placement.getAbsoluteZ()) {
					return i;
				}
				if(containing == -1) {
					containing = i;
				}
			}
		}
		return containing;
	}

	public boolean addObstacle(Placement placement) {
		// find a point which holds the placement
		for(int i = 0; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);
			if(point.fits3D(placement)) {
				insert(point, i, placement);
				return true;
			}
		}
		return false;
	}

	public boolean add(SimplePoint3D source, int pointIndex, Placement placement, boolean supportedXYPlane, boolean supportedXZPlane, boolean supportedYZPlane ) {

		// overall approach:
		// Do not iterate over placements to find point max / mins, rather
		// project existing points. 
		//  
		// project points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side
		// add points shadowed by the two new points (if they could be moved in the negative direction)
		// remove points which are eclipsed by others

		// keep track of placement borders, where possible

		// copy intensively used items to local variables
		Point3DFlagList values = this.values;
		Point3DFlagList otherValues = this.otherValues;

		ensureCapacity(values.size() + 1);
		
		int xx = placement.getAbsoluteEndX() + 1;
		int yy = placement.getAbsoluteEndY() + 1;
		int zz = placement.getAbsoluteEndZ() + 1;

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

		// must be to the right of the current index, so set its first sibling as a minimum
		int endIndex = values.binarySearchPlusMinX(pointIndex + 1, placement.getAbsoluteEndX());

		if(supportedYZPlane) {
			// b and c only

			// already have index for point at absoluteX, find the lowest value with the same x coordinate
			while (pointIndex > 0 && values.get(pointIndex - 1).getMinX() == placement.getAbsoluteX()) {
				pointIndex--;
			}
		} else {
			pointIndex = 0;
		}

		classify(values, placement, pointIndex, endIndex, xx, yy, zz, supported);

		moveX(values, placement, endIndex, xx);
		moveY(values, placement, yy);
		moveZ(values, placement, zz);

		// Constrain max values to the new placement

		if(supported) {
			// not necessary
		} else if(supportedXYPlane && supportedXZPlane) {
			// must be directly left of placement
			constrainMaxYZ(placement, endIndex);
		} else if(supportedXYPlane && supportedYZPlane) {
			// must be directly in front of placement
			constrainMaxXZ(placement, pointIndex, endIndex);
		} else if(supportedXZPlane && supportedYZPlane) {
			// must be directly below placement
			constrainMaxXY(placement, pointIndex, endIndex);
		} else {
			// Constrain max values to the new placement
			if(immutablePoints) {
				constrainFloatingMaxWithCopy(placement, endIndex);
			} else {
				constrainFloatingMax(placement, endIndex);
			}
		}

		placements.add(placement);

		// Overview of the points we have accumulated above
		// these must be placed in the right order into the resulting output
		//
		//                                                                    XX
		//              | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10| 11| 12| 13| 14| 15| 16| 17| 18| 19| 20| 21 
		//  addXX       |   |   |   |   |   |   |   |   |   |   |   |   |   |   | a |   |   |   |   |   |   |   
		//  addYY       |   | 1 |   |   |   | 1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   
		//  addZZ       |   |   |   |   |   | 1 |   | 1 |   |   |   | 1 |   |   |   |   |   |   |   |   |   |   
		//  constrainXX |   |   | 1 | 1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   
		//  constrainYY |   |   | 1 | 1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   
		//  constrainZZ | 1 |   |   |   | 1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   
		//  values      | x | x | 1 | 1 | 1 | x | x | x | 1 | 1 | 1 | 1 | 1 | x | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1   
		//

		merge(values, otherValues, endIndex, xx);

		saveValues(values, otherValues);

		addedXX.clear();
		addedYY.clear();
		addedZZ.clear();

		// already cleaned up: 
		// constrainXX
		// constrainYY 
		// constrainZZ

		updateIndexes(otherValues);
		
		return !values.isEmpty();
	}

	/** Flag swallowed points and collect the points to move, between pointIndex and endIndex. */
	private void classify(Point3DFlagList values, Placement placement, int pointIndex, int endIndex, int xx, int yy, int zz, boolean supported) {
		// loop invariants: placement bounds are computed from the stack value on every call
		final int placementX = placement.getAbsoluteX();
		final int placementY = placement.getAbsoluteY();
		final int placementZ = placement.getAbsoluteZ();
		final int placementEndY = yy - 1;
		final int placementEndZ = zz - 1;

		for (int i = pointIndex; i < endIndex; i++) {
			SimplePoint3D point = values.get(i);

			if(point.getMinY() > placementEndY || point.getMinZ() > placementEndZ) {
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

			boolean swallowed = point.getMinX() >= placementX && point.getMinY() >= placementY && point.getMinZ() >= placementZ;
			if(swallowed ) {
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
				
				values.flag(i);
			} else {
				if(supported) {
					// 
					// |
					// |          ║
					// |          ║
					// |   *      ║------|
					// |          ║      |
					// |          ║══════════
					// |                       
					// |   *         *    
					// |                       
					// ---------------------------
					// 

					continue;
				} else {
					
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
				}
				
			}

			// move xx
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
	}

	/** Move points in the x direction, past the placement; the moved points are inserted at or after endIndex. */
	private void moveX(Point3DFlagList values, Placement placement, int endIndex, int xx) {
		if(!moveToXX.isEmpty()) {
			xxComparator.setValues(values, xx);
			// the order is total: the sorting algorithm does not matter. Insertion sort is better when items are already close to ordered
			moveToXX.insertionSortThis(xxComparator);

			int moveToXXSize = moveToXX.size();
			int targetIndex = endIndex;

			add: for (int i = 0; i < moveToXXSize; i++) {
				int currentIndex = moveToXX.get(i);
				SimplePoint3D p = values.get(currentIndex);
				// add point on the other side
				// with x support
				for (int k = 0; k < addedXX.size(); k++) {
					SimplePoint3D add = addedXX.get(k);
					if(add.eclipsesMovedX(p, xx)) {
						continue add;
					}
				}

				SimplePoint3D added;
				if(p.getMinY() < placement.getAbsoluteY() || p.getMinZ() < placement.getAbsoluteZ()) {
					// too low, no support
					added = p.moveX(xx);
				} else {
					// moved point still has support
					added = p.moveX(xx, placement);
				}

				// find right insertion point
				// TODO skip x
				while (targetIndex < values.size() && SimplePoint3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(added, values.get(targetIndex)) > 0) {
					targetIndex++;
				}

				// lists are created on first use, so only the index which receives the point gets capacity
				addXX.ensurePointAdditionalCapacity(targetIndex, 1);
				addXX.add(added, targetIndex);
				addedXX.add(added);
			}

			moveToXX.clear();
		}
	}

	/** Move points in the y direction, past the placement. */
	private void moveY(Point3DFlagList values, Placement placement, int yy) {
		if(!moveToYY.isEmpty()) {
			yyComparator.setValues(values, yy);
			moveToYY.insertionSortThis(yyComparator);

			int moveToYYSize = moveToYY.size();
			add: for (int i = 0; i < moveToYYSize; i++) {
				int currentIndex = moveToYY.get(i);

				SimplePoint3D p = values.get(currentIndex);

				// add point on the other side
				// with x support
				for (int k = 0; k < addedYY.size(); k++) {
					SimplePoint3D add = addedYY.get(k);
					if(add.eclipsesMovedY(p, yy)) {
						continue add;
					}
				}

				SimplePoint3D added;
				if(p.getMinX() < placement.getAbsoluteX() || p.getMinZ() < placement.getAbsoluteZ()) {
					// too low, no support
					added = p.moveY(yy);
				} else {
					// moved point still has support
					added = p.moveY(yy, placement);
				}

				// find right insertion point
				int targetIndex = currentIndex + 1;

				// TODO skip y
				while (targetIndex < values.size() && SimplePoint3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(added, values.get(targetIndex)) > 0) {
					targetIndex++;
				}

				addYY.ensurePointAdditionalCapacity(targetIndex, 1);

				addYY.add(added, targetIndex);
				addedYY.add(added);
			}

			moveToYY.clear();
		}
	}

	/** Move points in the z direction, past the placement. */
	private void moveZ(Point3DFlagList values, Placement placement, int zz) {
		if(!moveToZZ.isEmpty()) {
			zzComparator.setValues(values, zz);

			// insertion sort: Better when items are already close to ordered
			// which should be the case here, i.e. sorted by x, y, z.
			moveToZZ.insertionSortThis(zzComparator);

			int moveToZZSize = moveToZZ.size();
			add: for (int i = 0; i < moveToZZSize; i++) {
				int currentIndex = moveToZZ.get(i);

				SimplePoint3D p = values.get(currentIndex);

				// add point on the other side
				for (int k = 0; k < addedZZ.size(); k++) {
					SimplePoint3D add = addedZZ.get(k);
					if(add.eclipsesMovedZ(p, zz)) {
						continue add;
					}
				}

				SimplePoint3D added;
				if(p.getMinX() < placement.getAbsoluteX() || p.getMinY() < placement.getAbsoluteY()) {
					// too low, no support
					added = p.moveZ(zz);
				} else {
					// moved point still has support
					added = p.moveZ(zz, placement);
				}

				// find right insertion point
				int targetIndex = currentIndex + 1;
				// TODO skip z
				while (targetIndex < values.size() && SimplePoint3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(added, values.get(targetIndex)) > 0) {
					targetIndex++;
				}

				addZZ.ensurePointAdditionalCapacity(targetIndex, 1);

				addZZ.add(added, targetIndex);
				addedZZ.add(added);
			}
			moveToZZ.clear();
		}
	}

	/** Merge existing, moved and constrained points, in x order, into the output list, dropping eclipsed points. */
	private void merge(Point3DFlagList values, Point3DFlagList otherValues, int endIndex, int xx) {
		int added = addedXX.size() + addedYY.size() + addedZZ.size() + constrainXX.size() + constrainYY.size() + constrainZZ.size();

		otherValues.ensureCapacity(values.size() + added);

		for (int i = 0; i < endIndex; i++) {

			Point3DList addZZPoint3d = addZZ.get(i);
			if(!addZZPoint3d.isEmpty()) {
				for (int k = 0; k < addZZPoint3d.size(); k++) {
					SimplePoint3D p = addZZPoint3d.get(k);
					if(!isEclipsed(p)) {
						otherValues.add(p);
					}
				}
				addZZPoint3d.clear();
			}

			Point3DList addYYPoint3d = addYY.get(i);
			if(!addYYPoint3d.isEmpty()) {
				for (int k = 0; k < addYYPoint3d.size(); k++) {
					SimplePoint3D p = addYYPoint3d.get(k);
					if(!isEclipsed(p)) {
						otherValues.add(p);
					}
				}
				addYYPoint3d.clear();
			}

			if(!values.isFlag(i)) {
				otherValues.add(values.get(i));
			} else {
				// clear flag here so we dont have to reset later
				values.unflag(i);
			}

			SimplePoint3D constrainXXPoint = constrainXX.get(i);
			if(constrainXXPoint != null) {
				if(!isEclipsed(constrainXXPoint)) {
					otherValues.add(constrainXXPoint);
				}
				// clean up here so we do not need to reset the array
				constrainXX.clear(i);
			}
			SimplePoint3D constrainYYPoint = constrainYY.get(i);
			if(constrainYYPoint != null) {
				if(!isEclipsed(constrainYYPoint)) {
					otherValues.add(constrainYYPoint);
				}
				// clean up here so we do not need to reset the array
				constrainYY.clear(i);
			}
			SimplePoint3D constrainZZPoint = constrainZZ.get(i);
			if(constrainZZPoint != null) {
				if(!isEclipsed(constrainZZPoint)) {
					otherValues.add(constrainZZPoint);
				}
				// clean up here so we do not need to reset the array
				constrainZZ.clear(i);
			}
		}

		Point3DList addZZPoint3d = addZZ.get(endIndex);
		if(!addZZPoint3d.isEmpty()) {
			for (int k = 0; k < addZZPoint3d.size(); k++) {
				SimplePoint3D p = addZZPoint3d.get(k);
				if(!isEclipsed(p)) {
					otherValues.add(p);
				}
			}
			addZZPoint3d.clear();
		}

		Point3DList addYYPoint3d = addYY.get(endIndex);
		if(!addYYPoint3d.isEmpty()) {
			for (int k = 0; k < addYYPoint3d.size(); k++) {
				SimplePoint3D p = addYYPoint3d.get(k);
				if(!isEclipsed(p)) {
					otherValues.add(p);
				}
			}
			addYYPoint3d.clear();
		}

		for (int i = endIndex; i < values.size(); i++) {
			Point3DList addXXPoint3d = addXX.get(i);
			if(!addXXPoint3d.isEmpty()) {
				for (int k = 0; k < addXXPoint3d.size(); k++) {
					SimplePoint3D p = addXXPoint3d.get(k);
					if(p.isSupportedYZPlane()) {
						if(!isEclipsedAtXX(p, xx)) {
							otherValues.add(p);
						}
					} else {
						if(!isEclipsed(p)) {
							otherValues.add(p);
						}
					}
				}
				addXXPoint3d.clear();
			}

			if(!values.isFlag(i)) {
				otherValues.add(values.get(i));
			} else {
				// clear flag here so we dont have to reset later
				values.unflag(i);
			}
		}

		// get the last element, if any
		Point3DList addXXPoint3d = addXX.get(values.size());
		if(!addXXPoint3d.isEmpty()) {
			for (int k = 0; k < addXXPoint3d.size(); k++) {
				SimplePoint3D p = addXXPoint3d.get(k);
				
				if(p.isSupportedYZPlane()) {
					if(!isEclipsedAtXX(p, xx)) {
						otherValues.add(p);
					}
				} else {
					if(!isEclipsed(p)) {
						otherValues.add(p);
					}
				}
			}
			addXXPoint3d.clear();
		}
	}

	private void ensureCapacity(int size) {
		if(size >= addXX.getCapacity()) {
			int capacity = size + 32;
			
			addXX.ensureCapacity(capacity);
			addYY.ensureCapacity(capacity);
			addZZ.ensureCapacity(capacity);

			constrainXX.ensureCapacity(capacity);
			constrainYY.ensureCapacity(capacity);
			constrainZZ.ensureCapacity(capacity);
			
			addedXX.ensureCapacity(capacity);
			addedYY.ensureCapacity(capacity);
			addedZZ.ensureCapacity(capacity);
		}
	}

	protected void updateIndexes(Point3DFlagList values) {
		for(int i = 0; i < values.size(); i++) {
			SimplePoint3D simplePoint3D = values.get(i);
			if(simplePoint3D.getIndex() != i) {
				simplePoint3D.setIndex(i);
			}
		}
	}

	protected void saveValues(Point3DFlagList values, Point3DFlagList otherValues) {
		// Copy output to input + reset current input and set as next output.
		// this saves a good bit of cleanup
		this.values = otherValues;

		// note: assumes already reset flags 
		values.resetWithoutFlags();
		this.otherValues = values;
	}

	private boolean isEclipsed(SimplePoint3D point) {
		// check if one of the existing values contains the new value
		
		final int pointMinX = point.getMinX();
		final Point3DFlagList otherValues = this.otherValues;

		// otherValues is sorted by x. Scan backwards: an eclipsing point is most often
		// one of the last merged points. The result does not depend on the scan order.
		// There is no volume / area pre-filter: containment implies it, and the pre-filter measured as
		// more expensive than the early-exit containment test it guards.
		int index = otherValues.size() - 1;
		while (index >= 0 && otherValues.get(index).getMinX() > pointMinX) {
			index--;
		}
		for (; index >= 0; index--) {
			if(otherValues.get(index).eclipses(point)) {
				// discard 
				return true;
			}
		}
		return false;
	}

	private boolean isEclipsedAtXX(SimplePoint3D point, int xx) {
		// check if one of the existing values contains the new value

		// otherValues is sorted by x
		for (int index = otherValues.size() - 1; index >= 0; index--) {
			SimplePoint3D otherValue = otherValues.get(index);
			if(otherValue.getMinX() < xx) {
				return false;
			}
			if(otherValue.eclipses(point)) {
				// discard 
				return true;
			}
		}
		return false;
	}

	private int constrainMaxYZLimit(int x, int endIndex) {
		// only get the ones to the left of the placement
		for(int i = endIndex - 1; i >= 0; i--) {
			if(values.isFlag(i)) {
				continue;
			}
			if(values.get(i).getMinX() < x) {
				return i + 1;
			}
		}
		return 0;
	}

	private void constrainMaxYZ(Placement placement, int endIndex) {
		// minX must be to the left 
		// maxX must be above

		// only get the ones to the left of the placement
		endIndex = constrainMaxYZLimit(placement.getAbsoluteX(), endIndex);
		
		for (int i = 0; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			SimplePoint3D point = values.get(i);
			// point must be directly left of the placement

			// minX must be below
			// maxX must be above
			if(!withinY(point.getMinY(), placement)) {
				continue;
			}

			if(!withinZ(point.getMinZ(), placement)) {
				continue;
			}
			
			// needs constrain?
			if(placement.getAbsoluteX() > point.getMaxX()) {
				continue;
			}
			
			if(immutablePoints) {
				long area = (placement.getAbsoluteX() - point.getMinX()) * (long)point.getDy();
				if(area >= minAreaLimit) {
					SimplePoint3D copy = point.copy(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ());
					constrainXX.set(copy, i);
				}
			} else {
				point.setMaxX(placement.getAbsoluteX() - 1);
				constrainXX.set(point, i);
			}
			values.flag(i);
		}
	}
	
	private void constrainMaxXY(Placement placement, int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			SimplePoint3D point = values.get(i);
			// point must be directly below area of the placement

			// minZ must be below
			// maxZ must be above
			
			// below?
			if(point.getMinZ() >= placement.getAbsoluteZ() || placement.getAbsoluteZ() > point.getMaxZ()) {
				continue;
			}
			
			if(!withinY(point.getMinY(), placement)) {
				continue;
			}
			
			if(!withinX(point.getMinX(), placement)) {
				continue;
			}
			
			if(immutablePoints) {
				// area is same as before, so not necessary to constrain
				SimplePoint3D copy = point.copy(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1);
				constrainZZ.set(copy, i);
			} else {
				point.setMaxZ(placement.getAbsoluteZ() - 1);
				constrainZZ.set(point, i);
			}
			values.flag(i);
		}
	}
	

	private void constrainMaxXZ(Placement placement, int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			SimplePoint3D point = values.get(i);
			// point must be directly in front of the placement

			// minY must be below
			// maxY must be above
			
			// in front?
			if(point.getMinY() >= placement.getAbsoluteY() || placement.getAbsoluteY() > point.getMaxY()) {
				continue;
			}
			
			if(!withinZ(point.getMinZ(), placement)) {
				continue;
			}
			
			if(!withinX(point.getMinX(), placement)) {
				continue;
			}

			if(immutablePoints) {
				long area = (placement.getAbsoluteY() - point.getMinY()) * (long)point.getDx();
				if(area >= minAreaLimit) {
					SimplePoint3D copy = point.copy(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ());
					constrainYY.set(copy, i);
				}
			} else {
				point.setMaxY(placement.getAbsoluteY() - 1);
				/*
				if(point.getArea() >= minAreaLimit) {
					constrainYY.set(point, i);
				}
				*/
				constrainYY.set(point, i);
			}
			values.flag(i);
		}
	}
	
	private boolean canMoveZ(SimplePoint3D p, int zz) {
		if(p.getMaxZ() < zz) {
			return false;
		}
		return !isConstrainedAtZ(p, zz);
	}

	private boolean isConstrainedAtZ(SimplePoint3D p, int zz) {
		return p.getVolumeAtZ(zz) < minVolumeLimit;
	}

	private boolean canMoveX(SimplePoint3D p, int xx) {
		if(p.getMaxX() < xx) {
			return false;
		}
		return !isConstrainedAtX(p, xx);
	}

	private boolean isConstrainedAtX(SimplePoint3D p, int xx) {
		long areaAtX = p.getAreaAtX(xx);
		if(areaAtX >= minAreaLimit) {
			return false;
		}
		return areaAtX * p.getDz() < minVolumeLimit;
	}

	private boolean isConstrainedAtMaxX(SimplePoint3D p, int maxX) {
		long areaAtMaxX = p.getAreaAtMaxX(maxX);
		if(areaAtMaxX >= minAreaLimit) {
			return false;
		}
		return areaAtMaxX * p.getDz() < minVolumeLimit;
	}

	private boolean isConstrainedAtMaxY(SimplePoint3D p, int maxY) {
		long areaAtMaxY = p.getAreaAtMaxY(maxY);
		if(areaAtMaxY >= minAreaLimit) {
			return false;
		}
		return areaAtMaxY * p.getDz() < minVolumeLimit;
	}

	private boolean isConstrainedAtMaxZ(SimplePoint3D p, int maxZ) {
		return p.getVolumeAtMaxZ(maxZ) < minVolumeLimit;
	}

	private boolean canMoveY(SimplePoint3D p, int yy) {
		if(p.getMaxY() < yy) {
			return false;
		}
		return !isConstraintedAtY(p, yy);
	}

	private boolean isConstraintedAtY(SimplePoint3D p, int yy) {
		long areaAtY = p.getAreaAtY(yy);
		if(areaAtY >= minAreaLimit) {
			return false;
		}
		return areaAtY * p.getDz() < minVolumeLimit;
	}

	private void filterMinimums() {
		boolean flagged = false;
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D p = values.get(i);

			if(p.getVolume() < minVolumeLimit || p.getArea() < minAreaLimit) {
				values.flag(i);

				flagged = true;
			}
		}
		if(flagged) {
			values.removeFlagged();
			
			updateIndexes(values);
		}
	}

	public void remove(int index) {
		values.flag(index);
		values.removeFlagged();
		updateIndexes(values);
	}
	
	protected void removeEclipsed(int limit) {

		// implementation note:
		// this does not scale too well for many points

		//   unsorted        sorted
		// |   new    |   existing current   |
		// |----------|----------------------|--> x

		Point3DFlagList values = this.otherValues;

		int size = values.size();

		added: for (int i = 0; i < limit; i++) {
			SimplePoint3D unsorted = values.get(i);

			// check if one of the existing values contains the new value
			for (int index = limit; index < size; index++) {
				if(values.isFlag(index)) {
					continue;
				}

				SimplePoint3D sorted = values.get(index);
				if(sorted.getMinX() > unsorted.getMinX()) {
					// so sorted cannot contain unsorted
					// at this index or later
					break;
				}
				if(sorted.eclipses(unsorted)) {
					// discard unsorted
					values.flag(i);

					continue added;
				}
			}

			// all new points are the result of moving or constraining
			// existing points, so none of the new points 
			// can contain the old, less the previous points would
			// already have contained them.
		}
	}

	protected void constrainFloatingMaxWithCopy(Placement placement, int limit) {
		// A copy is not checked against the free points here: merge(..) checks every copy against the points before it,
		// which are the points which stay and the copies which this check would have compared it with (and more), and
		// discards an eclipsed copy. A check here only saves the allocation of a copy which is discarded, and costs a scan
		// of the list for every copy which is kept. No moved point can eclipse a copy: a moved point starts behind the
		// placement in the direction of the copy, and the copy ends before the placement.
		for (int i = 0; i < limit; i++) {
			SimplePoint3D point = values.get(i);

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

			if(point.getMinX() < placement.getAbsoluteX()) {
				int copyMaxX = placement.getAbsoluteX() - 1;
				if(!isConstrainedAtMaxX(point, copyMaxX)) {
					constrainXX.set(point.copy(copyMaxX, point.getMaxY(), point.getMaxZ()), i);
				}
			}

			if(point.getMinY() < placement.getAbsoluteY()) {
				int copyMaxY = placement.getAbsoluteY() - 1;
				if(!isConstrainedAtMaxY(point, copyMaxY)) {
					constrainYY.set(point.copy(point.getMaxX(), copyMaxY, point.getMaxZ()), i);
				}
			}

			if(point.getMinZ() < placement.getAbsoluteZ()) {
				int copyMaxZ = placement.getAbsoluteZ() - 1;
				if(!isConstrainedAtMaxZ(point, copyMaxZ)) {
					constrainZZ.set(point.copy(point.getMaxX(), point.getMaxY(), copyMaxZ), i);
				}
			}
			values.flag(i);
		}

	}

	protected void constrainFloatingMax(Placement placement, int limit) {

		Point3DFlagList values = this.values;

		// each examined point can add at most one copy per direction
		addedXX.ensureAdditionalCapacity(limit);
		addedYY.ensureAdditionalCapacity(limit);
		addedZZ.ensureAdditionalCapacity(limit);

		long minAreaLimit = this.minAreaLimit;
		long minVolumeLimit = this.minVolumeLimit;

		int startAddXX = addedXX.size();
		int startAddYY = addedYY.size();
		int startAddZZ = addedZZ.size();

		boolean splitXX = false;
		boolean splitYY = false;
		boolean splitZZ = false;
		

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
		// and
		//
		//    |         
		//    |                 
		//    |                 
		//    |--------|
		//    |        |         
		//    |        |         
		//    *--------|---------------------
		//             
		//
		// so points which are contained within two coordinates (i.e. directly below, directly to the left or directly behind)
		// the placement,  
		//
		//    |        |---------|
		//    |--------|         | 
		//    |        |         |
		//    |--------|         |
		//    |   *    |         |
		//    | *    * |         |
		// a  |    *   |------|--|------|
		//    |          *    |         |
		//    |             * |         |
		//    |-----------*---|---------|-----
		//    c        b
		// 
		// do not require splits, so constraining the max limit is sufficient. 
		// This is faster than creating another point.
		// 
		
		limitLoop: for (int i = 0; i < limit; i++) {
			SimplePoint3D point = values.get(i);

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

			if(point.getMinY() >= placement.getAbsoluteY() && point.getMinZ() >= placement.getAbsoluteZ()) {
				// adjusting x is sufficient
				if(point.getMinX() < placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);

					if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
						values.flag(i);

						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						SimplePoint3D point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}
						if(point3d.eclipses(point)) {
							values.flag(i);

							continue limitLoop;
						}
					}

					if(splitXX) {
						// is the point now eclipsed by new points?
						for (int j = startAddXX; j < addedXX.size(); j++) {
							SimplePoint3D point3d = addedXX.get(j);

							if(point3d.eclipses(point)) {
								values.flag(i);

								break;
							}
						}
					}
				} else {
					values.flag(i);
				}
				continue;
			}
			if(point.getMinX() >= placement.getAbsoluteX() && point.getMinZ() >= placement.getAbsoluteZ()) {
				// adjusting y is sufficient
				if(point.getMinY() < placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);

					if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
						values.flag(i);

						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						SimplePoint3D point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}
						if(point3d.eclipses(point)) {
							values.flag(i);

							continue limitLoop;
						}
					}

					if(splitYY) {
						// is the point now eclipsed by new points?
						for (int j = startAddYY; j < addedYY.size(); j++) {
							SimplePoint3D point3d = addedYY.get(j);

							if(point3d.eclipses(point)) {
								values.flag(i);

								break;
							}
						}
					}

				} else {
					values.flag(i);
				}
				continue;
			}
			if(point.getMinY() >= placement.getAbsoluteY() && point.getMinX() >= placement.getAbsoluteX()) {
				// adjusting z is sufficient
				if(point.getMinZ() < placement.getAbsoluteZ()) {
					point.setMaxZ(placement.getAbsoluteZ() - 1);

					if(point.getVolume() < minVolumeLimit || point.getArea() < minAreaLimit) {
						values.flag(i);

						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						SimplePoint3D point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}
						if(point3d.eclipses(point)) {
							values.flag(i);

							continue limitLoop;
						}
					}

					if(splitZZ) {
						// is the point now eclipsed by new points?
						for (int j = startAddZZ; j < addedZZ.size(); j++) {
							SimplePoint3D point3d = addedZZ.get(j);

							if(point3d.eclipses(point)) {
								values.flag(i);

								break;
							}
						}
					}

				} else {
					values.flag(i);
				}
				continue;
			}

			// fall through: must add multiple points

			if(point.getMinX() < placement.getAbsoluteX() &&!isConstrainedAtMaxX(point, placement.getAbsoluteX() - 1)) {
				SimplePoint3D copy = point.copy(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ());
				constrainXX.set(copy, i);
				addedXX.add(copy);
				splitXX = true;
			}
			
			if(point.getMinY() < placement.getAbsoluteY() && !isConstrainedAtMaxY(point, placement.getAbsoluteY() - 1)) {
				SimplePoint3D copy = point.copy(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ());
				constrainYY.set(copy, i);
				addedYY.add(copy);
				splitYY = true;
			}

			if(point.getMinZ() < placement.getAbsoluteZ() &&!isConstrainedAtMaxZ(point, placement.getAbsoluteZ() - 1)) {
				SimplePoint3D copy = point.copy(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1);
				constrainZZ.set(copy, i);
				addedZZ.add(copy);
				splitZZ = true;
			}
			values.flag(i);
		}

	}

	protected boolean withinX(int x, Placement placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}

	protected boolean withinY(int y, Placement placement) {
		return placement.getAbsoluteY() <= y && y <= placement.getAbsoluteEndY();
	}

	protected boolean withinZ(int z, Placement placement) {
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
		return "DefaultPointCalculator3D [width=" + containerMaxX + ", depth=" + containerMaxY + ", values=" + values + "]";
	}

	public List<Placement> getPlacements() {
		return placements.toList();
	}

	public SimplePoint3D get(int i) {
		return values.get(i);
	}

	public List<Point> getAll() {
		return values.toList();
	}

	public int size() {
		return values.size();
	}

	public List<Point> getValuesAsList() {
		return values.toList();
	}

	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);

			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);

			if(point.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinZ() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			SimplePoint3D point2d = values.get(i);

			if(point2d.getMinZ() < values.get(min).getMinZ()) {
				min = i;
			}
		}
		return min;
	}

	public int get(int x, int y, int z) {
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);

			if(point.getMinX() == x && point.getMinY() == y && point.getMinZ() == z) {
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
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);
			if(maxPointArea < point.getArea()) {
				maxPointArea = point.getArea();
			}
		}
		return maxPointArea;
	}

	public void clear() {
		values.clear();
		placements.clear();

		Point3DList initialPoints = this.initialPoints;
		if(initialPoints == null || initialPoints.isEmpty()) {
			SimplePoint3D origin = createContainerPoint();
			values.add(origin);
		} else {
			for (int i = 0; i < initialPoints.size(); i++) {
				SimplePoint3D simplePoint3D = initialPoints.get(i);
				SimplePoint3D copy = simplePoint3D.copy();
				copy.setIndex(values.size());
				values.add(copy);
			}
		}
		minAreaLimit = 0;
		minVolumeLimit = 0;
	}

	public void setPoints(List<Point> points) {
		// transform coordinates to internal representation, i.e. with support etc
		Point3DList initialPoints = prepareInitialPoints(points.size());
		
		for(Point p: points) {
			if(p.getMaxX() > containerMaxX) {
				throw new IllegalArgumentException("Expected point maxX " + p.getMaxX() + " <= " + containerMaxX + " container maxX");
			}
			if(p.getMaxY() > containerMaxY) {
				throw new IllegalArgumentException("Expected point maxY " + p.getMaxY() + " <= " + containerMaxY + " container maxY");
			}
			if(p.getMaxZ() > containerMaxZ) {
				throw new IllegalArgumentException("Expected point maxZ " + p.getMaxZ() + " <= " + containerMaxZ + " container maxZ");
			}

			boolean xyPlane = p.getMinZ() == 0;
			boolean yzPlane = p.getMinX() == 0;
			boolean xzPlane = p.getMinY() == 0;

			if(xyPlane && yzPlane && xzPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement, containerPlacement, containerPlacement));
			} else if(yzPlane && xzPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement, containerPlacement, null));
			} else if(xyPlane && xzPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), null, containerPlacement, containerPlacement));
			} else if(xyPlane && yzPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement, null, containerPlacement));
			} else if(xyPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), null, null, containerPlacement));
			} else if(xzPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), null, containerPlacement, null));
			} else if(yzPlane) {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement, null, null));
			} else {
				initialPoints.add(new DefaultPoint3D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ()));
			}
		}
		
		for(int i = 0; i < initialPoints.size(); i++) {
			initialPoints.get(i).setIndex(i);
		}
	}

	// set points, but limit to a specific box
	public boolean setPoints(List<Point> points, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		// transform coordinates to internal representation, i.e. with support etc
		Point3DList initialPoints = prepareInitialPoints(points.size());
		
		for(Point p: points) {
			
			// fully above current level?
			if(p.getMinZ() > maxZ) {
				continue;
			}
			// fully below current level?
			if(p.getMaxZ() < minZ) {
				continue;
			}

			// fully to the right? 
			if(p.getMinX() > maxX) {
				continue;
			}
			// fully to the left? 
			if(p.getMaxX() < minX) {
				continue;
			}

			// fully to the rear? 
			if(p.getMinY() > maxY) {
				continue;
			}
			// fully to the front?
			if(p.getMaxY() < minY) {
				continue;
			}
			
			// TODO support calculation could be better, but it fairly good for most cases
			boolean xyPlane = p.getMinZ() == 0;
			boolean yzPlane = p.getMinX() == 0;
			boolean xzPlane = p.getMinY() == 0;
			
			int limitedMinX = Math.max(p.getMinX(), minX);
			int limitedMinY = Math.max(p.getMinY(), minY);
			int limitedMinZ = Math.max(p.getMinZ(), minZ);
			int limitedMaxX = Math.min(p.getMaxX(), maxX);
			int limitedMaxY = Math.min(p.getMaxY(), maxY);
			int limitedMaxZ = Math.min(p.getMaxZ(), maxZ);
			
			if(xyPlane && yzPlane && xzPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, containerPlacement, containerPlacement, containerPlacement));
			} else if(yzPlane && xzPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, containerPlacement, containerPlacement, null));
			} else if(xyPlane && xzPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, null, containerPlacement, containerPlacement));
			} else if(xyPlane && yzPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, containerPlacement, null, containerPlacement));
			} else if(xyPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, null, null, containerPlacement));
			} else if(xzPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, null, containerPlacement, null));
			} else if(yzPlane) {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, limitedMaxX, limitedMaxY, limitedMaxZ, containerPlacement, null, null));
			} else {
				initialPoints.add(new DefaultPoint3D(limitedMinX, limitedMinY, limitedMinZ, p.getMaxX(), p.getMaxY(), p.getMaxZ()));
			}
		}
		
		for(int i = 0; i < initialPoints.size(); i++) {
			initialPoints.get(i).setIndex(i);
		}
		
		return !initialPoints.isEmpty();
	}

	private Point3DList prepareInitialPoints(int size) {
		if(initialPoints == null) {
			initialPoints = new Point3DList(size);
		} else {
			initialPoints.reset();
			initialPoints.ensureCapacity(size);
		}
		return initialPoints;
	}

	protected SimplePoint3D createContainerPoint() {
		SimplePoint3D firstPoint = new DefaultPoint3D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ, containerPlacement, containerPlacement, containerPlacement);
		
		firstPoint.setIndex(0);
		return firstPoint;
	}

	@Override
	public void clearToSize(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		clear();
	}

	public int findPoint(int x, int y, int z) {
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D point = values.get(i);
			if(point.getMinX() == x && point.getMinY() == y && point.getMinZ() == z) {
				return i;
			}
		}
		return -1;
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

	public long calculateUsedVolume() {
		long used = 0;
		for(int i = 0; i < placements.size(); i++) {
			Placement stackPlacement = placements.get(i);
			
			used += stackPlacement.getStackValue().getBox().getVolume();
		}
		return used;
	}
	
	public long calculateUsedWeight() {
		long used = 0;
		for(int i = 0; i < placements.size(); i++) {
			Placement stackPlacement = placements.get(i);
			
			used += stackPlacement.getStackValue().getBox().getWeight();
		}
		return used;
	}

	@Override
	public Iterator<Point> iterator() {
		return values.iterator();
	}
	
	public void updateMinimums(BoxStackValue stackValue, BoxItemSource filteredBoxItems) {
		boolean minArea = stackValue.getArea() == minAreaLimit;
		boolean minVolume = stackValue.getVolume() == minVolumeLimit;
		if(minArea && minVolume) {
			setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
		} else if(minArea) {
			setMinimumAreaLimit(filteredBoxItems.getMinArea());
		} else if(minVolume) {
			setMinimumVolumeLimit(filteredBoxItems.getMinVolume());
		}
	}
	
	public void updateMinimums(BoxItemSource filteredBoxItems) {
		boolean minArea = filteredBoxItems.getMinArea() == minAreaLimit;
		boolean minVolume = filteredBoxItems.getMinVolume() == minVolumeLimit;
		if(minArea && minVolume) {
			setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
		} else if(minArea) {
			setMinimumAreaLimit(filteredBoxItems.getMinArea());
		} else if(minVolume) {
			setMinimumVolumeLimit(filteredBoxItems.getMinVolume());
		}
	}

	
	public void updateMinimums(BoxStackValue stackValue, BoxItemGroupSource filteredBoxItemGroups) {
		boolean minArea = stackValue.getArea() == minAreaLimit;
		boolean minVolume = stackValue.getVolume() == minVolumeLimit;
		if(minArea && minVolume) {
			setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		} else if(minArea) {
			setMinimumAreaLimit(filteredBoxItemGroups.getMinArea());
		} else if(minVolume) {
			setMinimumVolumeLimit(filteredBoxItemGroups.getMinVolume());
		}
	}
	
	public int getDx() {
		return containerMaxX + 1; 
	}
	
	public int getDy() {
		return containerMaxY + 1; 
	}
	
	public int getDz() {
		return containerMaxZ + 1; 
	}

	public void clearInitialPoints() {
		if(initialPoints != null) {
			initialPoints.reset();
		}
	}

}
