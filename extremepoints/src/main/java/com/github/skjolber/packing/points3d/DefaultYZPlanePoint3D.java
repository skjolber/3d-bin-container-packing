package com.github.skjolber.packing.points3d;

public class DefaultYZPlanePoint3D extends Point3D implements YZPlanePoint3D  {

	/** range constrained to current minX */
	private final int yzPlaneMinY;
	private final int yzPlaneMaxY;

	private final int yzPlaneMinZ;
	private final int yzPlaneMaxZ;

	public DefaultYZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int fixedY, int fixedYy, 
			int fixedZ, int fixedZz
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.yzPlaneMinY = fixedY;
		this.yzPlaneMaxY = fixedYy;
		this.yzPlaneMinZ = fixedZ;
		this.yzPlaneMaxZ = fixedZz;
	}

	public int getSupportedYZPlaneMinY() {
		return yzPlaneMinY;
	}
	
	public int getSupportedYZPlaneMaxY() {
		return yzPlaneMaxY;
	}
	@Override
	public int getSupportedYZPlaneMinZ() {
		return yzPlaneMinZ;
	}
	
	@Override
	public int getSupportedYZPlaneMaxZ() {
		return yzPlaneMaxZ;
	}
	
	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlaneMinY <= y && y <= yzPlaneMaxY && yzPlaneMinZ <= z && z <= yzPlaneMaxZ;
	}
	
	
}
