package com.github.skjolber.packing.api;

public class StackPlacement implements Placement3D {

	protected Stackable stackable;
	protected StackValue value;
	
	protected int x; // width coordinate
	protected int y; // depth coordinate
	protected int z; // height coordinate
	
	// TODO weight constraint
	protected int maxSupportedPressure; // i.e.  
	protected int maxSupportedWeight;

	public StackPlacement(Stackable stackable, StackValue value, int x, int y, int z, int maxSupportedPressure,
			int maxSupportedWeight) {
		super();
		this.stackable = stackable;
		this.value = value;
		this.x = x;
		this.y = y;
		this.z = z;
		this.maxSupportedPressure = maxSupportedPressure;
		this.maxSupportedWeight = maxSupportedWeight;
	}
	public Stackable getStackable() {
		return stackable;
	}
	public void setStackable(Stackable stackable) {
		this.stackable = stackable;
	}
	public StackValue getStackValue() {
		return value;
	}
	public void setStackValue(StackValue stackValue) {
		this.value = stackValue;
	}
	
	boolean intersects(StackPlacement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(StackPlacement placement) {
		int startY = y;
		int endY = startY + value.getDy() - 1;

		if (startY <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return startY <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 &&
				placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(StackPlacement placement) {
		int startX = x;
		int endX = startX + value.getDx() - 1;

		if (startX <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return startX <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 &&
				placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}
	
	public boolean intersectsZ(StackPlacement placement) {
		int startZ = z;
		int endZ = startZ + value.getDz() - 1;

		if (startZ <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
			return true;
		}

		return startZ <= placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 &&
				placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 <= endZ;
	}

	public int getAbsoluteX() {
		return x;
	}
	
	public int getAbsoluteY() {
		return y;
	}

	public int getAbsoluteZ() {
		return z;
	}

	public int getAbsoluteEndX() {
		return x + value.getDx();
	}
	
	public int getAbsoluteEndY() {
		return y + value.getDy();
	}

	public int getAbsoluteEndZ() {
		return z + value.getDz();
	}
	
	public long getVolume() {
		return stackable.getVolume();
	}
	public boolean intersects(Placement2D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > getAbsoluteEndX() || point.getAbsoluteEndY() < y || point.getAbsoluteY() > getAbsoluteEndY());
	}
	@Override
	public boolean intersects(Placement3D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > getAbsoluteEndX() || point.getAbsoluteEndY() < y || point.getAbsoluteY() > point.getAbsoluteEndY() || point.getAbsoluteEndZ() < z || point.getAbsoluteZ() >  point.getAbsoluteEndZ());
	}	
}
