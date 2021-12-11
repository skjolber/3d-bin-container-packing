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

public class ExtremePoints2D<P extends Placement2D> implements ExtremePoints<P, Point2D> {
	
	protected int containerMaxX;
	protected int containerMaxY;

	protected final List<Point2D> values = new ArrayList<>();
	protected final List<P> placements = new ArrayList<>();
	
	/** optimization: avoid some extra max searches */
	protected final List<P> floatingPlacements = new ArrayList<>();

	// reuse working variables
	protected final List<Point2D> deleted = new ArrayList<>();
	protected final List<Point2D> addY = new ArrayList<>();
	protected final List<Point2D> addX = new ArrayList<>();

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
		// project points points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side
		// add points shadowed by the two new points (if they could be moved in the negative direction)
		// remove points which are eclipsed by others
		
		// keep track of placement borders, where possible
		Point2D source = values.get(index);
		
		boolean hasSupport = source instanceof XSupportPoint2D || source instanceof YSupportPoint2D;

		if(!hasSupport) {
			System.out.println("Floating " + placement + " at " + index);
			floatingPlacements.add(placement);
		}

		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		
		boolean moveX = xx <= containerMaxX;
		boolean moveY = yy <= containerMaxY;

		if(moveX || moveY) { 
			boolean xSupport = source.isXSupport(xx); // i.e. is source minY also minY at XX?
			boolean xEdge = source.isXEdge(xx);
			
			boolean ySupport = source.isYSupport(yy); // i.e. is source minX also minX at YY?
			boolean yEdge = source.isYEdge(yy);
	
			if(moveX) {
				int maxY = constrainIfNotMaxY(source, xx);
				if(xSupport) {
					DefaultXYSupportPoint2D supported = getSupportedAtXX(source, placement, xx, yy, maxY);
					if(supported != null) {
						if(!floatingPlacements.isEmpty()) {
							addOutOfBoundsXX(source, supported);
						} else {
							addX.add(supported);
						}
					}
					if(ySupport) {
						appendSwallowedAtXX(placement, source, xx, yy, addX.isEmpty());
					} else {
						appendSwallowedOrShadowedAtXX(placement, source, xx, yy, addX.isEmpty());
					}
				} else if(xEdge) {
					if(ySupport) {
						appendSwallowedAtXX(placement, source, xx, yy, true);
					} else {
						appendSwallowedOrShadowedAtXX(placement, source, xx, yy, true);
					}
				} else {
					Point2D dx = projectNegativeYAtXX(source, placement, xx, yy, maxY);
					if(dx != null) {
						if(!floatingPlacements.isEmpty()) {
							addOutOfBoundsXX(source, dx);
						} else {
							addX.add(dx);
						}
						
						appendFirstNegativeShadowXX(source, xx, dx);
					}
					
					appendSwallowedOrShadowedAtXX(placement, source, xx, yy, true);
					
					removeIdentialMaxAtXX();

					raiseToXX(placement, xx, yy, maxY);
				}
				removeEclipsedXX();
			}

			if(moveY) {
				int maxX = constrainIfNotMaxX(source, yy);
				if(ySupport || yEdge) {
					DefaultXYSupportPoint2D supported = getSupportedAtYY(source, placement, xx, yy, maxX);
					if(supported != null) {
						if(!floatingPlacements.isEmpty()) {
							addOutOfBoundsYY(source, supported);
						} else {
							addY.add(supported);
						}
					}
					if(xSupport) {
						appendSwallowedAtYY(placement, source, xx, yy, addY.isEmpty());
					} else {
						appendSwallowedOrShadowedAtYY(placement, source, xx, yy, addY.isEmpty());
					}
				} else if(yEdge) {
					if(xSupport) {
						appendSwallowedAtYY(placement, source, xx, yy, true);
					} else {
						appendSwallowedOrShadowedAtYY(placement, source, xx, yy, true);
					}
				} else {
					Point2D dy = projectNegativeXAtYY(source, placement, xx, yy, maxX);
					if(dy != null) {
						if(!floatingPlacements.isEmpty()) {
							addOutOfBoundsYY(source, dy);
						} else {
							addY.add(dy);
						}
						
						appendFirstNegativeShadowYY(source, yy, dy);
					}

					appendSwallowedOrShadowedAtYY(placement, source, xx, yy, true);
					
					removeIdenticalMaxAtYY();

					raiseToYY(placement, xx, yy, maxX);
				}
				removeEclipsedYY();
			}
			
			deleted.add(source);
			values.removeAll(deleted);
		} else {
			values.remove(source);

			// remove points swallowed
			for(int i = 0; i < values.size(); i++) {
				Point2D point2d = values.get(i);
				if(point2d.swallowsMinX(placement.getAbsoluteX(), placement.getAbsoluteEndX()) && point2d.swallowsMinY(placement.getAbsoluteY(), placement.getAbsoluteEndY())) {
					values.remove(i);
					i--;
				}
			}
		}
	
		for(int i = 0; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			
			boolean remove;
			if(hasSupport) {
				remove = !constrainPositiveMax(point2d, placement);
			} else {
				remove = !constrainFloatingMax(point2d, placement);
			}
			if(remove) {
				values.remove(i);
				i--;
			}
		}
		
		removeEclipsed();

		addX();
		addY();

		Collections.sort(values, Point2D.COMPARATOR);
		
		placements.add(placement);
		
		addX.clear();
		addY.clear();
		deleted.clear();

		return !values.isEmpty();
	}

	private void addOutOfBoundsYY(Point2D source, Point2D dy) {
		if(dy.getMaxX() > source.getMaxX() || dy.getMinX() < source.getMinX()) {
			// outside the known limits of the source point
			
			int maxY = constrainFloatingY(dy.getMinX(), dy.getMinY(), dy.getMaxY());
			int maxX = constrainFloatingX(dy.getMinX(), dy.getMaxY(), dy.getMaxX());

			if(maxY < dy.getMaxY() && maxX < dy.getMaxX()) {
				// split in two

				addY.add(dy.clone(maxX, dy.getMaxY()));
				addY.add(dy.clone(dy.getMaxX(), maxY));
			} else {
				// just constrain
				if(maxY < dy.getMaxY()) {
					dy.setMaxY(maxY);
				}
				if(maxX < dy.getMaxX()) {
					dy.setMaxX(maxX);
				}
				addY.add(dy);
			}
		} else {
			addY.add(dy);
		}
	}

	private void addOutOfBoundsXX(Point2D source, Point2D dx) {
		if(dx.getMaxY() > source.getMaxY() || dx.getMinY() < source.getMinY()) {
			// outside the known limits of the source point

			int maxY = constrainFloatingY(dx.getMaxX(), dx.getMinY(), dx.getMaxY());
			int maxX = constrainFloatingX(dx.getMinX(), dx.getMaxY(), dx.getMaxX());

			if(maxY < dx.getMaxY() && maxX < dx.getMaxX()) {
				// split in two

				addX.add(dx.clone(maxX, dx.getMaxY()));
				addX.add(dx.clone(dx.getMaxX(), maxY));
			} else {
				// just constrain
				if(maxY < dx.getMaxY()) {
					dx.setMaxY(maxY);
				}
				if(maxX < dx.getMaxX()) {
					dx.setMaxX(maxX);
				}
				addX.add(dx);
			}
		} else {
			addX.add(dx);
		}
	}
	
	protected void addX() {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			for (int i = 0; i < addX.size(); i++) {
				Point2D p1 = addX.get(i);

				if(existing.eclipses(p1)) {
					addX.remove(i);
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
		
		values.addAll(addX);	
	}
	
	protected void addY() {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			for (int i = 0; i < addY.size(); i++) {
				Point2D p1 = addY.get(i);
				
				if(existing.eclipses(p1)) {
					addY.remove(i);
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
		
		values.addAll(addY);	
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
						addX.add(point);
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
						addY.add(point);
					}
				}							
			}
		}
	}

	protected void appendSwallowedOrShadowedAtXX(P placement, Point2D source, int xx, int yy, boolean includeSource) {
		
		// Move points swallowed or shadowed by the placement
		
		//    swallowed:
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    |         *
		//    |         | 
		//    |---------|---------------			
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
			if(point.isShadowedOrSwallowedByX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support

					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), point.getMaxY(),
							placement
							);
					
					addX.add(next);
				}
				
				if(point.getMinX() < source.getMinX()) {
					// constrain current point
					point.setMaxX(source.getMinX() - 1);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
	}

	private void raiseToXX(P placement, int xx, int yy, int maxY) {
		// complete adding
		// if some were only shadowed, add a new point
		for (int i = 0; i < addX.size(); i++) {
			Point2D point = addX.get(i);					
			
			if(point.getMinX() < xx) {
				Point2D p;
				if(point.getMinY() < placement.getAbsoluteY()) {
					p = new DefaultPoint2D(xx, point.getMinY(), point.getMaxX(), maxY);
				} else {
					p = new DefaultYSupportPoint2D(xx, point.getMinY(), point.getMaxX(), maxY, placement);
				}

				addX.set(i, p);
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
	
	protected void appendSwallowedAtXX(P placement, Point2D source, int xx, int yy, boolean includeSource) {
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
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			if(point == source && !includeSource) {
				continue;
			}
			// Move points swallowed by the placement
			if(point.swallowsMinX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() >= xx) {
					// add point on the other side
					// vertical support
					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), point.getMaxY(),
							placement
							);

					addX.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
		// removeIdentialMaxAtXX(added);
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
					
					addY.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}

		// removeIdenticalMaxAtYY(added);
	}
	
	protected void removeEclipsedYY() {
		Collections.sort(addY, Point2D.X_COMPARATOR);

		for(int index = 0; index < addY.size(); index++) {
			Point2D lowest = addY.get(index);
			for (int i = index + 1; i < addY.size(); i++) {
				Point2D p1 = addY.get(i);

				if(lowest.eclipses(p1)) {
					addY.remove(i);
					i--;
				}
			}
		}
	}

	protected void removeEclipsedXX() {
		Collections.sort(addX, Point2D.Y_COMPARATOR);

		for(int index = 0; index < addX.size(); index++) {
			Point2D lowest = addX.get(index);
			for (int i = index + 1; i < addX.size(); i++) {
				Point2D p1 = addX.get(i);

				if(lowest.eclipses(p1)) {
					addX.remove(i);
					i--;
				}
			}
		}
	}
	
	protected void removeEclipsed() {
		for(int index = 0; index < values.size(); index++) {
			Point2D lowest = values.get(index);
			for (int i = index + 1; i < values.size(); i++) {
				Point2D p1 = values.get(i);

				if(lowest.eclipses(p1)) {
					values.remove(i);
					i--;
				}
			}
		}
	}

	protected void removeIdentialMaxAtXX() {
		// x coordinate fixed
		Collections.sort(addX, Point2D.Y_COMPARATOR);
		
		removeIdenticalMax(addX);
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

					addY.add(next);
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
		for (int i = 0; i < addY.size(); i++) {
			Point2D point = addY.get(i);					
			
			if(point.getMinY() < yy) {
				Point2D p;
				if(point.getMinX() < placement.getAbsoluteX()) {
					p = new DefaultPoint2D(point.getMinX(), yy, maxX, point.getMaxY());
				} else {
					p = new DefaultXSupportPoint2D(point.getMinX(), yy, maxX, point.getMaxY(), placement);
				}

				addY.set(i, p);
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
		Collections.sort(addY, Point2D.X_COMPARATOR);

		removeIdenticalMax(addY);
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

	protected int constrainFloatingX(int x, int y, int maxX) {
		// constrain up
		P closestX = closestPositiveFloatingX(x, y);
		if(closestX != null) {
			if(closestX.getAbsoluteX() - 1 < maxX) {
				return closestX.getAbsoluteX() - 1;
			}
		}
		return maxX;
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
	
	protected int constrainFloatingY(int x, int y, int maxY) {
		// constrain up
		P closestY = closestPositiveFloatingY(x, y);
		if(closestY != null) {
			if(closestY.getAbsoluteY() - 1 < maxY) {
				return closestY.getAbsoluteY() - 1;
			};
		}
		return maxY;
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
			addX.add(point.clone(placement.getAbsoluteX() - 1, point.getMaxY()));
		}
		
		if(y) {
			addY.add(point.clone(point.getMaxX(), placement.getAbsoluteY() - 1));
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
	
	protected P closestPositiveFloatingY(int x, int y) {
		P closest = null;
		for (P placement : floatingPlacements) {
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
	
	protected P closestPositiveFloatingX(int x, int y) {
		P closest = null;
		for (P placement : floatingPlacements) {
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
		floatingPlacements.clear();
		
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
