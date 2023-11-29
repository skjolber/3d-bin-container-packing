package com.github.skjolber.packing.api.ep;

import java.util.Comparator;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.StackValue;

public abstract class Point3D<P extends Placement3D> extends Point2D<P> {

	private static final long serialVersionUID = 1L;

	public static final Comparator<Point3D<?>> X_COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minX < o2.minX) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}
			return Integer.compare(o2.maxX, o1.maxX);
		}
	};

	public static final Comparator<Point3D<?>> Y_COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minY < o2.minY) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}
			return Integer.compare(o2.maxY, o1.maxY);
		
		}
	};

	public static final Comparator<Point3D<?>> Z_COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minZ < o2.minZ) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			return Integer.compare(o2.maxZ, o1.maxZ);
		}
	};

	public static final Comparator<Point3D<?>> COMPARATOR_X_THEN_Y_THEN_Z = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {

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

	public static final Comparator<Point3D<?>> COMPARATOR_X_THEN_Y = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
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

	public static final Comparator<Point3D<?>> COMPARATOR_Y_THEN_Z_THEN_X = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
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

	public static final Comparator<Point3D<?>> COMPARATOR_Z_THEN_X_THEN_Y = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
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

	public static final Comparator<Point3D<?>> COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
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

	protected final int minZ;
	protected int maxZ;
	protected int dz;

	public Point3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, maxX, maxY);
		/*
		if(maxZ < minZ) {
			throw new IllegalArgumentException("Z: "+ maxZ + " < " + minZ);
		}
		*/
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

	public boolean intersects(P p) {
		return !(p.getAbsoluteEndX() < minX || p.getAbsoluteX() > maxX || p.getAbsoluteEndY() < minY || p.getAbsoluteY() > maxY || p.getAbsoluteEndZ() < minZ || p.getAbsoluteZ() > maxZ);
	}

	public boolean intersects(Point3D<?> point) {
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

	public abstract Point3D<P> clone(int maxX, int maxY, int maxZ);

	@Override
	public Point2D<P> clone(int maxX, int maxY) {
		return clone(maxX, maxY, this.maxZ);
	}

	public boolean containsInYZPlane(Point3D<P> point) {
		if(point.getMinX() == minX) {
			return point.swallowsMinY(minY, maxY) && point.swallowsMinZ(minZ, maxZ);
		}
		return false;
	}

	public boolean containsInXYPlane(Point3D<P> point) {
		if(point.getMinZ() == minZ) {
			return point.swallowsMinY(minY, maxY) && point.swallowsMinX(minX, maxX);
		}
		return false;
	}

	public boolean containsInXZPlane(Point3D<P> point) {
		if(point.getMinY() == minY) {
			return point.swallowsMinZ(minZ, maxZ) && point.swallowsMinX(minX, maxX);
		}
		return false;
	}

	public boolean isInXZPlane(P point) {
		if(point.getAbsoluteY() == minY) {
			return fitsInXZPlane(point);
		}
		return false;
	}

	public boolean isInXYPlane(P point) {
		if(point.getAbsoluteZ() == minZ) {
			return fitsInXYPlane(point);
		}
		return false;
	}

	public boolean isInYZPlane(P point) {
		if(point.getAbsoluteX() == minX) {
			return fitsInYZPlane(point);
		}
		return false;
	}

	public boolean fitsInXZPlane(P point) {
		return swallowsMinZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowsMinX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}

	public boolean fitsInXYPlane(P point) {
		return swallowsMinY(point.getAbsoluteY(), point.getAbsoluteEndY()) && swallowsMinX(point.getAbsoluteX(), point.getAbsoluteEndX());
	}

	public boolean fitsInYZPlane(P point) {
		return swallowsMinZ(point.getAbsoluteZ(), point.getAbsoluteEndZ()) && swallowsMinY(point.getAbsoluteY(), point.getAbsoluteEndY());
	}

	public boolean fits3D(StackValue stackValue) {
		return !(stackValue.getDx() > dx || stackValue.getDy() > dy || stackValue.getDz() > dz);
	}

	public boolean isMax(Point3D<P> existing) {
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

	public boolean eclipses(Point3D<P> point) {
		return minX <= point.getMinX() &&
				minY <= point.getMinY() &&
				minZ <= point.getMinZ() &&
				point.getMaxX() <= maxX &&
				point.getMaxY() <= maxY &&
				point.getMaxZ() <= maxZ;
	}

	public boolean eclipsesX(Point2D<P> point) {
		return minX <= point.getMinX() && point.getMaxX() <= maxX;
	}

	public boolean eclipsesY(Point2D<P> point) {
		return minY <= point.getMinY() && point.getMaxY() <= maxY;
	}

	public boolean eclipsesZ(Point3D<P> point) {
		return minZ <= point.getMinZ() && point.getMaxZ() <= maxZ;
	}

	public boolean eclipsesMovedX(Point3D<P> point, int x) {
		return minX <= x && point.getMaxX() <= maxX && eclipsesY(point) && eclipsesZ(point);
	}

	public boolean eclipsesMovedY(Point3D<P> point, int y) {
		return minY <= y && point.getMaxY() <= maxY && eclipsesX(point) && eclipsesZ(point);
	}

	public boolean eclipsesMovedZ(Point3D<P> point, int z) {
		return minZ <= z && point.getMaxZ() <= maxZ && eclipsesX(point) && eclipsesY(point);
	}

	public abstract Point3D<P> rotate();

	public long getVolumeAtZ(int zz) {
		return (long)dx * (long)dy * (maxZ - zz + 1);
	}

	public long getVolumeAtMaxZ(int maxZ) {
		return (long)dx * (long)dy * (maxZ - minZ + 1);
	}


}
