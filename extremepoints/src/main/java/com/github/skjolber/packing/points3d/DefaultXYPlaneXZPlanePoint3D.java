package com.github.skjolber.packing.points3d;

public class DefaultXYPlaneXZPlanePoint3D extends Point3D implements XYPlanePoint3D, XZPlanePoint3D {

	/** range constrained to current minY */
	private final int xzPlaneMinX;
	private final int xzPlaneMaxX;

	private final int xzPlaneMinZ;
	private final int xzPlaneMaxZ;

	/** range constrained to current minZ */
	private final int xyPlaneMinX;
	private final int xyPlaneMaxX;

	private final int xyPlaneMinY;
	private final int xyPlaneMaxY;

	public DefaultXYPlaneXZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int xzPlaneMinX, int xzPlaneMaxX, 
			int xzPlaneMinZ, int xzPlaneMaxZ, 
			
			int xyPlaneMinX, int xyPlaneMaxX,
			int xyPlaneMinY, int xyPlaneMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.xzPlaneMinX = xzPlaneMinX;
		this.xzPlaneMaxX = xzPlaneMaxX;

		this.xzPlaneMinZ = xzPlaneMinZ;
		this.xzPlaneMaxZ = xzPlaneMaxZ;

		this.xyPlaneMinX = xyPlaneMinX;
		this.xyPlaneMaxX = xyPlaneMaxX;
		
		this.xyPlaneMinY = xyPlaneMinY;
		this.xyPlaneMaxY = xyPlaneMaxY;
	}
	
	public int getSupportedXZPlaneMinX() {
		return xzPlaneMinX;
	}

	public int getSupportedXZPlaneMaxX() {
		return xzPlaneMaxX;
	}

	@Override
	public int getSupportedXZPlaneMinZ() {
		return xzPlaneMinZ;
	}

	@Override
	public int getSupportedXZPlaneMaxZ() {
		return xzPlaneMaxZ;
	}

	public int getSupportedXYPlaneMinX() {
		return xyPlaneMinX;
	}

	public int getSupportedXYPlaneMaxX() {
		return xyPlaneMaxX;
	}

	public int getSupportedXYPlaneMinY() {
		return xyPlaneMinY;
	}

	public int getSupportedXYPlaneMaxY() {
		return xyPlaneMaxY;
	}
	
	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlaneMinX <= x && x <= xzPlaneMaxX && xzPlaneMinZ <= z && z <= xzPlaneMaxZ;
	}

	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlaneMinX <= x && x <= xyPlaneMaxX && xyPlaneMinY <= y && y <= xyPlaneMaxY;
	}
	
	public boolean isXZPlaneEdgeX(int x) {
		return xzPlaneMaxX == x - 1;
	}

	public boolean isXZPlaneEdgeZ(int z) {
		return xzPlaneMaxZ == z - 1;
	}

	public boolean isXYPlaneEdgeX(int x) {
		return xyPlaneMaxX == x - 1;
	}

	public boolean isXYPlaneEdgeY(int y) {
		return xyPlaneMaxY == y - 1;
	}

	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
	
		return new DefaultXYPlaneXZPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				
				xzPlaneMinX, Math.min(maxX, xzPlaneMaxX), 
				xzPlaneMinZ, Math.min(maxZ, xzPlaneMaxZ), 
				
				xyPlaneMinX, Math.min(maxX, xyPlaneMaxX),
				xyPlaneMinY, Math.min(maxY, xyPlaneMaxY));
	}
}
