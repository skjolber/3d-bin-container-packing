package com.github.skjolber.packing.points3d;

public class Default3DPlanePoint3D extends Point3D implements XZPlanePoint3D, YZPlanePoint3D, XYPlanePoint3D {


	/** range constrained to current minX */
	private final int yzPlaneMinY;
	private final int yzPlaneXMaxY;

	private final int yzPlaneMinZ;
	private final int yzPlaneMaxZ;
	
	/** range constrained to current minY */
	private final int xzPlaneMinX;
	private final int xzPlaneMaxX;

	private final int xzPlaneMinZ;
	private final int xzPlaneMaxZ;

	/** range constrained to current minZ */
	private final int xyPlaneMinX;
	private final int xyPlaneMaxX;

	private final int xyPlaneMinY;
	private final int xyPlaneMaxY;

	public Default3DPlanePoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ,
			
			int yzPlaneMinY, int yzPlaneXMaxY,
			int yzPlaneMinZ, int yzPlaneMaxZ,

			int xzPlaneMinX, int xzPlaneMaxX, 
			int xzPlaneMinZ, int xzPlaneMaxZ, 
			
			int xyPlaneMinX, int xyPlaneMaxX,
			int xyPlaneMinY, int xyPlaneMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.yzPlaneMinY = yzPlaneMinY;
		this.yzPlaneXMaxY = yzPlaneXMaxY;
		
		this.yzPlaneMinZ = yzPlaneMinZ;
		this.yzPlaneMaxZ = yzPlaneMaxZ;
		
		this.xzPlaneMinX = xzPlaneMinX;
		this.xzPlaneMaxX = xzPlaneMaxX;

		this.xzPlaneMinZ = xzPlaneMinZ;
		this.xzPlaneMaxZ = xzPlaneMaxZ;

		this.xyPlaneMinX = xyPlaneMinX;
		this.xyPlaneMaxX = xyPlaneMaxX;
		
		this.xyPlaneMinY = xyPlaneMinY;
		this.xyPlaneMaxY = xyPlaneMaxY;
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
		return yzPlaneXMaxY;
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

	public int getSupportedXYPlaneMinX() {
		return xyPlaneMinX;
	}

	public int getSupportedXYPlaneMaxX() {
		return xyPlaneMaxX;
	}

	public int getSupportedXYPlaneMinY() {
		return xyPlaneMinY;
	}

	public int getSupportedXYPlaneMaxY() {
		return xyPlaneMaxY;
	}
	
	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlaneMinX <= x && x <= xzPlaneMaxX && xzPlaneMinZ <= z && z <= xzPlaneMaxZ;
	}
	
	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlaneMinY <= y && y <= yzPlaneXMaxY && yzPlaneMinZ <= z && z <= yzPlaneMaxZ;
	}
	
	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlaneMinX <= x && x <= xyPlaneMaxX && xyPlaneMinY <= y && y <= xyPlaneMaxY;
	}
	
	
}
