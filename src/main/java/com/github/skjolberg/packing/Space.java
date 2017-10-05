package com.github.skjolberg.packing;

public class Space extends Dimension {

	protected Space parent;
	protected Space remainder;

	protected int x; // width
	protected int y; // depth
	protected int z; // height
	
	public Space(Space parent, int w, int d, int h, int x, int y, int z) {
		this(parent, null, w, d, h, x, y, z);
	}
	
	public Space(Space parent, String name, int w, int d, int h, int x, int y, int z) {
		super(name, w, d, h);
		
		this.parent = parent;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Space(int w, int d, int h, int x, int y, int z) {
		this(null, null, w, d, h, x, y, z);
	}
	
	public Space(String name, int w, int d, int h, int x, int y, int z) {
		this(null, name, w, d, h, x, y, z);
	}
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	
	public Space getParent() {
		return parent;
	}
	
	public void setParent(Space parent) {
		this.parent = parent;
	}
	
	public void setRemainder(Space dual) {
		this.remainder = dual;
	}
	
	public Space getRemainder() {
		return remainder;
	}

	@Override
	public String toString() {
		return "Space [name=" + name + ", " + x + "x" + y + "x" + z + ", width=" + width + ", depth=" + depth + ", height="
				+ height + "]";
	}
	
}
