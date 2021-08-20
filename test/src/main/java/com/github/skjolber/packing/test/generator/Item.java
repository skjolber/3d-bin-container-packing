package com.github.skjolber.packing.test.generator;

public class Item {

	protected final int dx;
	protected final int dy;
	protected final int dz;
	
	protected final int count;
	protected final long volume;
	
	public Item(int dx, int dy, int dz, int count) {
		super();
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.count = count;
		
		this.volume = (long)dx * (long)dy * (long)dz;
	}
	
	public int getCount() {
		return count;
	}
	
	public int getDx() {
		return dx;
	}
	
	public int getDy() {
		return dy;
	}
	
	public int getDz() {
		return dz;
	}

	public long getVolume() {
		return volume;
	}

	@Override
	public String toString() {
		return "Item [dx=" + dx + ", dy=" + dy + ", dz=" + dz + ", count=" + count + ", volume=" + volume + "]";
	}

	
	
}
