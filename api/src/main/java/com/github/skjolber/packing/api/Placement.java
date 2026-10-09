package com.github.skjolber.packing.api;

import java.io.Serializable;

import com.github.skjolber.packing.api.point.Point;

public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxStackValue stackValue;
	protected int x;
	protected int y;
	protected int z;
	
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
		return y <= placement.getAbsoluteEndY() && placement.getAbsoluteY() <= getAbsoluteEndY();
	}

	public boolean intersectsX(Placement placement) {
		return x <= placement.getAbsoluteEndX() && placement.getAbsoluteX() <= getAbsoluteEndX();
	}

	public boolean intersectsZ(Placement placement) {
		return z <= placement.getAbsoluteEndZ() && placement.getAbsoluteZ() <= getAbsoluteEndZ();
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

}
