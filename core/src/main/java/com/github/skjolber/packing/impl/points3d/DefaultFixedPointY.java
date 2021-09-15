package com.github.skjolber.packing.impl.points3d;

public class DefaultFixedPointY extends Point implements FixedPointY {

	/** range constrained to current minY */
	private final int fixedYMinX;
	private final int fixedYMaxX;

	private final int fixedYMinZ;
	private final int fixedYMaxZ;

	public DefaultFixedPointY(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ, 
			
			int fixedYMinX, int fixedYMaxX, 
			int fixedYMinZ, int fixedYMaxZ
			) {
		super(minX, minY, minZ, maxY, maxX, maxZ);
		this.fixedYMinX = fixedYMinX;
		this.fixedYMaxX = fixedYMaxX;
		this.fixedYMinZ = fixedYMinZ;
		this.fixedYMaxZ = fixedYMaxZ;
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
		return false;
	}

	public int getFixedYMinX() {
		return fixedYMinX;
	}
	
	public int getFixedYMaxX() {
		return fixedYMaxX;
	}
	
	@Override
	public int getFixedYMaxZ() {
		return fixedYMaxZ;
	}
	
	@Override
	public int getFixedYMinZ() {
		return fixedYMinZ;
	}


}
