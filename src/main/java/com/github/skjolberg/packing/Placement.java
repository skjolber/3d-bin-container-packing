package com.github.skjolberg.packing;

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
	public Space getSpace() {
		return space;
	}
	public void setSpace(Space space) {
		this.space = space;
	}
	public Box getBox() {
		return box;
	}
	public void setBox(Box box) {
		this.box = box;
	}
	
	@Override
	public String toString() {
		return "Placement [" + space.getX() + "x" + space.getY() + "x" + space.getZ() + ", width=" + box.getWidth() + ", depth=" + box.getDepth()+ ", height="
				+ box.getHeight()+ "]";
	}
	
	public int getCenterX() {
		return space.getX() + (box.getWidth() / 2);
	}
	
	public int getCenterY() {
		return space.getY() + (box.getDepth() / 2);
	}
	
}
