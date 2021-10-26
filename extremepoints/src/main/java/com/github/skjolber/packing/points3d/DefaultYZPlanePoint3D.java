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
			
			int yzPlaneMinY, int yzPlaneMaxY, 
			int yzPlaneMinZ, int yzPlaneMaxZ
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.yzPlaneMinY = yzPlaneMinY;
		this.yzPlaneMaxY = yzPlaneMaxY;
		this.yzPlaneMinZ = yzPlaneMinZ;
		this.yzPlaneMaxZ = yzPlaneMaxZ;
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
	
	public boolean isYZPlaneEdgeZ(int z) {
		return yzPlaneMaxZ == z - 1;
	}

	public boolean isYZPlaneEdgeY(int y) {
		return yzPlaneMaxY == y - 1;
	}

	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultYZPlanePoint3D(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				
				yzPlaneMinY, Math.min(maxY, yzPlaneMaxY), 
				yzPlaneMinZ, Math.min(maxZ, yzPlaneMaxZ)				
			);
	}
	
}
