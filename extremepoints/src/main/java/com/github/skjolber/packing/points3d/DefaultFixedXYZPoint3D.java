package com.github.skjolber.packing.points3d;

public class DefaultFixedXYZPoint3D extends Point3D implements SupportedXZPlanePoint3D, SupportedYZPlanePoint3D, SupportedXYPlanePoint3D {


	/** range constrained to current minX */
	private final int fixedXMinY;
	private final int fixedXMaxY;

	private final int fixedXMinZ;
	private final int fixedXMaxZ;
	
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

	public DefaultFixedXYZPoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ,
			
			int fixedXMinY, int fixedXMaxY,
			int fixedXMinZ, int fixedXMaxZ,

			int fixedYMinX, int fixedYMaxX, 
			int fixedYMinZ, int fixedYMaxZ, 
			
			int fixedZMinX, int fixedZMaxX,
			int fixedZMinY, int fixedZMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.fixedXMinY = fixedXMinY;
		this.fixedXMaxY = fixedXMaxY;
		
		this.fixedXMinZ = fixedXMinZ;
		this.fixedXMaxZ = fixedXMaxZ;
		
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
	public boolean isSupportedYZPlane(int y, int z) {
		return fixedXMinY <= y && y <= fixedXMaxY && fixedXMinZ <= z && z <= fixedXMaxZ;
	}
	
	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return fixedZMinX <= x && x <= fixedZMaxX && fixedZMinY <= y && y <= fixedZMaxY;
	}
}
