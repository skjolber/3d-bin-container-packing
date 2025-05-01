package com.github.skjolber.packing.api.ep;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.StackPlacement;

public abstract class Point {

	private static final long serialVersionUID = 1L;

	public static final Comparator<Point> X_COMPARATOR = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}
			return Integer.compare(o2.maxX, o1.maxX);
		}
	};

	public static final Comparator<Point> Y_COMPARATOR = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}
			return Integer.compare(o2.maxY, o1.maxY);
		
		}
	};

	public static final Comparator<Point> Z_COMPARATOR = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			if(o1.minZ < o2.minZ) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			return Integer.compare(o2.maxZ, o1.maxZ);
		}
	};

	public static final Comparator<Point> COMPARATOR_X_THEN_Y_THEN_Z = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {

			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			if(o1.minZ < o2.minZ) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			if(o1.maxX < o2.maxX) {
				return -1;
			} else if(o1.maxX != o2.maxX) {
				return 1;
			}

			if(o1.maxY < o2.maxY) {
				return -1;
			} else if(o1.maxY != o2.maxY) {
				return 1;
			}

			return Integer.compare(o1.maxZ, o2.maxZ);
		}
	};

	public static final Comparator<Point> COMPARATOR_X_THEN_Y = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			return Long.compare(o1.volume, o2.volume);
		}
	};

	public static final Comparator<Point> COMPARATOR_Y_THEN_Z_THEN_X = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			if(o1.minZ < o2.minZ) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.maxY < o2.maxY) {
				return -1;
			} else if(o1.maxY != o2.maxY) {
				return 1;
			}

			if(o1.maxZ < o2.maxZ) {
				return -1;
			} else if(o1.maxZ != o2.maxZ) {
				return 1;
			}

			return Integer.compare(o1.maxX, o2.maxX);
		}
	};

	public static final Comparator<Point> COMPARATOR_Z_THEN_X_THEN_Y = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			if(o1.minZ < o2.minZ) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			if(o1.maxZ < o2.maxZ) {
				return -1;
			} else if(o1.maxZ != o2.maxZ) {
				return 1;
			}

			if(o1.maxX < o2.maxX) {
				return -1;
			} else if(o1.maxX != o2.maxX) {
				return 1;
			}

			return Integer.compare(o1.maxY, o2.maxY);
		}
	};

	public static final Comparator<Point> COMPARATOR = new Comparator<Point>() {

		@Override
		public int compare(Point o1, Point o2) {
			int compare = X_COMPARATOR.compare(o1, o2);
			if(compare != 0) {
				return compare;
			}

			compare = Y_COMPARATOR.compare(o1, o2);
			if(compare != 0) {
				return compare;
			}

			return Z_COMPARATOR.compare(o1, o2);
		}
	};
	
	protected final int minX;
	protected final int minY;

	protected int maxY;
	protected int maxX;

	protected int dx;
	protected int dy;

	protected long area;
	protected long volume;

	protected final int minZ;
	protected int maxZ;
	protected int dz;
	
	protected int index = -1;

	public Point(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		
		this.minX = minX;
		this.minY = minY;
		this.maxY = maxY;
		this.maxX = maxX;

		this.dx = maxX - minX + 1;
		this.dy = maxY - minY + 1;

		calculateArea();

		this.minZ = minZ;
		this.maxZ = maxZ;
		this.dz = maxZ - minZ + 1;

		calculateVolume();
	}

	public void setMaxZ(int maxZ) {
		if(maxX < 0) {
			throw new RuntimeException("Cannot set max z to " + maxZ + " for " + minZ + "x" + minY + "x" + minZ);
		}
		this.maxZ = maxZ;

		this.dz = maxZ - minZ + 1;

		calculateVolume();
	}

	public void setMaxX(int maxX) {
		if(maxX < 0) {
			throw new RuntimeException("Cannot set max x to " + maxX + " for " + minX + "x" + minY);
		}
		this.maxX = maxX;

		this.dx = maxX - minX + 1;

		calculateArea();
		calculateVolume();
	}

	public void setMaxY(int maxY) {
		if(maxY < 0) {
			throw new RuntimeException("Cannot set max y to " + maxY + " for " + minX + "x" + minY);
		}
		this.maxY = maxY;

		this.dy = maxY - minY + 1;
		
		calculateArea();
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
	
	public int getDy() {
		return dy;
	}

	public int getDx() {
		return dx;
	}

	public int getMaxZ() {
		return maxZ;
	}

	public int getMinZ() {
		return minZ;
	}

	public boolean intersects(StackPlacement p) {
		return !(p.getAbsoluteEndX() < minX || p.getAbsoluteX() > maxX || p.getAbsoluteEndY() < minY || p.getAbsoluteY() > maxY || p.getAbsoluteEndZ() < minZ || p.getAbsoluteZ() > maxZ);
	}

	public boolean intersects(Point point) {
		return !(point.getMaxX() < minX || point.getMinX() > maxX || point.getMaxY() < minY || point.getMinY() > maxY || point.getMaxZ() < minZ || point.getMinZ() > maxZ);
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

	public boolean shadowsOrSwallowsX(int min, int max) {
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

	private void calculateArea() {
		this.area = (long)dx * (long)dy;
	}
	
	public abstract Point clone(int maxX, int maxY, int maxZ);

	public boolean containsInYZPlane(Point point) {
		if(point.getMinX() == minX) {
			return point.swallowsMinY(minY, maxY) && point.swallowsMinZ(minZ, maxZ);
		}
		return false;
	}

	public boolean containsInXYPlane(Point point) {
		if(point.getMinZ() == minZ) {
			return point.swallowsMinY(minY, maxY) && point.swallowsMinX(minX, maxX);
		}
		return false;
	}

	public boolean containsInXZPlane(Point point) {
		if(point.getMinY() == minY) {
			return point.swallowsMinZ(minZ, maxZ) && point.swallowsMinX(minX, maxX);
		}
		return false;
	}

	public boolean isInXZPlane(StackPlacement point) {
		if(point.getAbsoluteY() == minY) {
			return fitsInXZPlane(point);
		}
		return false;
	}

	public boolean isInXYPlane(StackPlacement point) {
		if(point.getAbsoluteZ() == minZ) {
			return fitsInXYPlane(point);
		}
		return false;
	}

	public boolean isInYZPlane(StackPlacement point) {
		if(point.getAbsoluteX() == minX) {
			return fitsInYZPlane(point);
		}
		return false;
	}

	public boolean fitsInXZPlane(StackPlacement point) {
		return swallowsMinZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowsMinX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}

	public boolean fitsInXYPlane(StackPlacement point) {
		return swallowsMinY(point.getAbsoluteY(), point.getAbsoluteEndY()) && swallowsMinX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}

	public boolean fitsInYZPlane(StackPlacement point) {
		return swallowsMinZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowsMinY(point.getAbsoluteY(), point.getAbsoluteEndY());
	}

	public boolean fits3D(BoxStackValue stackValue) {
		return !(stackValue.getDx() > dx || stackValue.getDy() > dy || stackValue.getDz() > dz);
	}

	public boolean isMax(Point existing) {
		return maxY == existing.getMaxY() && maxX == existing.getMaxX() && maxZ == existing.getMaxZ();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY
				+ "x" + maxZ + "]";
	}

	public long getVolume() {
		return volume;
	}

	public boolean eclipses(Point point) {
		return minX <= point.getMinX() &&
				minY <= point.getMinY() &&
				minZ <= point.getMinZ() &&
				point.getMaxX() <= maxX &&
				point.getMaxY() <= maxY &&
				point.getMaxZ() <= maxZ;
	}

	public boolean eclipsesX(Point point) {
		return minX <= point.getMinX() && point.getMaxX() <= maxX;
	}

	public boolean eclipsesY(Point point) {
		return minY <= point.getMinY() && point.getMaxY() <= maxY;
	}

	public boolean eclipsesZ(Point point) {
		return minZ <= point.getMinZ() && point.getMaxZ() <= maxZ;
	}

	public boolean eclipsesMovedX(Point point, int x) {
		return minX <= x && point.getMaxX() <= maxX && eclipsesY(point) && eclipsesZ(point);
	}

	public boolean eclipsesMovedY(Point point, int y) {
		return minY <= y && point.getMaxY() <= maxY && eclipsesX(point) && eclipsesZ(point);
	}

	public boolean eclipsesMovedZ(Point point, int z) {
		return minZ <= z && point.getMaxZ() <= maxZ && eclipsesX(point) && eclipsesY(point);
	}

	public long getVolumeAtZ(int zz) {
		return (long)dx * (long)dy * (maxZ - zz + 1);
	}

	public long getVolumeAtMaxZ(int maxZ) {
		return (long)dx * (long)dy * (maxZ - minZ + 1);
	}

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}

	/**
	 * 
	 * Get y constraint (inclusive)
	 * 
	 * @return max y
	 */

	public int getMaxY() {
		return maxY;
	}

	/**
	 * 
	 * Get x constraint (inclusive)
	 * 
	 * @return max x
	 */

	public int getMaxX() {
		return maxX;
	}

	public long getArea() {
		return area;
	}
	
	public long getAreaAtX(int xx) {
		return dy * (long)(maxX - xx + 1);
	}

	public long getAreaAtY(int yy) {
		return dx * (long)(maxY - yy + 1);
	}

	public long getAreaAtMaxX(int maxX) {
		return dy * (long)(maxX - minX + 1);
	}

	public long getAreaAtMaxY(int maxY) {
		return dx * (long)(maxY - minY + 1);
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
}
