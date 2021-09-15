package com.github.skjolber.packing.points3d;

public class DefaultFixedYZPoint3D extends Point3D implements FixedYPoint3D, FixedZPoint3D {

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

	@Override
	public boolean isFixedY() {
		return true;
	}
	
	@Override
	public boolean isFixedX() {
		return false;
	}
	
	@Override
	public boolean isFixedZ() {
		return true;
	}
	
	public int getFixedYMinX() {
		return fixedYMinX;
	}

	public int getFixedYMaxX() {
		return fixedYMaxX;
	}

	@Override
	public int getFixedYMinZ() {
		return fixedYMinZ;
	}

	@Override
	public int getFixedYMaxZ() {
		return fixedYMaxZ;
	}

	public int getFixedZMinX() {
		return fixedZMinX;
	}

	public int getFixedZMaxX() {
		return fixedZMaxX;
	}

	public int getFixedZMinY() {
		return fixedZMinY;
	}

	public int getFixedZMaxY() {
		return fixedZMaxY;
	}
	
	

}
