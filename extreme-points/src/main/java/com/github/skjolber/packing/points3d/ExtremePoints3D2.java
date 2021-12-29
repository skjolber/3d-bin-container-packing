package com.github.skjolber.packing.points3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.ExtremePoints;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.Point3D;

/**
 * 
 * TODO emulate all edges as virtual placements to avoid special case for contained edge
 */

public class ExtremePoints3D2<P extends Placement3D> implements ExtremePoints<P, Point3D<P>> {
	
	protected int containerMaxX;
	protected int containerMaxY;
	protected int containerMaxZ;

	protected List<Point3D<P>> values = new ArrayList<>();
	protected List<P> placements = new ArrayList<>();

	// reused working objects
	protected List<Point3D<P>> deleted = new ArrayList<>();
	protected List<Point3D<P>> addedX = new ArrayList<>();
	protected List<Point3D<P>> addedY = new ArrayList<>();
	protected List<Point3D<P>> addedZ = new ArrayList<>();

	protected Placement3D containerPlacement;
	protected final boolean cloneOnConstrain;
	
	protected boolean floatingPlacementXY = false;
	protected boolean floatingPlacementXZ = false;
	protected boolean floatingPlacementYZ = false;

	public ExtremePoints3D2(int dx, int dy, int dz) {
		this(dx, dy, dz, false);
	}

	public ExtremePoints3D2(int dx, int dy, int dz, boolean cloneOnConstrain) {
		setSize(dx, dy, dz);
		this.cloneOnConstrain = cloneOnConstrain;
		addFirstPoint();
	}
	
	public ExtremePoints3D2(boolean cloneOnConstrain, Placement3D containerPlacement, List<Point3D<P>> values, List<P> placements) {
		this.containerMaxX = containerPlacement.getAbsoluteEndX();
		this.containerMaxY = containerPlacement.getAbsoluteEndY();
		this.containerMaxZ = containerPlacement.getAbsoluteEndZ();
		
		this.cloneOnConstrain = cloneOnConstrain;
		this.containerPlacement = containerPlacement;
				
		this.values.addAll(values);
		this.placements.addAll(placements);
	}


	protected void setSize(int dx, int dy, int dz) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;

		this.containerPlacement = new DefaultPlacement3D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ, Collections.emptyList());
	}

	protected void addFirstPoint() {
		values.add(new Default3DPlanePoint3D(
				0, 0, 0, 
				containerMaxX, containerMaxY, containerMaxZ, 
				containerPlacement,
				containerPlacement,
				containerPlacement
				));
	}
	
	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1, placement.getAbsoluteEndZ() - placement.getAbsoluteZ() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy, int boxDz) {		
		Point3D source = values.get(index);

		if(source.getDx() < boxDx) {
			throw new RuntimeException("Max size is " + source.getDx() + ", requested " + boxDx);
		}
		if(source.getDy() < boxDy) {
			throw new RuntimeException();
		}
		if(source.getDz() < boxDz) {
			throw new RuntimeException();
		}
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
			System.out.println("Constraint 3d for " + values.size());
			for (int i = 0; i < values.size(); i++) {
				Point3D point3d = values.get(i);
				
				int maxX = projectPositiveX(point3d, placement, point3d.getMaxX());
				if(maxX < point3d.getMinX()) {
					deleted.add(point3d);
					continue;
				}
				int maxY = projectPositiveY(point3d, placement, point3d.getMaxY());
				if(maxY < point3d.getMinY()) {
					deleted.add(point3d);
					continue;
				}
				int maxZ = projectPositiveZ(point3d, placement, point3d.getMaxZ());
				if(maxZ < point3d.getMinZ()) {
					deleted.add(point3d);
					continue;
				}

				if(cloneOnConstrain) {
					if(point3d.getMaxX() > maxX || point3d.getMaxY() > maxY || point3d.getMaxZ() > maxZ) {
						System.out.println("Constraint " + point3d + " to " + maxX + " " + maxY + " " + maxZ);

						values.set(i, point3d.clone(maxX, maxY, maxZ));
					} else {
						System.out.println("No constraint for " + point3d);						
					}
				} else {
					System.out.println("Constraint " + point3d + " to " + maxX + " " + maxY + " " + maxZ);
					if(point3d.getMaxX() > maxX) {
						point3d.setMaxX(maxX);
					}
					if(point3d.getMaxY() > maxY) {
						point3d.setMaxY(maxY);
					}
					if(point3d.getMaxZ() > maxZ) {
						point3d.setMaxZ(maxZ);
					}
				}
			}
		} else {
			System.out.println("Constraint other");
			for(Point3D point : values) {
				if(!constrainFloatingMax(point, placement, addedX, addedY, addedZ)) {
					deleted.add(point);
				}
			}
		}
		
		if(!deleted.isEmpty()) {
			values.removeAll(deleted);
		}
		
		System.out.println("Added x:");
		for(Point2D v : addedX) {
			if(v == source) {
				System.out.println(" " + v + " (*)");
			} else {
				System.out.println(" " + v);
			}
		}
		System.out.println("Added y:");
		for(Point2D v : addedY) {
			if(v == source) {
				System.out.println(" " + v + " (*)");
			} else {
				System.out.println(" " + v);
			}
		}
		System.out.println("Added z:");
		for(Point2D v : addedZ) {
			if(v == source) {
				System.out.println(" " + v + " (*)");
			} else {
				System.out.println(" " + v);
			}
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

		for(Point3D point : values) {
			if(point.intersects(placement)) {
				throw new RuntimeException(placement + " " + point);
			}
		}

		for(Point3D point : values) {
			for(Placement3D p : placements) {
				if(point.intersects(p)) {
					throw new RuntimeException(p + " " + point);
				}
			}
		}
		
		return !values.isEmpty();
	}	
	
	private void appendZ(P placement, Point3D source, List<Point3D> deleted, int xx, int yy, int zz, List<Point3D> added) {
		boolean yz = source.isSupportedYZPlane(source.getMinY(), zz);
		boolean xz = source.isSupportedXZPlane(source.getMinX(), zz);

		boolean xzEdge = source.isXZPlaneEdgeZ(yy);
		boolean yzEdge = source.isYZPlaneEdgeZ(yy);

		int maxX = projectPositiveX(source.getMinX(), source.getMinY(), zz);
		int maxY = projectPositiveY(source.getMinX(), source.getMinY(), zz);
		int maxZ = projectPositiveZ(source.getMinX(), source.getMinY(), zz);
		
		DefaultXZPlanePoint3D unconnectedXz = null;
		DefaultYZPlanePoint3D unconnectedYz = null;
		
		if(yz && xz) {
			// 3x corner point
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			
			int x = source.getMinX();
			int y = source.getMinY();
			int z = zz; 

			if(x <= maxX && y <= maxY && z <= maxZ) {
				added.add(new Default3DPlanePoint3D(
						x, y, z,
						maxX, maxY, maxZ,
						
						// supported planes
						// yz plane
						yzPlane.getYZPlane(),
						
						// xz plane
						xzPlane.getXZPlane(),
						
						// xy plane (i.e. top of the new placement)
						placement
					)
				);
			}
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

						if(pointMaxX >= x && maxY >= y && pointMaxZ >= z) {

							if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									pointMaxX, maxY, pointMaxZ,
									
									// supported planes
									// yz plane
									yzPlane.getYZPlane(),
									
									// xz plane - some placement
									xzPlane,
									
									// xy plane (i.e. top of the new placement)
									placement
								));

							} else {
								// found unconnected plane, so there is a 1x plane support
								added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, xzPlane));
							}
						}
					}
				} else if(maxY >= 0) {
					// found unconnected plane (container wall)
					int x = source.getMinX();
					int y = 0;
					int z = zz;
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxZ >= z && pointMaxX >= x) {
						added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, containerPlacement));
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

						if(maxX >= x && pointMaxY >= y && pointMaxZ >= z) {

							if(yzPlane.getAbsoluteEndX() >= source.getMinX()) {
								// found connected plane, so there is a 3x plane support still
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									maxX, pointMaxY, pointMaxZ,
									
									// supported planes
									// yz plane - some placement
									yzPlane,
									
									// xz plane
									xzPlane.getXZPlane(),
									
									// xy plane (i.e. top of the new placement)
									placement
								));
							} else {
								// found unconnected plane, so there is a 1x plane support
								added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, yzPlane));
							}
						}
					}
				} else if(maxX >= 0) {
					// found unconnected plane (container wall)
					int x = 0;
					int y = source.getMinY();
					int z = zz;

					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxZ >= z && pointMaxY >= y) {
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, containerPlacement));
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
	
					if(maxX >= x && pointMaxY >= y && pointMaxZ >= z ) {
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, yzPlane));
					}
				}
			} else if(maxX >= 0) {
				// found container wall
				int x = 0;
				int y = source.getMinY();
				int z = zz;

				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);
				
				if(pointMaxZ >= z && pointMaxY >= y) {
					added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, maxX, pointMaxY, pointMaxZ, containerPlacement));
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
	
					if(pointMaxX >= x && maxY >= y && pointMaxZ >= z) {
						added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, xzPlane));
					}
				}
			} else if(maxY >= 0) {
				// found container wall
				int x = source.getMinX();
				int y = 0;
				int z = zz;

				int pointMaxX = constrainX(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxZ >= z && pointMaxX >= x) {
					added.add(unconnectedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, maxY, pointMaxZ, containerPlacement));

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
								
								if(pointMaxX >= x && maxY >= y && pointMaxZ >= z) {
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
		
								/*
								int pointMaxY = constrainY(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								*/
								int pointMaxX = point.getMaxX();
								int pointMaxY = point.getMaxY();
								int pointMaxZ = point.getMaxZ();
								
								if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ));
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
				
		int maxX = projectPositiveX(xx, source.getMinY(), source.getMinZ());
		int maxY = projectPositiveY(xx, source.getMinY(), source.getMinZ());
		int maxZ = projectPositiveZ(xx, source.getMinY(), source.getMinZ());

		DefaultXYPlanePoint3D unconnetedXy = null;
		DefaultXZPlanePoint3D unconnetedXz = null;

		if(xy && xz) {
			// 3x corner point
			XZPlanePoint3D xzPlane = (XZPlanePoint3D)source;
			XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
			
			int x = xx;
			int y = source.getMinY();
			int z = source.getMinZ(); 
			
			if(x <= maxX && y <= maxY && z <= maxZ) {
				
				Default3DPlanePoint3D supported = new Default3DPlanePoint3D(
						x, y, z,
						maxX, maxY, maxZ,
						
						// supported planes
						// yz plane (i.e. side of the new placement)
						placement,
						
						// xz plane
						xzPlane.getXZPlane(),
						
						// xy plane
						xyPlane.getXYPlane()
					);
				
				added.add(supported);
			}
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
						int pointMaxY = constrainY(x, y, z);
						int pointMaxZ = constrainZ(x, y, z);

						if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
						
							if(xzPlane.getAbsoluteEndY() >= source.getMinY()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									pointMaxX, pointMaxY, pointMaxZ,
									
									// supported planes
									// yz plane (i.e. side of the new placement, adjusted to xz plane)
									placement,
									// xz plane -
									xzPlane,
									
									// xy plane
									xyPlane.getXYPlane()
								));
							} else {
								// found unconnected plane, so there is a 1x plane support
								System.out.println("1 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
								added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, xzPlane));
							}
						}
					}
				} else if(maxY >= 0) {
					// found unconnected plane (container wall)
					int x = xx;
					int y = 0;
					int z = source.getMinZ();
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxX >= x && pointMaxZ >= z) {
						System.out.println("2 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
						int pointMaxZ = constrainZ(x, y, z);

						if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
	
							if(xyPlane.getAbsoluteEndZ() >= source.getMinZ()) {
								// found connected plane, so there is a 3x plane support still
	
								added.add(new Default3DPlanePoint3D(
										
									x, y, z,
									pointMaxX, pointMaxY, pointMaxZ,
									
									// supported planes
									// yz plane (i.e. side of the new placement, adjusted to xy plane)
									placement,
		
									// xz plane
									xzPlane.getXZPlane(),
									
									// xy plane - some placement
									xyPlane
								));
							} else {
								// found unconnected plane, so there is a 1x plane support
								System.out.println("3 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
								added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, xyPlane));
							}
						}
					}
				} else if(maxZ >= 0) {
					// found unconnected plane (container wall)
					int x = xx;
					int y = source.getMinY();
					int z = 0;
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);

					if(pointMaxX >= x && pointMaxY >= y) {
						System.out.println("4 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
	
					if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
						System.out.println("5 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, xzPlane));
					}
				}
			} else if(maxY >= 0) {
				// found unconnected plane (container wall)
				
				int x = xx;
				int y = 0;
				int z = source.getMinZ();

				int pointMaxX = constrainX(x, y, z);
				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxX >= x && pointMaxZ >= z) {
					System.out.println("6 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
					added.add(unconnetedXz = new DefaultXZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
					int pointMaxZ = constrainZ(x, y, z);

					if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
						System.out.println("7 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, xyPlane));
					}
				}
			} else if(maxZ >= 0) {
				// found unconnected plane (container wall)
				int x = xx;
				int y = source.getMinY();
				int z = 0;

				int pointMaxX = constrainX(x, y, z);
				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxX >= x && pointMaxY >= y) {
					System.out.println("8 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
					added.add(unconnetedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
								int pointMaxZ = constrainZ(x, y, z);

								if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
									System.out.println("9 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ));
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

								/*
								int pointMaxX = constrainX(x, y, z);
								int pointMaxY = constrainY(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								*/
								
								
								int pointMaxX = point.getMaxX();
								int pointMaxY = point.getMaxY();
								int pointMaxZ = point.getMaxZ();

								if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
									System.out.println("10 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ));
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
					/*
					int maxX = constrainIfNotMaxX(source, point.getMinY(), point.getMinZ());
					int maxY = constrainIfNotMaxY(source, xx, point.getMinZ());
					int maxZ = constrainIfNotMaxZ(source, xx, point.getMinY());
*/
					int maxX = point.getMaxX();
					int maxY = point.getMaxY();
					int maxZ = point.getMaxZ();

					
					DefaultYZPlanePoint3D next = new DefaultYZPlanePoint3D(
							xx, point.getMinY(), point.getMinZ(),
							maxX, maxY, maxZ,
							placement
							);

					System.out.println("Add swallowed xx " + next);

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
			if(point.shadowsOrSwallowsX(source.getMinX(), xx) && point.fitsInYZPlane(placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support
					//int maxX = constrainIfNotMaxX(source, point.getMinY(), point.getMinZ());
					//int maxY = constrainIfNotMaxY(source, xx, point.getMinZ());
					//int maxZ = constrainIfNotMaxZ(source, xx, point.getMinY());

					int maxX = point.getMaxX();
					int maxY = point.getMaxY();
					int maxZ = point.getMaxZ();

					if(maxX >= xx && maxY >= point.getMinY() && maxZ >= point.getMinZ()) {
						DefaultYZPlanePoint3D next = new DefaultYZPlanePoint3D(
								xx, point.getMinY(), point.getMinZ(),
								maxX, maxY, maxZ,
								placement
								);
	
						System.out.println("Add shadowed or swallowed xx " + next);

						added.add(next);
					}
				}
				
				if(point.getMinX() < source.getMinX()) {
					// constrain current point
					int maxX = constrainX(source.getMinX(), source.getMinY(), source.getMinZ());
					if(cloneOnConstrain) {
						values.set(i, point.clone(maxX, point.getMaxY(), point.getMaxZ()));
					} else {

						point.setMaxX(maxX);
					}
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
					
					/*
					int maxX = constrainIfNotMaxX(source, yy, point.getMinZ());
					int maxY = constrainIfNotMaxY(source, point.getMinX(), yy);
					int maxZ = constrainIfNotMaxZ(source, point.getMinX(), yy);
*/
					int maxX = point.getMaxX();
					int maxY = point.getMaxY();
					int maxZ = point.getMaxZ();

					DefaultXZPlanePoint3D next = new DefaultXZPlanePoint3D(
							point.getMinX(), yy, point.getMinZ(),
							maxX, maxY, maxZ,
							placement
						);
					
					System.out.println("Add swallowed yy " + next);
					
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

					/*
					int maxX = constrainIfNotMaxX(source, yy, point.getMinZ());
					int maxY = constrainIfNotMaxY(source, point.getMinX(), point.getMinZ());
					int maxZ = constrainIfNotMaxZ(source, point.getMinX(), yy);
*/
					int maxX = point.getMaxX();
					int maxY = point.getMaxY();
					int maxZ = point.getMaxZ();

					if(maxX >= point.getMinX() && maxY >= yy && maxZ >= point.getMinZ()) {
						
						DefaultXZPlanePoint3D next = new DefaultXZPlanePoint3D(
								point.getMinX(), yy, point.getMinZ(),
								maxX, maxY, maxZ,
								placement
							);
						System.out.println("Add shadowed or swallowed yy " + next);

						added.add(next);
					}
				} 

				if(point.getMinY() < source.getMinY()) {
					// constrain current point
					int maxY = constrainY(source.getMinX(), source.getMinY(), source.getMinZ());
					if(cloneOnConstrain) {
						values.set(i, point.clone(point.getMaxX(), maxY, point.getMaxZ()));
					} else {
						point.setMaxY(maxY);
					}
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
					
					/*
					int maxX = constrainIfNotMaxX(source, point.getMinY(), zz);
					int maxY = constrainIfNotMaxY(source, point.getMinX(), zz);
					int maxX = point.getMaxX()
						*/	;
						
						int maxX = point.getMaxX();
						int maxY = point.getMaxY();
						int maxZ = point.getMaxZ();
					
					DefaultXYPlanePoint3D next = new DefaultXYPlanePoint3D(
							point.getMinX(), point.getMinY(), zz,
							maxX, maxY, maxZ,
							placement
						);
					System.out.println("Add swallowed zz " + next);

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
					
					/*
					int maxX = constrainIfNotMaxX(source, point.getMinY(), zz);
					int maxY = constrainIfNotMaxY(source, point.getMinX(), zz);
					int maxZ = constrainIfNotMaxZ(source, point.getMinX(), point.getMinY());
*/
					int maxX = point.getMaxX();
					int maxY = point.getMaxY();
					int maxZ = point.getMaxZ();

					if(maxX >= point.getMinX() && maxY >= point.getMinY() && maxZ >= zz) {
						
						DefaultXYPlanePoint3D next = new DefaultXYPlanePoint3D(
								point.getMinX(), point.getMinY(), zz,
								maxX, maxY, maxZ,
								placement
							);
						System.out.println("Add shadowed or swallowed zz " + next);

						added.add(next);
					}
				}
				
				if(point.getMinZ() < source.getMinZ()) {
					// constrain current point
					int maxZ = constrainZ(source.getMinX(), source.getMinY(), source.getMinZ());
					if(cloneOnConstrain) {
						values.set(i, point.clone(point.getMaxX(), point.getMaxY(), maxZ));
					} else {
						point.setMaxZ(maxZ);
					}
					
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

		int maxX = projectPositiveX(source.getMinX(), yy, source.getMinZ());
		int maxY = projectPositiveY(source.getMinX(), yy, source.getMinZ());
		int maxZ = projectPositiveZ(source.getMinX(), yy, source.getMinZ());
		
		DefaultXYPlanePoint3D unconnectedXy = null;
		DefaultYZPlanePoint3D unconnectedYz = null;
		
		if(xy && yz) {
			// 3x corner point
			YZPlanePoint3D yzPlane = (YZPlanePoint3D)source;
			XYPlanePoint3D xyPlane = (XYPlanePoint3D)source;
			
			int x = source.getMinX();
			int y = yy;
			int z = source.getMinZ(); 
			
			if(x <= maxX && y <= maxY && z <= maxZ) {
				
				added.add(new Default3DPlanePoint3D(
						x, y, z, 
						maxX, maxY, maxZ,
						
						// supported planes
						// yz plane 
						yzPlane.getYZPlane(),
						
						// xz plane (i.e. side of the new placement)
						placement,
	
						// xy plane
						xyPlane.getXYPlane()
					)
				);
			}
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
						
						int pointMaxX = constrainX(x, y, z);
						int pointMaxY = constrainY(x, y, z);
						int pointMaxZ = constrainZ(x, y, z);
						
						if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {

							if(zyPlane.getAbsoluteEndX() >= source.getMinX()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
									x, y, z,
									pointMaxX, pointMaxY, pointMaxZ,
									
									// supported planes
									// yz plane - placement
									zyPlane,
									
									// xz plane - (i.e. side of the new placement, adjusted to zy plane)
									placement,
		
									// xy plane (adjusted by zy plane)
									xyPlane.getXYPlane()
								));
							} else {
								System.out.println("1 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
								// found unconnected plane, so there is a 1x plane support
								added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, zyPlane));
							}
						}
					}
				} else if(maxX >= 0) {
					// found unconnected plane (container wall)
					
					int x = 0;
					int y = yy;
					int z = source.getMinZ();
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);
					
					if(pointMaxZ >= z && pointMaxY >= y) {
						System.out.println("2 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
						int pointMaxZ = constrainZ(x, y, z);

						if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {

							if(xyPlane.getAbsoluteEndZ() >= source.getMinZ()) {
								// found connected plane, so there is a 3x plane support still
								
								added.add(new Default3DPlanePoint3D(
		
									x, y, z,
									pointMaxX, pointMaxY, pointMaxZ,
									
									// supported planes
									// yz plane 
									yzPlane.getYZPlane(),
									// xz plane
									placement,
									
									// xy plane - some placement
									xyPlane
								));
								
							} else {
								System.out.println("3 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
								// found unconnected plane, so there is a 1x plane support
								added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, xyPlane));
							}
						}
					}
				} else if(maxZ >= 0) {
					// found unconnected plane (container wall)
					int x = source.getMinX();
					int y = yy;
					int z = 0;
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);

					System.out.println(" " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
					
					if(pointMaxX >= x && pointMaxY >= y) {
						System.out.println("4 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
					
					int pointMaxX = constrainX(x, y, z);
					int pointMaxY = constrainY(x, y, z);
					int pointMaxZ = constrainZ(x, y, z);

					if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
						System.out.println("5 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, zyPlane));
					}
				}
			} else if(maxX >= 0){
				// found container wall
				int x = 0;
				int y = yy;
				int z = source.getMinZ();
				
				int pointMaxX = constrainX(x, y, z);
				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxY >= y && pointMaxZ >= z) {
					System.out.println("6 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
					added.add(unconnectedYz = new DefaultYZPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
					int pointMaxZ = constrainZ(x, y, z);
	
					if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
						System.out.println("7 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
						added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, xyPlane));
					}
				}
			} else if(maxZ >= 0) {
				// found container wall
				int x = source.getMinX();
				int y = yy;
				int z = 0;

				int pointMaxX = constrainX(x, y, z);
				int pointMaxY = constrainY(x, y, z);
				int pointMaxZ = constrainZ(x, y, z);

				if(pointMaxX >= x && pointMaxY >= y) {
					System.out.println("8 " + x + "x" + y + "x" + z + " " + pointMaxX + "x" + pointMaxY + "x" + pointMaxZ);
					added.add(unconnectedXy = new DefaultXYPlanePoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ, containerPlacement));
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
								
								int pointMaxX = point.getMaxX();
								int pointMaxY = point.getMaxY();
								int pointMaxZ = point.getMaxZ();
/*
								int pointMaxX = constrainX(x, y, z);
								int pointMaxY = constrainY(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
	*/							
								if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ));
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
		
								int pointMaxX = constrainX(x, y, z);
								int pointMaxY = constrainY(x, y, z);
								int pointMaxZ = constrainZ(x, y, z);
								
								if(pointMaxX >= x && pointMaxY >= y && pointMaxZ >= z) {
									added.add(new DefaultPoint3D(x, y, z, pointMaxX, pointMaxY, pointMaxZ));
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

	public void redo() {
		values.clear();
		placements.clear();
		
		addFirstPoint();
	}

	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);
		
		redo();
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}	

	public long getMaxArea() {
		long maxPointArea = -1L;
		for (Point2D point2d : values) {
			if(maxPointArea < point2d.getArea()) {
				maxPointArea = point2d.getArea(); 
			}
		}
		return maxPointArea;
	}

	public long getMaxVolume() {
		long maxPointVolume = -1L;
		for (Point3D point2d : values) {
			if(maxPointVolume < point2d.getArea()) {
				maxPointVolume = point2d.getVolume(); 
			}
		}
		return maxPointVolume;
	}

	@Override
	public String toString() {
		return "ExtremePoints3D [" + containerMaxX + "x" + containerMaxY + "x" + containerMaxZ + "]";
	}

	public ExtremePoints3D2<P> clone() {
		return new ExtremePoints3D2<>(cloneOnConstrain, containerPlacement, values, placements);
	}

	public int findPoint(int x, int y, int z) {
		for(int i = 0; i < values.size(); i++) {
			Point3D point2d = values.get(i);
			if(point2d.getMinX() == x && point2d.getMinY() == y && point2d.getMinZ() == z) {
				return i;
			}
		}
		return -1;
	}

	private void addOutOfBoundsXX(Point3D source, Point3D dx) {
		if(dx.getMinX() < source.getMinX() || dx.getMaxX() > source.getMaxX() || dx.getMaxY() > source.getMaxY() || dx.getMaxZ() > source.getMaxZ() ) {
			// outside the known limits of the source point

			int maxX = constrainX(dx.getMinX(), dx.getMaxY(), dx.getMinZ());
			int maxY = constrainY(dx.getMaxX(), dx.getMinY(), dx.getMinZ());
			int maxZ = constrainZ(dx.getMaxX(), dx.getMinY(), dx.getMinZ());

			if(maxY < dx.getMaxY() && maxX < dx.getMaxX()) {
				// split in two

				addX.add(dx.clone(maxX, dx.getMaxY()));
				addX.add(dx.clone(dx.getMaxX(), maxY));
			} else {
				// just constrain
				if(maxY < dx.getMaxY()) {
					dx.setMaxY(maxY);
				}
				if(maxX < dx.getMaxX()) {
					dx.setMaxX(maxX);
				}
				addX.add(dx);
			}
		} else {
			addX.add(dx);
		}
	}

}
