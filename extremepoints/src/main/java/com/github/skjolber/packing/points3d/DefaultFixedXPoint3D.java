package com.github.skjolber.packing.points3d;

public class DefaultFixedXPoint3D extends Point3D implements SupportedYZPlanePoint3D  {

	/** range constrained to current minX */
	private final int fixedXMinY;
	private final int fixedXMaxY;

	/** range constrained to current minX */
	private final int fixedXMinZ;
	private final int fixedXMaxZ;

	public DefaultFixedXPoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int fixedY, int fixedYy, 
			int fixedZ, int fixedZz
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.fixedXMinY = fixedY;
		this.fixedXMaxY = fixedYy;
		this.fixedXMinZ = fixedZ;
		this.fixedXMaxZ = fixedZz;
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
	public boolean isSupportedYZPlane(int y, int z) {
		return fixedXMinY <= y && y <= fixedXMaxY && fixedXMinZ <= z && z <= fixedXMaxZ;
	}
	
	
}
