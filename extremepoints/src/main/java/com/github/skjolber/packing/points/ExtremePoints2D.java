package com.github.skjolber.packing.points;

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
		this.containerMaxX = dx;
		this.containerMaxY = dy;
		
		values.add(new DefaultFixedXYPoint2D(0, 0, dx, dy, 0, dx, 0, dy));
	}

	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX(), placement.getAbsoluteEndY() - placement.getAbsoluteY());
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {
		
		Point2D source = values.get(index);
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		
		Point2D dx = null;
		Point2D dy = null;
		
		if(source.isFixedY() && source.isFixedX()) {
			FixedYPoint2D fixedPointY = (FixedYPoint2D)source;
			FixedXPoint2D fixedPointX = (FixedXPoint2D)source;

			if(xx < fixedPointY.getFixedMaxX() && yy < fixedPointX.getFixedMaxY()) {

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
			} else if(xx < fixedPointY.getFixedMaxX()) {

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
			} else if(yy < fixedPointX.getFixedMaxY()) {

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
			
		} else if(source.isFixedY()) {
			FixedYPoint2D fixedPointY = (FixedYPoint2D)source;
			if(xx < fixedPointY.getFixedMaxX()) {
				
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
				//  minY |----|---------------|
				//       |    |               |
				//       |    |               |
				//       |----|---------------|-----
				//           minX      xx    fmaxX				
				//
				
				// using dx
				dx = new DefaultFixedXYPoint2D(xx, source.getMinY(), containerMaxX, containerMaxY, xx, fixedPointY.getFixedMaxX(), source.getMinY(), yy);
				
				dy = unsupportedDy(source, xx, yy);
			} else {
				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
		} else if(source.isFixedX()) {
			FixedXPoint2D fixedPointX = (FixedXPoint2D)source;
			if(yy < fixedPointX.getFixedMaxY()) {
				
				//
				// fmaxY |----|
				//       |    |                  
				//   yy  |    |-------------------|
				//       |    |                   |
				//       |    |                   | 
				//       |    |                   |
				//  minY |    |--------------------
				//       |                   
				//       |                   
				//       |-------------------------- 
				//                                xx

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
				values.add(new DefaultFixedXYPoint2D(source.getMinX(), yy, containerMaxX, containerMaxY,  source.getMinX(), xx, yy, fixedPointX.getFixedMaxY()));

				dx = unsupportedDx(source, xx, yy);
			} else {
				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
		} else {
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
		if(dy != null) {
			if(constrainMax(dy)) {
				values.add(index, dy);
				index++;
				
				if(dy.getMaxY() < containerMaxY) {
					System.out.println("Find more points along x axis with more relaxed maxy");
				}
			}
		}
		if(dx != null) {
			if(constrainMax(dx)) {
				values.add(index, dx);
				index++;
				
				if(dx.getMaxX() < containerMaxX) {
					System.out.println("Find more points along y axis with more relaxed maxx");
				}
			}
		}

		placements.add(placement);
		Collections.sort(values, Point2D.COMPARATOR);

		return !values.isEmpty();
	}

	private boolean constrainMax(Point2D point, P placement) {
		int maxX = projectRight(point.getMinX(), point.getMinY(), placement, point.getMaxX());
		int maxY = projectUp(point.getMinX(), point.getMinY(), placement, point.getMaxY());
		
		if(maxX <= point.getMinX() || maxY <= point.getMinY()) {
			return false;
		} else {
			point.setMaxX(maxX);
			point.setMaxY(maxY);
		}

		return true;
	}

	protected int projectUp(int x, int y, P placement, int maxY) {
		if(placement.getAbsoluteY() >= y) {
			if(placement.getAbsoluteX() <= x && x < placement.getAbsoluteEndX()) {
				if(placement.getAbsoluteY() < maxY) {
					maxY = placement.getAbsoluteY();
				}
			}
		}
		
		return maxY;
	}

	protected int projectRight(int x, int y, P placement, int maxX) {
		if(placement.getAbsoluteX() >= x) {
			if(placement.getAbsoluteY() <= y && y < placement.getAbsoluteEndY()) {
				if(placement.getAbsoluteX() < maxX) {
					maxX = placement.getAbsoluteX();
				}
			}
		}
		return maxX;
	}

	protected boolean constrainMax(Point2D point) {
		int maxX = projectRight(point.getMinX(), point.getMinY());
		int maxY = projectUp(point.getMinX(), point.getMinY() );
		
		if(maxX <= point.getMinX() || maxY <= point.getMinY()) {
			return false;
		} else {
			point.setMaxX(maxX);
			point.setMaxY(maxY);
		}

		return true;
	}

	protected Point2D unsupportedDx(Point2D source, int xx, int yy) {
		P moveY = projectDownRight(xx, yy);
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

			if(moveY.getAbsoluteX() <= xx && xx < moveY.getAbsoluteEndX() ) {

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
	
				return new DefaultFixedYPoint2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX());
			} else {

				// unsupported both ways
				//
				//      |    |-------------------|
				//      |    |                   |
				//      |    |                   |
				//      |    |                   |
				// minY |    |--------------------
				//      |    |               |   ↓
				//      |    |               |   *    |--------|
				//      |    |               |        |        |
				//      |----|---------------|--------|--------|--------
				//          minX            maxX

				return new DefaultPoint2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY);
			}
		} else {
			if(moveY.getAbsoluteX() <= xx && xx < moveY.getAbsoluteEndX() ) {
				
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
	
				return new DefaultFixedXYPoint2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX(), moveY.getAbsoluteEndY(), yy);
			} else {

				// supported one way
				//
				//      |    |-------------------|
				//      |    |                   |
				//      |    |                   |
				//      |    |                   |
				//      |    |                   ↓
				//      |    |                   *  |-------|
				//      |    |                   |  |       |
				// minY |    |-------------------|  |       |
				//      |    |               |      |       |
				//      |    |               |      |       |
				//      |    |               |      |       |
				//      |----|---------------|------|-------|--------
				//          minX            maxX

				return new DefaultFixedXPoint2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY, moveY.getAbsoluteEndY(), yy);
			}
		}
	}

	private Point2D unsupportedDy(Point2D source, int xx, int yy) {
		P moveX = projectLeftTop(xx, yy);
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
			
			if(moveX.getAbsoluteY() <= yy && yy < moveX.getAbsoluteEndY()) {

				// supported one way
				//
				// aendy |-|
				//       | |
				// yy    | |←----------|
				//       | |  |        |
				// fmaxY |----|        |
				//
				//       aendx
				
				return new DefaultFixedXPoint2D(moveX.getAbsoluteEndX(), yy, containerMaxX, containerMaxY, yy, moveX.getAbsoluteEndY());
			} else {

				// unsupported both ways
				//
				//
				//       |-|
				//       | |
				// aendy |-|
				//       | 
				// yy    | *←----------|
				//       |    |        |
				// fmaxY |----|        |
				//
				//       aendx

				return new DefaultPoint2D(moveX.getAbsoluteEndX(), yy, containerMaxX, containerMaxY);
			}

		} else {

			if(moveX.getAbsoluteY() <= yy && yy < moveX.getAbsoluteEndY()) {

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
				
				return new DefaultFixedXYPoint2D(moveX.getAbsoluteX(), yy, containerMaxX, containerMaxY,  moveX.getAbsoluteEndX(), xx, yy, moveX.getAbsoluteEndY());
			} else {

				// unsupported one way
				//
				//
				//       |-------|
				//       |       |
				// aendy |-------|
				//       |       
				//       |       
				// yy    |    |--*←----|
				//       |    |        |
				// fmaxY |----|        |
				//
				//             aendx

				return new DefaultFixedYPoint2D(moveX.getAbsoluteEndX(), yy, containerMaxX, containerMaxY, moveX.getAbsoluteEndX(), xx);
			}
		}
	}
	
	private P projectLeftTop(int x, int y) {
		
		// included:
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
			if(placement.getAbsoluteEndY() >= y && placement.getAbsoluteX() < x) {
				
				// most to the right
				if(rightmost == null || placement.getAbsoluteEndX() > rightmost.getAbsoluteEndX()) {
					rightmost = placement;
				}
			}
		}
		
		return rightmost;
	}

	private P projectDownRight(int x, int y) {

		// included:
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
			if(placement.getAbsoluteY() <= y && placement.getAbsoluteEndX() > x) {
				
				// the highest
				if(leftmost == null || placement.getAbsoluteEndY() > leftmost.getAbsoluteEndY()) {
					leftmost = placement;
				}
			}
		}
		
		return leftmost;
	}

	private int projectRight(int x, int y) {
		int closest = containerMaxX;
		for (P placement : placements) {
			if(placement.getAbsoluteX() >= x) {
				if(placement.getAbsoluteY() <= y && y < placement.getAbsoluteEndY()) {
					if(placement.getAbsoluteX() < closest) {
						closest = placement.getAbsoluteX();
					}
				}
			}
		}
		
		return closest;
	}

	private int projectUp(int x, int y) {
		int closest = containerMaxY;
		for (P placement : placements) {
			if(placement.getAbsoluteY() >= y) {
				if(placement.getAbsoluteX() <= x && x < placement.getAbsoluteEndX()) {
					if(placement.getAbsoluteY() < closest) {
						closest = placement.getAbsoluteY();
					}
				}
			}
		}
		
		return closest;
	}

	public List<Point2D> getValues() {
		return values;
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
	
	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			
			if(point2d.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			
			if(point2d.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}

	public Point2D getValue(int i) {
		return values.get(i);
	}

}
