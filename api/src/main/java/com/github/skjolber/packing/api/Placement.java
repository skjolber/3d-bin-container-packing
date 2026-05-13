package com.github.skjolber.packing.api;

import java.io.Serializable;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.support.Support;

public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxStackValue stackValue;
	protected int x;
	protected int y;
	protected int z;
	protected Support support;
	
	protected int pointIndex;

	public Placement(BoxStackValue stackValue, int index, int x, int y, int z) {
		super();
		this.stackValue = stackValue;
		this.pointIndex = index;
		
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Placement(BoxStackValue stackValue, Point point) {
		this(stackValue, point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
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
		int endY = y + stackValue.getDy() - 1;

		if (y <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return y <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1
				&& placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(Placement placement) {
		int endX = x + stackValue.getDx() - 1;

		if (x <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return x <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1
				&& placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}

	public boolean intersectsZ(Placement placement) {
		int endZ = z + stackValue.getDz() - 1;

		if (z <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
			return true;
		}

		return z <= placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1
				&& placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 <= endZ;
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
		return stackValue.getBox().getVolume();
	}

	public boolean intersects2D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x || placement.getAbsoluteX() > getAbsoluteEndX() || 
				placement.getAbsoluteEndY() < y || placement.getAbsoluteY() > getAbsoluteEndY()
				);
	}

	public boolean intersects3D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x ||
				placement.getAbsoluteX() > getAbsoluteEndX() ||
				placement.getAbsoluteEndY() < y ||
				placement.getAbsoluteY() > getAbsoluteEndY() ||
				placement.getAbsoluteEndZ() < z ||
				placement.getAbsoluteZ() > getAbsoluteEndZ()
				);
	}

	@Override
	public String toString() {		
		Box box = stackValue.getBox();
		return (box != null ? box.getId() : "") + "[" +x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x"
				+ getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}
	
	public void setPoint(int index, int x, int y, int z) {
		this.pointIndex = index;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setPoint(Point point) {
		setPoint(point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
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

	public int getPointIndex() {
		return pointIndex;
	}
	
	public void setSupport(Support support) {
		this.support = support;
	}
	
	public Support getSupport() {
		return support;
	}

	public boolean hasSupport() {
		return support != null;
	}
}
