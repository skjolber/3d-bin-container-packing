package com.github.skjolber.packing.points3d;

public class DefaultFixedYPoint3D extends Point3D implements SupportedXZPlanePoint3D {

	/** range constrained to current minY */
	private final int fixedYMinX;
	private final int fixedYMaxX;

	private final int fixedYMinZ;
	private final int fixedYMaxZ;

	public DefaultFixedYPoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ, 
			
			int fixedYMinX, int fixedYMaxX, 
			int fixedYMinZ, int fixedYMaxZ
			) {
		super(minX, minY, minZ, maxY, maxX, maxZ);
		this.fixedYMinX = fixedYMinX;
		this.fixedYMaxX = fixedYMaxX;
		this.fixedYMinZ = fixedYMinZ;
		this.fixedYMaxZ = fixedYMaxZ;
	}

	public int getSupportedXZPlaneMinX() {
		return fixedYMinX;
	}
	
	public int getSupportedXZPlaneMaxX() {
		return fixedYMaxX;
	}
	
	@Override
	public int getSupportedXZPlaneMaxZ() {
		return fixedYMaxZ;
	}
	
	@Override
	public int getSupportedXZPlaneMinZ() {
		return fixedYMinZ;
	}

	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return fixedYMinX <= x && x <= fixedYMaxX && fixedYMinZ <= z && z <= fixedYMaxZ;
	}
}
