package com.github.skjolber.packing.points3d;

public class DefaultXYPlanePoint3D extends Point3D implements XYPlanePoint3D  {

	/** range constrained to current minZ */
	private final int xyPlaneMinX;
	private final int xyPlaneMaxX;

	private final int xyPlaneMinY;
	private final int xyPlaneMaxY;

	public DefaultXYPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int fixedX, int fixedXx, 
			int fixedY, int fixedYy
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.xyPlaneMinX = fixedX;
		this.xyPlaneMaxX = fixedXx;
		this.xyPlaneMinY = fixedY;
		this.xyPlaneMaxY = fixedYy;
	}

	public int getSupportedXYPlaneMinY() {
		return xyPlaneMinY;
	}
	
	public int getSupportedXYPlaneMaxY() {
		return xyPlaneMaxY;
	}
	@Override
	public int getSupportedXYPlaneMinX() {
		return xyPlaneMinX;
	}
	
	@Override
	public int getSupportedXYPlaneMaxX() {
		return xyPlaneMaxX;
	}
	
	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlaneMinX <= x && x <= xyPlaneMaxX && xyPlaneMinY <= y && y <= xyPlaneMaxY;
	}
	
	public boolean isXYPlaneEdgeX(int x) {
		return xyPlaneMaxX == x - 1;
	}

	public boolean isXYPlaneEdgeY(int y) {
		return xyPlaneMaxY == y - 1;
	}

}
