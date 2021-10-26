package com.github.skjolber.packing.points3d;

public class DefaultXYPlaneYZPlanePoint3D extends Point3D implements XYPlanePoint3D, YZPlanePoint3D {

	/** range constrained to current minX */
	private final int yzPlaneMinY;
	private final int yzPlaneMaxY;

	private final int yzPlaneMinZ;
	private final int yzPlaneMaxZ;
	
	/** range constrained to current minZ */
	private final int xyPlaneMinX;
	private final int xyPlaneMaxX;

	private final int xyPlaneMinY;
	private final int xyPlaneMaxY;

	public DefaultXYPlaneYZPlanePoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, 
			int yzPlaneMinY,int yzPlaneMaxY,
			int yzPlaneMinZ, int yzPlaneMaxZ,
			
			int xyPlaneMinX, int xyPlaneMaxX,
			int xyPlaneMinY, int xyPlaneMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.yzPlaneMinY = yzPlaneMinY;
		this.yzPlaneMaxY = yzPlaneMaxY;
		
		this.yzPlaneMinZ = yzPlaneMinZ;
		this.yzPlaneMaxZ = yzPlaneMaxZ;
		
		this.xyPlaneMinX = xyPlaneMinX;
		this.xyPlaneMaxX = xyPlaneMaxX;
		
		this.xyPlaneMinY = xyPlaneMinY;
		this.xyPlaneMaxY = xyPlaneMaxY;
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
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlaneMinY <= y && y <= yzPlaneMaxY && yzPlaneMinZ <= z && z <= yzPlaneMaxZ;
	}

	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlaneMinX <= x && x <= xyPlaneMaxX && xyPlaneMinY <= y && y <= xyPlaneMaxY;
	}
	
	public boolean isYZPlaneEdgeZ(int z) {
		return yzPlaneMaxZ == z - 1;
	}

	public boolean isYZPlaneEdgeY(int y) {
		return yzPlaneMaxY == y - 1;
	}
	
	public boolean isXYPlaneEdgeX(int x) {
		return xyPlaneMaxX == x - 1;
	}

	public boolean isXYPlaneEdgeY(int y) {
		return xyPlaneMaxY == y - 1;
	}

	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new DefaultXYPlaneYZPlanePoint3D(
				minX, minY, minZ, 
				maxX, maxY, maxZ,
				
				yzPlaneMinY, Math.min(maxY, yzPlaneMaxY),
				yzPlaneMinZ, Math.min(maxZ, yzPlaneMaxZ),
				
				xyPlaneMinX, Math.min(maxX, xyPlaneMaxX),
				xyPlaneMinY, Math.min(maxY, xyPlaneMaxY)
				
				);
	}

}
