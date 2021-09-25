package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * 
 *
 */

public class ExtremePoints2D<P extends Placement2D> {
	
	private final int containerMaxX;
	private final int containerMaxY;

	private List<Point2D> values = new ArrayList<>();
	private List<P> placements = new ArrayList<>();

	public ExtremePoints2D(int dx, int dy) {
		super();
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		
		values.add(new DefaultHorizontalVerticalSupportPoint2D(0, 0, containerMaxX, containerMaxY, 0, containerMaxX, 0, containerMaxY));
	}

	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {
		
		// overall approach:
		//
		// project points points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side, then constrain them
		// add points shadowed by the two new points, if they could be moved in the negative direction

		// keep track of placement borders, where possible
		
		Point2D source = values.get(index);
		
		List<Point2D> deleted = new ArrayList<>();
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;

		List<Point2D> addX = addX(placement, source, deleted, xx, yy);
		List<Point2D> addY = addY(placement, source, deleted, xx, yy);
		
		deleted.add(source);
		values.removeAll(deleted);
		
		addAll(addX);
		addAll(addY);
		
		placements.add(placement);
		Collections.sort(values, Point2D.COMPARATOR);

		return !values.isEmpty();
	}

	private void addAll(List<Point2D> add) {
		
		search:
		for (int i = 0; i < add.size(); i++) {
			Point2D p1 = add.get(i);
			
			// does a corresponding point already exist?
			
			// TODO binary search
			for (Point2D p2 : values) {
				if(p1.getMinX() == p2.getMinX() && p2.getMinY() == p1.getMinY()) {
					continue search;
				}
			}
			
			values.add(p1);	
		}
	}

	private List<Point2D> addY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy) {
		List<Point2D> added = new ArrayList<>();

		boolean horizontalSupport = source.isHorizontalSupport(xx);
		if(horizontalSupport) { // i.e. when adding dx
			
			//       |
			//       |
			// yy    |    |--------|
			//       |    |        |
			// fmaxY |----|        | 	
			//       |    |        | 
			//       |    |        |
			//       |    |        |
			//       |    |        | 
			//       |    |        |
			//  minY |    |---------------|   <---- horizontal support
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----
			//           minX      xx    fmaxX

			// or
			
			//
			// fmaxY |----|
			//       |    |   dx
			// yy    |    |--------|
			//       |    |        |
			//       |    |        | dy
			//       |    |        |
			// minY  |    |---------------|  <---- horizontal support
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----
			//          minX      xx    fmaxX

			// using dx
			//
			// yy   |             |
			//      |             |
			//      |             |
			//      |             |
			// minY |             *-------     <---- horizontal support
			//      |                    
			//      |                    
			//      |--------------------------
			//                    xx   fmaxX
			
			if(source.getMaxX() > xx) {
				HorizontalSupportPoint2D fixedPointY = (HorizontalSupportPoint2D)source;
				
				int maxY;
				if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
					maxY = containerMaxY;
				} else {
					maxY = constrainY(xx, source.getMinY());
				}
				
				if(maxY > source.getMinY()) {
					DefaultHorizontalVerticalSupportPoint2D next = new DefaultHorizontalVerticalSupportPoint2D(xx, source.getMinY(), source.getMaxX(), maxY, xx, fixedPointY.getHorizontalSupportMaxX(), source.getMinY(), yy);

					added.add(next);
				}
			}
		} else {
			// using dy
			Point2D dx = unsupportedDx(source, xx, yy);
			if(dx != null) {
				
				constrain(dx);

				if(dx.getMinY() < dx.getMaxY()) {
				
					added.add(dx);
					
					if(dx.getMinY() < source.getMinY()) {
						for (int i = 0; i < values.size(); i++) {
							Point2D point = values.get(i);
						
							// Move points swallowed or shadowed by dx
							if(point.getMinX() < xx && point.getMaxX() > xx && dx.getMinY() < point.getMinY() && point.getMinY() < source.getMinY()) {
								added.add(point);
							}
						}
					}
				}
			}
		}
		
		if(!added.isEmpty()) {
	
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
				if(point.getMinX() < xx && point.getMaxX() > source.getMinX() && withinY(point.getMinY(), placement)) {
					
					if(point.getMaxX() > xx) {
						
						// add point on the other side
						// vertical support

						int maxY;
						if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
							maxY = containerMaxY;
						} else {
							maxY = constrainY(xx, point.getMinY());
						}
						
						DefaultVerticalSupportPoint2D next = new DefaultVerticalSupportPoint2D(
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
			
			Collections.sort(added, Point2D.Y_COMPARATOR);

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
			
			Point2D first = added.get(0);
			for (int i = 1; i < added.size(); i++) {
				Point2D point = added.get(i);					
				
				if(point.getMinX() < first.getMinX()) {
					Point2D p = new DefaultVerticalSupportPoint2D(xx, point.getMinY(), point.getMaxX(), first.getMaxY(), point.getMinY(), yy);
	
					added.set(i, p);
				}
			}
		}
		return added;
	}

	private List<Point2D> addX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy) {
		boolean verticalSupport = source.isVerticalSupport(yy);

		List<Point2D> added = new ArrayList<>();
		if(verticalSupport) { // i.e. when adding dy
			
			//
			// vmaxY |----|                      <-- vertical support max y
			//       |    |          
			//  yy   |    *-------------------|
			//       |    |                   |
			//       |    |                   | 
			//       |    |                   |
			//  minY |    |--------------------  <-- vertical support min y
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----
			//           minX            maxX
			
			// or
			
			//
			// vmaxY |----|                      <-- vertical support max y
			//       |    |   dx
			// yy    |    *--------|
			//       |    |        |
			//       |    |        | dy
			//       |    |        |
			// minY  |    |---------------|      <-- vertical support min y
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----
			//          minX      xx    fmaxX
			//          fixedX
			
			// using dy
			//
			//
			// vmaxY |    |                      <-- vertical support max y
			//       |    |   dx
			// yy    |    *---------
			//       |
			//       |
			//       |
			//       |
			//       |
			//       |
			//       |--------------------------
			//           minX      xx    		
			//          fixedX
			//
			
			if(source.getMaxY() > yy) {
				VerticalSupportPoint2D fixedPointX = (VerticalSupportPoint2D)source;
				
				int maxX;
				if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
					maxX = containerMaxX;
				} else {
					maxX = constrainX(source.getMinX(), yy);
				}
				if(maxX > source.getMinX()) {
					DefaultHorizontalVerticalSupportPoint2D next = new DefaultHorizontalVerticalSupportPoint2D(source.getMinX(), yy, maxX, source.getMaxY(), source.getMinX(), xx, yy, fixedPointX.getVeriftalSupportMaxY());
					
					added.add(next);
				}
			}
		} else {
			// using dy
			Point2D negativeX = unsupportedDy(source, xx, yy);
			if(negativeX != null) {
				
				constrain(negativeX);
				
				if(negativeX.getMinX() < negativeX.getMaxX()) {
	
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
						
							// Move points swallowed or shadowed by the dy
							if(point.getMinY() < yy && point.getMaxY() > yy && negativeX.getMinX() < point.getMinX() && point.getMinX() < source.getMinX()) {
								added.add(point);
							}
						}
					}
				}
			}
		}
		
		if(!added.isEmpty()) {

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
			//          yy |--------------*------|  <-- added
			//             |                     |
			// source minY |---------|-----------|
			//             |         | 
			// point minY  |---------|----*----------
			//
			
			for (int i = 0; i < values.size(); i++) {
				Point2D point = values.get(i);
			
				// Move points swallowed or shadowed by the placement
				if(point.getMinY() < yy && point.getMaxY() > source.getMinY() && withinX(point.getMinX(), placement)) {
					
					if(point.getMaxY() > yy) {
						// add point
						// horizontal support

						int maxX;
						if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
							maxX = containerMaxX;
						} else {
							maxX = constrainX(point.getMinX(), yy);
						}
						
						DefaultHorizontalSupportPoint2D next = new DefaultHorizontalSupportPoint2D(
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
			
			Collections.sort(added, Point2D.X_COMPARATOR);

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

			Point2D first = added.get(0);
			for (int i = 1; i < added.size(); i++) {
				Point2D point = added.get(i);					
				
				if(point.getMinY() < first.getMinY()) {
					Point2D p = new DefaultHorizontalSupportPoint2D(point.getMinX(), yy, first.getMaxX(), point.getMaxY(), point.getMinX(), xx);
	
					added.set(i, p);
				}
			}
		}
		return added;
	}

	protected boolean constrain(Point2D dx) {
		// constrain to right
		P closestX = closestPositiveX(dx.getMinX(), dx.getMinY());
		if(closestX != null) {
			dx.setMaxX(closestX.getAbsoluteX() - 1);
		} else {
			dx.setMaxX(containerMaxX);
		}
		if(dx.getMaxX() <= dx.getMinX()) {
			return false;
		}

		// constrain up
		P closestY = closestPositiveY(dx.getMinX(), dx.getMinY());
		if(closestY != null) {
			dx.setMaxY(closestY.getAbsoluteY() - 1);
		} else {
			dx.setMaxY(containerMaxY);
		}
		if(dx.getMaxY() <= dx.getMinY()) {
			return false;
		}
		return true;
	}
	
	protected Point2D unsupportedDx(Point2D source, int xx, int yy) {
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
			
			return new DefaultHorizontalSupportPoint2D(xx, 0, containerMaxX, containerMaxY, xx, containerMaxX);
			
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

			return new DefaultHorizontalSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX() + 1);
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

			return new DefaultHorizontalVerticalSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX() + 1, moveY.getAbsoluteEndY() + 1, yy);
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

	private Point2D unsupportedDy(Point2D source, int xx, int yy) {
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
			
			return new DefaultVerticalSupportPoint2D(0, yy, containerMaxX, containerMaxY, yy, containerMaxY);
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
			
			return new DefaultVerticalSupportPoint2D(moveX.getAbsoluteEndX() + 1, yy, containerMaxX, containerMaxY, yy, moveX.getAbsoluteEndY() + 1);

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
			
			return new DefaultHorizontalVerticalSupportPoint2D(moveX.getAbsoluteX() + 1, yy, containerMaxX, containerMaxY,  moveX.getAbsoluteEndX() + 1, xx, yy, moveX.getAbsoluteEndY() + 1);
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

	protected boolean constrainMax(Point2D point, P placement) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY(), placement, point.getMaxX());
		if(maxX <= point.getMinX()) {
			return false;
		}
		int maxY = projectPositiveY(point.getMinX(), point.getMinY(), placement, point.getMaxY());
		if(maxY <= point.getMinY()) {
			return false;
		}
		
		point.setMaxX(maxX);
		point.setMaxY(maxY);

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
		
		P leftmost = null;
		for (P placement : placements) {
			if(placement.getAbsoluteEndY() <= y && withinX(x, placement)) {
				
				// the highest
				if(leftmost == null || placement.getAbsoluteEndY() > leftmost.getAbsoluteEndY()) {
					leftmost = placement;
				}
			}
		}
		
		return leftmost;
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
