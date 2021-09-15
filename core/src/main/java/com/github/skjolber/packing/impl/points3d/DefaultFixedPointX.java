package com.github.skjolber.packing.impl.points3d;

public class DefaultFixedPointX extends Point implements FixedPointX  {

	/** range constrained to current minX */
	private final int fixedXMinY;
	private final int fixedXMaxY;

	/** range constrained to current minX */
	private final int fixedXMinZ;
	private final int fixedXMaxZ;

	public DefaultFixedPointX(
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
		return false;
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
	
	
}
