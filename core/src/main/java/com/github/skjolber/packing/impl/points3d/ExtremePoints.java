package com.github.skjolber.packing.impl.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.Placement;

/**
 * 
 *
 */

public class ExtremePoints {
	
	private final int dx;
	private final int dy;
	private final int dz;

	private List<Point> values = new ArrayList<>();
	private List<Placement> placements = new ArrayList<>();

	public ExtremePoints(int dx, int dy, int dz) {
		super();
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		
		values.add(new DefaultFixedPointXYZ(
				0, 0, 0, 
				dx, dy, dz, 
				0, dy, 0, dz, // fixed x
				0, dx, 0, dz, // fixed y
				0, dx, 0, dy // fixed z
				));
	}
	
	public boolean add(int index, Placement placement) {
		
		Point source = values.get(index);
		
		System.out.println();
		System.out.println("**************************************");
		System.out.println("Add " + source.getMinX() + "x" + source.getMinY() + " " + placement.getBox().getWidth() + "x" + placement.getBox().getDepth());
		
		int xx = source.getMinX() + placement.getBox().getWidth();
		int yy = source.getMinY() + placement.getBox().getDepth();
		int zz = source.getMinZ() + placement.getBox().getHeight();
		
		Point dx = null;
		Point dy = null;

		boolean fixedY; // need not project in y direction
		if(source.isFixedY()) {
			FixedPointY fixedPointY = (FixedPointY)source;
			
			fixedY = fixedPointY.getFixedYMaxX() > xx && fixedPointY.getFixedYMaxZ() > zz;
		} else {
			fixedY = false;
		}

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		if(source.isFixedY() && source.isFixedX() && source.isFixedZ()) {
			FixedPointX fixedPointX = (FixedPointX)source;
			FixedPointY fixedPointY = (FixedPointY)source;
			FixedPointZ fixedPointZ = (FixedPointZ)source;

			System.out.println("Could be fixed in x y z");

			if(xx < fixedPointY.getFixedYMaxX() && yy < fixedPointX.getFixedXMaxY() && zz < fixedPointZ.getFixedXMaxZ()) {
				
			}
			
			
		} else if(source.isFixedY() && source.isFixedX()) {
			FixedPointY fixedPointY = (FixedPointY)source;
			FixedPointX fixedPointX = (FixedPointX)source;

			System.out.println("Could be fixed both ways");

			if(xx < fixedPointY.getFixedYMaxX() && yy < fixedPointX.getFixedXMaxY()) {

				System.out.println("Fixed both ways");
				
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

				dx = new DefaultFixedPointXY(xx, source.getMinY(), dy, dx, xx, fixedPointY.getFixedYMaxX(), source.getMinY(), yy);
				
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

				dy = new DefaultFixedPointXY(source.getMinX(), yy, dy, dx,  source.getMinX(), xx, yy, fixedPointX.getFixedXMaxY());
			} else if(xx < fixedPointY.getFixedYMaxX()) {

				System.out.println("Fixed dx");

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
				//           minX      xx    fmaxX
				
				// using dx
				dx = new DefaultFixedPointXY(xx, source.getMinY(), dy, dx, xx, fixedPointY.getFixedYMaxX(), source.getMinY(), yy);

				// using dy
				dy = unsupportedDy(source, xx, yy);
			} else if(yy < fixedPointX.getFixedXMaxY()) {

				System.out.println("Fixed dy");

				//
				// maxY |----|
				//      |    |   dx
				//      |    |-------------------|
				//      |    |                   |
				//      |    |                   | dy
				//      |    |                   |
				// minY |    |--------------------
				//      |    |               |
				//      |    |               |
				//      |----|---------------|-----
				//          minX            maxX

				// using dy
				dy = new DefaultFixedPointXY(source.getMinX(), yy, dy, dx,  source.getMinX(), xx, yy, fixedPointX.getFixedXMaxY());

				// using dx
				dx = unsupportedDx(source, xx, yy);
				
			} else {

				//      |
				//      |           dx
				// xx   |    |------------------------|
				//      |    |                        |
				// maxY |----|                        |
				//      |    |                        |  dy
				//      |    |                        |
				//      |    |                        |
				//      |    |                        |
				//      |    |                        |
				// minY |    |-------------------------
				//      |    |               |
				//      |    |               |
				//      |----|---------------|-------------	
				//          minX            maxX     yy
				
				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
			
		} else if(source.isFixedY()) {
			System.out.println("Could be fixed y");

			FixedPointY fixedPointY = (FixedPointY)source;
			if(xx < fixedPointY.getFixedYMaxX()) {
				
				System.out.println("Is fixed y");
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
				//           minX      xx    fmaxX				
				
				// using dx
				dx = new DefaultFixedPointXY(xx, source.getMinY(), dy, dx, xx, fixedPointY.getFixedYMaxX(), source.getMinY(), yy);
				
				dy = unsupportedDy(source, xx, yy);
			} else {
				System.out.println("Is not fixed y");

				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
		} else if(source.isFixedX()) {
			System.out.println("Could be fixed x");
			
			FixedPointX fixedPointX = (FixedPointX)source;
			if(yy < fixedPointX.getFixedXMaxY()) {
				
				System.out.println("Fixed x, unsupported y");
				//
				// maxY |----|
				//      |    |                  xx
				//      |    |-------------------| yy
				//      |    |                   |
				//      |    |                   | 
				//      |    |                   |
				// minY |    |--------------------
				//      |                   
				//      |                   
				//      |--------------------------
				//                          

				// using dy
				values.add(new DefaultFixedPointXY(source.getMinX(), yy, dy, dx,  source.getMinX(), xx, yy, fixedPointX.getFixedXMaxY()));

				dx = unsupportedDx(source, xx, yy);
			} else {
				System.out.println("Not fixed x or y");
				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
		} else {
			System.out.println("Not fixed any way");
			
			dx = unsupportedDx(source, xx, yy);
			dy = unsupportedDy(source, xx, yy);
		}

		values.remove(index);
		if(index != 0) {
			index--;
		}
		
		// Constrain max values to the new placement
		for (int i = 0; i < values.size(); i++) {
			Point point = values.get(i);
			
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

	private boolean constrainMax(Point point, Placement placement) {
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

	private int projectUp(int x, int y, Placement placement, int maxY) {
		if(placement.getAbsoluteY() >= y) {
			if(placement.getAbsoluteX() <= x && x < placement.getAbsoluteEndX()) {
				if(placement.getAbsoluteY() < maxY) {
					maxY = placement.getAbsoluteY();
				}
			}
		}
		
		return maxY;
	}

	private int projectRight(int x, int y, Placement placement, int maxX) {
		if(placement.getAbsoluteX() >= x) {
			if(placement.getAbsoluteY() <= y && y < placement.getAbsoluteEndY()) {
				if(placement.getAbsoluteX() < maxX) {
					maxX = placement.getAbsoluteX();
				}
			}
		}
		return maxX;
	}

	private boolean constrainMax(Point point) {
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

	private Point unsupportedDx(Point source, int xx, int yy) {
		System.out.println("Add unsupported dx");
		Placement moveY = projectDownRight(xx, yy);
		if(moveY == null) {
			
			System.out.println(" all the way");
			
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
			
			return new DefaultFixedPointY(xx, 0, dy, dx, xx, dx);
			
		} else if(moveY.getAbsoluteEndY() < source.getMinY()) {

			System.out.println(" directly below");

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
	
				System.out.println(" directly below one way");

				return new DefaultFixedPointY(xx, moveY.getAbsoluteEndY(), dy, dx, xx, moveY.getAbsoluteEndX());

			} else {

				System.out.println(" below no ways");

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

				return new DefaultPoint(xx, moveY.getAbsoluteEndY(), dx, dy);
			}
			
		} else {

			if(moveY.getAbsoluteX() <= xx && xx < moveY.getAbsoluteEndX() ) {
				
				System.out.println("Supported both ways");
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
	
				return new DefaultFixedPointXY(xx, moveY.getAbsoluteEndY(), dy, dx, xx, moveY.getAbsoluteEndX(), moveY.getAbsoluteEndY(), yy);
			} else {

				System.out.println("Supported one way");
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

				return new DefaultFixedPointX(xx, moveY.getAbsoluteEndY(), dy, dx, moveY.getAbsoluteEndY(), yy);
			}
		}
	}

	private Point unsupportedDy(Point source, int xx, int yy) {
		System.out.println("Add unsupported dy");
		
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
			
			return new DefaultFixedPointX(0, yy, dy, dx, yy, dy);
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
				
				return new DefaultFixedPointX(moveX.getAbsoluteEndX(), yy, dy, dx, yy, moveX.getAbsoluteEndY());
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

				return new DefaultPoint(moveX.getAbsoluteEndX(), yy, dx, dy);
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
				
				return new DefaultFixedPointXY(moveX.getAbsoluteX(), yy, dy, dx,  moveX.getAbsoluteEndX(), xx, yy, moveX.getAbsoluteEndY());
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

				return new DefaultFixedPointY(moveX.getAbsoluteEndX(), yy, dy, dx, moveX.getAbsoluteEndX(), xx);
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
		int closest = dx;
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
		int closest = dy;
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

	public List<Point> getValues() {
		return values;
	}
	
	public int getDepth() {
		return dy;
	}
	
	public int getWidth() {
		return dx;
	}

	@Override
	public String toString() {
		return "Points [width=" + dx + ", depth=" + dy + ", values=" + values + "]";
	}
	
	public List<Placement> getPlacements() {
		return placements;
	}

	public Point getPoint(int pointIndex) {
		return values.get(pointIndex);
	}
	
}
