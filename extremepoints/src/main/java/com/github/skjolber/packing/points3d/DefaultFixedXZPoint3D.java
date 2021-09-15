package com.github.skjolber.packing.points3d;

public class DefaultFixedXZPoint3D extends Point3D implements FixedZPoint3D, FixedXPoint3D {

	/** range constrained to current minX */
	private final int fixedXMinY;
	private final int fixedXMaxY;

	private final int fixedXMinZ;
	private final int fixedXMaxZ;
	
	/** range constrained to current minZ */
	private final int fixedZMinX;
	private final int fixedZMaxX;

	private final int fixedZMinY;
	private final int fixedZMaxY;

	public DefaultFixedXZPoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, 
			int fixedXMinY,int fixedXMaxY,
			int fixedXMinZ, int fixedXMaxZ,
			
			int fixedZMinX, int fixedZMaxX,
			int fixedZMinY, int fixedZMaxY
			
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.fixedXMinY = fixedXMinY;
		this.fixedXMaxY = fixedXMaxY;
		
		this.fixedXMinZ = fixedXMinZ;
		this.fixedXMaxZ = fixedXMaxZ;
		
		this.fixedZMinX = fixedZMinX;
		this.fixedZMaxX = fixedZMaxX;
		
		this.fixedZMinY = fixedZMinY;
		this.fixedZMaxY = fixedZMaxY;
	}

	@Override
	public boolean isFixedY() {
		return false;
	}
	
	@Override
	public boolean isFixedX() {
		return true;
	}
	
	@Override
	public boolean isFixedZ() {
		return true;
	}
	
	public int getFixedXMinY() {
		return fixedXMinY;
	}
	
	public int getFixedXMaxY() {
		return fixedXMaxY;
	}

	@Override
	public int getFixedXMinZ() {
		return fixedXMinZ;
	}

	@Override
	public int getFixedXMaxZ() {
		return fixedXMaxZ;
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
