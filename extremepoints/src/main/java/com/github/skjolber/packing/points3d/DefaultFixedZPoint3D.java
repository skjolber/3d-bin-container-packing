package com.github.skjolber.packing.points3d;

public class DefaultFixedZPoint3D extends Point3D implements SupportedXYPlanePoint3D  {

	/** range constrained to current minZ */
	private final int fixedZMinX;
	private final int fixedZMaxX;

	/** range constrained to current minZ */
	private final int fixedZMinY;
	private final int fixedZMaxY;

	public DefaultFixedZPoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int fixedX, int fixedXx, 
			int fixedY, int fixedYy
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.fixedZMinX = fixedX;
		this.fixedZMaxX = fixedXx;
		this.fixedZMinY = fixedY;
		this.fixedZMaxY = fixedYy;
	}

	public int getSupportedXYPlaneMinY() {
		return fixedZMinY;
	}
	
	public int getSupportedXYPlaneMaxY() {
		return fixedZMaxY;
	}
	@Override
	public int getSupportedXYPlaneMinX() {
		return fixedZMinX;
	}
	
	@Override
	public int getSupportedXYPlaneMaxX() {
		return fixedZMaxX;
	}
	
	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return fixedZMinX <= x && x <= fixedZMaxX && fixedZMinY <= y && y <= fixedZMaxY;
	}
	
	
}
