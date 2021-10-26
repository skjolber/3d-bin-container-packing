package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

/**
 * 
 * Implementation of so-called extreme points in 2D.
 *
 */

public class ExtremePoints2D<P extends Placement2D> {
	
	protected final int containerMaxX;
	protected final int containerMaxY;

	protected List<Point2D> values = new ArrayList<>();
	protected List<P> placements = new ArrayList<>();

	public ExtremePoints2D(int dx, int dy) {
		super();
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		
		values.add(new DefaultXYSupportPoint2D(0, 0, containerMaxX, containerMaxY, 0, containerMaxX, 0, containerMaxY));
	}

	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {
		
		System.out.println("Add " + placement);
		for(Point2D v : values) {
			System.out.println(" " + v);
		}
		
		// overall approach:
		//
		// project points points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side, then constrain them
		// add points shadowed by the two new points (if they could be moved in the negative direction)

		// keep track of placement borders, where possible
		
		Point2D source = values.get(index);
		
		List<Point2D> deleted = new ArrayList<>();
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;

		boolean moveX = xx <= containerMaxX;
		boolean moveY = yy <= containerMaxY;
		
		if(moveX || moveY) { 
		
			boolean xSupport = source.isXSupport(xx); // i.e. is minY the y coordinate?
			boolean xEdge = source.isXEdge(xx);
			
			boolean ySupport = source.isYSupport(yy); // i.e. is minX the x coordinate?
			boolean yEdge = source.isYEdge(yy);
	
			List<Point2D> addX = new ArrayList<>();
			if(moveX) {
				int maxY = constrainIfNotMaxY(source, xx);
				if(xSupport || xEdge) {
					if(xSupport) {
						DefaultXYSupportPoint2D addSupportedX = addSupportedAtXX(source, xx, yy, maxY);
						if(addSupportedX != null) {
							addX.add(addSupportedX);
						}
					}
					if(ySupport) {
						addSwallowedXX(placement, source, deleted, xx, yy, addX);
					} else {
						addSwallowedOrShadowedAtXX(placement, source, deleted, xx, yy, addX, maxY);
					}
				} else {
					addAtXX(placement, source, deleted, xx, yy, maxY, addX);
					
					addSwallowedOrShadowedAtXX(placement, source, deleted, xx, yy, addX, maxY);
				}
			}
	
			List<Point2D> addY = new ArrayList<>();
			if(moveY) {
				int maxX = constrainIfNotMaxX(source, yy);
				if(ySupport || yEdge) {
					if(ySupport) {
						DefaultXYSupportPoint2D addSupportedY = addSupportedAtYY(source, xx, yy, maxX);
						if(addSupportedY != null) {
							addY.add(addSupportedY);
						}
					}
					if(xSupport) {
						addSwallowedYY(placement, source, deleted, xx, yy, addY);
					} else {
						addSwallowedOrShadowedAtYY(placement, source, deleted, xx, yy, addY, maxX);
					}
		
				} else {
					addAtYY(placement, source, deleted, xx, yy, maxX, addY);
					
					addSwallowedOrShadowedAtYY(placement, source, deleted, xx, yy, addY, maxX);
				}
			}
			
			deleted.add(source);
			values.removeAll(deleted);
			
			boolean supported = source instanceof XSupportPoint2D || source instanceof YSupportPoint2D;
			
			for(int i = 0; i < values.size(); i++) {
				Point2D point2d = values.get(i);
				
				boolean remove;
				if(supported) {
					remove = !constrainPositiveMax(point2d, placement);
				} else {
					remove = !constrainPositiveNegativeMax(point2d, placement, addX, addY);
				}
				if(remove) {
					values.remove(i);
					i--;
				}
			}
			
			if(xSupport || xEdge) {
				addX(addX, xx);
			} else {
				values.addAll(addX);
			}
	
			if(ySupport || yEdge) {
				addY(addY, yy);
			} else {
				values.addAll(addY);
			}
	
			placements.add(placement);
			Collections.sort(values, Point2D.COMPARATOR);
		} else {
			values.remove(source);
		}

		return !values.isEmpty();
	}

	protected void addX(List<Point2D> add, int x) {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			if(existing.getMinX() == x) {
				for (int i = 0; i < add.size(); i++) {
					Point2D p1 = add.get(i);

					// is the new point shadowed, or shadowing an existing point?
					if(p1.getMaxY() == existing.getMaxY() && p1.getMaxX() == existing.getMaxX()) {
						add.remove(i);
						i--;

						if(p1.getMinY() < existing.getMinY()) {
							values.set(index, p1);
							existing = p1;
						}
					}
				}
			}
			index++;
		}
		
		values.addAll(add);	
	}
	
	protected void addY(List<Point2D> add, int y) {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			if(existing.getMinY() == y) {
				for (int i = 0; i < add.size(); i++) {
					Point2D p1 = add.get(i);
					
					// is the new point shadowed, or shadowing an existing point?
					if(p1.getMaxY() == existing.getMaxY() && p1.getMaxX() == existing.getMaxX()) {
						add.remove(i);
						i--;

						if(p1.getMinX() < existing.getMinX()) {
							values.set(index, p1);
							existing = p1;
						}
					}

				}
			}
			index++;
		}
		
		values.addAll(add);	
	}

	protected void addAtXX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, int maxY, List<Point2D> added) {
		Point2D dx = unsupportedAtXX(source, xx, yy, maxY);
		if(dx != null) {
			if(constrainMaxX(dx)) {
				added.add(dx);
				
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
							
						if(point.innerY(dx.getMinY(), source.getMinY())) { // vertical constraint
							if(point.crossesX(xx)) { // horizontal constraint (crosses xx)'
								added.add(point);
							}
						}
					}
				}
			}
		}
	}

	protected void addSwallowedOrShadowedAtXX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy,
			List<Point2D> added, int maxY) {
		
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
		
			// Move points swallowed or shadowed by the placement
			if(point.shadowedOrSwallowedX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support

					// TODO contrain max y per point?
					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), maxY,
							xx, point.getMinY()
							);
					
					added.add(next);
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
		
		removeShadowedY(added);
		
		// complete adding
		// if some were only shadowed, add a new point
		for (int i = 0; i < added.size(); i++) {
			Point2D point = added.get(i);					
			
			if(point.getMinX() < xx) {
				Point2D p;
				if(point.getMinY() < placement.getAbsoluteY()) {
					p = new DefaultPoint2D(xx, point.getMinY(), point.getMaxX(), maxY);
				} else {
					p = new DefaultYSupportPoint2D(xx, point.getMinY(), point.getMaxX(), maxY, point.getMinY(), yy);
				}

				added.set(i, p);
			}
		}
	}

	protected DefaultXYSupportPoint2D addSupportedAtXX(Point2D source, int xx, int yy, int maxY) {
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
		
		if(source.getMaxX() > xx) {
			XSupportPoint2D fixedPointY = (XSupportPoint2D)source;
			
			if(maxY > source.getMinY()) {
				return new DefaultXYSupportPoint2D(xx, source.getMinY(), source.getMaxX(), maxY, xx, fixedPointY.getXSupportMaxX(), source.getMinY(), yy - 1);
			}
		}
		return null;
	}
	
	protected void addSwallowedXX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, List<Point2D> added) {
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
		
		int maxY = constrainIfNotMaxY(source, xx);
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);

			// Move points swallowed by the placement
			if(point.swallowedX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support

					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), maxY,
							xx, point.getMinY()
							);

					added.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
		removeShadowedY(added);
	}
	
	protected void addSwallowedYY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, List<Point2D> added) {
		//    swallowed:
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    |         |
		//    |         | 
		//    |---------|---------------
		//
		//

		int maxX = constrainIfNotMaxX(source, yy);

		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
		
			// Move points swallowed by the placement
			if(point.swallowedY(source.getMinY(), yy) && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() > yy) {
					// add point
					// horizontal support
					
					DefaultXSupportPoint2D next = new DefaultXSupportPoint2D(
							point.getMinX(), yy, 
							maxX, point.getMaxY(), 
							point.getMinX(), xx
							);
					
					added.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}

		removeShadowedX(added);
	}
	

	protected void removeShadowedY(List<Point2D> added) {
		Collections.sort(added, Point2D.Y_COMPARATOR);

		removeShadowed(added);
	}

	protected void removeShadowed(List<Point2D> added) {
		// remove those points which have the same extreme points (they have the same x coordinate)
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
		int maxY;
		if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
			maxY = containerMaxY;
		} else {
			maxY = constrainY(x, source.getMinY());
		}
		return maxY;
	}

	protected List<Point2D> addAtYY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, int maxX, List<Point2D> added) {
		// using dy
		Point2D negativeX = unsupportedAtYY(source, xx, yy, maxX);
		if(negativeX != null) {
			
			if(constrainMaxY(negativeX)) {
				added.add(negativeX);

				if(negativeX.getMinX() < source.getMinX()) {
				
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
						if(point.innerX(negativeX.getMinX(), source.getMinX())) { // vertical constraint
							if(point.crossesY(yy)) { // horizontal constraint (crosses xx)
								added.add(point);
							}
						}							
					}
				}
			}
		}
				
		return added;
	}

	protected void addSwallowedOrShadowedAtYY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, List<Point2D> added, int maxX) {
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
		// point minY  |---------|----*---------- <-- constrained
		//
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
		
			// Move points swallowed or shadowed by the placement
			if(point.shadowedOrSwallowedY(source.getMinY(), yy) && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() > yy) {
					// add point
					// horizontal support
					
					DefaultXSupportPoint2D next = new DefaultXSupportPoint2D(
							point.getMinX(), yy, 
							maxX, point.getMaxY(), 
							point.getMinX(), xx
							);

					added.add(next);
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
		
		removeShadowedX(added);

		for (int i = 0; i < added.size(); i++) {
			Point2D point = added.get(i);					
			
			if(point.getMinY() < yy) {
				Point2D p;
				if(point.getMinX() < placement.getAbsoluteX()) {
					p = new DefaultPoint2D(point.getMinX(), yy, maxX, point.getMaxY());
				} else {
					p = new DefaultXSupportPoint2D(point.getMinX(), yy, maxX, point.getMaxY(), point.getMinX(), xx);
				}

				added.set(i, p);
			}
		}
	}
	
	protected DefaultXYSupportPoint2D addSupportedAtYY(Point2D source, int xx, int yy, int maxX) {
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
		
		if(source.getMaxY() > yy) {
			YSupportPoint2D fixedPointX = (YSupportPoint2D)source;
			
			if(maxX > source.getMinX()) {
				return new DefaultXYSupportPoint2D(source.getMinX(), yy, maxX, source.getMaxY(), source.getMinX(), xx - 1, yy, fixedPointX.getYSupportMaxY());
			}
		}
		return null;
	}

	protected void removeShadowedX(List<Point2D> added) {
		Collections.sort(added, Point2D.X_COMPARATOR);

		removeShadowed(added);
	}

	protected int constrainIfNotMaxX(Point2D source, int yy) {
		int maxX;
		if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
			maxX = containerMaxX;
		} else {
			maxX = constrainX(source.getMinX(), yy);
		}
		return maxX;
	}

	protected boolean constrainMaxX(Point2D point) {
		// constrain to right
		P closestX = closestPositiveX(point.getMinX(), point.getMinY());
		if(closestX != null) {
			point.setMaxX(closestX.getAbsoluteX() - 1);
		} else {
			point.setMaxX(containerMaxX);
		}
		if(point.getMaxX() <= point.getMinX()) {
			return false;
		}

		return true;
	}
	
	protected boolean constrainMaxY(Point2D point) {
		// constrain up
		P closestY = closestPositiveY(point.getMinX(), point.getMinY());
		if(closestY != null) {
			point.setMaxY(closestY.getAbsoluteY() - 1);
		} else {
			point.setMaxY(containerMaxY);
		}
		if(point.getMaxY() <= point.getMinY()) {
			return false;
		}
		return true;
	}
	
	protected Point2D unsupportedAtXX(Point2D source, int xx, int yy, int maxY) {
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
			
			return new DefaultXSupportPoint2D(xx, 0, containerMaxX, maxY, xx, containerMaxX);
			
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

			return new DefaultXSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, maxY, xx, moveY.getAbsoluteEndX());
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

			return new DefaultXYSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, maxY, xx, moveY.getAbsoluteEndX(), moveY.getAbsoluteEndY(), yy);
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

	protected Point2D unsupportedAtYY(Point2D source, int xx, int yy, int maxX) {
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
			
			return new DefaultYSupportPoint2D(0, yy, maxX, containerMaxY, yy, containerMaxY);
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
			
			return new DefaultYSupportPoint2D(moveX.getAbsoluteEndX() + 1, yy, maxX, containerMaxY, yy, moveX.getAbsoluteEndY());

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
			
			return new DefaultXYSupportPoint2D(moveX.getAbsoluteX() + 1, yy, maxX, containerMaxY,  moveX.getAbsoluteEndX(), xx, yy, moveX.getAbsoluteEndY());
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


	protected boolean constrainMaxX(Point2D point, P placement) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY(), placement, point.getMaxX());
		if(maxX <= point.getMinX()) {
			return false;
		}
		point.setMaxX(maxX);

		return true;
	}
	
	protected boolean constrainMaxY(Point2D point, P placement) {
		int maxY = projectPositiveY(point.getMinX(), point.getMinY(), placement, point.getMaxY());
		if(maxY <= point.getMinY()) {
			return false;
		}
		
		point.setMaxY(maxY);

		return true;
	}
	
	protected boolean constrainPositiveNegativeMax(Point2D point, P placement, List<Point2D> addX, List<Point2D> addY) {

		System.out.println("Constraint positive negative max");
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

		boolean yLine = placement.getAbsoluteX() <= point.getMinX();
		if(yLine) { 
			int limit = placement.getAbsoluteY() - 1;
			if(limit <= point.getMinY()) {
				return false;
			}
			if(point.getMaxY() > limit) {
				point.setMaxY(limit);
			}
		}
		
		boolean xLine = placement.getAbsoluteY() <= point.getMinY();
		if(xLine) {
			int limit = placement.getAbsoluteX() - 1;
			if(limit <= point.getMinX()) {
				return false;
			}
			if(point.getMaxX() > limit) {
				point.setMaxX(limit);
			}
			
		}
		
		if(!xLine && !yLine) {
			// placement is 'floating' in the x-y quadrant
			// between max and min points
			
			System.out.println("FLOATING");
			
			addX.add(point.clone(point.getMaxX(), placement.getAbsoluteY()));
			addY.add(point.clone(placement.getAbsoluteX(), point.getMaxY()));
			
			return false;
		}
		
		return true;
	}	
	
	protected boolean constrainPositiveMax(Point2D point, P placement) {
		if(placement.getAbsoluteX() >= point.getMinX()) {
			if(withinY(point.getMinY(), placement)) {
				int limit = placement.getAbsoluteX() - 1;
				if(limit <= point.getMinX()) {
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
				if(limit <= point.getMinY()) {
					return false;
				}
				if(point.getMaxY() > limit) {
					point.setMaxY(limit);
				}
			}
		}
		
		if( !(point instanceof XSupportPoint2D || point instanceof YSupportPoint2D)) {
			// 
		}
		
		return true;
	}	

	protected int projectPositiveY(int x, int y, P placement, int maxY) {
		if(placement.getAbsoluteY() >= y) {
			if(withinX(x, placement)) {
				if(placement.getAbsoluteY() < maxY) {
					maxY = placement.getAbsoluteY();
				}
			}
		}
		
		return maxY;
	}

	protected int projectPositiveX(int x, int y, P placement, int maxX) {
		if(placement.getAbsoluteX() >= x) {
			if(withinY(y, placement)) {
				if(placement.getAbsoluteX() < maxX) {
					maxX = placement.getAbsoluteX();
				}
			}
		}
		return maxX;
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

	protected boolean withinX(int x, P placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
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
	
	protected int constrainX(int x, int y) {
		// constrain up
		P closestX = closestPositiveX(x, y);
		if(closestX != null) {
			return closestX.getAbsoluteX() - 1;
		} else {
			return containerMaxX;
		}
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
	
	public int get(int x, int y) {
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(point.getMinY() == y && point.getMinX() == x) {
				return i;
			}
		}
		return -1;
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

}
