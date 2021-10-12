package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.points2d.DefaultYSupportPoint2D;
import com.github.skjolber.packing.points2d.DefaultXYSupportPoint2D;
import com.github.skjolber.packing.api.Placement3D;
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
		
		List<Point3D> deleted = new ArrayList<>();

		List<Point3D> addX = addX(placement, source, deleted, xx, yy, zz);
		List<Point3D> addY = addY(placement, source, deleted, xx, yy, zz);
		List<Point3D> addZ = addZ(placement, source, deleted, xx, yy, zz);
		
		deleted.add(source);
		values.removeAll(deleted);
		
		addAll(addX);
		addAll(addY);
		addAll(addZ);
		
		placements.add(placement);
		Collections.sort(values, Point2D.COMPARATOR);

		return !values.isEmpty();
	}	
	
	private List<Point3D> addZ(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz) {
		boolean yz = source.isSupportedYZPlane(source.getMinY(), zz);
		boolean xz = source.isSupportedXZPlane(source.getMinX(), zz);

		int maxX = projectPositiveX(source.getMinX(), source.getMinY(), zz);
		int maxY = projectPositiveY(source.getMinX(), source.getMinY(), zz);

		List<Point3D> added = new ArrayList<>();
		if(yz && xz) {
			// 3x corner point
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			
			int x = source.getMinX();
			int y = source.getMinY();
			int z = zz; 

			
			added.add(new Default3DPlanePoint3D(
					x, y, z,
					maxX, maxY, source.getMaxZ(),
					
					// supported planes
					// yz plane
					y, yzPlane.getSupportedYZPlaneMaxY(), // y
					z, yzPlane.getSupportedYZPlaneMaxZ(), // z
					
					// xz plane
					x, xzPlane.getSupportedXZPlaneMaxX(), // x
					z, xzPlane.getSupportedXZPlaneMaxZ(),  // z
					
					// xy plane (i.e. top of the new placement)
					x, xx - 1, // x
					y, yy - 1 // y
				)
			);
		} else if(yz) {

			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			
			P xzPlane = projectNegativeY(source.getMinX(), yy, zz);
			if(xzPlane != null) {
				if(xzPlane.getAbsoluteEndY() + 1 < yy) {
					if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
						// found connected plane, so there is a 3x plane support still

						int x = source.getMinX();
						int y = xzPlane.getAbsoluteEndY() + 1;
						int z = zz;

						
						added.add(new Default3DPlanePoint3D(
							x, y, z,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane
							y, yzPlane.getSupportedYZPlaneMaxY(), // y
							z, yzPlane.getSupportedYZPlaneMaxZ(), // z
							
							// xz plane - some placement
							x, xzPlane.getAbsoluteEndX(), // x
							z, xzPlane.getAbsoluteEndZ(),  // z
							
							// xy plane (i.e. top of the new placement)
							x, xx - 1, // x
							y, yy - 1 // y
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
			
			P yzPlane = projectNegativeX(xx, source.getMinY(), zz);
			if(yzPlane != null) {
				if(yzPlane.getAbsoluteEndX() + 1 < xx) {
					if(yzPlane.getAbsoluteEndX() >= source.getMinX()) {
						// found connected plane, so there is a 3x plane support still
						
						int x = yzPlane.getAbsoluteEndX() + 1;
						int y = source.getMinY();
						int z = zz;
						
						added.add(new Default3DPlanePoint3D(
							x, y, z,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane - some placement
							y, yzPlane.getAbsoluteEndY(), // y
							z, yzPlane.getAbsoluteEndZ(),  // z

							// xz plane
							x, xzPlane.getSupportedXZPlaneMaxX(), // x
							z, xzPlane.getSupportedXZPlaneMaxZ(), // z
							
							// xy plane (i.e. top of the new placement)
							x, xx - 1, // x
							y, yy - 1 // y
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
		
		if(!added.isEmpty()) {
			// TODO
		}
		return added;
	}
		
	
	private List<Point3D> addX(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz) {
		boolean xy = source.isSupportedXYPlane(xx, source.getMinY());
		boolean xz = source.isSupportedXZPlane(xx, source.getMinZ());

		int maxZ = projectPositiveZ(xx, source.getMinY(), source.getMinZ());
		int maxY = projectPositiveY(xx, source.getMinY(), source.getMinZ());

		List<Point3D> added = new ArrayList<>();
		if(xy && xz) {
			// 3x corner point
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
			
			int x = xx;
			int y = source.getMinY();
			int z = source.getMinZ(); 
			
			added.add(new Default3DPlanePoint3D(
					x, y, z,
					source.getMaxX(), maxY, maxZ,
					
					// supported planes
					// yz plane (i.e. side of the new placement)
					y, yy - 1, // y
					z, zz - 1, // z
					
					// xz plane
					x, xzPlane.getSupportedXZPlaneMaxX(), // x
					z, xzPlane.getSupportedXZPlaneMaxZ(),  // z
					
					// xy plane
					x, xyPlane.getSupportedXYPlaneMaxX(), // x
					y, xyPlane.getSupportedXYPlaneMaxY() // y
				)
			);
		} else if(xy) {

			XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
			
			P xzPlane = projectNegativeY(xx, yy, source.getMinZ());
			if(xzPlane != null) {
				if(xzPlane.getAbsoluteEndY() + 1 < yy) {
					if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
						// found connected plane, so there is a 3x plane support still
						
						int x = xx;
						int y = xzPlane.getAbsoluteEndY() + 1;
						int z = source.getMinZ();
						
						added.add(new Default3DPlanePoint3D(
							x, y, z,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane (i.e. side of the new placement, adjusted to xz plane)
							y, yy - 1, // y
							z, zz - 1, // z
							
							// xz plane - 
							x, xzPlane.getAbsoluteEndX(), // x
							z, xzPlane.getAbsoluteEndZ(),  // z

							// xy plane
							x, xyPlane.getSupportedXYPlaneMaxX(), // x
							y, xyPlane.getSupportedXYPlaneMaxY() // y
						));
						
					} else {
						// found unconnected plane, so there is a 1x plane support
						added.add(new DefaultXZPlanePoint3D(xx, xzPlane.getAbsoluteEndY() + 1, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, xx, xzPlane.getAbsoluteEndX(), source.getMinZ(), xzPlane.getAbsoluteEndZ()));
					}
				} else {
					// placement is in line with another point, skip
				}
			} else {
				// found container wall
				added.add(new DefaultXZPlanePoint3D(xx, 0, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, xx, containerMaxX, source.getMinZ(), containerMaxZ));
			}
			
		} else if(xz) {
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			
			P xyPlane = projectNegativeZ(xx, source.getMinY(), zz);
			if(xyPlane != null) {
				if(xyPlane.getAbsoluteEndZ() + 1 < zz) {
					if(xyPlane.getAbsoluteEndZ() >= source.getMinZ()) {
						// found connected plane, so there is a 3x plane support still
						
						int x = xx;
						int y = source.getMinY();
						int z = xyPlane.getAbsoluteEndZ() + 1;
						
						added.add(new Default3DPlanePoint3D(
								
							x, y, z,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane (i.e. side of the new placement, adjusted to xy plane)
							y, yy - 1, // y
							z, zz - 1,  // z

							// xz plane
							x, xzPlane.getSupportedXZPlaneMaxX(), // x
							z, xzPlane.getSupportedXZPlaneMaxZ(), // z
							
							// xy plane - some placement
							x, xyPlane.getAbsoluteEndX(), // x
							y, xyPlane.getAbsoluteEndY() // y
						));
						
					} else {
						// found unconnected plane, so there is a 1x plane support
						added.add(new DefaultXYPlanePoint3D(xx, source.getMinY(), xyPlane.getAbsoluteEndZ() + 1, containerMaxX, containerMaxY, containerMaxZ, xx, xyPlane.getAbsoluteEndX(), source.getMinY(), xyPlane.getAbsoluteEndY()));
					}
				} else {
					// placement is in line with another point, skip
				}
			} else {
				// found container wall
				added.add(new DefaultXYPlanePoint3D(xx, source.getMinY(), 0, containerMaxX, containerMaxY, containerMaxZ, xx, containerMaxX, source.getMinY(), containerMaxY));
			}
			
		} else {
			
			// two unsupported points - negative y and z direction
			P xzPlane = projectNegativeY(xx, yy, source.getMinZ());
			if(xzPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				added.add(new DefaultXZPlanePoint3D(xx, xzPlane.getAbsoluteEndY() + 1, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, xx, xzPlane.getAbsoluteEndX(), source.getMinZ(), xzPlane.getAbsoluteEndZ()));
			} else {
				// found container wall
				added.add(new DefaultXZPlanePoint3D(xx, 0, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, xx, containerMaxX, source.getMinZ(), containerMaxZ));
			}
			
			
			P xyPlane = projectNegativeZ(xx, source.getMinY(), zz);
			if(xyPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				added.add(new DefaultXYPlanePoint3D(xx, source.getMinY(), xyPlane.getAbsoluteEndZ() + 1, containerMaxX, containerMaxY, containerMaxZ, xx, xyPlane.getAbsoluteEndX(), source.getMinY(), xyPlane.getAbsoluteEndY()));
			} else {
				// found container wall
				added.add(new DefaultXYPlanePoint3D(xx, source.getMinY(), 0, containerMaxX, containerMaxY, containerMaxZ, xx, containerMaxX, source.getMinY(), containerMaxY));
			}
		}
		
		if(!added.isEmpty()) {
			// TODO
		}
		return added;
	}

	private List<Point3D> addY(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz) {
		boolean xy = source.isSupportedXYPlane(source.getMinX(), yy);
		boolean yz = source.isSupportedYZPlane(yy, source.getMinZ());

		int maxZ = projectPositiveZ(source.getMinX(), yy, source.getMinZ());
		int maxX = projectPositiveX(source.getMinX(), yy, source.getMinZ());
		
		
		List<Point3D> added = new ArrayList<>();
		if(xy && yz) {
			// 3x corner point
			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
			
			int x = source.getMinX();
			int y = yy;
			int z = source.getMinZ(); 
			
			added.add(new Default3DPlanePoint3D(
					x, y, z, 
					maxX, source.getMaxY(), maxZ,
					
					// supported planes
					// yz plane 
					y, yzPlane.getSupportedYZPlaneMaxY(), // y
					z, yzPlane.getSupportedYZPlaneMaxZ(), // z
					
					// xz plane (i.e. side of the new placement)
					x, xx - 1, // y
					z, zz - 1, // z

					// xy plane
					x, xyPlane.getSupportedXYPlaneMaxX(), // x
					y, xyPlane.getSupportedXYPlaneMaxY() // y
				)
			);
		} else if(xy) {

			XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
			
			P zyPlane = projectNegativeX(xx, yy, source.getMinZ());
			if(zyPlane != null) {
				if(zyPlane.getAbsoluteEndX() + 1 < xx) {
					if(zyPlane.getAbsoluteEndX() >= source.getMinX()) {
						// found connected plane, so there is a 3x plane support still
						
						int x = zyPlane.getAbsoluteEndX() + 1;
						int y = yy;
						int z = source.getMinZ();
						
						added.add(new Default3DPlanePoint3D(
							x, y, z,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane - placement
							y, zyPlane.getAbsoluteEndY(), // y
							z, zyPlane.getAbsoluteEndZ(), // z
							
							// xz plane - (i.e. side of the new placement, adjusted to zy plane)
							x, xx - 1, // x
							z, zz - 1,  // z

							// xy plane (adjusted by zy plane)
							x, xyPlane.getSupportedXYPlaneMaxX(), // x
							y, xyPlane.getSupportedXYPlaneMaxY() // y
						));
						
					} else {
						// found unconnected plane, so there is a 1x plane support
						added.add(new DefaultYZPlanePoint3D(zyPlane.getAbsoluteEndX() + 1, yy, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, yy, zyPlane.getAbsoluteEndY(), source.getMinZ(), zyPlane.getAbsoluteEndZ()));
					}
				} else {
					// placement is in line with another point, skip
				}
			} else {
				// found container wall
				added.add(new DefaultYZPlanePoint3D(0, yy, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, yy, containerMaxZ, source.getMinZ(), containerMaxZ));
			}
			
		} else if(yz) {
			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			
			P xyPlane = projectNegativeZ(source.getMinX(), yy, zz);
			if(xyPlane != null) {
				if(xyPlane.getAbsoluteEndZ() + 1 < zz) {
					if(xyPlane.getAbsoluteEndZ() >= source.getMinZ()) {
						// found connected plane, so there is a 3x plane support still
						
						int x = source.getMinX();
						int y = yy;
						int z = xyPlane.getAbsoluteEndZ() + 1;

						added.add(new Default3DPlanePoint3D(
								
							x, y, z,
							containerMaxX, containerMaxY, containerMaxZ,
							
							// supported planes
							// yz plane 
							y, yzPlane.getSupportedYZPlaneMaxY(), // y
							z, yzPlane.getSupportedYZPlaneMaxZ(),  // z

							// xz plane
							x, xx - 1, // x
							z, zz - 1, // z
							
							// xy plane - some placement
							x, xyPlane.getAbsoluteEndX(), // x
							y, xyPlane.getAbsoluteEndY() // y
						));
						
					} else {
						// found unconnected plane, so there is a 1x plane support
						added.add(new DefaultXYPlanePoint3D(source.getMinX(), yy, xyPlane.getAbsoluteEndZ() + 1, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), xyPlane.getAbsoluteEndX(), yy, xyPlane.getAbsoluteEndY()));
					}
				} else {
					// placement is in line with another point, skip
				}
			} else {
				// found container wall
				added.add(new DefaultXYPlanePoint3D(source.getMinX(), yy, 0, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), containerMaxX, yy, containerMaxY));
			}
			
		} else {
			
			// two unsupported points - negative y and z direction
			P zyPlane = projectNegativeX(xx, yy, source.getMinZ());
			if(zyPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				added.add(new DefaultYZPlanePoint3D(zyPlane.getAbsoluteEndX() + 1, yy, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, yy, zyPlane.getAbsoluteEndY(), source.getMinZ(), zyPlane.getAbsoluteEndZ()));
			} else {
				// found container wall
				added.add(new DefaultYZPlanePoint3D(0, yy, source.getMinZ(), containerMaxX, containerMaxY, containerMaxZ, yy, containerMaxZ, source.getMinZ(), containerMaxZ));
			}
			
			
			P xyPlane = projectNegativeZ(xx, source.getMinY(), zz);
			if(xyPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				added.add(new DefaultXYPlanePoint3D(source.getMinX(), yy, xyPlane.getAbsoluteEndZ() + 1, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), xyPlane.getAbsoluteEndX(), yy, xyPlane.getAbsoluteEndY()));
			} else {
				// found container wall
				added.add(new DefaultXYPlanePoint3D(source.getMinX(), yy, 0, containerMaxX, containerMaxY, containerMaxZ, source.getMinX(), containerMaxX, yy, containerMaxY));
			}
		}
		
		if(!added.isEmpty()) {
			// TODO
		}
		return added;
	}

	private void addAll(List<Point3D> add) {
		
		search:
		for (int i = 0; i < add.size(); i++) {
			Point3D p1 = add.get(i);
			
			// does a corresponding point already exist?
			
			// TODO binary search
			for (Point3D p2 : values) {
				if(p1.getMinX() == p2.getMinX() && p2.getMinY() == p1.getMinY() && p2.getMinZ() == p1.getMinZ()) {
					continue search;
				}
			}
			
			values.add(p1);	
		}
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
