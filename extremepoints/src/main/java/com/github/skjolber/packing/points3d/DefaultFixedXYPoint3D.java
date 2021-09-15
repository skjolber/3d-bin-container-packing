package com.github.skjolber.packing.points3d;

public class DefaultFixedXYPoint3D extends Point3D implements FixedYPoint3D, FixedXPoint3D {

	/** range constrained to current minX */
	private final int fixedXMinY;
	private final int fixedXMaxY;

	private final int fixedXMinZ;
	private final int fixedXMaxZ;

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

	public DefaultFixedXYPoint3D(
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,

			int fixedYMinX, int fixedYMaxX, 
			int fixedYMinZ, int fixedYMaxZ, 

			int fixedXMinY, int fixedXMaxY,
			int fixedXMinZ, int fixedXMaxZ,

			int fixedZMinX, int fixedZMaxX,
			int fixedZMinY, int fixedZMaxY
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.fixedYMinX = fixedYMinX;
		this.fixedYMaxX = fixedYMaxX;

		this.fixedYMinZ = fixedYMinZ;
		this.fixedYMaxZ = fixedYMaxZ;

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
		return true;
	}

	@Override
	public boolean isFixedX() {
		return true;
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
