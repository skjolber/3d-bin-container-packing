package com.github.skjolber.packing.points3d;

public class DefaultXZPlaneYZPlanePoint3D extends Point3D implements XZPlanePoint3D, YZPlanePoint3D {

	/** range constrained to current minX */
	private final int yzPlaneMinY;
	private final int yzPlaneMaxY;

	private final int yzPlaneMinZ;
	private final int yzPlaneMaxZ;

	/** range constrained to current minY */
	private final int xzPlaneMinX;
	private final int xzPlaneMaxX;

	private final int xzPlaneMinZ;
	private final int xzPlaneMaxZ;

	public DefaultXZPlaneYZPlanePoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,

			int xzPlaneMinX, int xzPlaneMaxX, 
			int xzPlaneMinZ, int xzPlaneMaxZ, 

			int yzPlaneMinY, int yzPlaneMaxY,
			int yzPlaneMinZ, int yzPlaneMaxZ
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.xzPlaneMinX = xzPlaneMinX;
		this.xzPlaneMaxX = xzPlaneMaxX;

		this.xzPlaneMinZ = xzPlaneMinZ;
		this.xzPlaneMaxZ = xzPlaneMaxZ;

		this.yzPlaneMinY = yzPlaneMinY;
		this.yzPlaneMaxY = yzPlaneMaxY;

		this.yzPlaneMinZ = yzPlaneMinZ;
		this.yzPlaneMaxZ = yzPlaneMaxZ;

	}

	public int getSupportedXZPlaneMinX() {
		return xzPlaneMinX;
	}

	public int getSupportedXZPlaneMaxX() {
		return xzPlaneMaxX;
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
	public int getSupportedXZPlaneMinZ() {
		return xzPlaneMinZ;
	}


	@Override
	public int getSupportedXZPlaneMaxZ() {
		return xzPlaneMaxZ;
	}


	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlaneMinY <= y && y <= yzPlaneMaxY && yzPlaneMinZ <= z && z <= yzPlaneMaxZ;
	}

	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlaneMinX <= x && x <= xzPlaneMaxX && xzPlaneMinZ <= z && z <= xzPlaneMaxZ;
	}
}
