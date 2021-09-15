package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 *
 */

public class ExtremePoints3D<P extends Placement3D> {
	
	private final int dx;
	private final int dy;
	private final int dz;

	private List<Point3D> values = new ArrayList<>();
	private List<P> placements = new ArrayList<>();

	public ExtremePoints3D(int dx, int dy, int dz) {
		super();
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		
		values.add(new DefaultFixedXYZPoint3D(
				0, 0, 0, 
				dx, dy, dz, 
				0, dy, 0, dz, // fixed x
				0, dx, 0, dz, // fixed y
				0, dx, 0, dy // fixed z
				));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX(), placement.getAbsoluteEndY() - placement.getAbsoluteY(), placement.getAbsoluteEndZ() - placement.getAbsoluteZ());
	}

	public boolean add(int index, P placement, int boxDx, int boxDy, int boxDz) {		
		Point3D source = values.get(index);
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		int zz = source.getMinZ() + boxDz;
		
		Point3D dx = null;
		Point3D dy = null;
		Point3D dz = null;

		boolean fixedY; // need not project in y direction
		if(source.isFixedY()) {
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			
			fixedY = fixedPointY.getFixedYMaxX() > xx && fixedPointY.getFixedYMaxZ() > zz;
		} else {
			fixedY = false;
		}

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		if(source.isFixedY() && source.isFixedX() && source.isFixedZ()) {
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			FixedZPoint3D fixedPointZ = (FixedZPoint3D)source;

			System.out.println("Could be fixed in x y z");

			if(xx < fixedPointY.getFixedYMaxX() && yy < fixedPointX.getFixedXMaxY() && zz < fixedPointZ.getFixedXMaxZ()) {
				
			}
			
			
		} else if(source.isFixedY() && source.isFixedX()) {
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;

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

				dx = new DefaultFixedXYPoint3D(xx, source.getMinY(), dy, dx, xx, fixedPointY.getFixedYMaxX(), source.getMinY(), yy);
				
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

				dy = new DefaultFixedXYPoint3D(source.getMinX(), yy, dy, dx,  source.getMinX(), xx, yy, fixedPointX.getFixedXMaxY());
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
				dx = new DefaultFixedXYPoint3D(xx, source.getMinY(), dy, dx, xx, fixedPointY.getFixedYMaxX(), source.getMinY(), yy);

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
				dy = new DefaultFixedXYPoint3D(source.getMinX(), yy, dy, dx,  source.getMinX(), xx, yy, fixedPointX.getFixedXMaxY());

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

			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
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
				dx = new DefaultFixedXYPoint3D(xx, source.getMinY(), dy, dx, xx, fixedPointY.getFixedYMaxX(), source.getMinY(), yy);
				
				dy = unsupportedDy(source, xx, yy);
			} else {
				System.out.println("Is not fixed y");

				dx = unsupportedDx(source, xx, yy);
				dy = unsupportedDy(source, xx, yy);
			}
		} else if(source.isFixedX()) {
			System.out.println("Could be fixed x");
			
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;
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
				values.add(new DefaultFixedXYPoint3D(source.getMinX(), yy, dy, dx,  source.getMinX(), xx, yy, fixedPointX.getFixedXMaxY()));

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
			Point3D point = values.get(i);
			
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

	private boolean constrainMax(Point3D point, Placement placement) {
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

	private boolean constrainMax(Point3D point) {
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

	private Point3D unsupportedDx(Point3D source, int xx, int yy) {
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
			
			return new DefaultFixedYPoint3D(xx, 0, dy, dx, xx, dx);
			
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

				return new DefaultFixedYPoint3D(xx, moveY.getAbsoluteEndY(), dy, dx, xx, moveY.getAbsoluteEndX());

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
	
				return new DefaultFixedXYPoint3D(xx, moveY.getAbsoluteEndY(), dy, dx, xx, moveY.getAbsoluteEndX(), moveY.getAbsoluteEndY(), yy);
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

				return new DefaultFixedXPoint3D(xx, moveY.getAbsoluteEndY(), dy, dx, moveY.getAbsoluteEndY(), yy);
			}
		}
	}

	private Point3D unsupportedDy(Point3D source, int xx, int yy) {
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
			
			return new DefaultFixedXPoint3D(0, yy, dy, dx, yy, dy);
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
				
				return new DefaultFixedXPoint3D(moveX.getAbsoluteEndX(), yy, dy, dx, yy, moveX.getAbsoluteEndY());
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
				
				return new DefaultFixedXYPoint3D(moveX.getAbsoluteX(), yy, dy, dx,  moveX.getAbsoluteEndX(), xx, yy, moveX.getAbsoluteEndY());
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

				return new DefaultFixedYPoint3D(moveX.getAbsoluteEndX(), yy, dy, dx, moveX.getAbsoluteEndX(), xx);
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

	public List<Point3D> getValues() {
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

	public Point3D getPoint(int pointIndex) {
		return values.get(pointIndex);
	}
	
}
