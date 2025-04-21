package com.github.skjolber.packing.api;

import java.io.Serializable;

public class StackPlacement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxItemGroup boxItemGroup;
	protected BoxItem boxItem;
	protected BoxStackValue stackValue;

	protected int x; // width coordinate
	protected int y; // depth coordinate
	protected int z; // height coordinate

	public StackPlacement(BoxItemGroup boxItemGroup, BoxItem boxItem, BoxStackValue stackValue, int x, int y, int z) {
		super();
		this.boxItemGroup = boxItemGroup;
		this.boxItem = boxItem;
		this.stackValue = stackValue;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public StackPlacement() {
	}

	public BoxStackValue getStackValue() {
		return stackValue;
	}

	public void setStackValue(BoxStackValue stackValue) {
		this.stackValue = stackValue;
	}

	public boolean intersects(StackPlacement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(StackPlacement placement) {
		int startY = y;
		int endY = startY + stackValue.getDy() - 1;

		if(startY <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return startY <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 &&
				placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(StackPlacement placement) {
		int startX = x;
		int endX = startX + stackValue.getDx() - 1;

		if(startX <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return startX <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 &&
				placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}

	public boolean intersectsZ(StackPlacement placement) {
		int startZ = z;
		int endZ = startZ + stackValue.getDz() - 1;

		if(startZ <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
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
		return x + stackValue.getDx() - 1;
	}

	public int getAbsoluteEndY() {
		return y + stackValue.getDy() - 1;
	}

	public int getAbsoluteEndZ() {
		return z + stackValue.getDz() - 1;
	}

	public long getVolume() {
		return boxItem.getBox().getVolume();
	}

	public boolean intersects2D(StackPlacement point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > getAbsoluteEndX() || point.getAbsoluteEndY() < y || point.getAbsoluteY() > getAbsoluteEndY());
	}

	public boolean intersects3D(StackPlacement point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > getAbsoluteEndX() || point.getAbsoluteEndY() < y || point.getAbsoluteY() > point.getAbsoluteEndY() || point.getAbsoluteEndZ() < z
				|| point.getAbsoluteZ() > point.getAbsoluteEndZ());
	}

	@Override
	public String toString() {
		return (boxItem != null ? boxItem.getBox().getId() : "") + "[" + x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x" + getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
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

	public int getWeight() {
		return boxItem.getBox().getWeight();
	}
	
	public BoxItem getBoxItem() {
		return boxItem;
	}
	
	public BoxItemGroup getBoxItemGroup() {
		return boxItemGroup;
	}

}
