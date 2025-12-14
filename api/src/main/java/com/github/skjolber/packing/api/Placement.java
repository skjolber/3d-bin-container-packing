package com.github.skjolber.packing.api;

import java.io.Serializable;

import com.github.skjolber.packing.api.point.Point;

public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxStackValue stackValue;
	protected Point point;
	
	public Placement(BoxStackValue stackValue, Point point) {
		super();
		this.stackValue = stackValue;
		this.point = point;
	}

	public Placement() {
	}

	public BoxStackValue getStackValue() {
		return stackValue;
	}

	public void setStackValue(BoxStackValue stackValue) {
		this.stackValue = stackValue;
	}

	public boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(Placement placement) {
		int startY = point.getMinY();
		int endY = startY + stackValue.getDy() - 1;

		if (startY <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return startY <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1
				&& placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(Placement placement) {
		int startX = point.getMinX();
		int endX = startX + stackValue.getDx() - 1;

		if (startX <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return startX <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1
				&& placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}

	public boolean intersectsZ(Placement placement) {
		int startZ = point.getMinZ();
		int endZ = startZ + stackValue.getDz() - 1;

		if (startZ <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
			return true;
		}

		return startZ <= placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1
				&& placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 <= endZ;
	}

	public int getAbsoluteX() {
		return point.getMinX();
	}

	public int getAbsoluteY() {
		return point.getMinY();
	}

	public int getAbsoluteZ() {
		return point.getMinZ();
	}

	public int getAbsoluteEndX() {
		return point.getMinX() + stackValue.getDx() - 1;
	}

	public int getAbsoluteEndY() {
		return point.getMinY() + stackValue.getDy() - 1;
	}

	public int getAbsoluteEndZ() {
		return point.getMinZ() + stackValue.getDz() - 1;
	}

	public long getVolume() {
		return stackValue.getBox().getVolume();
	}

	public boolean intersects2D(Placement placement) {
		return !(placement.getAbsoluteEndX() < point.getMinX() || placement.getAbsoluteX() > getAbsoluteEndX() || placement.getAbsoluteEndY() < point.getMinY()
				|| placement.getAbsoluteY() > getAbsoluteEndY());
	}

	public boolean intersects3D(Placement placement) {
		return !(placement.getAbsoluteEndX() < point.getMinX() || placement.getAbsoluteX() > getAbsoluteEndX() || placement.getAbsoluteEndY() < point.getMinY()
				|| placement.getAbsoluteY() > placement.getAbsoluteEndY() || placement.getAbsoluteEndZ() < point.getMinZ()
				|| placement.getAbsoluteZ() > placement.getAbsoluteEndZ());
	}

	@Override
	public String toString() {
		return (stackValue.getBox().getId()) + "[" + point.getMinX() + "x" + point.getMinY() + "x" + point.getMinZ() + " " + getAbsoluteEndX() + "x"
				+ getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}

	public void setPoint(Point point) {
		this.point = point;
	}
	
	public int getWeight() {
		return stackValue.getBox().getWeight();
	}

	public BoxItem getBoxItem() {
		return stackValue.getBox().getBoxItem();
	}
	
	public Box getBox() {
		return stackValue.getBox();
	}
	
	public Point getPoint() {
		return point;
	}

}
