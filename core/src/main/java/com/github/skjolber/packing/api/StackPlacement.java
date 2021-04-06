package com.github.skjolber.packing.api;

public class StackPlacement<T extends StackSpace> {

	protected Stackable stackable;
	protected StackValue value;
	protected T space;
	
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
	public T getSpace() {
		return space;
	}
	public void setSpace(T space) {
		this.space = space;
	}

	boolean intersects(StackPlacement<?> placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(StackPlacement<?> placement) {
		int startY = space.getY();
		int endY = startY + value.getDy() - 1;

		if (startY <= placement.getSpace().getY() && placement.getSpace().getY() <= endY) {
			return true;
		}

		return startY <= placement.getSpace().getY() + placement.getStackValue().getDy() - 1 &&
				placement.getSpace().getY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(StackPlacement<?> placement) {
		int startX = space.getX();
		int endX = startX + value.getDx() - 1;

		if (startX <= placement.getSpace().getX() && placement.getSpace().getX() <= endX) {
			return true;
		}

		return startX <= placement.getSpace().getX() + placement.getStackValue().getDx() - 1 &&
				placement.getSpace().getX() + placement.getStackValue().getDx() - 1 <= endX;
	}
	
	public boolean intersectsZ(StackPlacement<?> placement) {
		int startZ = space.getZ();
		int endZ = startZ + value.getDz() - 1;

		if (startZ <= placement.getSpace().getZ() && placement.getSpace().getZ() <= endZ) {
			return true;
		}

		return startZ <= placement.getSpace().getZ() + placement.getStackValue().getDz() - 1 &&
				placement.getSpace().getZ() + placement.getStackValue().getDz() - 1 <= endZ;
	}

	public int getAbsoluteX() {
		return space.getX();
	}
	
	public int getAbsoluteY() {
		return space.getY();
	}

	public int getAbsoluteZ() {
		return space.getZ();
	}

	public int getAbsoluteEndX() {
		return space.getX() + value.getDx();
	}
	
	public int getAbsoluteEndY() {
		return space.getY() + value.getDy();
	}

	public int getAbsoluteEndZ() {
		return space.getZ() + value.getDz();
	}
	
	public long getVolume() {
		return stackable.getVolume();
	}
}
