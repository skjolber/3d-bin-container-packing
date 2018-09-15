package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.Box;

/**
 * Placement as in box in a space.
 *
 * The box does not necessarily fill the whole space.
 */

public class Placement {

	private Space space;
	private Box box;

	public Placement(Space space, Box box) {
		this.space = space;
		this.box = box;
	}

	public Placement(Space space) {
		this.space = space;
	}

	public Space getSpace() {
		return space;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}

	@Override
	public String toString() {
		return "Placement [" + space.x + "x" + space.y + "x" + space.z + ", width=" + box.getWidth() + ", depth=" + box.getDepth() + ", height="
				+ box.getHeight() + "]";
	}

	public int getCenterX() {
		return space.x + (box.getWidth() / 2);
	}

	public int getCenterY() {
		return space.y + (box.getDepth() / 2);
	}

	boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement);
	}

	private boolean intersectsY(Placement placement) {

		int startY = space.y;
		int endY = startY + box.getDepth() - 1;

		if (startY <= placement.getSpace().y && placement.getSpace().y <= endY) {
			return true;
		}

		return startY <= placement.getSpace().y + placement.getBox().getDepth() - 1 &&
				placement.getSpace().y + placement.getBox().getDepth() - 1 <= endY;

	}

	private boolean intersectsX(Placement placement) {

		int startX = space.x;
		int endX = startX + box.getWidth() - 1;

		if (startX <= placement.getSpace().x && placement.getSpace().x <= endX) {
			return true;
		}

		return startX <= placement.getSpace().x + placement.getBox().getWidth() - 1 &&
				placement.getSpace().x + placement.getBox().getWidth() - 1 <= endX;

	}
}
