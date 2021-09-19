package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.points2d.DefaultFixedXPoint2D;
import com.github.skjolber.packing.points2d.DefaultFixedXYPoint2D;
import com.github.skjolber.packing.points2d.DefaultFixedYPoint2D;
import com.github.skjolber.packing.points2d.FixedXPoint2D;
import com.github.skjolber.packing.points2d.FixedYPoint2D;
import com.github.skjolber.packing.points2d.Point2D;

/**
 * 
 *
 */

public class ExtremePoints3D<P extends Placement3D> {
	
	private final int containerMaxX;
	private final int containerMaxY;
	private final int containerMaxZ;

	private List<Point3D> values = new ArrayList<>();
	private List<P> placements = new ArrayList<>();

	public ExtremePoints3D(int dx, int dy, int dz) {
		super();
		this.containerMaxX = dx;
		this.containerMaxY = dy;
		this.containerMaxZ = dz;
		
		values.add(new DefaultFixedXYZPoint3D(
				0, 0, 0, 
				dx, dy, dz, 
				0, dy, 0, dz, // fixed x
				0, dx, 0, dz, // fixed y
				0, dx, 0, dy // fixed z
				));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1, placement.getAbsoluteEndZ() - placement.getAbsoluteZ() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy, int boxDz) {		
		Point3D source = values.get(index);
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		int zz = source.getMinZ() + boxDz;
		
		Point3D dx = null;
		Point3D dy = null;
		Point3D dz = null;
		
		if(source.isFixedX(yy, zz) && source.isFixedY(xx, zz) && source.isFixedZ(xx, yy)) {
			// all three planes constrained
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			FixedZPoint3D fixedPointZ = (FixedZPoint3D)source;
			
/*
			public DefaultFixedXYZPoint3D(
					int minX, int minY, int minZ, 
					int maxX, int maxY, int maxZ,
					
					int fixedXMinY, int fixedXMaxY,
					int fixedXMinZ, int fixedXMaxZ,

					int fixedYMinX, int fixedYMaxX, 
					int fixedYMinZ, int fixedYMaxZ, 
					
					int fixedZMinX, int fixedZMaxX,
					int fixedZMinY, int fixedZMaxY
*/					
			
			
			dx = new DefaultFixedXYZPoint3D(
					xx, source.getMinY(), source.getMinZ(), 
					containerMaxX, containerMaxY, containerMaxZ, 
					
					// fixed x
					source.getMinY(), yy, 
					source.getMinZ(), zz,
					
					// fixed y
					source.getMinX(), fixedPointY.getFixedYMaxX(),
					source.getMinZ(), zz,
					
					// fixed z
					source.getMinX(), fixedPointZ.getFixedZMaxX(), 
					source.getMinY(), yy
					);

			dy = new DefaultFixedXYZPoint3D(
					source.getMinX(), yy, source.getMinZ(), 
					containerMaxX, containerMaxY, containerMaxZ, 
					
					// fixed x
					source.getMinY(), fixedPointX.getFixedXMaxY(), 
					source.getMinZ(), zz,
					
					// fixed y
					source.getMinX(), xx, 
					source.getMinZ(), zz,
					
					// fixed z
					source.getMinX(), xx, 
					source.getMinY(), fixedPointZ.getFixedZMaxY()
					);
			
			dy = new DefaultFixedXYZPoint3D(
					source.getMinX(), source.getMinY(), zz, 
					containerMaxX, containerMaxY, containerMaxZ, 
					
					// fixed x
					source.getMinY(), yy, 
					source.getMinZ(), fixedPointX.getFixedXMaxZ(),
					
					// fixed y
					source.getMinX(), xx, 
					source.getMinZ(), fixedPointY.getFixedYMaxZ(),
					
					// fixed z
					source.getMinX(), xx, 
					source.getMinY(), yy
					);
			
			
			
		} else if(source.isFixedX(yy, zz) && source.isFixedY(xx, zz)) {
			// two planes constrained
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			
		} else if(source.isFixedX(yy, zz) && source.isFixedZ(xx, yy)) {
			// two planes constrained
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;
			FixedZPoint3D fixedPointZ = (FixedZPoint3D)source;
			
		} else if(source.isFixedY(xx, zz) && source.isFixedZ(xx, yy)) {
			// two planes constrained
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			FixedZPoint3D fixedPointZ = (FixedZPoint3D)source;
			
		} else if(source.isFixedX(yy, zz)) {
			// one plane constrained
			FixedXPoint3D fixedPointX = (FixedXPoint3D)source;
			
		} else if(source.isFixedY(xx, zz)) {
			// one plane constrained
			FixedYPoint3D fixedPointY = (FixedYPoint3D)source;
			
		} else if(source.isFixedZ(xx, yy)) {
			// one plane constrained
			FixedZPoint3D fixedPointZ = (FixedZPoint3D)source;
			
		} else {
			// no planes constrained
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
		constrainDy:
		if(dy != null) {
			
			// constrain to right
			P closestRight = closestPositiveX(dy.getMinX(), dy.getMinY(), dy.getMinZ());
			if(closestRight != null) {
				dy.setMaxX(closestRight.getAbsoluteX());
			} else {
				dy.setMaxX(containerMaxX);
			}
			if(dy.getMaxX() <= dy.getMinX()) {
				break constrainDy;
			}

			// constrain up
			P closestUp = closestPositiveY(dy.getMinX(), dy.getMinY(), dy.getMinZ());
			if(closestUp != null) {
				dy.setMaxY(closestUp.getAbsoluteY());
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

				int x = closestUp.getAbsoluteEndX() + 1;
				while(x < xx) {
					P nextClosestUp = closestPositiveY(x, dy.getMinY(), dy.getMinZ());
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
			P closestRight = closestPositiveX(dx.getMinX(), dx.getMinY(), dx.getMinZ());
			if(closestRight != null) {
				dx.setMaxX(closestRight.getAbsoluteX());
			} else {
				dx.setMaxX(containerMaxX);
			}
			if(dx.getMaxX() <= dx.getMinX()) {
				break constrainDx;
			}

			// constrain up
			P closestUp = closestPositiveY(dx.getMinX(), dx.getMinY(), dx.getMinZ());
			if(closestUp != null) {
				dx.setMaxY(closestUp.getAbsoluteY());
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
				
				int y = closestRight.getAbsoluteEndY() + 1;
				while(y < yy) {
					P nextClosestRight = closestPositiveX(dx.getMinX(), y, dx.getMinZ());
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
		
		constrainDz:
		if(dz != null) {
			
			// constrain to right
			P closestRight = closestPositiveZ(dz.getMinX(), dz.getMinY(), dz.getMinZ());
			if(closestRight != null) {
				dz.setMaxZ(closestRight.getAbsoluteZ());
			} else {
				dz.setMaxZ(containerMaxZ);
			}
			if(dz.getMaxZ() <= dx.getMinZ()) {
				break constrainDz;
			}

			// constrain up
			P closestUp = closestPositiveZ(dx.getMinX(), dx.getMinY(), dx.getMinZ());
			if(closestUp != null) {
				dx.setMaxY(closestUp.getAbsoluteY());
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
				
				int y = closestRight.getAbsoluteEndY() + 1;
				while(y < yy) {
					P nextClosestRight = closestPositiveX(dx.getMinX(), y, dx.getMinZ());
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
		
	protected Point2D unsupportedDxy(Point2D source, int xx, int yy, int zz) {
		return null;
	}

	protected Point2D unsupportedDxz(Point2D source, int xx, int yy, int zz) {
		return null;
	}

	protected Point2D unsupportedDyz(Point2D source, int xx, int yy, int zz) {
		return null;
	}

	
	protected boolean constrainMax(Point3D point, P placement) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY(), point.getMinZ(), placement, point.getMaxX());
		if(maxX <= point.getMinX()) {
			return false;
		}
		int maxY = projectPositiveY(point.getMinX(), point.getMinY(), point.getMinZ(), placement, point.getMaxY());
		if(maxY <= point.getMinY()) {
			return false;
		}
		int maxZ = projectPositiveZ(point.getMinX(), point.getMinY(), point.getMinZ(), placement, point.getMaxZ());
		if(maxZ <= point.getMinZ()) {
			return false;
		}
		
		point.setMaxX(maxX);
		point.setMaxY(maxY);
		point.setMaxZ(maxZ);

		return true;
	}

	protected int projectPositiveY(int x, int y, int z, P placement, int maxY) {
		if(placement.getAbsoluteY() >= y) {
			if(withinX(x, placement) && withinY(y, placement)) {
				if(placement.getAbsoluteY() < maxY) {
					maxY = placement.getAbsoluteY();
				}
			}
		}
		
		return maxY;
	}

	protected int projectPositiveX(int x, int y, int z, P placement, int maxX) {
		if(placement.getAbsoluteX() >= x) {
			if(withinY(y, placement) && withinZ(z, placement)) {
				if(placement.getAbsoluteX() < maxX) {
					maxX = placement.getAbsoluteX();
				}
			}
		}
		return maxX;
	}

	protected int projectPositiveZ(int x, int y, int z, P placement, int maxZ) {
		if(placement.getAbsoluteZ() >= z) {
			if(withinX(x, placement) && withinY(y, placement)) {
				if(placement.getAbsoluteZ() < maxZ) {
					maxZ = placement.getAbsoluteZ();
				}
			}
		}
		return maxZ;
	}	
	
	protected boolean constrainMax(Point3D point) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY(), point.getMinZ());
		if(maxX <= point.getMinX()) {
			return false;
		}
		int maxY = projectPositiveY(point.getMinX(), point.getMinY(), point.getMinZ());
		if(maxY <= point.getMinY()) {
			return false;
		}
		int maxZ = projectPositiveZ(point.getMinX(), point.getMinY(), point.getMinZ());
		if(maxZ < point.getMinZ()) {
			return false;
		}
		
		point.setMaxX(maxX);
		point.setMaxY(maxY);
		point.setMaxZ(maxZ);

		
		
		return true;
	}
	
	protected P projectNegativeX(int x, int y, int z) {
		
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
			if(placement.getAbsoluteEndX() <= x && withinY(y, placement) && withinZ(z, placement)) {
				// most to the right
				if(rightmost == null || placement.getAbsoluteEndX() > rightmost.getAbsoluteEndX()) {
					rightmost = placement;
				}
			}
		}
		
		return rightmost;
	}	

	protected P projectNegativeY(int x, int y, int z) {

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
			if(placement.getAbsoluteEndY() <= y && withinX(x, placement) && withinZ(z, placement)) {
				
				// the highest
				if(leftmost == null || placement.getAbsoluteEndY() > leftmost.getAbsoluteEndY()) {
					leftmost = placement;
				}
			}
		}
		
		return leftmost;
	}
	
	protected P projectNegativeZ(int x, int y, int z) {
		
		P downmost = null;
		for (P placement : placements) {
			if(placement.getAbsoluteEndZ() <= z && withinX(x, placement) && withinY(y, placement)) {
				
				// the highest
				if(downmost == null || placement.getAbsoluteEndZ() > downmost.getAbsoluteEndZ()) {
					downmost = placement;
				}
			}
		}
		
		return downmost;
	}


	protected int projectPositiveX(int x, int y, int z) {
		P closestUp = closestPositiveX(x, y, z);
		if(closestUp != null) {
			return closestUp.getAbsoluteX();
		}
		return containerMaxX;
	}

	protected int projectPositiveY(int x, int y, int z) {
		P closestUp = closestPositiveY(x, y, z);
		if(closestUp != null) {
			return closestUp.getAbsoluteY();
		}
		return containerMaxY;
	}
	
	protected int projectPositiveZ(int x, int y, int z) {
		P closestUp = closestPositiveZ(x, y, z);
		if(closestUp != null) {
			return closestUp.getAbsoluteZ();
		}
		return containerMaxZ;
	}

	protected P closestPositiveX(int x, int y, int z) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteX() >= x) {
				if(withinY(y, placement) && withinZ(z, placement)) {
					if(closest == null || placement.getAbsoluteX() < closest.getAbsoluteX()) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
	}
	
	protected P closestPositiveY(int x, int y, int z) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteY() >= y) {
				if(withinX(x, placement) && withinZ(z, placement)) {
					if(closest == null || placement.getAbsoluteY() < closest.getAbsoluteY()) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
	}

	protected P closestPositiveZ(int x, int y, int z) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteZ() >= z) {
				if(withinX(x, placement) && withinY(y, placement)) {
					if(closest == null || placement.getAbsoluteZ() < closest.getAbsoluteZ()) {
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
	
	protected boolean withinZ(int z, P placement) {
		return placement.getAbsoluteZ() <= z && z <= placement.getAbsoluteEndZ();
	}
	
	public int getDepth() {
		return containerMaxY;
	}
	
	public int getWidth() {
		return containerMaxX;
	}
	
	public int getHeight() {
		return containerMaxX;
	}

	@Override
	public String toString() {
		return "ExtremePoints3D [dx=" + containerMaxX + ", dy=" + containerMaxY + ", dz=" + containerMaxZ + "]";
	}
	
	public List<P> getPlacements() {
		return placements;
	}
	
	public int get(int x, int y, int z) {
		for (int i = 0; i < values.size(); i++) {
			Point3D point2d = values.get(i);
			
			if(point2d.getMinY() == y && point2d.getMinX() == x && point2d.getMinZ() == z) {
				return i;
			}
		}
		return -1;
	}
	
	public List<Point3D> getValues() {
		return values;
	}

	
	public Point2D getValue(int i) {
		return values.get(i);
	}
	
}
