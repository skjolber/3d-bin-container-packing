package com.github.skjolber.packing.points3d;

import java.util.Comparator;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.points2d.Point2D;

public abstract class Point3D extends Point2D {

	public static final Comparator<Point3D> Z_COMPARATOR = new Comparator<Point3D>() {
		
		@Override
		public int compare(Point3D o1, Point3D o2) {
			return Integer.compare(o1.minZ, o2.minZ);
		}
	};
	
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
		
		if(maxZ < minZ) {
			throw new RuntimeException("Expected max z " + maxZ + " >= min z " + minZ);
		}
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

	public boolean intersects(Placement3D p) {
		return !(p.getAbsoluteEndX() < minX || p.getAbsoluteX() > maxX || p.getAbsoluteEndY() < minY || p.getAbsoluteY() > maxY || p.getAbsoluteEndZ() < minZ || p.getAbsoluteZ() > maxZ);
	}

	public boolean intersects(Point3D point) {
		return !(point.getMaxX() < minX || point.getMinX() > maxX || point.getMaxY() < minY || point.getMinY() > maxY || point.getMaxZ() < minZ || point.getMinZ() > maxZ);
	}

	public boolean isYZPlaneEdgeZ(int z) {
		return false;
	}

	public boolean isYZPlaneEdgeY(int z) {
		return false;
	}

	public boolean isXYPlaneEdgeX(int z) {
		return false;
	}

	public boolean isXYPlaneEdgeY(int z) {
		return false;
	}

	public boolean isXZPlaneEdgeX(int x) {
		return false;
	}
	
	public boolean isXZPlaneEdgeZ(int z) {
		return false;
	}

	public boolean shadowedX(int min, int max) {
		return minX < min && maxX > max;
	}

	public boolean shadowedY(int min, int max) {
		return minY < min && maxY > max;
	}

	public boolean shadowedZ(int min, int max) {
		return minZ < min && maxZ > max;
	}
	
	public boolean isShadowedOrSwallowedByX(int min, int max) {
		return minX < max && maxX > min;
	}

	public boolean isShadowedOrSwallowedByY(int min, int max) {
		return minY < max && maxY > min;
	}

	public boolean shadowedOrSwallowedZ(int min, int max) {
		return minZ < max && maxZ > min;
	}

	public boolean swallowedY(int min, int max) {
		return min <= minY && minY <= max;
	}

	public boolean swallowedX(int min, int max) {
		return min <= minX && minX <= max;
	}

	public boolean swallowedZ(int min, int max) {
		return min <= minZ && minZ <= max;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY 
				+ "x" + maxZ + "]";
	}

	public abstract Point3D clone(int maxX, int maxY, int maxZ);

	@Override
	public Point2D clone(int maxX, int maxY) {
		return clone(maxX, maxY, this.maxZ);
	}
	
	public boolean containsInYZPlane(Point3D point) {
		if(point.getMinX() == minX) {
			return point.swallowedY(minY, maxY) && point.swallowedZ(minZ, maxZ);
		}
		return false;
	}
	
	public boolean containsInXYPlane(Point3D point) {
		if(point.getMinZ() == minZ) {
			return point.swallowedY(minY, maxY) && point.swallowedX(minX, maxX);
		}
		return false;
	}
	
	public boolean containsInXZPlane(Point3D point) {
		if(point.getMinY() == minY) {
			return point.swallowedZ(minZ, maxZ) && point.swallowedX(minX, maxX);
		}
		return false;
	}
	
	public boolean isInXZPlane(Placement3D point) {
		if(point.getAbsoluteY() == minY) {
			return fitsInXZPlane(point);
		}
		return false;
	}
	
	public boolean isInXYPlane(Placement3D point) {
		if(point.getAbsoluteZ() == minZ) {
			return fitsInXYPlane(point);
		}
		return false;
	}
	
	public boolean isInYZPlane(Placement3D point) {
		if(point.getAbsoluteX() == minX) {
			return fitsInYZPlane(point);
		}
		return false;
	}

	public boolean fitsInXZPlane(Placement3D point) {
		return swallowedZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowedX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}
	
	public boolean fitsInXYPlane(Placement3D point) {
		return swallowedY(point.getAbsoluteY(), point.getAbsoluteEndY()) && swallowedX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}
	
	public boolean fitsInYZPlane(Placement3D point) {
		return swallowedZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowedY(point.getAbsoluteY(), point.getAbsoluteEndY());
	}


	public boolean isMax(Point3D existing) {
		return maxY == existing.getMaxY() && maxX == existing.getMaxX() && maxZ == existing.getMaxZ();
	}

}
