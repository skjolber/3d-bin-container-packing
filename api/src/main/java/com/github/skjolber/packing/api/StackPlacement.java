package com.github.skjolber.packing.api;

import java.util.List;

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
	
	public StackPlacement() {
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
	
	public boolean intersects(StackPlacement placement) {
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
		return x + value.getDx() - 1;
	}
	
	public int getAbsoluteEndY() {
		return y + value.getDy() - 1;
	}

	public int getAbsoluteEndZ() {
		return z + value.getDz() - 1;
	}
	
	public long getVolume() {
		return stackable.getVolume();
	}
	public boolean intersects2D(Placement2D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > getAbsoluteEndX() || point.getAbsoluteEndY() < y || point.getAbsoluteY() > getAbsoluteEndY());
	}
	@Override
	public boolean intersects3D(Placement3D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > getAbsoluteEndX() || point.getAbsoluteEndY() < y || point.getAbsoluteY() > point.getAbsoluteEndY() || point.getAbsoluteEndZ() < z || point.getAbsoluteZ() >  point.getAbsoluteEndZ());
	}
	@Override
	public String toString() {
		return stackable.getDescription() + "[" + x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x" + getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setZ(int z) {
		this.z = z;
	}
	
	public void setValue(StackValue value) {
		this.value = value;
	}
	public void setMaxSupportedPressure(int maxSupportedPressure) {
		this.maxSupportedPressure = maxSupportedPressure;
	}
	public void setMaxSupportedWeight(int maxSupportedWeight) {
		this.maxSupportedWeight = maxSupportedWeight;
	}
	
	@Override
	public StackPlacement clone() {
		return new StackPlacement(stackable, value, x, y, z,  maxSupportedPressure, maxSupportedWeight);
	}

	public int getWeight() {
		return stackable.getWeight();
	}
	
}
