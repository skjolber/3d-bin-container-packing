package com.github.skjolber.packing.api.ep;

import java.math.BigDecimal;
import java.util.Comparator;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.StackValue;

public abstract class Point3D<P extends Placement3D> extends Point2D<P> {

	private static final long serialVersionUID = 1L;

	public static final Comparator<Point3D<?>> X_COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minX.compareTo(o2.minX) < 0) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}
			return o2.maxX.compareTo(o1.maxX);
		}
	};

	public static final Comparator<Point3D<?>> Y_COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minY.compareTo(o2.minY) < 0) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}
			return o2.maxY.compareTo(o1.maxY);
		}
	};

	public static final Comparator<Point3D<?>> Z_COMPARATOR = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minZ.compareTo(o2.minZ) < 0) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			return o2.maxZ.compareTo(o1.maxZ);
		}
	};

	public static final Comparator<Point3D<?>> COMPARATOR_X_THEN_Y_THEN_Z = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {

			if(o1.minX.compareTo(o2.minX) < 0) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY.compareTo(o2.minY) < 0) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			if(o1.minZ.compareTo(o2.minZ) < 0) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			if(o1.maxX.compareTo(o2.maxX) < 0) {
				return -1;
			} else if(o1.maxX != o2.maxX) {
				return 1;
			}

			if(o1.maxY.compareTo(o2.maxY) < 0) {
				return -1;
			} else if(o1.maxY != o2.maxY) {
				return 1;
			}

			return o1.maxZ.compareTo(o2.maxZ);
		}
	};

	public static final Comparator<Point3D<?>> COMPARATOR_X_THEN_Y = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minX.compareTo(o2.minX) < 0) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY.compareTo(o2.minY) < 0) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			return o1.volume.compareTo(o2.volume);
		}
	};

	public static final Comparator<Point3D<?>> COMPARATOR_Y_THEN_Z_THEN_X = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minY.compareTo(o2.minY) < 0) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			if(o1.minZ.compareTo(o2.minZ) < 0) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			if(o1.minX.compareTo(o2.minX) < 0) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.maxY.compareTo(o2.maxY) < 0) {
				return -1;
			} else if(o1.maxY != o2.maxY) {
				return 1;
			}

			if(o1.maxZ.compareTo(o2.maxZ) < 0) {
				return -1;
			} else if(o1.maxZ != o2.maxZ) {
				return 1;
			}

			return o1.maxX.compareTo(o2.maxX);
		}
	};

	public static final Comparator<Point3D<?>> COMPARATOR_Z_THEN_X_THEN_Y = new Comparator<Point3D<?>>() {

		@Override
		public int compare(Point3D<?> o1, Point3D<?> o2) {
			if(o1.minZ.compareTo(o2.minZ) < 0) {
				return -1;
			} else if(o1.minZ != o2.minZ) {
				return 1;
			}

			if(o1.minX.compareTo(o2.minX) < 0) {
				return -1;
			} else if(o1.minX != o2.minX) {
				return 1;
			}

			if(o1.minY.compareTo(o2.minY) < 0) {
				return -1;
			} else if(o1.minY != o2.minY) {
				return 1;
			}

			if(o1.maxZ.compareTo(o2.maxZ) < 0) {
				return -1;
			} else if(o1.maxZ != o2.maxZ) {
				return 1;
			}

			if(o1.maxX.compareTo(o2.maxX) < 0) {
				return -1;
			} else if(o1.maxX != o2.maxX) {
				return 1;
			}

			return o1.maxY.compareTo(o2.maxY);
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

	protected final BigDecimal minZ;
	protected BigDecimal maxZ;
	protected BigDecimal dz;

	public Point3D(BigDecimal minX, BigDecimal minY, BigDecimal minZ, BigDecimal maxX, BigDecimal maxY, BigDecimal maxZ) {
		super(minX, minY, maxX, maxY);
		/*
		if(maxZ < minZ) {
			throw new IllegalArgumentException("Z: "+ maxZ + " < " + minZ);
		}
		*/
		this.minZ = minZ;
		this.maxZ = maxZ;
		this.dz = maxZ.subtract(minZ).add(BigDecimal.ONE);

		calculateVolume();
	}

	public boolean isSupportedXYPlane(BigDecimal x, BigDecimal y) { // i.e. z is fixed
		return false;
	}

	public boolean isSupportedYZPlane(BigDecimal y, BigDecimal z) { // i.e. x is fixed
		return false;
	}

	public boolean isSupportedXZPlane(BigDecimal x, BigDecimal z) { // i.e. y is fixed
		return false;
	}

	public void setMaxZ(BigDecimal maxZ) {
		if(maxX.compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("Cannot set max z to " + maxZ + " for " + minZ + "x" + minY + "x" + minZ);
		}
		this.maxZ = maxZ;

		this.dz = maxZ.subtract(minZ).add(BigDecimal.ONE);

		calculateVolume();
	}

	@Override
	public void setMaxX(BigDecimal maxX) {
		super.setMaxX(maxX);
		calculateVolume();
	}

	@Override
	public void setMaxY(BigDecimal maxY) {
		super.setMaxY(maxY);
		calculateVolume();
	}

	private void calculateVolume() {
		this.volume = dz.multiply(dy).multiply(dx);
	}

	public boolean isWithin(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
		return dx.compareTo(this.dx) <= 0 && dy.compareTo(this.dy) <= 0 && dz.compareTo(this.dz) <= 0;
	}

	public BigDecimal getDz() {
		return dz;
	}

	public BigDecimal getMaxZ() {
		return maxZ;
	}

	public BigDecimal getMinZ() {
		return minZ;
	}

	public boolean intersects(P p) {
		return !(p.getAbsoluteEndX().compareTo(minX) < 0 || p.getAbsoluteX().compareTo(maxX) > 0 || p.getAbsoluteEndY().compareTo(minY) < 0 || p.getAbsoluteY().compareTo(maxY) > 0 || p.getAbsoluteEndZ().compareTo(minZ) < 0 || p.getAbsoluteZ().compareTo(maxZ) > 0);
	}

	public boolean intersects(Point3D<?> point) {
		return !(point.getMaxX().compareTo(minX) < 0 || point.getMinX().compareTo(maxX) > 0 || point.getMaxY().compareTo(minY) < 0 || point.getMinY().compareTo(maxY) > 0 || point.getMaxZ().compareTo(minZ) < 0 || point.getMinZ().compareTo(maxZ) > 0);
	}

	public boolean isYZPlaneEdgeZ(BigDecimal z) {
		return false;
	}

	public boolean isYZPlaneEdgeY(BigDecimal z) {
		return false;
	}

	public boolean isXYPlaneEdgeX(BigDecimal x) {
		return false;
	}

	public boolean isXYPlaneEdgeY(BigDecimal y) {
		return false;
	}

	public boolean isXZPlaneEdgeX(BigDecimal x) {
		return false;
	}

	public boolean isXZPlaneEdgeZ(BigDecimal z) {
		return false;
	}

	public boolean shadowedX(BigDecimal min, BigDecimal max) {
		return minX.compareTo(min) < 0 && maxX.compareTo(max) > 0;
	}

	public boolean shadowedY(BigDecimal min, BigDecimal max) {
		return minY.compareTo(min) < 0 && maxY.compareTo(max) > 0;
	}

	public boolean shadowedZ(BigDecimal min, BigDecimal max) {
		return minZ.compareTo(min) < 0 && maxZ.compareTo(max) > 0;
	}

	public boolean shadowsOrSwallowsX(BigDecimal min, BigDecimal max) {
		return minX.compareTo(max) < 0 && maxX.compareTo(min) > 0;
	}

	public boolean isShadowedOrSwallowedByY(BigDecimal min, BigDecimal max) {
		return minY.compareTo(max) < 0 && maxY.compareTo(min) > 0;
	}

	public boolean isShadowedOrSwallowedZ(BigDecimal min, BigDecimal max) {
		return minZ.compareTo(max) < 0 && maxZ.compareTo(min) > 0;
	}

	public boolean swallowsMinY(BigDecimal min, BigDecimal max) {
		return min.compareTo(minY) <= 0 && minY.compareTo(max) <= 0;
	}

	public boolean swallowsMinX(BigDecimal min, BigDecimal max) {
		return min.compareTo(minX) <= 0 && minX.compareTo(max) <= 0;
	}

	public boolean swallowsMinZ(BigDecimal min, BigDecimal max) {
		return min.compareTo(minZ) <= 0 && minZ.compareTo(max) <= 0;
	}

	public abstract Point3D<P> clone(BigDecimal maxX, BigDecimal maxY, BigDecimal maxZ);

	@Override
	public Point2D<P> clone(BigDecimal maxX, BigDecimal maxY) {
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
		return !(stackValue.getDx().compareTo(dx) > 0 || stackValue.getDy().compareTo(dy) > 0 || stackValue.getDz().compareTo(dz) > 0);
	}

	public boolean isMax(Point3D<P> existing) {
		return maxY == existing.getMaxY() && maxX == existing.getMaxX() && maxZ == existing.getMaxZ();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + minX + "x" + minY + "x" + minZ + " " + maxX + "x" + maxY
				+ "x" + maxZ + "]";
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public boolean eclipses(Point3D<P> point) {
		return minX.compareTo(point.getMinX()) <= 0 &&
				minY.compareTo(point.getMinY()) <= 0 &&
				minZ.compareTo(point.getMinZ()) <= 0 &&
				point.getMaxX().compareTo(maxX) <= 0 &&
				point.getMaxY().compareTo(maxY) <= 0 &&
				point.getMaxZ().compareTo(maxZ) <= 0;
	}

	public boolean eclipsesX(Point2D<P> point) {
		return minX.compareTo(point.getMinX()) <= 0 && point.getMaxX().compareTo(maxX) <= 0;
	}

	public boolean eclipsesY(Point2D<P> point) {
		return minY.compareTo(point.getMinY()) <= 0 && point.getMaxY().compareTo(maxY) <= 0;
	}

	public boolean eclipsesZ(Point3D<P> point) {
		return minZ.compareTo(point.getMinZ()) <= 0 && point.getMaxZ().compareTo(maxZ) <= 0;
	}

	public boolean eclipsesMovedX(Point3D<P> point, BigDecimal x) {
		return minX.compareTo(x) <= 0 && point.getMaxX().compareTo(maxX) <= 0 && eclipsesY(point) && eclipsesZ(point);
	}

	public boolean eclipsesMovedY(Point3D<P> point, BigDecimal y) {
		return minY.compareTo(y) <= 0 && point.getMaxY().compareTo(maxY) <= 0 && eclipsesX(point) && eclipsesZ(point);
	}

	public boolean eclipsesMovedZ(Point3D<P> point, BigDecimal z) {
		return minZ.compareTo(z) <= 0 && point.getMaxZ().compareTo(maxZ) <= 0 && eclipsesX(point) && eclipsesY(point);
	}

	//       |                  
	//       |                  
	//       |                  
	//       |   |-------|      
	//       |   |       |      
	//       |   |       |      
	//       |---x=========================
	//
	//       |                  
	//       |                  
	//       |         |-------|
	//       |         |       |
	//       |         |       |
	//       |         |       |
	//       |---------x===================
	//

	public abstract Point3D<P> moveX(BigDecimal x);

	//       |                  
	//       |                  
	//       |                  
	//       |   |-------|      
	//       |   |       |      
	//       |   |       |      
	//       |---x=========================
	//
	//       |      added y support        
	//       |         |         
	//       |---------║         
	//       |         ║         
	//       |         ║-------|
	//       |         ║       |
	//       |         ║       |
	//       |         ║       |
	//       |---------x===================
	//

	public abstract Point3D<P> moveX(BigDecimal x, P yzSupport);

	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║-------|      
	//       |   ║       |      
	//       |   ║       |      
	//       |---x-------|----------------
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║---------|      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   x---------|      
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract Point3D<P> moveY(BigDecimal y);

	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║-------|      
	//       |   ║       |      
	//       |   ║       |      
	//       |---x-------|----------------
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║---------|      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   x===================  <-- added xz support
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract Point3D<P> moveY(BigDecimal y, P xzSupport);

	public abstract Point3D<P> moveZ(BigDecimal z);

	public abstract Point3D<P> moveZ(BigDecimal z, P xySupport);

	public abstract Point3D<P> rotate();

	public BigDecimal getVolumeAtZ(BigDecimal zz) {
		return dx.multiply(dy).multiply(maxZ.subtract(zz).add(BigDecimal.ONE));
	}

	public BigDecimal getVolumeAtMaxZ(BigDecimal maxZ) {
		return dx.multiply(dy).multiply(maxZ.subtract(minZ).add(BigDecimal.ONE));
	}

	public abstract BigDecimal calculateXYSupport(BigDecimal dx, BigDecimal dy);

	public abstract BigDecimal calculateXZSupport(BigDecimal dx, BigDecimal dz);

	public abstract BigDecimal calculateYZSupport(BigDecimal dy, BigDecimal dz);

}
