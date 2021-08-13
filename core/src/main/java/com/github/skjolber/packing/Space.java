package com.github.skjolber.packing;

public class Space extends Dimension {

	protected static boolean between(int start, int value, int end) {
		return start <= value && value <= end;
	}
	
	protected boolean intersects(int start, int end, int value, int distance) {
		return between(start, value, end) || between(start, value + distance, end) || (value < start && end < value + distance);
	}

	private Space parent;
	private Space remainder;

	private int x; // width
	private int y; // depth
	private int z; // height

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

	public Space() {
	}
	
	public Space(Space clone) {
		copyFrom(clone);
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Space other = (Space) obj;
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

	public void copyFrom(Space space) {

		this.parent = space.parent;
		this.x = space.x;
		this.y = space.y;
		this.z = space.z;

		this.width = space.width;
		this.depth = space.depth;
		this.height = space.height;
	}

	public void copyFrom(int w, int d, int h, int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;

		this.width = w;
		this.depth = d;
		this.height = h;
	}

	public boolean intersects(Space space) {
		return intersectsX(space) && intersectsY(space) && intersectsZ(space);
	}

	public boolean intersectsY(Space space) {
		int startY = space.getY();
		int endY = startY + space.getDepth() - 1;
	
		return intersectsY(startY, endY);
	}

	public boolean intersectsY(int startY, int endY) {
		return intersects(startY, endY, y, depth);
	}

	public boolean intersectsX(Space space) {
		int startX = space.getX();
		int endX = startX + space.getWidth() - 1;
		
		return intersectsX(startX, endX);
	}
	
	public boolean intersectsX(int startX, int endX) {
		return intersects(startX, endX, x, width);
	}

	public boolean intersectsZ(Space space) {
		int startZ = space.getZ();
		int endZ = startZ + space.getHeight() - 1;

		return intersectsZ(startZ, endZ);
	}

	public boolean intersectsZ(int startZ, int endZ) {
		return intersects(startZ, endZ, z, height);
	}

	public boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}
	
	public boolean intersectsY(Placement placement) {
		int startY = placement.getSpace().getY();
		int endY = startY + placement.getBox().getDepth() - 1;
		return intersectsY(startY, endY);
	}
	
	public boolean intersectsX(Placement placement) {
		int startX = placement.getSpace().getX();
		int endX = startX + placement.getBox().getWidth() - 1;
		return intersectsX(startX, endX);
	}

	public boolean intersectsZ(Placement placement) {
		int startZ = placement.getSpace().getZ();
		int endZ = startZ + placement.getBox().getHeight() - 1;
		return intersectsZ(startZ, endZ);
	}

	public void subtractX(Placement placement) {
		int endX = placement.getSpace().getX() + placement.getBox().getWidth();
		
		if(endX > x) {
			width -= endX - x;
			
			x = endX;
			
			calculateVolume();
		}
	}

	public void subtractY(Placement placement) {
		int endY = placement.getSpace().getY() + placement.getBox().getDepth();
		
		if(endY > y) {
			depth -= endY - y;
			
			y = endY;
			
			calculateVolume();
		}
	}
	
	public void subtractZ(Placement placement) {
		int endZ = placement.getSpace().getZ() + placement.getBox().getHeight();
		
		if(endZ > z) {
			height -= endZ - z;
			
			z = endZ;
			
			calculateVolume();
		}
	}

	public Space getParent() {
		return parent;
	}	

}
