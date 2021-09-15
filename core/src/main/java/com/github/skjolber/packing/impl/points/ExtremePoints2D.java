package com.github.skjolber.packing.impl.points;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.Placement;

/**
 * 
 *
 */

public class ExtremePoints2D {
	
	private final int containerMaxX;
	private final int containerMaxY;

	private List<Point2D> values = new ArrayList<>();
	private List<Placement> placements = new ArrayList<>();

	public ExtremePoints2D(int dx, int dy) {
		super();
		this.containerMaxX = dx;
		this.containerMaxY = dy;
		
		values.add(new DefaultFixedPointXY2D(0, 0, dx, dy, 0, dx, 0, dy));
	}
	
	public boolean add(int index, Placement placement) {
		
		Point2D source = values.get(index);
		
		int xx = source.getMinX() + placement.getBox().getWidth();
		int yy = source.getMinY() + placement.getBox().getDepth();
		
		Point2D dx = null;
		Point2D dy = null;
		
		if(source.isFixedY() && source.isFixedX()) {
			FixedPointY2D fixedPointY = (FixedPointY2D)source;
			FixedPointX2D fixedPointX = (FixedPointX2D)source;

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

				dx = new DefaultFixedPointXY2D(xx, source.getMinY(), containerMaxX, containerMaxY, xx, fixedPointY.getFixedMaxX(), source.getMinY(), yy);
				
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

				dy = new DefaultFixedPointXY2D(source.getMinX(), yy, containerMaxX, containerMaxY,  source.getMinX(), xx, yy, fixedPointX.getFixedMaxY());
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
				
				dx = new DefaultFixedPointXY2D(xx, source.getMinY(), containerMaxX, containerMaxY, xx, fixedPointY.getFixedMaxX(), source.getMinY(), yy);

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
				dy = new DefaultFixedPointXY2D(source.getMinX(), yy, containerMaxX, containerMaxY,  source.getMinX(), xx, yy, fixedPointX.getFixedMaxY());

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
			FixedPointY2D fixedPointY = (FixedPointY2D)source;
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
				dx = new DefaultFixedPointXY2D(xx, source.getMinY(), containerMaxX, containerMaxY, xx, fixedPointY.getFixedMaxX(), source.getMinY(), yy);
				
				dy = unsupportedDy(source, xx, yy);
			} else {
				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
		} else if(source.isFixedX()) {
			FixedPointX2D fixedPointX = (FixedPointX2D)source;
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
				values.add(new DefaultFixedPointXY2D(source.getMinX(), yy, containerMaxX, containerMaxY,  source.getMinX(), xx, yy, fixedPointX.getFixedMaxY()));

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
			}
		}
		if(dx != null) {
			if(constrainMax(dx)) {
				values.add(index, dx);
			}
		}

		placements.add(placement);
		Collections.sort(values);

		return !values.isEmpty();
	}

	private boolean constrainMax(Point2D point, Placement placement) {
		int maxX = projectRight(point.getMinX(), point.getMinY(), placement, point.getMaxX());
		int maxY = projectUp(point.getMinX(), point.getMinY(), placement, point.getMaxY());
		
		if(maxX <= point.getMinX() || maxY <= point.getMinY()) {
			System.out.println("Remove " + point.getMinX() + "x" + point.getMinY() + " due to " + maxX + "x" + maxY);

			return false;
		} else {
			point.setMaxX(maxX);
			point.setMaxY(maxY);
		}

		return true;
	}

	protected int projectUp(int x, int y, Placement placement, int maxY) {
		if(placement.getAbsoluteY() >= y) {
			if(placement.getAbsoluteX() <= x && x < placement.getAbsoluteEndX()) {
				if(placement.getAbsoluteY() < maxY) {
					maxY = placement.getAbsoluteY();
				}
			}
		}
		
		return maxY;
	}

	protected int projectRight(int x, int y, Placement placement, int maxX) {
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
		Placement moveY = projectDownRight(xx, yy);
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
			
			return new DefaultFixedPointY2D(xx, 0, containerMaxX, containerMaxY, xx, containerMaxX);
			
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
	
				return new DefaultFixedPointY2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX());
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
	
				return new DefaultFixedPointXY2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY, xx, moveY.getAbsoluteEndX(), moveY.getAbsoluteEndY(), yy);
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

				return new DefaultFixedPointX2D(xx, moveY.getAbsoluteEndY(), containerMaxX, containerMaxY, moveY.getAbsoluteEndY(), yy);
			}
		}
	}

	private Point2D unsupportedDy(Point2D source, int xx, int yy) {
		Placement moveX = projectLeftTop(xx, yy);
		if(moveX == null) {
			
			// supported one way (by container border)
			//
			//       |  
			//       |  
			// yy    |←------------|
			//       |    |        |
			// fmaxY |----|        |
			//
			
			return new DefaultFixedPointX2D(0, yy, containerMaxX, containerMaxY, yy, containerMaxY);
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
				
				return new DefaultFixedPointX2D(moveX.getAbsoluteEndX(), yy, containerMaxX, containerMaxY, yy, moveX.getAbsoluteEndY());
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
				
				return new DefaultFixedPointXY2D(moveX.getAbsoluteX(), yy, containerMaxX, containerMaxY,  moveX.getAbsoluteEndX(), xx, yy, moveX.getAbsoluteEndY());
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

				return new DefaultFixedPointY2D(moveX.getAbsoluteEndX(), yy, containerMaxX, containerMaxY, moveX.getAbsoluteEndX(), xx);
			}
		}
	}
	
	private Placement projectLeftTop(int x, int y) {
		
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
		
		Placement rightmost = null;
		for (Placement placement : placements) {
			if(placement.getAbsoluteEndY() >= y && placement.getAbsoluteX() < x) {
				
				// most to the right
				if(rightmost == null || placement.getAbsoluteEndX() > rightmost.getAbsoluteEndX()) {
					rightmost = placement;
				}
			}
		}
		
		return rightmost;
	}

	private Placement projectDownRight(int x, int y) {

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
		
		Placement leftmost = null;
		for (Placement placement : placements) {
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
		for (Placement placement : placements) {
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
		for (Placement placement : placements) {
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
	
	public List<Placement> getPlacements() {
		return placements;
	}
	
}
