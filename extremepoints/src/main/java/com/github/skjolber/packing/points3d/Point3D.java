package com.github.skjolber.packing.points3d;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
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
			int compare = Integer.compare(o1.minX, o2.minX);

			if(compare == 0) {
				return Integer.compare(o1.minY, o2.minY);
			}
			
			if(compare == 0) {
				compare= Integer.compare(o1.minZ, o2.minZ);
			}
			return compare;
		}
	};
	
	protected final int minZ;
	protected int maxZ;
	protected int dz;
	
	protected long volume;
	
	public Point3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, maxX, maxY);
		
		if(maxZ < minZ) {
			throw new RuntimeException("Z: "+ maxZ + " < " + minZ);
		}
		this.minZ = minZ;
		this.maxZ = maxZ;
		this.dz = maxZ - minZ + 1;
		
		calculateVolume();
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
		
		calculateVolume();
	}
	
	@Override
	public void setMaxX(int maxX) {
		super.setMaxX(maxX);
		calculateVolume();
	}
	
	@Override
	public void setMaxY(int maxY) {
		super.setMaxY(maxY);
		calculateVolume();
	}

	private void calculateVolume() {
		this.volume = (long)dz * (long)dy * (long)dx;
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

	public boolean isShadowedOrSwallowedZ(int min, int max) {
		return minZ < max && maxZ > min;
	}

	public boolean swallowsMinY(int min, int max) {
		return min <= minY && minY <= max;
	}

	public boolean swallowsMinX(int min, int max) {
		return min <= minX && minX <= max;
	}

	public boolean swallowsMinZ(int min, int max) {
		return min <= minZ && minZ <= max;
	}

	public abstract Point3D clone(int maxX, int maxY, int maxZ);

	@Override
	public Point2D clone(int maxX, int maxY) {
		return clone(maxX, maxY, this.maxZ);
	}
	
	public boolean containsInYZPlane(Point3D point) {
		if(point.getMinX() == minX) {
			return point.swallowsMinY(minY, maxY) && point.swallowsMinZ(minZ, maxZ);
		}
		return false;
	}
	
	public boolean containsInXYPlane(Point3D point) {
		if(point.getMinZ() == minZ) {
			return point.swallowsMinY(minY, maxY) && point.swallowsMinX(minX, maxX);
		}
		return false;
	}
	
	public boolean containsInXZPlane(Point3D point) {
		if(point.getMinY() == minY) {
			return point.swallowsMinZ(minZ, maxZ) && point.swallowsMinX(minX, maxX);
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
		return swallowsMinZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowsMinX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}
	
	public boolean fitsInXYPlane(Placement3D point) {
		return swallowsMinY(point.getAbsoluteY(), point.getAbsoluteEndY()) && swallowsMinX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}
	
	public boolean fitsInYZPlane(Placement3D point) {
		return swallowsMinZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowsMinY(point.getAbsoluteY(), point.getAbsoluteEndY());
	}

	public boolean fits3D(StackValue stackValue) {
		return !(stackValue.getDx() > dx || stackValue.getDy() > dy || stackValue.getDz() > dz);
	}

	public boolean isMax(Point3D existing) {
		return maxY == existing.getMaxY() && maxX == existing.getMaxX() && maxZ == existing.getMaxZ();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY 
				+ "x" + maxZ + "]";
	}

	public Point3D rotate() {
		DefaultPoint3D defaultPoint3D = new DefaultPoint3D(minY, minZ, minX, maxY, maxZ, maxX);
		
		return defaultPoint3D;
	}

	public long getVolume() {
		return volume;
	}
	
	public abstract List<Placement3D> getPlacements3D();
	
}
