package com.github.skjolber.packing.impl.points3d;

public abstract class Point implements Comparable<Point>{

	protected final int minX;
	protected final int minY;
	protected final int minZ;
	
	protected int maxY;
	protected int maxX;
	protected int maxZ;
	
	protected int dx;
	protected int dy;
	protected int dz;
	
	public Point(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super();
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;

		
		this.dx = maxX - minX;
		this.dy = maxY - minY;
		this.dz = maxZ - minZ;
	}

	public abstract boolean isFixedY();
	
	public abstract boolean isFixedX();

	public abstract boolean isFixedZ();

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMaxX() {
		return maxX;
	}
	
	@Override
	public int compareTo(Point o) {
		
		int x = Integer.compare(this.minX, o.minX);

		if(x == 0) {
			x = Integer.compare(this.minY, o.minY);
		}
		if(x == 0) {
			x = Integer.compare(this.minZ, o.minZ);
		}
		return x;
	}
	
	public void setMaxX(int maxX) {
		this.maxX = maxX;
		
		this.dx = maxX - minX;
	}
	
	public void setMaxY(int maxY) {
		this.maxY = maxY;
		
		this.dy = maxY - minY;
	}
	
	public void setMaxZ(int maxZ) {
		this.maxZ = maxZ;
		
		this.dz = maxZ - minZ;
	}
	
	
	public boolean isWithin(int dx, int dy, int dz) {
		return dx <= this.dx && dy <= this.dy && dz <= this.dz;
	}
	
	public int getDy() {
		return dy;
	}

	public int getDx() {
		return dx;
	}
	
	public int getDz() {
		return dz;
	}
	
	public int getMaxZ() {
		return maxZ;
	}
	
	public int getMinZ() {
		return minZ;
	}

}
