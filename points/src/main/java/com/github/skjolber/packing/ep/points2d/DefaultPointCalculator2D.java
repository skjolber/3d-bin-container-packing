package com.github.skjolber.packing.ep.points2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;

/**
 * 
 * Implementation of so-called extreme points in 2D.
 *
 */

public class DefaultPointCalculator2D implements PointCalculator {

	public static final Comparator<Point2D> COMPARATOR_X = new Comparator<Point2D>() {

		@Override
		public int compare(Point2D o1, Point2D o2) {
			return Integer.compare(o1.getMinX(), o2.getMinX());
		}
	};

	protected int containerMaxX;
	protected int containerMaxY;
	protected int containerMaxZ;

	protected Point2DFlagList values = new Point2DFlagList();
	protected ArrayList<Placement> placements = new ArrayList<>();

	// reuse working variables
	protected final Point2DList addXX = new Point2DList();
	protected final Point2DList addYY = new Point2DList();

	protected final IntArrayList moveToYY = new IntArrayList();
	protected final IntArrayList moveToXX = new IntArrayList();

	protected Placement containerPlacement;

	protected long minAreaLimit = 0;

	protected final boolean cloneOnConstrain;

	protected List<SimplePoint2D> initialPoints = Collections.emptyList();

	private IntComparator COMPARATOR_MOVE_TO_YY = (a, b) -> {
		return Point2D.COMPARATOR_MOVE_YY.compare(values.get(a), values.get(b));
	};

	private IntComparator COMPARATOR_MOVE_TO_XX = (a, b) -> {
		return Point2D.COMPARATOR_MOVE_XX.compare(values.get(a), values.get(b));
	};

	public DefaultPointCalculator2D() {
		this(false);
	}

	public DefaultPointCalculator2D(boolean immutablePoints) {
		this.cloneOnConstrain = immutablePoints;
	}	

	@SuppressWarnings("unchecked")
	public void setSize(int dx, int dy, int dz) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;

		BoxStackValue stackValue = new BoxStackValue(dx, dy, dz, null, -1);
		
		this.containerPlacement = new Placement(stackValue, new DefaultPoint2D(0, 0, 0, dx - 1, dy - 1, dz - 1));
	}

	private DefaultXYSupportPoint2D createContainerPoint() {
		DefaultXYSupportPoint2D point = new DefaultXYSupportPoint2D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ, containerPlacement, containerPlacement);
		point.setIndex(0);
		return point;
	}

	public boolean add(Point point, Placement placement) {
		if(point.getIndex() == -1) {
			return add(binarySearch(point, 0), placement);
		} 
		return add(point.getIndex(), placement);
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
			
			return add(binarySearch(point, filteredIndex), placement);
		} 
		return add(point.getIndex(), placement);
	}	
	
	public boolean add(int index, Placement placement) {
		// overall approach:
		// Do not iterate over placements to find point max / mins, rather
		// project existing points. 
		//  
		// project points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side
		// add points shadowed by the two new points (if they could be moved in the negative direction)
		// remove points which are eclipsed by others

		// keep track of placement borders, where possible
		
		if(addXX.getCapacity() < values.size()) {
			int capacity = values.size() + 32;
			moveToXX.ensureCapacity(capacity);
			moveToYY.ensureCapacity(capacity);
			
			addXX.ensureAdditionalCapacity(capacity);
			addYY.ensureAdditionalCapacity(capacity);
		}

		Point2D source = values.get(index);

		boolean xSupport = source.isXSupport(source.getMinX());
		boolean ySupport = source.isYSupport(source.getMinY());

		int xx = placement.getAbsoluteEndX() + 1;
		int yy = placement.getAbsoluteEndY() + 1;

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
			// b and c only

			// already have index for point at absoluteX
			pointIndex = index;
			while (pointIndex > 0 && values.get(pointIndex - 1).getMinX() == placement.getAbsoluteX()) {
				pointIndex--;
			}
		} else {
			pointIndex = 0;
		}
		int endIndex = binarySearchPlusMinX(placement.getAbsoluteEndX());

		for (int i = pointIndex; i < endIndex; i++) {
			Point2D point = values.get(i);

			if(point.getMinY() > placement.getAbsoluteEndY()) {
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

			if(point.getMinX() >= source.getMinX() && point.getMinY() >= source.getMinY()) {

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

				values.flag(i);

				continue;
			}

			if(xxSupport && yySupport) {
				// do not move points to xx, yy, or zz
				// 
				// |
				// |          ║
				// |          ║
				// |          ║------|
				// |   *      ║      |
				// |      *   ║══════════
				// |                       
				// |    *    *    *   
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
		}

		if(!moveToXX.isEmpty()) {
			moveToXX.sortThis(COMPARATOR_MOVE_TO_XX);

			add: for (int i = 0; i < moveToXX.size(); i++) {
				SimplePoint2D p = values.get(moveToXX.get(i));
				// add point on the other side
				// with x support
				for (int k = 0; k < addXX.size(); k++) {
					Point2D add = addXX.get(k);
					if(add.eclipsesMovedX(p, xx)) {
						continue add;
					}
				}

				// note: the new point might shadow one of the previous points
				SimplePoint2D moveX = p.moveX(xx, placement);
				addXX.add(moveX);
			}
		}

		if(!moveToYY.isEmpty()) {
			moveToYY.sortThis(COMPARATOR_MOVE_TO_YY);

			add: for (int i = 0; i < moveToYY.size(); i++) {
				SimplePoint2D p = values.get(moveToYY.get(i));

				// add point on the other side
				// with x support
				for (int k = 0; k < addYY.size(); k++) {
					Point2D add = addYY.get(k);
					if(add.eclipsesMovedY(p, yy)) {
						continue add;
					}
				}

				SimplePoint2D moveY = p.moveY(yy, placement);
				addYY.add(moveY);
			}
		}

		if(xxSupport && yySupport) {
			// no constrain necessary
		} else if(yySupport) {

			//
			// vmaxY |                    
			//       |          
			//  yy   |        ║ - - - |
			//       |        ║
			//       |        ║       |
			//       |        ║              
			//       |        ║       |
			//       |        ║  
			//  minY |        x - - - |
			//       |          *   *
			//       |        *   *
			//       |          *    *
			//       |-----------------------------
			//               minX    maxX
			//   
			// so only those points between 0 and minY 
			// and between minX and maxX need to be constrained

			if(cloneOnConstrain) {
				constrainMaxYWithClone(placement, pointIndex, endIndex);
			} else {
				constrainMaxY(placement, pointIndex, endIndex);
			}
		} else if(xxSupport) {

			//       |
			//       |
			//       |     *  |---------------| 
			//       | *     
			//       |    *   |               |
			//       |  * 
			//  minY |    *   x════════════════        
			//       |                     
			//       |                    
			//       |--------------------------
			//               minX             maxX
			//
			// so only those points between 0 and minX 
			// and between minY and maxY need to be constrained

			pointIndex = index;
			while (pointIndex > 0 && values.get(pointIndex - 1).getMinX() == placement.getAbsoluteX()) {
				pointIndex--;
			}

			if(cloneOnConstrain) {
				constrainMaxXWithClone(placement, 0, pointIndex);
			} else {
				constrainMaxX(placement, 0, pointIndex);
			}
		} else {
			if(cloneOnConstrain) {
				constrainFloatingMaxWithClone(placement, endIndex);
			} else {
				constrainFloatingMax(placement, endIndex);
			}
		}

		endIndex -= values.removeFlagged();

		placements.add(placement);

		int added = addXX.size() + addYY.size();
		// the new points have x coordinate between zero and xx. 
		// insert them at the start of the existing data
		// so that the sorting algorithm does not have to do a full sort
		// rather only sort points with x coordinates from 0 to xx.
		values.ensureAdditionalCapacity(added);

		// insert xx last, because it has the highest x coordinate
		values.move(added);
		values.setAll(addYY, 0);
		values.setAll(addXX, addYY.size());

		removeEclipsed(added);

		endIndex += added - values.removeFlagged();

		// make sure to capture all point <= xx
		while (endIndex < values.size() && values.get(endIndex).getMinX() <= xx) {
			endIndex++;
		}

		values.sort(Point2D.COMPARATOR_X_THEN_Y, endIndex);

		moveToXX.clear();
		moveToYY.clear();

		addXX.clear();
		addYY.clear();
		
		updateIndexes(values);

		return !values.isEmpty();
	}

	private void constrainMaxXWithClone(Placement placement, int pointIndex, int endIndex) {
		for (int i = pointIndex; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point2D point = values.get(i);
			if(point.getMinX() < placement.getAbsoluteX() && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() >= placement.getAbsoluteX()) {
					int limitX = placement.getAbsoluteX() - 1;
					if(!isConstrainedAtMaxX(point, limitX)) {
						Point2D clone = point.clone(limitX, point.getMaxY());

						addXX.add(clone);
					}

					values.flag(i);
				}
			}
		}
	}

	private void constrainMaxYWithClone(Placement placement, int pointIndex, int endIndex) {
		for (int i = pointIndex; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point2D point = values.get(i);
			if(point.getMinY() < placement.getAbsoluteY() && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() >= placement.getAbsoluteY()) {
					int limitY = placement.getAbsoluteY() - 1;
					if(!isConstrainedAtMaxY(point, limitY)) {
						Point2D clone = point.clone(point.getMaxX(), limitY);

						addXX.add(clone);
					}
					values.flag(i);
				}
			}
		}
	}

	private void constrainMaxX(Placement placement, int pointIndex, int endIndex) {
		for (int i = pointIndex; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point2D point = values.get(i);
			if(point.getMinX() < placement.getAbsoluteX() && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() >= placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);
					if(point.getArea() < minAreaLimit) {
						values.flag(i);
					}
				}
			}
		}
	}

	private void constrainMaxY(Placement placement, int pointIndex, int endIndex) {
		for (int i = pointIndex; i < endIndex; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point2D point = values.get(i);
			if(point.getMinY() < placement.getAbsoluteY() && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() >= placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);
					if(point.getArea() < minAreaLimit) {
						values.flag(i);
					}
				}
			}
		}
	}

	protected void removeEclipsed(int limit) {

		//   unsorted        sorted
		// |   new    |   existing / current |
		// |----------|----------------------|--> x

		Point2DFlagList values = this.values;

		int size = values.size();

		added: for (int i = 0; i < limit; i++) {
			if(values.isFlag(i)) {
				continue;
			}
			Point2D unsorted = values.get(i);

			for (int index = limit; index < size; index++) {
				if(values.isFlag(index)) {
					continue;
				}
				Point2D sorted = values.get(index);
				if(sorted.getMinX() > unsorted.getMinX()) {
					// so sorted cannot contain unsorted
					// at this index or later
					break;
				}
				if(unsorted.getArea() <= sorted.getArea()) {
					if(sorted.eclipses(unsorted)) {
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

	protected void constrainFloatingMaxWithClone(Placement placement, int limit) {

		Point2DFlagList values = this.values;
		Point2DList addXX = this.addXX;
		Point2DList addYY = this.addYY;

		addXX.ensureAdditionalCapacity(limit);
		addYY.ensureAdditionalCapacity(limit);

		for (int i = 0; i < limit; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point2D point = values.get(i);

			if(
				placement.getAbsoluteEndY() < point.getMinY() ||
						placement.getAbsoluteEndX() < point.getMinX() ||
						placement.getAbsoluteX() > point.getMaxX() ||
						placement.getAbsoluteY() > point.getMaxY()
			) {
				continue;
			}

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

			addX: if(point.getMinX() < placement.getAbsoluteX()) {
				int limitX = placement.getAbsoluteX() - 1;
				if(!isConstrainedAtMaxX(point, limitX)) {
					Point2D clone = point.clone(limitX, point.getMaxY());

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point2D point3d = values.get(j);
						if(point3d.getMinX() > clone.getMinX()) {
							break;
						}

						if(point3d.getArea() >= clone.getArea()) {
							if(point3d.eclipses(clone)) {
								break addX;
							}
						}
					}

					// is the point now eclipsed by new points?
					for (int j = 0; j < addXX.size(); j++) {
						Point2D point3d = addXX.get(j);

						if(point3d.getArea() >= clone.getArea()) {
							if(point3d.eclipses(clone)) {
								break addX;
							}
						}
					}

					addXX.add(clone);
				}
			}

			addY: if(point.getMinY() < placement.getAbsoluteY()) {
				int limitY = placement.getAbsoluteY() - 1;
				if(!isConstrainedAtMaxY(point, limitY)) {
					Point2D clone = point.clone(point.getMaxX(), limitY);

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point2D point3d = values.get(j);
						if(point3d.getMinX() > clone.getMinX()) {
							break;
						}

						if(point3d.getArea() >= clone.getArea()) {
							if(point3d.eclipses(clone)) {
								break addY;
							}
						}
					}

					// is the point now eclipsed by new points?
					for (int j = 0; j < addYY.size(); j++) {
						Point2D point3d = addYY.get(j);

						if(point3d.getArea() >= clone.getArea()) {
							if(point3d.eclipses(clone)) {
								break addY;
							}
						}
					}

					addYY.add(clone);
				}
			}

			values.flag(i);
		}
	}

	protected void constrainFloatingMax(Placement placement, int limit) {

		Point2DFlagList values = this.values;
		Point2DList addXX = this.addXX;
		Point2DList addYY = this.addYY;
		long minAreaLimit = this.minAreaLimit;

		addXX.ensureAdditionalCapacity(limit);
		addYY.ensureAdditionalCapacity(limit);

		int startAddXX = addXX.size();
		int startAddYY = addYY.size();

		boolean splitXX = false;
		boolean splitYY = false;

		limitLoop: for (int i = 0; i < limit; i++) {
			if(values.isFlag(i)) {
				continue;
			}

			Point2D point = values.get(i);

			if(
				placement.getAbsoluteEndY() < point.getMinY() ||
						placement.getAbsoluteEndX() < point.getMinX() ||
						placement.getAbsoluteX() > point.getMaxX() ||
						placement.getAbsoluteY() > point.getMaxY()
			) {
				continue;
			}

			if(point.getMinY() >= placement.getAbsoluteY()) {
				// point is to the left of placement
				// adjusting max x is sufficient
				if(point.getMinX() < placement.getAbsoluteX()) {
					point.setMaxX(placement.getAbsoluteX() - 1);
					if(point.getArea() < minAreaLimit) {
						values.flag(i);

						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point2D point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}

						if(point3d.getArea() >= point.getArea()) {
							if(point3d.eclipses(point)) {
								values.flag(i);

								continue limitLoop;
							}
						}
					}

					if(splitXX) {
						// is the point now eclipsed by new points?
						for (int j = startAddXX; j < addXX.size(); j++) {
							Point2D point3d = addXX.get(j);

							if(point3d.getArea() >= point.getArea()) {
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

			if(point.getMinX() >= placement.getAbsoluteX()) {
				// point is below placement
				// adjusting max y is sufficient
				if(point.getMinY() < placement.getAbsoluteY()) {
					point.setMaxY(placement.getAbsoluteY() - 1);

					if(point.getArea() < minAreaLimit) {
						values.flag(i);

						continue;
					}

					// is the point now eclipsed by current points?
					for (int j = 0; j < i - 1; j++) {
						if(values.isFlag(j)) {
							continue;
						}
						Point2D point3d = values.get(j);
						if(point3d.getMinX() > point.getMinX()) {
							break;
						}

						if(point3d.getArea() >= point.getArea()) {
							if(point3d.eclipses(point)) {
								values.flag(i);

								continue limitLoop;
							}
						}
					}

					if(splitYY) {
						// is the point now eclipsed by new points?
						for (int j = startAddYY; j < addYY.size(); j++) {
							Point2D point3d = addYY.get(j);

							if(point3d.getArea() >= point.getArea()) {
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
			//
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

			if(!isConstrainedAtMaxX(point, placement.getAbsoluteX() - 1)) {
				Point2D clone = point.clone(placement.getAbsoluteX() - 1, point.getMaxY());
				addXX.add(clone);

				splitXX = true;
			}
			if(!isConstrainedAtMaxY(point, placement.getAbsoluteY() - 1)) {
				Point2D clone = point.clone(point.getMaxX(), placement.getAbsoluteY() - 1);
				addYY.add(clone);

				splitYY = true;
			}

			values.flag(i);
		}
	}

	private boolean isConstrainedAtMaxX(Point2D p, int maxX) {
		return p.getAreaAtMaxX(maxX) < minAreaLimit;
	}

	private boolean isConstrainedAtMaxY(Point2D p, int maxY) {
		return p.getAreaAtMaxY(maxY) < minAreaLimit;
	}

	protected boolean withinX(int x, Placement placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}

	protected boolean withinY(int y, Placement placement) {
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
		return "DefaultPointCalculator2D [" + containerMaxX + "x" + containerMaxY + ": " + values + "]";
	}

	public List<Placement> getPlacements() {
		return placements;
	}

	public SimplePoint2D get(int i) {
		return values.get(i);
	}

	public List<Point> getAll() {
		return values.toList();
	}

	public int size() {
		return values.size();
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
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			if(maxPointArea < point.getArea()) {
				maxPointArea = point.getArea();
			}
		}
		return maxPointArea;
	}

	public void redo() {
		values.clear();
		placements.clear();
	}

	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		redo();
	}

	public int findPoint(int x, int y) {
		for (int i = 0; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			if(point2d.getMinX() == x && point2d.getMinY() == y) {
				return i;
			}
		}
		return -1;
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

				// so if there was multiple points at key, we are at the index of the last of them, plus one.

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

	public void setMinimumAreaLimit(long minArea) {
		if(minAreaLimit != minArea) {
			this.minAreaLimit = minArea;
			filterMinimums();
		}
	}

	private void filterMinimums() {
		for (int i = 0; i < values.size(); i++) {
			Point2D p = values.get(i);

			if(p.getArea() < minAreaLimit) {
				values.flag(i);
			}
		}
		values.removeFlagged();
	}

	private boolean canMoveX(Point2D p, int xx) {
		if(p.getMaxX() < xx) {
			return false;
		}
		return p.getAreaAtX(xx) >= minAreaLimit;
	}

	private boolean canMoveY(Point2D p, int yy) {
		if(p.getMaxY() < yy) {
			return false;
		}
		return p.getAreaAtY(yy) >= minAreaLimit;
	}

	public long getMinAreaLimit() {
		return minAreaLimit;
	}

	public void remove(int index) {
		values.flag(index);
		values.removeFlagged();
	}

	@Override
	public Iterator<Point> iterator() {
		return values.iterator();
	}
	@Override
	public void clearToSize(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		clear();
	}

	@Override
	public void clear() {
		values.clear();
		placements.clear();
		
		if(initialPoints.isEmpty()) {
			SimplePoint2D origin = createContainerPoint();
			values.add(origin);
		} else {
			for (SimplePoint2D simplePoint3D : initialPoints) {
				SimplePoint2D clone = simplePoint3D.clone();
				clone.setIndex(values.size());
				values.add(clone);
			}
		}
		minAreaLimit = 0;
	}

	public long calculateUsedVolume() {
		long used = 0;
		for (Placement stackPlacement : placements) {
			used += stackPlacement.getStackValue().getBox().getVolume();
		}
		return used;
	}
	
	public long calculateUsedWeight() {
		long used = 0;
		for (Placement stackPlacement : placements) {
			used += stackPlacement.getStackValue().getBox().getWeight();
		}
		return used;
	}

	
	public void setPoints(List<Point> points) {
		// transform coordinates to internal representation, i.e. with support etc
		initialPoints = new ArrayList<>(points.size());
		
		for(Point p: points) {
			boolean yzPlane = p.getMinX() == 0; // ySupport
			boolean xzPlane = p.getMinY() == 0; // xSupport
			
			if(p.getMaxX() > containerMaxX) {
				throw new IllegalArgumentException();
			}
			if(p.getMaxY() > containerMaxY) {
				throw new IllegalArgumentException();
			}
			if(yzPlane && xzPlane) {
				initialPoints.add(new DefaultXYSupportPoint2D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement, containerPlacement));
			} else if(xzPlane) {
				initialPoints.add(new DefaultXSupportPoint2D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement));
			} else if(yzPlane) {
				initialPoints.add(new DefaultYSupportPoint2D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ(), containerPlacement));
			} else {
				initialPoints.add(new DefaultPoint2D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ()));
			}
		}
		
		for(int i = 0; i < initialPoints.size(); i++) {
			initialPoints.get(i).setIndex(i);
		}
	}


	public int binarySearch(Point point, int low) {
		// return inclusive result
		
		int key = point.getMinX();

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
				SimplePoint2D simplePoint = values.get(mid);
				if(simplePoint == point) {
					return mid;
				}
				
				int compare = Point.COMPARATOR_X_THEN_Y.compare(point, simplePoint);
				if(compare <= 0) {
					// check below
					do {
						mid--;
						if(mid < 0) {
							throw new IllegalStateException("Cannot locate point " + point);
						}
						if(values.get(mid) == point) {
							return mid;
						}
					} while(true);
				}  
					
				if(compare >= 0) {
					// check above
					do {
						mid++;
						if(mid == values.size()) {
							throw new IllegalStateException("Cannot locate point " + point);
						}
						if(values.get(mid) == point) {
							return mid;
						}
					} while(true);
				}
				
				throw new IllegalStateException("Cannot locate point " + point);
			}
		}
		// key not found
		return low;
	}
	
	protected void updateIndexes(Point2DFlagList values) {
		for(int i = 0; i < values.size(); i++) {
			SimplePoint2D p = values.get(i);
			if(p.getIndex() != i) {
				p.setIndex(i);
			}
		}
	}

	public void updateMinimums(BoxStackValue stackValue, BoxItemSource filteredBoxItems) {
		boolean minArea = stackValue.getArea() == minAreaLimit;
		if(minArea) {
			setMinimumAreaLimit(filteredBoxItems.getMinArea());
		}
	}
	
	
	public void updateMinimums(BoxStackValue stackValue, BoxItemGroupSource filteredBoxItemGroups) {
		boolean minArea = stackValue.getArea() == minAreaLimit;
		if(minArea) {
			setMinimumAreaLimit(filteredBoxItemGroups.getMinArea());
		}
	}

	@Override
	public void setMinimumAreaAndVolumeLimit(long area, long volume) {
		setMinimumAreaLimit(area);
	}

	@Override
	public void remove(Predicate<Point> test) {
		for(int i = 0; i < values.size(); i++) {
			if(!test.test(values.get(i))) {
				values.flag(i);
			}
		}
		values.removeFlagged();
	}
}
