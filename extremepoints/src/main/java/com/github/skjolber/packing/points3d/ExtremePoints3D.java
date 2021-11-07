package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.points2d.Point2D;

/**
 * 
 * TODO emulate all edges as virtual placements to avoid special case for contained edge
 */

public class ExtremePoints3D<P extends Placement3D> {
	
	private final int containerMaxX;
	private final int containerMaxY;
	private final int containerMaxZ;

	private List<Point3D> values = new ArrayList<>();
	private List<P> placements = new ArrayList<>();

	// reused working objects
	private List<Point3D> deleted = new ArrayList<>();
	private List<Point3D> addedX = new ArrayList<>();
	private List<Point3D> addedY = new ArrayList<>();
	private List<Point3D> addedZ = new ArrayList<>();


	public ExtremePoints3D(int dx, int dy, int dz) {
		super();
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;
		
		values.add(new Default3DPlanePoint3D(
				0, 0, 0, 
				containerMaxX, containerMaxY, containerMaxZ, 
				0, containerMaxY, 0, containerMaxZ, // fixed x
				0, containerMaxX, 0, containerMaxZ, // fixed y
				0, containerMaxX, 0, containerMaxY // fixed z
				));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1, placement.getAbsoluteEndZ() - placement.getAbsoluteZ() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy, int boxDz) {		
		Point3D source = values.get(index);

		/*
		System.out.println();
		System.out.println("*******************************************************************************");
		System.out.println();
		System.out.println("Add " + placement + " with " + values.size() + " points and " + placements.size() + " placements");
		for(Point2D v : values) {
			if(v == source) {
				System.out.println(" " + v + " (*)");
			} else {
				System.out.println(" " + v);
			}
		}
		
		System.out.println();
		for(P p : placements) {
			System.out.println( " " + p);
		}
*/
		values.remove(index);
		if(index != 0) {
			index--;
		}
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;
		int zz = source.getMinZ() + boxDz;
		
		boolean moveX = xx <= containerMaxX;
		boolean moveY = yy <= containerMaxY;
		boolean moveZ = zz <= containerMaxZ;
		
		// xx + zz
		// negative z
		// negative x
		//
		//
		//          ◄----------| without support
		//             ◄-------| with support yy
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
		if(moveX) {
			appendX(placement, source, deleted, xx, yy, zz, addedX);
		}
		if(moveY) {
			appendY(placement, source, deleted, xx, yy, zz, addedY);
		}
		if(moveZ) {
			appendZ(placement, source, deleted, xx, yy, zz, addedZ);
		}

		deleted.add(source);

		// Constrain max values to the new placement
		if(source instanceof XZPlanePoint3D && source instanceof YZPlanePoint3D && source instanceof XYPlanePoint3D) {
			for(Point3D point : values) {
				if(!constrainMax(point, placement)) {
					deleted.add(point);
				}
			}
		} else {
			for(Point3D point : values) {
				if(!constrainFloatingMax(point, placement, addedX, addedY, addedZ)) {
					deleted.add(point);
				}
			}
		}
		
		if(!deleted.isEmpty()) {
			values.removeAll(deleted);
		}

		addX(addedX, xx);
		addY(addedY, yy);
		addZ(addedZ, zz);
		
		// this should be optimized
		removeShadowedX(values);
		removeShadowedY(values);
		removeShadowedZ(values);

		placements.add(placement);
		Collections.sort(values, Point3D.COMPARATOR);

		addedX.clear();
		addedY.clear();
		addedZ.clear();
		
		deleted.clear();
		
		return !values.isEmpty();
	}	
	
	private void appendZ(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz, List<Point3D> added) {
		boolean yz = source.isSupportedYZPlane(source.getMinY(), zz);
		boolean xz = source.isSupportedXZPlane(source.getMinX(), zz);

		boolean xzEdge = source.isXZPlaneEdgeZ(yy);
		boolean yzEdge = source.isYZPlaneEdgeZ(yy);

		int maxX = projectPositiveX(source.getMinX(), source.getMinY(), zz);
		int maxY = projectPositiveY(source.getMinX(), source.getMinY(), zz);
		
		DefaultXZPlanePoint3D unconnectedXz = null;
		DefaultYZPlanePoint3D unconnectedYz = null;
		
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
			if(!xzEdge) {
				YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
				
				P xzPlane = projectNegativeY(source.getMinX(), yy, zz);
				if(xzPlane != null) {
					if(xzPlane.getAbsoluteEndY() + 1 == yy) {
						// placement is in line with another point, skip
					} else {
						
						int x = source.getMinX();
						int y = xzPlane.getAbsoluteEndY() + 1;
						int z = zz;
	
						int pointMaxX = constrainX(x, y, z);
						int pointMaxZ = constrainZ(x, y, z);

						if(pointMaxZ >= z && pointMaxX >= x) {

							if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									pointMaxX, maxY, pointMaxZ,
									
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
								added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, xzPlane.getAbsoluteEndX(), z, xzPlane.getAbsoluteEndZ()));
							}
						}
					}
				} else {
					// found unconnected plane (container wall)
					int x = source.getMinX();
					int y = 0;
					int z = zz;
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxZ >= z && pointMaxX >= x) {
						added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, containerMaxX, z, containerMaxZ));
					}
				}
			}
		} else if(xz) {
			if(!yzEdge) {
				XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
				
				P yzPlane = projectNegativeX(xx, source.getMinY(), zz);
				if(yzPlane != null) {
					if(yzPlane.getAbsoluteEndX() + 1 == xx) {
						// placement is in line with another point, skip
					} else {
						int x = yzPlane.getAbsoluteEndX() + 1;
						int y = source.getMinY();
						int z = zz;

						int pointMaxY = constrainY(x, y, z);
						int pointMaxZ = constrainZ(x, y, z);

						if(pointMaxZ >= z && pointMaxY >= y) {

							if(yzPlane.getAbsoluteEndX() >= source.getMinX()) {
								// found connected plane, so there is a 3x plane support still
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									maxX, pointMaxY, pointMaxZ,
									
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
								added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, yzPlane.getAbsoluteEndY(), z, yzPlane.getAbsoluteEndZ()));
							}
						}
					}
				} else {
					// found unconnected plane (container wall)
					int x = 0;
					int y = source.getMinY();
					int z = zz;

					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxZ >= z && pointMaxY >= y) {
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, containerMaxY, z, containerMaxZ));
					}
				}
			}
			
		} else {
			// two unsupported points - negative x and y direction
			P yzPlane = projectNegativeX(source.getMinX(), source.getMinY(), zz);
			if(yzPlane != null) {
				
				if(yzPlane.getAbsoluteEndX() + 1 == xx) {
					// placement is in line with another point, skip
				} else {

					// found unconnected plane, so there is a 1x plane support
					int x = yzPlane.getAbsoluteEndX() + 1;
					int y = source.getMinY();
					int z = zz;
	
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
	
					if(pointMaxZ >= z && pointMaxY >= y) {
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, yzPlane.getAbsoluteEndY(), zz, yzPlane.getAbsoluteEndZ()));
					}
				}
			} else {
				// found container wall
				int x = 0;
				int y = source.getMinY();
				int z = zz;

				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);
				
				if(pointMaxZ >= z && pointMaxY >= y) {
					added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, containerMaxY, zz, containerMaxZ));
				}
			}
			
			P xzPlane = projectNegativeY(source.getMinX(), source.getMinY(), zz);
			if(xzPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				if(xzPlane.getAbsoluteEndY() + 1 == yy) {
					// placement is in line with another point, skip
				} else {
					int x = source.getMinX();
					int y = xzPlane.getAbsoluteEndY() + 1;
					int z = zz;
	
					int pointMaxX = constrainX(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
	
					if(pointMaxZ >= z && pointMaxX >= x) {
						added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, xzPlane.getAbsoluteEndX(), z, xzPlane.getAbsoluteEndZ()));
					}
				}
			} else {
				// found container wall
				int x = source.getMinX();
				int y = 0;
				int z = zz;

				int pointMaxX = constrainX(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxZ >= z && pointMaxX >= x) {
					added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, containerMaxX, z, containerMaxZ));

				}
			}
		}
		
		addShadowedOrSwallowedZZ(placement, source, deleted, zz, added);

		if(unconnectedXz != null) { // negative y
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
				
				if(point.swallowsMinY(unconnectedXz.getMinY(), placement.getAbsoluteEndY())) {
					if(point.swallowsMinX(placement.getAbsoluteX(), placement.getAbsoluteEndX())) {
						if(point.getMinZ() < zz) {
							if(zz < point.getMaxZ()) {
								
								int x = point.getMinX();
								int y = point.getMinY();
								int z = zz;
		
								int pointMaxX = constrainX(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								
								if(pointMaxZ >= z && pointMaxX >= x) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, maxY, pointMaxZ));
								}
							}

						}
					}
				}
			}
		}
		
		if(unconnectedYz != null) { // negative x
			
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
				
				if(point.swallowsMinX(unconnectedYz.getMinX(), placement.getAbsoluteEndX())) {
					if(point.swallowsMinY(placement.getAbsoluteY(), placement.getAbsoluteEndY())) {
						if(point.getMinZ() < zz) {
							if(zz < point.getMaxZ()) {
								
								int x = point.getMinX();
								int y = point.getMinY();
								int z = zz;
		
								int pointMaxY = constrainY(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								
								if(pointMaxZ >= z && pointMaxY >= y) {
									added.add(new DefaultPoint3D(x, y, z, maxX, pointMaxY, pointMaxZ));
								}
							}

						}
					}
				}
			}
		}
		
		removeShadowedZ(added);
	}
	
	private void appendX(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz, List<Point3D> added) {
		boolean xy = source.isSupportedXYPlane(xx, source.getMinY());
		boolean xz = source.isSupportedXZPlane(xx, source.getMinZ());

		boolean xyEdge = source.isXYPlaneEdgeX(xx);
		boolean xzEdge = source.isXZPlaneEdgeX(xx);
				
		int maxZ = projectPositiveZ(xx, source.getMinY(), source.getMinZ());
		int maxY = projectPositiveY(xx, source.getMinY(), source.getMinZ());

		DefaultXYPlanePoint3D unconnetedXy = null;
		DefaultXZPlanePoint3D unconnetedXz = null;

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
					z, xzPlane.getSupportedXZPlaneMaxZ(), // z
					
					// xy plane
					x, xyPlane.getSupportedXYPlaneMaxX(), // x
					y, xyPlane.getSupportedXYPlaneMaxY() // y
				)
			);
		} else if(xy) {

			if(!xzEdge) {
				XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
				
				P xzPlane = projectNegativeY(xx, yy, source.getMinZ());
				if(xzPlane != null) {
					if(xzPlane.getAbsoluteEndY() + 1 == yy) {
						// placement is in line with another point, skip
					} else {
						int x = xx;
						int y = xzPlane.getAbsoluteEndY() + 1;
						int z = source.getMinZ();
						
						int pointMaxX = constrainX(x, y, z);
						int pointMaxZ = constrainZ(x, y, z);

						if(pointMaxX >= x && pointMaxZ >= z) {
						
							if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									pointMaxX, maxY, pointMaxZ,
									
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
								added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, xzPlane.getAbsoluteEndX(), z, xzPlane.getAbsoluteEndZ()));
							}
						}
					}
				} else {
					// found unconnected plane (container wall)
					int x = xx;
					int y = 0;
					int z = source.getMinZ();
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxX >= x && pointMaxZ >= z) {
						added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, xx, containerMaxX, z, containerMaxZ));
					}
				}
			}
		} else if(xz) {
			
			if(!xyEdge) {
				XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
				
				P xyPlane = projectNegativeZ(xx, source.getMinY(), zz);
				if(xyPlane != null) {
					if(xyPlane.getAbsoluteEndZ() + 1 == zz) {
						// placement is in line with another point, skip
					} else {
						
						int x = xx;
						int y = source.getMinY();
						int z = xyPlane.getAbsoluteEndZ() + 1;

						int pointMaxX = constrainX(x, y, z);
						int pointMaxY = constrainY(x, y, z);

						if(pointMaxX >= x && pointMaxY >= y) {
	
							if(xyPlane.getAbsoluteEndZ() >= source.getMinZ()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
										
									x, y, z,
									pointMaxX, pointMaxY, maxZ,
									
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
								added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, xyPlane.getAbsoluteEndX(), y, xyPlane.getAbsoluteEndY()));
							}
						}
					}
				} else {
					// found unconnected plane (container wall)
					int x = xx;
					int y = source.getMinY();
					int z = 0;
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
					
					if(pointMaxX >= x && pointMaxY >= y) {
						added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, containerMaxX, y, containerMaxY));
					}
				}
			}
			
		} else {
			// two unsupported points - negative y and z direction
			P xzPlane = projectNegativeY(xx, yy, source.getMinZ());
			if(xzPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				if(xzPlane.getAbsoluteEndY() + 1 == yy) {
					// placement is in line with another point, skip
				} else {
					int x = xx;
					int y = xzPlane.getAbsoluteEndY() + 1;
					int z = source.getMinZ();
	
					int pointMaxX = constrainX(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
	
					if(pointMaxX >= x && pointMaxZ >= z) {
						added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, xzPlane.getAbsoluteEndX(), z, xzPlane.getAbsoluteEndZ()));
					}
				}
			} else {
				// found unconnected plane (container wall)
				
				int x = xx;
				int y = 0;
				int z = source.getMinZ();

				int pointMaxX = constrainX(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxX >= x && pointMaxZ >= z) {
					added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, x, containerMaxX, z, containerMaxZ));
				}
			}
			
			P xyPlane = projectNegativeZ(xx, source.getMinY(), zz);
			if(xyPlane != null) {
				if(xyPlane.getAbsoluteEndZ() + 1 == zz) {
					// placement is in line with another point, skip
				} else {
					// found unconnected plane, so there is a 1x plane support
					int x = xx;
					int y = source.getMinY();
					int z = xyPlane.getAbsoluteEndZ() + 1;
	
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
	
					if(pointMaxX >= x && pointMaxY >= y) {
						added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, xyPlane.getAbsoluteEndX(), y, xyPlane.getAbsoluteEndY()));
					}
				}
			} else {
				// found unconnected plane (container wall)
				int x = xx;
				int y = source.getMinY();
				int z = 0;

				int pointMaxX = constrainX(x, y, z);
				int pointMaxY = constrainY(x, y, z);
				
				if(pointMaxX >= x && pointMaxY >= y) {
					added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, containerMaxX, y, containerMaxY));
				}
			}
		}
		
		// project within 
		addShadowedOrSwallowedXX(placement, source, deleted, xx, added);

		if(unconnetedXy != null) { // negative z
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
				
				if(point.swallowsMinZ(unconnetedXy.getMinZ(), placement.getAbsoluteEndZ())) {
					if(point.swallowsMinY(placement.getAbsoluteY(), placement.getAbsoluteEndY())) {
						if(point.getMinX() < xx ) {
							if(xx < point.getMaxX()) {
	
								int x = xx;
								int y = point.getMinY();
								int z = point.getMinZ();
		
								int pointMaxX = constrainX(x, y, z);
								int pointMaxY = constrainY(x, y, z);
								
								if(pointMaxX >= x && pointMaxY >= y) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, maxZ));
								}
							}
						}
					}
				}
			}
		}
		
		if(unconnetedXz != null) { // negative z
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
				
				if(point.swallowsMinY(unconnetedXz.getMinY(), placement.getAbsoluteEndY())) {
					if(point.swallowsMinZ(placement.getAbsoluteZ(), placement.getAbsoluteEndZ())) {
						if(point.getMinX() < xx) {
							if(xx < point.getMaxX()) {
								
								int x = xx;
								int y = point.getMinY();
								int z = point.getMinZ();
		
								int pointMaxX = constrainX(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								
								if(pointMaxX >= x && pointMaxZ >= z) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, maxY, pointMaxZ));
								}
							}
						}
					}
				}
			}
		}
		
		removeShadowedX(added);
	}
	
	protected void removeShadowedX(List<Point3D> added) {
		for (int j = 0; j < added.size(); j++) {
			Point3D point2d = added.get(j);
			
			for (int i = j + 1; i < added.size(); i++) {
				Point3D p = added.get(i);

				// is the new point shadowed, or shadowing an existing point?
				if(point2d.isMax(p)) {
					if(point2d.containsInYZPlane(p)) {
						added.remove(i);
						i--;
					} else if(p.containsInYZPlane(point2d)) {
						added.remove(j);
						j--;
						
						break;
					}
				}				
			}
		}
	}
	
	protected void removeShadowedY(List<Point3D> added) {
		for (int j = 0; j < added.size(); j++) {
			Point3D point2d = added.get(j);
			
			for (int i = j + 1; i < added.size(); i++) {
				Point3D p = added.get(i);

				// is the new point shadowed, or shadowing an existing point?
				if(point2d.isMax(p)) {
					if(point2d.containsInXZPlane(p)) {
						added.remove(i);
						i--;
					} else if(p.containsInXZPlane(point2d)) {
						added.remove(j);
						j--;
						
						break;
					}
				}				
			}
		}
	}

	protected void removeShadowedZ(List<Point3D> added) {
		for (int j = 0; j < added.size(); j++) {
			Point3D point2d = added.get(j);
			
			for (int i = j + 1; i < added.size(); i++) {
				Point3D p = added.get(i);

				// is the new point shadowed, or shadowing an existing point?
				if(point2d.isMax(p)) {
					if(point2d.containsInXYPlane(p)) {
						added.remove(i);
						i--;
					} else if(p.containsInXYPlane(point2d)) {
						added.remove(j);
						j--;
						
						break;
					}
				}				
			}
		}
	}
	
	
	protected void addSwallowedXX(P placement, Point3D source, List<Point3D> deleted, int xx, List<Point3D> added) {
		//    swallowed:
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------> x
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    |         *
		//    |         | 
		//    |---------|---------------> x			
		//
		
		for (int i = 0; i < values.size(); i++) {
			Point3D point = values.get(i);

			// Move points swallowed by the placement
			if(point.swallowsMinX(source.getMinX(), xx) && point.fitsInYZPlane(placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support
					int maxY = constrainIfNotMaxY(source, xx, point.getMinZ());
					int maxZ = constrainIfNotMaxZ(source, xx, point.getMinY());

					DefaultYZPlanePoint3D next = new DefaultYZPlanePoint3D(
							xx, point.getMinY(), point.getMinZ(),
							point.getMaxX(), maxY, maxZ,
							
							point.getMinY(), placement.getAbsoluteEndY(),
							point.getMinZ(), placement.getAbsoluteEndZ()
							);

					added.add(next);
				}
				
				// delete current point (which was swallowed)
				deleted.add(point);
			}
		}
	}
	
	protected void addShadowedOrSwallowedXX(P placement, Point3D source, List<Point3D> deleted, int xx, List<Point3D> added) {
		//    swallowed:
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------> x
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    |         *
		//    |         | 
		//    |---------|---------------> x			
		//
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
			Point3D point = values.get(i);

			// Move points swallowed by the placement
			if(point.isShadowedOrSwallowedByX(source.getMinX(), xx) && point.fitsInYZPlane(placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support
					int maxY = constrainIfNotMaxY(source, xx, point.getMinZ());
					int maxZ = constrainIfNotMaxZ(source, xx, point.getMinY());

					DefaultYZPlanePoint3D next = new DefaultYZPlanePoint3D(
							xx, point.getMinY(), point.getMinZ(),
							point.getMaxX(), maxY, maxZ,
							
							point.getMinY(), placement.getAbsoluteEndY(),
							point.getMinZ(), placement.getAbsoluteEndZ()
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
	}
	
	protected void addSwallowedYY(P placement, Point3D source, List<Point3D> deleted, int yy, List<Point3D> added) {
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

		for (int i = 0; i < values.size(); i++) {
			Point3D point = values.get(i);
		
			// Move points swallowed by the placement
			if(point.swallowsMinY(source.getMinY(), yy) && point.fitsInXZPlane(placement)) {
				if(point.getMaxY() > yy) {
					
					int maxX = constrainIfNotMaxX(source, yy, point.getMinZ());
					int maxZ = constrainIfNotMaxZ(source, point.getMinX(), yy);

					DefaultXZPlanePoint3D next = new DefaultXZPlanePoint3D(
							point.getMinX(), yy, point.getMinZ(),
							maxX, point.getMaxY(), maxZ,
							
							point.getMinX(), placement.getAbsoluteEndX(),
							point.getMinZ(), placement.getAbsoluteEndZ()
						);
					
					added.add(next);
				} 
				
				// delete current point (which was swallowed)
				deleted.add(point);
			}
		}

		// removeShadowedX(added);
	}
	
	
	protected void addShadowedOrSwallowedYY(P placement, Point3D source, List<Point3D> deleted, int yy, List<Point3D> added) {
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

		for (int i = 0; i < values.size(); i++) {
			Point3D point = values.get(i);
		
			// Move points swallowed by the placement
			if(point.isShadowedOrSwallowedByY(source.getMinY(), yy) && point.fitsInXZPlane(placement)) {
				if(point.getMaxY() > yy) {
					
					int maxX = constrainIfNotMaxX(source, yy, point.getMinZ());
					int maxZ = constrainIfNotMaxZ(source, point.getMinX(), yy);

					DefaultXZPlanePoint3D next = new DefaultXZPlanePoint3D(
							point.getMinX(), yy, point.getMinZ(),
							maxX, point.getMaxY(), maxZ,
							
							point.getMinX(), placement.getAbsoluteEndX(),
							point.getMinZ(), placement.getAbsoluteEndZ()
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

		// removeShadowedX(added);
	}
		
		
	protected void addSwallowedZZ(P placement, Point3D source, List<Point3D> deleted, int zz, List<Point3D> added) {
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

		for (int i = 0; i < values.size(); i++) {
			Point3D point = values.get(i);
		
			// Move points swallowed by the placement
			if(point.swallowsMinZ(source.getMinZ(), zz) && point.fitsInXYPlane(placement)) {
				if(point.getMaxZ() > zz) {
					
					int maxX = constrainIfNotMaxX(source, point.getMinY(), zz);
					int maxY = constrainIfNotMaxY(source, point.getMinX(), zz);

					DefaultXYPlanePoint3D next = new DefaultXYPlanePoint3D(
							point.getMinX(), point.getMinY(), zz,
							maxX, maxY, point.getMaxZ(),
							
							point.getMinX(), placement.getAbsoluteEndX(),
							point.getMinY(), placement.getAbsoluteEndY()
						);
					
					added.add(next);
				} 
				// delete current point (which was swallowed)
				deleted.add(point);
			}
		}

		// removeShadowedX(added);
	}
	
	
	protected void addShadowedOrSwallowedZZ(P placement, Point3D source, List<Point3D> deleted, int zz, List<Point3D> added) {
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

		for (int i = 0; i < values.size(); i++) {
			Point3D point = values.get(i);
		
			// Move points swallowed by the placement
			if(point.isShadowedOrSwallowedZ(source.getMinZ(), zz) && point.fitsInXYPlane(placement)) {
				if(point.getMaxZ() > zz) {
					
					int maxX = constrainIfNotMaxX(source, point.getMinY(), zz);
					int maxY = constrainIfNotMaxY(source, point.getMinX(), zz);

					DefaultXYPlanePoint3D next = new DefaultXYPlanePoint3D(
							point.getMinX(), point.getMinY(), zz,
							maxX, maxY, point.getMaxZ(),
							
							point.getMinX(), placement.getAbsoluteEndX(),
							point.getMinY(), placement.getAbsoluteEndY()
						);
					
					added.add(next);
				}
				
				if(point.getMinZ() < source.getMinZ()) {
					// constrain current point
					point.setMaxZ(source.getMinZ() - 1);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
	}

	private void appendY(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz, List<Point3D> added) {
		boolean xy = source.isSupportedXYPlane(source.getMinX(), yy);
		boolean yz = source.isSupportedYZPlane(yy, source.getMinZ());

		boolean xyEdge = source.isXYPlaneEdgeY(yy);
		boolean yzEdge = source.isYZPlaneEdgeY(yy);

		int maxZ = projectPositiveZ(source.getMinX(), yy, source.getMinZ());
		int maxX = projectPositiveX(source.getMinX(), yy, source.getMinZ());
		
		DefaultXYPlanePoint3D unconnectedXy = null;
		DefaultYZPlanePoint3D unconnectedYz = null;
		
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
					x, xx - 1, // x
					z, zz - 1, // z

					// xy plane
					x, xyPlane.getSupportedXYPlaneMaxX(), // x
					y, xyPlane.getSupportedXYPlaneMaxY() // y
				)
			);
		} else if(xy) {

			if(!yzEdge) {
				XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
				
				P zyPlane = projectNegativeX(xx, yy, source.getMinZ());
				if(zyPlane != null) {
					if(zyPlane.getAbsoluteEndX() + 1 == xx) {
						// placement is in line with another point, skip
					} else {
						
						int x = zyPlane.getAbsoluteEndX() + 1;
						int y = yy;
						int z = source.getMinZ();
						
						int pointMaxY = constrainY(x, y, z);
						int pointMaxZ = constrainZ(x, y, z);
						
						if(pointMaxZ >= z && pointMaxY >= y) {

							if(zyPlane.getAbsoluteEndX() >= source.getMinX()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									maxX, pointMaxY, pointMaxZ,
									
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
								added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, zyPlane.getAbsoluteEndY(), z, zyPlane.getAbsoluteEndZ()));
							}
						}
					}
				} else {
					// found unconnected plane (container wall)
					
					int x = 0;
					int y = yy;
					int z = source.getMinZ();
					
					int pointMaxY = constrainY(0, yy, source.getMinZ());
					int pointMaxZ = constrainZ(0, yy, source.getMinZ());
					
					if(pointMaxZ >= z && pointMaxY >= y) {
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, containerMaxY, source.getMinZ(), containerMaxZ));
					}
				}
			}
			
		} else if(yz) {
			if(!xyEdge) {
				YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
				
				P xyPlane = projectNegativeZ(source.getMinX(), yy, zz);
				if(xyPlane != null) {
					if(xyPlane.getAbsoluteEndZ() + 1 == zz) {
						// placement is in line with another point, skip
					} else {
						
						int x = source.getMinX();
						int y = yy;
						int z = xyPlane.getAbsoluteEndZ() + 1;

						int pointMaxX = constrainX(x, y, z);
						int pointMaxY = constrainY(x, y, z);

						if(pointMaxX >= x && pointMaxY >= y) {

							if(xyPlane.getAbsoluteEndZ() >= source.getMinZ()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
		
									x, y, z,
									pointMaxX, pointMaxY, maxZ,
									
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
								added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, xyPlane.getAbsoluteEndX(), y, xyPlane.getAbsoluteEndY()));
							}
						}
					}
				} else {
					// found unconnected plane (container wall)
					int x = source.getMinX();
					int y = yy;
					int z = 0;
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);

					if(pointMaxX >= x && pointMaxY >= y) {
						added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, containerMaxX, y, containerMaxY));
					}
				}
			}
				
		} else {
			
			// two unsupported points - negative x and z direction
			P zyPlane = projectNegativeX(xx, yy, source.getMinZ());
			if(zyPlane != null) {
				// found unconnected plane, so there is a 1x plane support
				if(zyPlane.getAbsoluteEndX() + 1 == xx) {
					// placement is in line with another point, skip
				} else {
	
					int x = zyPlane.getAbsoluteEndX() + 1;
					int y = yy;
					int z = source.getMinZ();
					
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxZ >= z && pointMaxY >= y) {
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, zyPlane.getAbsoluteEndY(), z, zyPlane.getAbsoluteEndZ()));
					}
				}
			} else {
				// found container wall
				int x = 0;
				int y = yy;
				int z = source.getMinZ();
				
				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxZ >= z && pointMaxY >= y) {
					added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, y, containerMaxY, z, containerMaxZ));
				}
			}
			
			P xyPlane = projectNegativeZ(xx, source.getMinY(), zz);
			if(xyPlane != null) {
				if(xyPlane.getAbsoluteEndZ() + 1 == zz) {
					// placement is in line with another point, skip
				} else {

					// found unconnected plane, so there is a 1x plane support
					int x = source.getMinX();
					int y = yy;
					int z = xyPlane.getAbsoluteEndZ() + 1;
	
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
	
					if(pointMaxX >= x && pointMaxY >= y) {
						added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, xyPlane.getAbsoluteEndX(), y, xyPlane.getAbsoluteEndY()));
					}
				}
			} else {
				// found container wall
				int x = source.getMinX();
				int y = yy;
				int z = 0;

				int pointMaxX = constrainX(x, y, z);
				int pointMaxY = constrainY(x, y, z);

				if(pointMaxX >= x && pointMaxY >= y) {
					added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, maxZ, x, containerMaxX, y, containerMaxY));
				}
			}
		}
		
		addShadowedOrSwallowedYY(placement, source, deleted, yy, added);

		if(unconnectedXy != null) { // negative z
			
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
				
				if(point.swallowsMinZ(unconnectedXy.getMinZ(), placement.getAbsoluteEndZ())) {
					if(point.swallowsMinX(placement.getAbsoluteX(), placement.getAbsoluteEndX())) {
						if(point.getMinY() < yy) {
							if(yy < point.getMaxY()) {
								int x = point.getMinX();
								int y = yy;
								int z = point.getMinZ();
								
								int pointMaxX = constrainX(x, y, z);
								int pointMaxY = constrainY(x, y, z);
								
								if(pointMaxX >= x && pointMaxY >= y) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, maxZ));
								}
							}

						}
					}
				}
			}
		}
		
		if(unconnectedYz != null) { // negative x
		
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
				
				if(point.swallowsMinX(unconnectedYz.getMinX(), placement.getAbsoluteEndX())) {
					if(point.swallowsMinZ(placement.getAbsoluteZ(), placement.getAbsoluteEndZ())) {
						if(point.getMinY() < yy) {
							if(yy < point.getMaxY()) {
								
								int x = point.getMinX();
								int y = yy;
								int z = point.getMinZ();
		
								int pointMaxY = constrainY(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								
								if(pointMaxZ >= z && pointMaxY >= y) {
									added.add(new DefaultPoint3D(x, y, z, maxX, pointMaxY, pointMaxZ));
								}
							}
						}
					}
				}
			}
		}
		
		removeShadowedY(added);
	}
	
	protected void addX(List<Point3D> add, int x) {
		int index = 0;
		while(index < values.size()) {
			Point3D existing = values.get(index);
			
			if(existing.getMinX() == x) {
				for (int i = 0; i < add.size(); i++) {
					Point3D p1 = add.get(i);

					if(p1.isMax(existing)) {

						// is the new point shadowed, or shadowing an existing point?
						if(existing.containsInYZPlane(p1)) {
							add.remove(i);
							i--;
						} else if(p1.containsInYZPlane(existing)) {
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
	
	protected void addY(List<Point3D> add, int y) {
		int index = 0;
		while(index < values.size()) {
			Point3D existing = values.get(index);
			
			if(existing.getMinY() == y) {
				for (int i = 0; i < add.size(); i++) {
					Point3D p1 = add.get(i);

					if(p1.isMax(existing)) {

						// is the new point shadowed, or shadowing an existing point?
						if(existing.containsInXZPlane(p1)) {
							add.remove(i);
							i--;
						} else if(p1.containsInXZPlane(existing)) {
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
	
	protected void addZ(List<Point3D> add, int z) {
		int index = 0;
		while(index < values.size()) {
			Point3D existing = values.get(index);
			
			if(existing.getMinZ() == z) {
				for (int i = 0; i < add.size(); i++) {
					Point3D p1 = add.get(i);

					if(p1.isMax(existing)) {

						// is the new point shadowed, or shadowing an existing point?
						if(existing.containsInXYPlane(p1)) {
							add.remove(i);
							i--;
						} else if(p1.containsInXYPlane(existing)) {
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
	
	protected boolean constrainMax(Point3D point, P placement) {
		int maxX = projectPositiveX(point, placement, point.getMaxX());
		if(maxX < point.getMinX()) {
			return false;
		}
		int maxY = projectPositiveY(point, placement, point.getMaxY());
		if(maxY < point.getMinY()) {
			return false;
		}
		int maxZ = projectPositiveZ(point, placement, point.getMaxZ());
		if(maxZ < point.getMinZ()) {
			return false;
		}
		
		point.setMaxX(maxX);
		point.setMaxY(maxY);
		point.setMaxZ(maxZ);

		return true;
	}

	protected int projectPositiveY(Point3D point, P placement, int maxY) {
		if(placement.getAbsoluteY() >= point.getMinY()) {
			if(point.fitsInXZPlane(placement)) {
				int limit = placement.getAbsoluteY() - 1;
				if(limit < maxY) {
					maxY = limit;
				}
			}
		}
		
		return maxY;
	}

	protected int projectPositiveX(Point3D point, P placement, int maxX) {
		if(placement.getAbsoluteX() >= point.getMinX()) {
			if(point.fitsInYZPlane(placement)) {
				int limit = placement.getAbsoluteX() - 1;
				if(limit < maxX) {
					maxX = limit;
				}
			}
		}
		return maxX;
		
	}

	protected int projectPositiveZ(Point3D point, P placement, int maxZ) {
		if(placement.getAbsoluteZ() >= point.getMinZ()) {
			if(point.fitsInXYPlane(placement)) {
				int limit = placement.getAbsoluteZ() - 1;
				if(limit < maxZ) {
					maxZ = limit;
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
			return closestUp.getAbsoluteX() - 1;
		}
		return containerMaxX;
	}

	protected int projectPositiveY(int x, int y, int z) {
		P closestUp = closestPositiveY(x, y, z);
		if(closestUp != null) {
			return closestUp.getAbsoluteY() - 1;
		}
		return containerMaxY;
	}
	
	protected int projectPositiveZ(int x, int y, int z) {
		P closestUp = closestPositiveZ(x, y, z);
		if(closestUp != null) {
			return closestUp.getAbsoluteZ() - 1;
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
	
	public boolean withinX(int x, P placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}

	public boolean withinY(int y, P placement) {
		return placement.getAbsoluteY() <= y && y <= placement.getAbsoluteEndY();
	}
	
	public boolean withinZ(int z, P placement) {
		return placement.getAbsoluteZ() <= z && z <= placement.getAbsoluteEndZ();
	}
	
	public int getDepth() {
		return containerMaxY;
	}
	
	public int getWidth() {
		return containerMaxX;
	}
	
	public int getHeight() {
		return containerMaxZ;
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

	public Point3D getValue(int i) {
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

	protected int constrainIfNotMaxY(Point3D source, int x, int z) {
		int maxY;
		if(isMaxContainer(source)) {
			maxY = containerMaxY;
		} else {
			maxY = constrainY(x, source.getMinY(), z);
		}
		return maxY;
	}

	private boolean isMaxContainer(Point3D source) {
		return source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY && source.getMaxZ() == containerMaxZ;
	}

	protected int constrainIfNotMaxZ(Point3D source, int x, int y) {
		int maxZ;
		if(isMaxContainer(source)) {
			maxZ = containerMaxZ;
		} else {
			maxZ = constrainZ(x, y, source.getMinZ());
		}
		return maxZ;
	}

	protected int constrainIfNotMaxX(Point3D source, int y, int z) {
		int maxX;
		if(isMaxContainer(source)) {
			maxX = containerMaxX;
		} else {
			maxX = constrainX(source.getMinX(), y, z);
		}
		return maxX;
	}

	protected int constrainX(int x, int y, int z) {
		// constrain up
		P closestX = closestPositiveX(x, y, z);
		if(closestX != null) {
			return closestX.getAbsoluteX() - 1;
		} else {
			return containerMaxX;
		}
	}

	protected int constrainY(int x, int y, int z) {
		// constrain up
		P closestY = closestPositiveY(x, y, z);
		if(closestY != null) {
			return closestY.getAbsoluteY() - 1;
		} else {
			return containerMaxY;
		}
	}

	protected int constrainZ(int x, int y, int z) {
		// constrain up
		P closestZ = closestPositiveZ(x, y, z);
		if(closestZ != null) {
			return closestZ.getAbsoluteZ() - 1;
		} else {
			return containerMaxZ;
		}
	}

	protected boolean constrainFloatingMax(Point3D point, P placement, List<Point3D> addX, List<Point3D> addY, List<Point3D> addZ) {

		if(placement.getAbsoluteEndX() < point.getMinX()) {
			return true;
		}

		if(placement.getAbsoluteEndY() < point.getMinY()) {
			return true;
		}

		if(placement.getAbsoluteEndZ() < point.getMinZ()) {
			return true;
		}

		if(placement.getAbsoluteX() > point.getMaxX()) {
			return true;
		}

		if(placement.getAbsoluteY() > point.getMaxY()) {
			return true;
		}	

		if(placement.getAbsoluteZ() > point.getMaxZ()) {
			return true;
		}
		
		boolean x = placement.getAbsoluteX() > point.getMinX();
		boolean y = placement.getAbsoluteY() > point.getMinY();
		boolean z = placement.getAbsoluteZ() > point.getMinZ();

		if(x) {
			addX.add(point.clone(placement.getAbsoluteX() - 1, point.getMaxY(), point.getMaxZ()));
		}
		
		if(y) {
			addY.add(point.clone(point.getMaxX(), placement.getAbsoluteY() - 1, point.getMaxZ()));
		}

		if(z) {
			addZ.add(point.clone(point.getMaxX(), point.getMaxY(), placement.getAbsoluteZ() - 1));
		}
		
		return !(x || y || z);
	}	

	
}
