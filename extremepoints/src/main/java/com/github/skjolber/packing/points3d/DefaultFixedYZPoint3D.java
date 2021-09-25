package com.github.skjolber.packing.points3d;

public class DefaultFixedYZPoint3D extends Point3D implements SupportedXZPlanePoint3D, SupportedXYPlanePoint3D {

	/** range constrained to current minY */
	private final int fixedYMinX;
	private final int fixedYMaxX;

	private final int fixedYMinZ;
	private final int fixedYMaxZ;

	/** range constrained to current minZ */
	private final int fixedZMinX;
	private final int fixedZMaxX;

	private final int fixedZMinY;
	private final int fixedZMaxY;

	public DefaultFixedYZPoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int fixedYMinX, int fixedYMaxX, 
			int fixedYMinZ, int fixedYMaxZ, 
			
			int fixedZMinX, int fixedZMaxX,
			int fixedZMinY, int fixedZMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.fixedYMinX = fixedYMinX;
		this.fixedYMaxX = fixedYMaxX;

		this.fixedYMinZ = fixedYMinZ;
		this.fixedYMaxZ = fixedYMaxZ;

		this.fixedZMinX = fixedZMinX;
		this.fixedZMaxX = fixedZMaxX;
		
		this.fixedZMinY = fixedZMinY;
		this.fixedZMaxY = fixedZMaxY;
	}
	
	public int getSupportedXZPlaneMinX() {
		return fixedYMinX;
	}

	public int getSupportedXZPlaneMaxX() {
		return fixedYMaxX;
	}

	@Override
	public int getSupportedXZPlaneMinZ() {
		return fixedYMinZ;
	}

	@Override
	public int getSupportedXZPlaneMaxZ() {
		return fixedYMaxZ;
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
	public boolean isSupportedXZPlane(int x, int z) {
		return fixedYMinX <= x && x <= fixedYMaxX && fixedYMinZ <= z && z <= fixedYMaxZ;
	}

	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return fixedZMinX <= x && x <= fixedZMaxX && fixedZMinY <= y && y <= fixedZMaxY;
	}
}
