package com.github.skjolber.packing;

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
		return "Placement [" + box.getName() + " " + space.getX() + "x" + space.getY() + "x" + space.getZ() + ", width=" + box.getWidth() + ", depth=" + box.getDepth() + ", height="
				+ box.getHeight() + "]";
	}

	int getCenterX() {
		return space.getX() + (box.getWidth() / 2);
	}

	int getCenterY() {
		return space.getY() + (box.getDepth() / 2);
	}

	int getCenterZ() {
		return space.getZ() + (box.getHeight() / 2);
	}

	boolean intersects(Placement placement) {

		// direction -->
		//
		//              |------------------|
		//              |      current     |
		//              |------------------|
		//
		// |------------------|
		// |      left        |
		// |------------------|
		//
		//                           |------------------|
		//                           |      right       |
		//                           |------------------|
		//
		//        |-------------------------------|
		//        |            outside            |
		//        |------------------------------ |
		//
		//                  |----------|
		//                  |  within  |
		//                  |----------|
		
		
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(Placement placement) {
		int startY = space.getY();
		int endY = startY + box.getDepth() - 1;

		if (startY <= placement.getSpace().getY() && placement.getSpace().getY() <= endY) {
			return true;
		}

		int placementEndY = placement.getSpace().getY() + placement.getBox().getDepth() - 1;
		
		if(startY <= placementEndY &&
				placementEndY <= endY) {
			return true;
		}

		return placement.getSpace().getY() < startY && endY < placementEndY;
		
	}

	public boolean intersectsX(Placement placement) {

		int startX = space.getX();
		int endX = startX + box.getWidth() - 1;

		if (startX <= placement.getSpace().getX() && placement.getSpace().getX() <= endX) {
			return true;
		}

		int placementEndX = placement.getSpace().getX() + placement.getBox().getWidth() - 1;
		if(startX <= placementEndX && placementEndX <= endX) {
			return true;
		}

		return placement.getSpace().getX() < startX && endX < placementEndX;
	}
	
	public boolean intersectsZ(Placement placement) {

		int startZ = space.getZ();
		int endZ = startZ + box.getHeight() - 1;

		if (startZ <= placement.getSpace().getZ() && placement.getSpace().getZ() <= endZ) {
			return true;
		}

		int placementEndZ = placement.getSpace().getZ() + placement.getBox().getHeight() - 1;
		if(startZ <= placementEndZ &&
				placementEndZ <= endZ) {
			return true;
		}
		
		return placement.getSpace().getZ() < startZ && endZ < placementEndZ;
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
		return space.getX() + box.getWidth();
	}
	
	public int getAbsoluteEndY() {
		return space.getY() + box.getDepth();
	}

	public int getAbsoluteEndZ() {
		return space.getZ() + box.getHeight();
	}

}
