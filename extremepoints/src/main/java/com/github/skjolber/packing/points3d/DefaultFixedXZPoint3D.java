package com.github.skjolber.packing.points3d;

public class DefaultFixedXZPoint3D extends Point3D implements SupportedXYPlanePoint3D, SupportedYZPlanePoint3D {

	/** range constrained to current minX */
	private final int fixedXMinY;
	private final int fixedXMaxY;

	private final int fixedXMinZ;
	private final int fixedXMaxZ;
	
	/** range constrained to current minZ */
	private final int fixedZMinX;
	private final int fixedZMaxX;

	private final int fixedZMinY;
	private final int fixedZMaxY;

	public DefaultFixedXZPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, 
			int fixedXMinY,int fixedXMaxY,
			int fixedXMinZ, int fixedXMaxZ,
			
			int fixedZMinX, int fixedZMaxX,
			int fixedZMinY, int fixedZMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.fixedXMinY = fixedXMinY;
		this.fixedXMaxY = fixedXMaxY;
		
		this.fixedXMinZ = fixedXMinZ;
		this.fixedXMaxZ = fixedXMaxZ;
		
		this.fixedZMinX = fixedZMinX;
		this.fixedZMaxX = fixedZMaxX;
		
		this.fixedZMinY = fixedZMinY;
		this.fixedZMaxY = fixedZMaxY;
	}
	
	public int getSupportedYZPlaneMinY() {
		return fixedXMinY;
	}
	
	public int getSupportedYZPlaneMaxY() {
		return fixedXMaxY;
	}

	@Override
	public int getSupportedYZPlaneMinZ() {
		return fixedXMinZ;
	}

	@Override
	public int getSupportedYZPlaneMaxZ() {
		return fixedXMaxZ;
	}

	public int getSupportedXYPlaneMinX() {
		return fixedZMinX;
	}

	public int getSupportedXYPlaneMaxX() {
		return fixedZMaxX;
	}

	public int getSupportedXYPlaneMinY() {
		return fixedZMinY;
	}

	public int getSupportedXYPlaneMaxY() {
		return fixedZMaxY;
	}

	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return fixedXMinY <= y && y <= fixedXMaxY && fixedXMinZ <= z && z <= fixedXMaxZ;
	}

	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return fixedZMinX <= x && x <= fixedZMaxX && fixedZMinY <= y && y <= fixedZMaxY;
	}
}
