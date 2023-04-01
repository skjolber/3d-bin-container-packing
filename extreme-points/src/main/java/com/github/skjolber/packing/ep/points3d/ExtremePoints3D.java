package com.github.skjolber.packing.ep.points3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.api.ep.YZPlanePoint3D;

/**
 * 
 * Implementation of so-called extreme points in 3D.
 *
 */

public class ExtremePoints3D<P extends Placement3D & Serializable> implements ExtremePoints<P, Point3D<P>> {
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
	protected Point3DFlagList<P> otherValues = new Point3DFlagList<>();

	protected ArrayList<P> placements = new ArrayList<>();

	// reuse working variables
	protected final Point3DListArray<P> addXX = new Point3DListArray<>();
	protected final Point3DListArray<P> addYY = new Point3DListArray<>();
	protected final Point3DListArray<P> addZZ = new Point3DListArray<>();

	protected final Point3DArray<P> constrainXX = new Point3DArray<>();
	protected final Point3DArray<P> constrainYY = new Point3DArray<>();
	protected final Point3DArray<P> constrainZZ = new Point3DArray<>();

	protected final IntArrayList moveToXX = new IntArrayList();
	protected final IntArrayList moveToYY = new IntArrayList();
	protected final IntArrayList moveToZZ = new IntArrayList();

	protected final List<Point3D<P>> addedXX = new ArrayList<>(128);
	protected final List<Point3D<P>> addedYY = new ArrayList<>(128);
	protected final List<Point3D<P>> addedZZ = new ArrayList<>(128);

	protected final boolean cloneOnConstrain;

	protected P containerPlacement;
	protected Default3DPlanePoint3D<P> firstPoint;

	protected static CustomIntXComparator xxComparator = new CustomIntXComparator();
	protected static CustomIntYComparator yyComparator = new CustomIntYComparator();
	protected static CustomIntZComparator zzComparator = new CustomIntZComparator();

	public ExtremePoints3D(int dx, int dy, int dz) {
		this(dx, dy, dz, false);
	}

	public ExtremePoints3D(int dx, int dy, int dz, boolean cloneOnConstrain) {
		setSize(dx, dy, dz);
		this.cloneOnConstrain = cloneOnConstrain;

		values.add(firstPoint);
		
		xxComparator.setValues(values);
		yyComparator.setValues(values);
		zzComparator.setValues(values);
	}

	protected void setSize(int dx, int dy, int dz) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;

		this.containerPlacement = createContainerPlacement();
		
		this.firstPoint = new Default3DPlanePoint3D<>(
				0, 0, 0,
				containerMaxX, containerMaxY, containerMaxZ,
				containerPlacement,
				containerPlacement,
				containerPlacement);
	}

	@SuppressWarnings("unchecked")
	private P createContainerPlacement() {
		return (P)new DefaultPlacement3D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ);
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
		
		// copy intensively used items to local variables
		Point3DFlagList<P> values = this.values;
		Point3DFlagList<P> otherValues = this.otherValues;
		
		Point3D<P> source = values.get(index);
		values.flag(index);

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

		addXX.ensureCapacity(values.size() + 1);
		addYY.ensureCapacity(values.size() + 1);
		addZZ.ensureCapacity(values.size() + 1);

		constrainXX.ensureCapacity(values.size());
		constrainYY.ensureCapacity(values.size());
		constrainZZ.ensureCapacity(values.size());

		int pointIndex;
		if(supportedYZPlane) {
			// b and c only

			// already have index for point at absoluteX, find the lowest value with the same x coordinate
			pointIndex = index;
			while (pointIndex > 0 && values.get(pointIndex - 1).getMinX() == placement.getAbsoluteX()) {
				pointIndex--;
			}
		} else {
			pointIndex = 0;
		}

		for (int i = pointIndex; i < endIndex; i++) {
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
			xxComparator.setXx(xx);
			moveToXX.sortThis(xxComparator);

			int moveToXXSize = moveToXX.size();
			int targetIndex = endIndex;
			addXX.ensureAdditionalCapacity(targetIndex, moveToXXSize);

			add: for (int i = 0; i < moveToXXSize; i++) {
				int currentIndex = moveToXX.get(i);
				Point3D<P> p = values.get(currentIndex);
				// add point on the other side
				// with x support
				for (int k = 0; k < addedXX.size(); k++) {
					Point3D<P> add = addedXX.get(k);
					if(add.eclipsesMovedX(p, xx)) {
						continue add;
					}
				}

				Point3D<P> added;
				if(p.getMinY() < placement.getAbsoluteY() || p.getMinZ() < placement.getAbsoluteZ()) {
					// too low, no support
					added = p.moveX(xx);
				} else {
					// moved point still has support
					added = p.moveX(xx, placement);
				}

				// find right insertion point
				// TODO skip x
				while (targetIndex < values.size() && Point3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(added, values.get(targetIndex)) > 0) {
					targetIndex++;

					addXX.ensureAdditionalCapacity(targetIndex, moveToXXSize - i);
				}

				addXX.add(added, targetIndex);
				addedXX.add(added);
			}
			
			moveToXX.clear();
		}

		if(!moveToYY.isEmpty()) {
			yyComparator.setYy(yy);
			moveToYY.sortThis(yyComparator);

			add: for (int i = 0; i < moveToYY.size(); i++) {
				int currentIndex = moveToYY.get(i);

				Point3D<P> p = values.get(currentIndex);

				// add point on the other side
				// with x support
				for (int k = 0; k < addedYY.size(); k++) {
					Point3D<P> add = addedYY.get(k);
					if(add.eclipsesMovedY(p, yy)) {
						continue add;
					}
				}

				Point3D<P> added;
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
				while (targetIndex < values.size() && Point3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(added, values.get(targetIndex)) > 0) {
					targetIndex++;
				}

				addYY.ensureAdditionalCapacity(targetIndex, 1);

				addYY.add(added, targetIndex);
				addedYY.add(added);
			}
			
			moveToYY.clear();
		}

		if(!moveToZZ.isEmpty()) {
			zzComparator.setZz(zz);
			moveToZZ.sortThis(zzComparator);

			add: for (int i = 0; i < moveToZZ.size(); i++) {
				int currentIndex = moveToZZ.get(i);

				Point3D<P> p = values.get(currentIndex);

				// add point on the other side
				for (int k = 0; k < addedZZ.size(); k++) {
					Point3D<P> add = addedZZ.get(k);
					if(add.eclipsesMovedZ(p, zz)) {
						continue add;
					}
				}

				Point3D<P> added;
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
				while (targetIndex < values.size() && Point3D.COMPARATOR_X_THEN_Y_THEN_Z.compare(added, values.get(targetIndex)) > 0) {
					targetIndex++;
				}

				addZZ.ensureAdditionalCapacity(targetIndex, 1);

				addZZ.add(added, targetIndex);
				addedZZ.add(added);
			}
			moveToZZ.clear();
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
			} else {
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

		int added = addedXX.size() + addedYY.size() + addedZZ.size() + constrainXX.size() + constrainYY.size() + constrainZZ.size();

		otherValues.ensureCapacity(values.size() + added);

		for (int i = 0; i < endIndex; i++) {

			Point3DList<P> addZZPoint3d = addZZ.get(i);
			if(!addZZPoint3d.isEmpty()) {
				for (int k = 0; k < addZZPoint3d.size(); k++) {
					Point3D<P> p = addZZPoint3d.get(k);
					if(!isEclipsed(p)) {
						otherValues.add(p);
					}
				}
				addZZPoint3d.clear();
			}

			Point3DList<P> addYYPoint3d = addYY.get(i);
			if(!addYYPoint3d.isEmpty()) {
				for (int k = 0; k < addYYPoint3d.size(); k++) {
					Point3D<P> p = addYYPoint3d.get(k);
					if(!isEclipsed(p)) {
						otherValues.add(p);
					}
				}
				addYYPoint3d.clear();
			}

			if(!values.isFlag(i)) {
				otherValues.add(values.get(i));
			}

			Point3D<P> constrainXXPoint = constrainXX.get(i);
			if(constrainXXPoint != null) {
				if(!isEclipsed(constrainXXPoint)) {
					otherValues.add(constrainXXPoint);
				}
				// clean up here so we do not need to reset the array
				constrainXX.clear(i);
			}
			Point3D<P> constrainYYPoint = constrainYY.get(i);
			if(constrainYYPoint != null) {
				if(!isEclipsed(constrainYYPoint)) {
					otherValues.add(constrainYYPoint);
				}
				// clean up here so we do not need to reset the array
				constrainYY.clear(i);
			}
			Point3D<P> constrainZZPoint = constrainZZ.get(i);
			if(constrainZZPoint != null) {
				if(!isEclipsed(constrainZZPoint)) {
					otherValues.add(constrainZZPoint);
				}
				// clean up here so we do not need to reset the array
				constrainZZ.clear(i);
			}
		}

		Point3DList<P> addZZPoint3d = addZZ.get(endIndex);
		if(!addZZPoint3d.isEmpty()) {
			for (int k = 0; k < addZZPoint3d.size(); k++) {
				Point3D<P> p = addZZPoint3d.get(k);
				if(!isEclipsed(p)) {
					otherValues.add(p);
				}
			}
			addZZPoint3d.clear();
		}

		Point3DList<P> addYYPoint3d = addYY.get(endIndex);
		if(!addYYPoint3d.isEmpty()) {
			for (int k = 0; k < addYYPoint3d.size(); k++) {
				Point3D<P> p = addYYPoint3d.get(k);
				if(!isEclipsed(p)) {
					otherValues.add(p);
				}
			}
			addYYPoint3d.clear();
		}

		for (int i = endIndex; i < values.size(); i++) {
			Point3DList<P> addXXPoint3d = addXX.get(i);
			if(!addXXPoint3d.isEmpty()) {
				for (int k = 0; k < addXXPoint3d.size(); k++) {
					Point3D<P> p = addXXPoint3d.get(k);
					if(p instanceof YZPlanePoint3D) {
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
			}
		}

		// get the last element, if any
		Point3DList<P> addXXPoint3d = addXX.get(values.size());
		if(!addXXPoint3d.isEmpty()) {
			for (int k = 0; k < addXXPoint3d.size(); k++) {
				Point3D<P> p = addXXPoint3d.get(k);
				if(p instanceof YZPlanePoint3D) {
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

		otherValues.copyInto(values);
		otherValues.reset();

		addedXX.clear();
		addedYY.clear();
		addedZZ.clear();

		// already cleaned up: 
		// constrainXX
		// constrainYY 
		// constrainZZ
		
		return !values.isEmpty();
	}

	private boolean isEclipsed(Point3D<P> point) {
		// check if one of the existing values contains the new value
		for (int index = 0; index < otherValues.size(); index++) {
			Point3D<P> otherValue = otherValues.get(index);

			if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
				if(otherValue.eclipses(point)) {
					// discard 
					return true;
				}
			}
		}
		return false;
	}

	private boolean isEclipsedAtXX(Point3D<P> point, int xx) {
		// check if one of the existing values contains the new value
		for (int index = otherValues.size() - 1; index >= 0; index--) {
			Point3D<P> otherValue = otherValues.get(index);
			if(otherValue.getMinX() < xx) {
				return false;
			}
			if(point.getVolume() <= otherValue.getVolume() && point.getArea() <= otherValue.getArea()) {
				if(otherValue.eclipses(point)) {
					// discard 
					return true;
				}
			}
		}
		return false;
	}

	private void constrainMax(P placement, int endIndex) {
		constrainXX.ensureAdditionalCapacity(endIndex);
		constrainYY.ensureAdditionalCapacity(endIndex);
		constrainZZ.ensureAdditionalCapacity(endIndex);

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
							/*
							if(point.getArea() < minAreaLimit) {
								values.flag(i);
							}
							*/
							values.flag(i);
							constrainXX.set(point, i);
						}
					}
				}
			} else if(!withinY(point.getMinY(), placement)) {
				// already within x
				if(withinZ(point.getMinZ(), placement)) {
					if(point.getMinY() < placement.getAbsoluteY()) {
						if(point.getMaxY() >= placement.getAbsoluteY()) {
							point.setMaxY(placement.getAbsoluteY() - 1);
							/*
							if(point.getArea() < minAreaLimit) {
								values.flag(i);
							}
							*/
							values.flag(i);
							constrainYY.set(point, i);
						}
					}
				}
			} else if(point.getMinZ() < placement.getAbsoluteZ()) { // i.e. not within z
				// already within x and y
				if(point.getMaxZ() >= placement.getAbsoluteZ()) {
					if(point.getMaxZ() >= placement.getAbsoluteZ()) {
						point.setMaxZ(placement.getAbsoluteZ() - 1);
						/*
						if(point.getArea() < minAreaLimit) {
							values.flag(i);
						}
						*/
						values.flag(i);
						constrainZZ.set(point, i);
					}
				}
			}

		}
	}

	private void constrainMaxWithClone(P placement, int endIndex) {
		constrainXX.ensureAdditionalCapacity(endIndex);
		constrainYY.ensureAdditionalCapacity(endIndex);
		constrainZZ.ensureAdditionalCapacity(endIndex);

		for (int i = 0; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point3D<P> point = values.get(i);
			if(!withinX(point.getMinX(), placement)) {
				if(withinZ(point.getMinZ(), placement) && withinY(point.getMinY(), placement)) {
					if(point.getMinX() < placement.getAbsoluteX()) {
						if(point.getMaxX() >= placement.getAbsoluteX()) {

							long area = (placement.getAbsoluteX() - point.getMinX()) * (long)point.getDy();

							if(area >= minAreaLimit) {
								Point3D<P> clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ());
								constrainXX.set(clone, i);
							}
							values.flag(i);
						}
					}
				}
			} else if(!withinY(point.getMinY(), placement)) {
				if(withinZ(point.getMinZ(), placement)) {
					if(point.getMinY() < placement.getAbsoluteY()) {
						if(point.getMaxY() >= placement.getAbsoluteY()) {
							long area = (placement.getAbsoluteY() - point.getMinY()) * (long)point.getDx();

							if(area >= minAreaLimit) {
								Point3D<P> clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ());
								constrainYY.set(clone, i);
							}
							values.flag(i);
						}
					}
				}
			} else if(point.getMinZ() < placement.getAbsoluteZ()) { // i.e. if(!withinZ(point.getMinZ(), placement)) {
				if(point.getMaxZ() >= placement.getAbsoluteZ()) {
					Point3D<P> clone = point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1);
					constrainZZ.set(clone, i);
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
		boolean flagged = false;
		for (int i = 0; i < values.size(); i++) {
			Point3D<P> p = values.get(i);

			if(p.getVolume() < minVolumeLimit || p.getArea() < minAreaLimit) {
				values.flag(i);

				flagged = true;
			}
		}
		if(flagged) {
			values.removeFlagged();
		}
	}

	protected void removeEclipsed(int limit) {

		// implementation note:
		// this does not scale too well for many points

		//   unsorted        sorted
		// |   new    |   existing current   |
		// |----------|----------------------|--> x

		Point3DFlagList<P> values = this.otherValues;

		int size = values.size();

		added: for (int i = 0; i < limit; i++) {
			Point3D<P> unsorted = values.get(i);

			// check if one of the existing values contains the new value
			for (int index = limit; index < size; index++) {
				if(values.isFlag(index)) {
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
		/*
		addXX.ensureAdditionalCapacity(limit);
		addYY.ensureAdditionalCapacity(limit);
		addZZ.ensureAdditionalCapacity(limit);
		*/

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

			addX: if(point.getMinX() < placement.getAbsoluteX()) {
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
					for (int j = 0; j < addedXX.size(); j++) {
						Point3D<P> point3d = addedXX.get(j);

						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addX;
							}
						}
					}

					addedXX.add(clone);
					constrainXX.set(clone, i);
				}
			}

			addY: if(point.getMinY() < placement.getAbsoluteY()) {
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
					for (int j = 0; j < addedYY.size(); j++) {
						Point3D<P> point3d = addedYY.get(j);

						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addY;
							}
						}
					}

					addedYY.add(clone);
					constrainYY.set(clone, i);
				}
			}

			addZ: if(point.getMinZ() < placement.getAbsoluteZ()) {
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
					for (int j = 0; j < addedZZ.size(); j++) {
						Point3D<P> point3d = addedZZ.get(j);

						if(point3d.getVolume() >= clone.getVolume()) {
							if(point3d.eclipses(clone)) {
								break addZ;
							}
						}
					}

					addedZZ.add(clone);
					constrainZZ.set(clone, i);
				}
			}
			values.flag(i);
		}

	}

	protected void constrainFloatingMax(P placement, int limit) {

		Point3DFlagList<P> values = this.values;

		long minAreaLimit = this.minAreaLimit;
		long minVolumeLimit = this.minVolumeLimit;

		int startAddXX = addedXX.size();
		int startAddYY = addedYY.size();
		int startAddZZ = addedZZ.size();

		boolean splitXX = false;
		boolean splitYY = false;
		boolean splitZZ = false;

		limitLoop: for (int i = 0; i < limit; i++) {
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
						for (int j = startAddXX; j < addedXX.size(); j++) {
							Point3D<P> point3d = addedXX.get(j);

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
						for (int j = startAddYY; j < addedYY.size(); j++) {
							Point3D<P> point3d = addedYY.get(j);

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
						for (int j = startAddZZ; j < addedZZ.size(); j++) {
							Point3D<P> point3d = addedZZ.get(j);

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
				Point3D<P> clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ());
				constrainXX.set(clone, i);
				addedXX.add(clone);
				splitXX = true;
			}
			if(!isConstrainedAtMaxY(point, placement.getAbsoluteY() - 1)) {
				Point3D<P> clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ());
				constrainYY.set(clone, i);
				addedYY.add(clone);
				splitYY = true;
			}

			if(!isConstrainedAtMaxZ(point, placement.getAbsoluteZ() - 1)) {
				Point3D<P> clone = point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1);
				constrainZZ.set(clone, i);
				addedZZ.add(clone);
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
		for (int i = 0; i < values.size(); i++) {
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

		values.add(firstPoint);
	}

	@Override
	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		redo();
	}

	public int findPoint(int x, int y, int z) {
		for (int i = 0; i < values.size(); i++) {
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

			if(cmp < 0) {
				low = mid + 1;
			} else if(cmp > 0) {
				high = mid - 1;
			} else {
				// key found
				do {
					mid++;
				} while (mid < values.size() && values.get(mid).getMinY() == key);

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

			if(cmp < 0) {
				low = mid + 1;
			} else if(cmp > 0) {
				high = mid - 1;
			} else {
				// key found
				do {
					mid++;
				} while (mid < values.size() && values.get(mid).getMinX() == key);

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

			if(cmp < 0) {
				low = mid + 1;
			} else if(cmp > 0) {
				high = mid - 1;
			} else {
				// key found
				while (mid > 0 && values.get(mid - 1).getMinX() == key) {
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
		for (int i = 0; i < values.size(); i++) {
			Point3D<P> point = values.get(i);
			if(maxPointVolume < point.getVolume()) {
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
