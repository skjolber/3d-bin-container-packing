package com.github.skjolber.packing.api;

public class StackSpace extends Dimension {

	private StackSpace parent;
	
	protected int x; // width coordinate
	protected int y; // depth coordinate
	protected int z; // height coordinate
	
	// TODO weight constraint
	protected int maxSupportedPressure; // i.e.  
	protected int maxSupportedWeight;
	
	public int getX() {
		return x;
	}

	public void setX(final int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(final int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(final int z) {
		this.z = z;
	}

	public StackSpace(StackSpace parent, String name, int w, int d, int h, int x, int y, int z) {
		super(name, w, d, h);

		this.parent = parent;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public StackSpace() {
		super(null, 0, 0, 0);
	}
	
	public StackSpace(int w, int d, int h, int x, int y, int z) {
		this(null, null, w, d, h, x, y, z);
	}

	public StackSpace getParent() {
		return parent;
	}
	
	public void setParent(StackSpace parent) {
		this.parent = parent;
	}
}
