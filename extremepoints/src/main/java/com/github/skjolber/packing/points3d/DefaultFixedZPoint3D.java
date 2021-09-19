package com.github.skjolber.packing.points3d;

public class DefaultFixedZPoint3D extends Point3D implements FixedZPoint3D  {

	/** range constrained to current minZ */
	private final int fixedZMinX;
	private final int fixedZMaxX;

	/** range constrained to current minZ */
	private final int fixedZMinY;
	private final int fixedZMaxY;

	public DefaultFixedZPoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			
			int fixedX, int fixedXx, 
			int fixedY, int fixedYy
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
		
		this.fixedZMinX = fixedX;
		this.fixedZMaxX = fixedXx;
		this.fixedZMinY = fixedY;
		this.fixedZMaxY = fixedYy;
	}

	public int getFixedZMinY() {
		return fixedZMinY;
	}
	
	public int getFixedZMaxY() {
		return fixedZMaxY;
	}
	@Override
	public int getFixedZMinX() {
		return fixedZMinX;
	}
	
	@Override
	public int getFixedZMaxX() {
		return fixedZMaxX;
	}
	
	@Override
	public boolean isFixedZ(int x, int y) {
		return x < fixedZMaxX && y < fixedZMaxY;
	}
	
	
}
