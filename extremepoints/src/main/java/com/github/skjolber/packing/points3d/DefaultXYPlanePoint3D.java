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
			
			int xyPlaneMinX, int xyPlaneMaxX, 
			int xyPlaneMinY, int xyPlaneMaxY
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.xyPlaneMinX = xyPlaneMinX;
		this.xyPlaneMaxX = xyPlaneMaxX;
		this.xyPlaneMinY = xyPlaneMinY;
		this.xyPlaneMaxY = xyPlaneMaxY;
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
	
	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXYPlanePoint3D(
			minX, minY, minZ,
			maxX, maxY, maxZ,
			
			xyPlaneMinX, Math.min(xyPlaneMaxX, maxX), 
			xyPlaneMinY, Math.min(xyPlaneMaxY, maxY)
		);
	}

}
