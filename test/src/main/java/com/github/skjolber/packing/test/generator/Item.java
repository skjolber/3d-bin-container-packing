package com.github.skjolber.packing.test.generator;

public class Item {

	protected int dx;
	protected int dy;
	protected int dz;
	
	protected int count;
	protected long volume;
	
	public Item() {
	}
	
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

	public void setDx(int dx) {
		this.dx = dx;
	}

	public void setDy(int dy) {
		this.dy = dy;
	}

	public void setDz(int dz) {
		this.dz = dz;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setVolume(long volume) {
		this.volume = volume;
	}	
	
}
