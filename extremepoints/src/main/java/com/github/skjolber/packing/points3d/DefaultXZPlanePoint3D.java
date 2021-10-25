package com.github.skjolber.packing.points3d;

public class DefaultXZPlanePoint3D extends Point3D implements XZPlanePoint3D {

	/** range constrained to current minY */
	private final int xzPlaneMinX;
	private final int xzPlaneMaxX;

	private final int xzPlaneMinZ;
	private final int xzPlaneMaxZ;

	public DefaultXZPlanePoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ, 
			
			int xzPlaneMinX, int xzPlaneMaxX, 
			int xzPlaneMinZ, int xzPlaneMaxZ
			) {
		super(minX, minY, minZ, maxY, maxX, maxZ);
		
		this.xzPlaneMinX = xzPlaneMinX;
		this.xzPlaneMaxX = xzPlaneMaxX;
		this.xzPlaneMinZ = xzPlaneMinZ;
		this.xzPlaneMaxZ = xzPlaneMaxZ;
	}

	public int getSupportedXZPlaneMinX() {
		return xzPlaneMinX;
	}
	
	public int getSupportedXZPlaneMaxX() {
		return xzPlaneMaxX;
	}
	
	@Override
	public int getSupportedXZPlaneMaxZ() {
		return xzPlaneMaxZ;
	}
	
	@Override
	public int getSupportedXZPlaneMinZ() {
		return xzPlaneMinZ;
	}

	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlaneMinX <= x && x <= xzPlaneMaxX && xzPlaneMinZ <= z && z <= xzPlaneMaxZ;
	}
	
	public boolean isXZPlaneEdgeX(int x) {
		return xzPlaneMaxX == x - 1;
	}

	public boolean isXZPlaneEdgeZ(int z) {
		return xzPlaneMaxZ == z - 1;
	}

}
