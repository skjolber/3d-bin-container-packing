package com.github.skjolber.packing.points3d;

import java.util.Comparator;

import com.github.skjolber.packing.points2d.Point2D;

public abstract class Point3D extends Point2D {

	public static final Comparator<Point3D> COMPARATOR = new Comparator<Point3D>() {
		
		@Override
		public int compare(Point3D o1, Point3D o2) {
			int x = Integer.compare(o1.minX, o2.minX);

			if(x == 0) {
				return Integer.compare(o1.minY, o2.minY);
			}
			
			if(x == 0) {
				x = Integer.compare(o1.minZ, o2.minZ);
			}
			return x;
		}
	};
	
	protected final int minZ;
	protected int maxZ;
	protected int dz;
	
	public Point3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, maxX, maxY);
		this.minZ = minZ;
		this.maxZ = maxZ;
		this.dz = maxZ - minZ + 1;
	}

	public boolean isSupportedXYPlane(int x, int y) { // i.e. z is fixed
		return false;
	}
	
	public boolean isSupportedYZPlane(int y, int z) { // i.e. x is fixed
		return false;
	}

	public boolean isSupportedXZPlane(int x, int z) { // i.e. y is fixed
		return false;
	}

	public void setMaxZ(int maxZ) {
		this.maxZ = maxZ;
		
		this.dz = maxZ - minZ + 1;
	}
	
	public boolean isWithin(int dx, int dy, int dz) {
		return dx <= this.dx && dy <= this.dy && dz <= this.dz;
	}
	
	public int getDz() {
		return dz;
	}
	
	public int getMaxZ() {
		return maxZ;
	}
	
	public int getMinZ() {
		return minZ;
	}
	
	public boolean intersects(Point3D point) {
		return !(point.getMaxX() < minX || point.getMinX() > maxX || point.getMaxY() < minY || point.getMinY() > maxY || point.getMaxZ() < minZ || point.getMinZ() > maxZ);
	}

	@Override
	public String toString() {
		return "Point3D [minX=" + minX + ", minY=" + minY + ", minZ=" + minZ + ", maxX=" + maxX + ", maxY=" + maxY 
				+ ", maxZ=" + maxZ + "]";
	}
	
	

}
