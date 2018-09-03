package com.github.skjolberg.packing;

public class Space extends Dimension {

	protected Space parent;
	protected Space remainder;

	protected int x; // width
	protected int y; // depth
	protected int z; // height
	
	public Space() {
		super();
	}
	
	public Space(final Space parent, final int w, final int d, final int h, final int x, final int y, final int z) {
		this(parent, null, w, d, h, x, y, z);
	}
	
	public Space(final Space parent, final String name, final int w, final int d, final int h, final int x, final int y, final int z) {
		super(name, w, d, h);
		
		this.parent = parent;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Space(final int w, final int d, final int h, final int x, final int y, final int z) {
		this(null, null, w, d, h, x, y, z);
	}
	
	public Space(final String name, final int w, final int d, final int h, final int x, final int y, final int z) {
		this(null, name, w, d, h, x, y, z);
	}
	
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
	
	public Space getParent() {
		return parent;
	}
	
	public void setParent(final Space parent) {
		this.parent = parent;
	}
	
	public void setRemainder(final Space dual) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((remainder == null) ? 0 : remainder.hashCode());
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Space other = (Space) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (remainder == null) {
			if (other.remainder != null)
				return false;
		} else if (!remainder.equals(other.remainder))
			return false;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		if (z != other.z)
			return false;
		return true;
	}

	public void copyFrom(final Space space) {

		this.parent = space.parent;
		this.x = space.x;
		this.y = space.y;
		this.z = space.z;
		
		this.width = space.width;
		this.depth = space.depth;
		this.height = space.height;
	}
	
	public void copyFrom(final int w, final int d, final int h, final int x, final int y, final int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.width = w;
		this.depth = d;
		this.height = h;
	}
	
	
}
