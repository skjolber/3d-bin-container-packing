package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.points2d.DefaultYSupportPoint2D;
import com.github.skjolber.packing.points2d.DefaultXYSupportPoint2D;
import com.github.skjolber.packing.points2d.DefaultXSupportPoint2D;
import com.github.skjolber.packing.points2d.YSupportPoint2D;
import com.github.skjolber.packing.points2d.XSupportPoint2D;
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
		
		values.add(new Default3DPlanePoint3D(
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
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		int zz = source.getMinZ() + boxDz;
		
		// xx + zz
		// negative z
		// negative x
		//
		//
		//          ◄----------| without support
		//             ◄-------| with support
		//       |
		//       |
		// zz    |    |--------|      - -
		//       |    |        |      | |
		//       |    |        |      | |
		//       |    |        |      | |
		// minZ  |    |--------|      ▼ |
		//       |                      ▼
		//       |                     
		//       |------------------------
		//           minX      xx    
		//
		
		List<Point3D> addZ = addZ(placement, source, values, xx, yy, zz);
		
		// xx + yy 
		// negative x
		// negative y
		
		// yy + zz
		// negative y
		// negative z
		
		
		
		
		// TODO not implemented.
		
		placements.add(placement);
		Collections.sort(values, Point2D.COMPARATOR);

		return !values.isEmpty();
	}	
	
	private List<Point3D> addZ(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz) {
		boolean yz = source.isSupportedYZPlane(source.getMinY(), zz);
		boolean xz = source.isSupportedXZPlane(source.getMinX(), zz);

		int maxX = projectPositiveX(source.getMinX(), source.getMinY(), zz);
		int maxY = projectPositiveY(source.getMinX(), source.getMinY(), zz);

		List<Point2D> added = new ArrayList<>();
		if(yz && xz) {
			// 3x corner point
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			
			added.add(new Default3DPlanePoint3D(
					source.getMinX(), source.getMinY(), zz, 
					maxX, maxY, source.getMaxZ(),
					
					// supported planes
					// yz plane
					source.getMinY(), yzPlane.getSupportedYZPlaneMaxY(), // y
					zz, yzPlane.getSupportedYZPlaneMaxZ(), // z
					
					// xz plane
					source.getMinX(), xzPlane.getSupportedXZPlaneMaxX(), // x
					zz, xzPlane.getSupportedXZPlaneMaxZ(),  // z
					
					// xy plane (i.e. top of the new placement)
					source.getMinX(), xx - 1, // x
					source.getMinY(), yy - 1 // y
				)
			);
		} else if(yz) {

			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			
			P xzPlane = projectNegativeY(source.getMinX(), source.getMinY(), zz);
			if(xzPlane != null) {
				if(xzPlane.getAbsoluteEndY() + 1 < yy) {
					if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
						// found connected plane, so there is a 3x plane support still
						
						added.add(new Default3DPlanePoint3D(
							source.getMinX(), xzPlane.getAbsoluteEndY() + 1, zz,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane
							xzPlane.getAbsoluteEndY() + 1, yy - 1, // y
							zz - 1, yzPlane.getSupportedYZPlaneMaxZ(), // z
							
							// xz plane - placement
							source.getMinX(), xzPlane.getAbsoluteEndX(), // x
							zz, xzPlane.getAbsoluteEndZ(),  // z
							
							// xy plane (i.e. top of the new placement)
							source.getMinX(), xx - 1, // x
							xzPlane.getAbsoluteEndY() + 1, yy - 1 // y
						));
						
					} else {
						// found unconnected plane, so there is a 1x plane support
						added.add(new DefaultXZPlanePoint3D(source.getMinX(), xzPlane.getAbsoluteEndY() + 1, zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), xzPlane.getAbsoluteEndX(), zz, xzPlane.getAbsoluteEndZ()));
					}
				} else {
					// placement is in line with another point, skip
				}
			} else {
				// found container wall
				added.add(new DefaultXZPlanePoint3D(source.getMinX(), 0, zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), containerMaxX, zz, containerMaxZ));
			}
			
		} else if(xz) {
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			
			P yzPlane = projectNegativeX(source.getMinX(), source.getMinY(), zz);
			if(yzPlane != null) {
				if(yzPlane.getAbsoluteEndX() + 1 < xx) {
					if(yzPlane.getAbsoluteEndX() >= source.getMinX()) {
						// found connected plane, so there is a 3x plane support still
						
						added.add(new Default3DPlanePoint3D(
							yzPlane.getAbsoluteEndX() + 1, source.getMinY(), zz,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane - placement
							source.getMinY(), yzPlane.getAbsoluteEndY(), // y
							zz, yzPlane.getAbsoluteEndZ(),  // z

							// xz plane
							yzPlane.getAbsoluteEndX() + 1, xzPlane.getSupportedXZPlaneMaxX(), // x
							zz, xzPlane.getSupportedXZPlaneMaxZ(), // z
							
							// xy plane (i.e. top of the new placement)
							yzPlane.getAbsoluteEndX() + 1, xx - 1, // x
							source.getMinY(), yy - 1 // y
						));
						
					} else {
						// found unconnected plane, so there is a 1x plane support
						added.add(new DefaultYZPlanePoint3D(yzPlane.getAbsoluteEndX() + 1, source.getMinY(), zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), yzPlane.getAbsoluteEndX(), zz, yzPlane.getAbsoluteEndZ()));
					}
				} else {
					// placement is in line with another point, skip
				}
			} else {
				// found container wall
				added.add(new DefaultYZPlanePoint3D(source.getMinX(), 0, zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), containerMaxX, zz, containerMaxZ));
			}
			
		} else {
			// two unsupported points - negative x and y direction
			P yzPlane = projectNegativeX(source.getMinX(), source.getMinY(), zz);
			if(yzPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				added.add(new DefaultYZPlanePoint3D(yzPlane.getAbsoluteEndX() + 1, source.getMinY(), zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), yzPlane.getAbsoluteEndX(), zz, yzPlane.getAbsoluteEndZ()));
			} else {
				// found container wall
				added.add(new DefaultYZPlanePoint3D(source.getMinX(), 0, zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), containerMaxX, zz, containerMaxZ));
			}
			
			P xzPlane = projectNegativeY(source.getMinX(), source.getMinY(), zz);
			if(xzPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				added.add(new DefaultXZPlanePoint3D(source.getMinX(), xzPlane.getAbsoluteEndY() + 1, zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), xzPlane.getAbsoluteEndX(), zz, xzPlane.getAbsoluteEndZ()));
			} else {
				// found container wall
				added.add(new DefaultXZPlanePoint3D(source.getMinX(), 0, zz, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), containerMaxX, zz, containerMaxZ));
			}
		}
		
		if(yz) { // i.e. when adding dy
			//       z
			//       |
			//       |
			// zz    |    |--------|
			//       |    |        |
			// fmaxZ |----|        | 	
			//       |    |        | 
			//       |    |        |
			//       |    |        |
			//       |    |        | 
			//       |    |        |
			//  minY |    |---------------|   
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----x
			//           minX      xx    fmaxX

			// or
			
			//       z
			// fmaxZ |----|
			//       |    |   
			// zz    |    |--------|
			//       |    |        |
			//       |    |        | 
			//       |    |        |
			// minY  |    |---------------| 
			//       |    |               |
			//       |    |               |
			//       |----|---------------|-----x
			//          minX      xx    fmaxX

			//       z
			// fmaxZ |----|
			//       |    |   
			// zz    |    |--------|
			//       |
			//       | 
			//       |
			// minY  | 
			//       |
			//       |
			//       |-------------------------x

			
			if(source.getMaxY() > yy) {
				YSupportPoint2D fixedPointX = (YSupportPoint2D)source;
				
				int maxX = constrainChildMaxX(source, yy);
				if(maxX > source.getMinX()) {
					DefaultXYSupportPoint2D next = new DefaultXYSupportPoint2D(source.getMinX(), yy, maxX, source.getMaxY(), source.getMinX(), xx, yy, fixedPointX.getYSupportMaxY());
					
					added.add(next);
				}
			}
		} else {
			// using dy
			Point2D negativeX = unsupportedDy(source, xx, yy);
			if(negativeX != null) {
				
				if(constrain(negativeX)) {
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

						int maxX = constrainX(source, point, yy);
						
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
					Point2D p = new DefaultXSupportPoint2D(point.getMinX(), yy, first.getMaxX(), point.getMaxY(), point.getMinX(), xx);
	
					added.set(i, p);
				}
			}
		}
		return added;
	}	
		
	protected Point3D unsupportedX(Point3D source, int x, int y, int z) {
		return null;
	}

	protected Point3D unsupportedY(Point3D source, int x, int y, int z) {
		if(z >= containerMaxZ || x >= containerMaxX) {
			return null;
		}
		
		P moveY = projectNegativeY(x, y, z);
		if(moveY == null) {
			// supported by one plane (container wall) 

			return new DefaultXZPlanePoint3D(x, 0, z, containerMaxX, containerMaxY, containerMaxZ, x, containerMaxX, z, containerMaxZ);
			
		} else if(moveY.getAbsoluteEndY() < source.getMinY()) {
			// supported by one plane (closest placement)

			return new DefaultXZPlanePoint3D(x, moveY.getAbsoluteEndY() + 1, z, containerMaxX, containerMaxY, containerMaxZ, x, moveY.getAbsoluteEndX(), z, moveY.getAbsoluteEndZ());
		} else if(moveY.getAbsoluteEndY() + 1 < y) {
			// hit while still within minY,
			// so new point is supported by 1 or more planes
			
			// moveY constrains xz plane
			
			if(source instanceof )
			
			
			return new Default3DPlanePoint3D(
					x, moveY.getAbsoluteEndY() + 1, z,
					moveY.getAbsoluteEndX(), containerMaxY, moveY.getAbsoluteEndZ(),
					
					// supported planes
					// yz plane
					moveY.getAbsoluteEndY() + 1, yzPlaneMaxY, // y
					z, yzPlaneMaxZ, // z
					
					// xz plane
					x, moveY.getAbsoluteEndX(), // x
					z, moveY.getAbsoluteEndZ(),  // z
					
					// xy plane (i.e. top of the new placement)
					source.getMinX(), x - 1, // x
					moveY.getAbsoluteEndY() + 1, y - 1 // y
				);
		}

		// no room left
		
		return null;
	}

	protected Point2D unsupportedZ(Point3D source, int x, int y, int z) {
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
		if(x == 0) {
			return null;
		}
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
		if(y == 0) {
			return null;
		}
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
		if(z == 0) {
			return null;
		}
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

	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point3D point2d = values.get(i);
			
			if(point2d.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point3D point2d = values.get(i);
			
			if(point2d.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}	

	public int getMinZ() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point3D point2d = values.get(i);
			
			if(point2d.getMinZ() < values.get(min).getMinZ()) {
				min = i;
			}
		}
		return min;
	}	

}
