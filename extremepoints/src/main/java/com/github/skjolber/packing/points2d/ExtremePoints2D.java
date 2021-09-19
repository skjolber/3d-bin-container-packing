package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
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
		
		values.add(new DefaultFixedXYPoint2D(0, 0, containerMaxX, containerMaxY, 0, containerMaxX, 0, containerMaxY));
	}

	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {
		
		Point2D source = values.get(index);
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		
		System.out.println("Add at " + source.getMinX() + "x" + source.getMinY());
		
		Point2D dx = null;
		Point2D dy = null;
		
		if(source.isFixedY(xx) && source.isFixedX(yy)) {
			FixedYPoint2D fixedPointY = (FixedYPoint2D)source;
			FixedXPoint2D fixedPointX = (FixedXPoint2D)source;

			//
			// fmaxY |----|
			//       |    |   dx
			// yy    |    |--------|
			//       |    |        |
			//       |    |        | dy
			//       |    |        |
			// minY  |    |---------------|
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
			// minY |             *-------
			//      |                    
			//      |                    
			//      |--------------------------
			//                    xx   fmaxX

			dx = new DefaultFixedXYPoint2D(xx, source.getMinY(), containerMaxX, containerMaxY, xx, fixedPointY.getFixedMaxX(), source.getMinY(), yy);
			
			// using dy
			//
			//
			// fmaxY |    |
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

			dy = new DefaultFixedXYPoint2D(source.getMinX(), yy, containerMaxX, containerMaxY,  source.getMinX(), xx, yy, fixedPointX.getFixedMaxY());
		} else if(source.isFixedY(xx)) {
			FixedYPoint2D fixedPointY = (FixedYPoint2D)source;

			//       |
			//       |
			// yy    |    |--------|
			//       |    |        |
			// fmaxY |----|        |
			//       |    |        | dy
			//       |    |        |
			//       |    |        |
			//       |    |        | 
			//       |    |        |
			//  minY |    |---------------|
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----
			//            minX      xx    fmaxX
			
			// using dx
			//
			//       |             |
			//       |             |
			//       |             |
			//  yy   |             |
			//       |             |
			//       |             |
			//       |             |
			//  minY |             *-------
			//       |                    
			//       |                    
			//       |--------------------------
			//                     xx   fmaxX
			
			dx = new DefaultFixedXYPoint2D(xx, source.getMinY(), containerMaxX, containerMaxY, xx, fixedPointY.getFixedMaxX(), source.getMinY(), yy);

			// using dy
			dy = unsupportedDy(source, xx, yy);
		} else if(source.isFixedX(yy)) {
			FixedXPoint2D fixedPointX = (FixedXPoint2D)source;

			//
			// fmaxY |----|
			//       |    |   dx
			//  yy   |    |-------------------|
			//       |    |                   |
			//       |    |                   | dy
			//       |    |                   |
			//  minY |    |--------------------
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----
			//           minX            maxX

			// using dy
			//
			//
			// fmaxY |    |
			//       |    |   dx
			// yy    |    *-------------------
			//       |
			//       |
			//       |
			//       |
			//       |
			//       |
			//       |--------------------------
			//           minX      xx    
			
			// using dy
			dy = new DefaultFixedXYPoint2D(source.getMinX(), yy, containerMaxX, containerMaxY,  source.getMinX(), xx, yy, fixedPointX.getFixedMaxY());

			// using dx
			dx = unsupportedDx(source, xx, yy);
			
		} else {

			//       |
			//       |           dx
			//  xx   |    |------------------------|
			//       |    |                        |
			// fmaxY |----|                        |
			//       |    |                        |  dy
			//       |    |                        |
			//       |    |                        |
			//       |    |                        |
			//       |    |                        |
			//  minY |----|-------------------------
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-------------	
			//           minX           fmaxX     yy
			
			dx = unsupportedDx(source, xx, yy);
			dy = unsupportedDy(source, xx, yy);
		}

		values.remove(index);
		if(index != 0) {
			index--;
		}
		
		// Constrain max values to the new placement
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(!constrainMax(point, placement)) {
				values.remove(i);
				i--;
			}
		}

		// constrain 
		constrainDy:
		if(dy != null) {
			
			// constrain to x
			P closestX = closestPositiveX(dy.getMinX(), dy.getMinY());
			if(closestX != null) {
				dy.setMaxX(closestX.getAbsoluteX());
			} else {
				dy.setMaxX(containerMaxX);
			}
			if(dy.getMaxX() <= dy.getMinX()) {
				break constrainDy;
			}

			// constrain y
			P closestY = closestPositiveY(dy.getMinX(), dy.getMinY());
			if(closestY != null) {
				dy.setMaxY(closestY.getAbsoluteY());
			} else {
				dy.setMaxY(containerMaxY);
			}
			if(dy.getMaxY() <= dy.getMinY()) {
				break constrainDy;
			}
			
			values.add(index, dy);
			index++;
			
			if(dy.getMaxY() < containerMaxY) {
				
				// does the closest box span whole the way to the end of the area?

				//    |
				//    |    
				//    |      ---------------
				//    |      | 
				//    |------|
				//    |
				//    |-------------|
				//    |             |
				//    |_____________|_________
				

				System.out.println("Find more points along x axis with more relaxed maxy");

				int x = closestY.getAbsoluteEndX() + 1;
				while(x < xx) {
					P nextClosestUp = closestPositiveY(x, dy.getMinY());
					int maxY;
					if(nextClosestUp != null) {
						maxY = nextClosestUp.getAbsoluteY() + 1;
					} else {
						maxY = containerMaxY;
					}

					values.add(index, new DefaultFixedYPoint2D(x, yy, dy.getMaxX(), maxY, x, xx));
					index++;
					
					if(nextClosestUp == null) {
						break;
					}

					x = nextClosestUp.getAbsoluteEndX() + 1;
				}				
			}

		}
		constrainDx:
		if(dx != null) {
			
			// constrain to right
			P closestX = closestPositiveX(dx.getMinX(), dx.getMinY());
			if(closestX != null) {
				dx.setMaxX(closestX.getAbsoluteX());
			} else {
				dx.setMaxX(containerMaxX);
			}
			if(dx.getMaxX() <= dx.getMinX()) {
				break constrainDx;
			}

			// constrain up
			P closestY = closestPositiveY(dx.getMinX(), dx.getMinY());
			if(closestY != null) {
				dx.setMaxY(closestY.getAbsoluteY());
			} else {
				dx.setMaxY(containerMaxY);
			}
			if(dx.getMaxY() <= dx.getMinY()) {
				break constrainDx;
			}

			values.add(index, dx);
			index++;
			
			if(dx.getMaxX() < containerMaxX) {
				// does the closest box span whole the way to the end of the area?

				//    |                        |
				//    |                        |
				//    |                        |
				//    |-------------|          |
				//    |             |          |
				//    |             |          |
				//    |             |     -----|
				//    |             |     |
				//    |             |     |
				//    |_____________|_____|______________
				
				int y = closestX.getAbsoluteEndY() + 1;
				while(y < yy) {
					P nextClosestRight = closestPositiveX(dx.getMinX(), y);
					int maxX;
					if(nextClosestRight != null) {
						maxX = nextClosestRight.getAbsoluteX() + 1;
					} else {
						maxX = containerMaxX;
					}

					values.add(index, new DefaultFixedXPoint2D(xx, y, maxX, dx.getMaxY(), y, yy));
					index++;
					
					if(nextClosestRight == null) {
						break;
					}
					y = nextClosestRight.getAbsoluteEndY() + 1;
				}				
				
			}
		}

		placements.add(placement);
		Collections.sort(values, Point2D.COMPARATOR);

		return !values.isEmpty();
	}

	protected boolean constrain(Point2D dx) {
		
		// constrain to right
		P closestX = closestPositiveX(dx.getMinX(), dx.getMinY());
		if(closestX != null) {
			dx.setMaxX(closestX.getAbsoluteX());
		} else {
			dx.setMaxX(containerMaxX);
		}
		if(dx.getMaxX() <= dx.getMinX()) {
			return false;
		}

		// constrain up
		P closestY = closestPositiveY(dx.getMinX(), dx.getMinY());
		if(closestY != null) {
			dx.setMaxY(closestY.getAbsoluteY());
		} else {
			dx.setMaxY(containerMaxY);
		}
		if(dx.getMaxY() <= dx.getMinY()) {
			return false;
		}
		return true;
	}
	
	protected Point2D unsupportedDx(Point2D source, int xx, int yy) {
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
			
			return new DefaultFixedYPoint2D(xx, 0, containerMaxX, containerMaxY, xx, containerMaxX);
			
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

			return new DefaultFixedYPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX() + 1);
		} else if(moveY.getAbsoluteEndY() < yy) {
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

			return new DefaultFixedXYPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX() + 1, moveY.getAbsoluteEndY() + 1, yy);
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
		P moveX = projectNegativeX(xx, yy);
		if(moveX == null) {
			// supported one way (by container border)
			//
			//       |  
			//       |  
			// yy    |←------------|
			//       |    |        |
			// fmaxY |----|        |
			//
			
			return new DefaultFixedXPoint2D(0, yy, containerMaxX, containerMaxY, yy, containerMaxY);
		} else if(moveX.getAbsoluteEndX() < source.getMinX()) {
			
			// supported one way
			//
			// aendy |-|
			//       | |
			// yy    | |←----------|
			//       | |  |        |
			// fmaxY |----|        |
			//
			//       aendx
			
			return new DefaultFixedXPoint2D(moveX.getAbsoluteEndX() + 1, yy, containerMaxX, containerMaxY, yy, moveX.getAbsoluteEndY() + 1);

		} else if(moveX.getAbsoluteEndX() < xx){

			// supported both ways
			//
			//
			// aendy |-------|
			//       |       |
			//       |       |
			// yy    |    |--*←----|
			//       |    |        |
			// fmaxY |----|        |
			//
			//             aendx
			
			return new DefaultFixedXYPoint2D(moveX.getAbsoluteX() + 1, yy, containerMaxX, containerMaxY,  moveX.getAbsoluteEndX() + 1, xx, yy, moveX.getAbsoluteEndY() + 1);
		}
		
		// no space to move
		//
		//
		// aendy |-------------|
		//       |             |
		//       |             |
		// yy    |    |--------*
		//       |    |        |
		// fmaxY |----|        |
		//
		//  
		
		return null;
	}	

	protected boolean constrainMax(Point2D point, P placement) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY(), placement, point.getMaxX());
		int maxY = projectPositiveY(point.getMinX(), point.getMinY(), placement, point.getMaxY());
		
		if(maxX <= point.getMinX() || maxY <= point.getMinY()) {
			return false;
		} else {
			point.setMaxX(maxX);
			point.setMaxY(maxY);
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

	protected boolean constrainMax(Point2D point) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY());
		int maxY = projectPositiveY(point.getMinX(), point.getMinY() );
		
		if(maxX <= point.getMinX() || maxY <= point.getMinY()) {
			return false;
		} else {
			point.setMaxX(maxX);
			point.setMaxY(maxY);
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
		//
		//
		// excluded:
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

	protected int projectPositiveX(int x, int y) {
		P closestUp = closestPositiveX(x, y);
		if(closestUp != null) {
			return closestUp.getAbsoluteX();
		}
		return containerMaxX;
	}

	protected int projectPositiveY(int x, int y) {
		P closestUp = closestPositiveY(x, y);
		if(closestUp != null) {
			return closestUp.getAbsoluteY();
		}
		return containerMaxY;
	}
	
	protected P closestPositiveY(int x, int y) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteY() >= y) {
				if(withinX(x, placement)) {
					if(closest == null || placement.getAbsoluteY() < closest.getAbsoluteY()) {
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
					if(closest == null || placement.getAbsoluteX() < closest.getAbsoluteX()) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
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
			Point2D point2d = values.get(i);
			
			if(point2d.getMinY() == y && point2d.getMinX() == x) {
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

}
