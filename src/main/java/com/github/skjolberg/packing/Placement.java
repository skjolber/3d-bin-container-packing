package com.github.skjolberg.packing;

/**
 * Placement as in box in a space. 
 * 
 * The box does not necessarily fill the whole space. 
 */

public class Placement {

	private Space space;
	private Box box;
	
	public Placement(final Space space, final Box box) {
		this.space = space;
		this.box = box;
	}
	
	public Placement(final Space space) {
		this.space = space;
	}
	
	public Space getSpace() {
		return space;
	}
	public void setSpace(final Space space) {
		this.space = space;
	}
	public Box getBox() {
		return box;
	}
	public void setBox(final Box box) {
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
	
	public boolean intercets(final Placement placement) {
		return intercetsX(placement) && intercetsY(placement);
	}
	
	public boolean intercetsY(final Placement placement) {
		
		final int startY = space.getY();
		final int endY = startY + box.getDepth() - 1;

		if(startY <= placement.getSpace().getY() && placement.getSpace().getY() <= endY) {
			return true;
		}
		
		if(startY <= placement.getSpace().getY() + placement.getBox().getDepth() - 1 && placement.getSpace().getY() + placement.getBox().getDepth() - 1 <= endY) {
			return true;
		}

		return false;
	}
	
	public boolean intercetsX(final Placement placement) {
		
		final int startX = space.getX();
		final int endX = startX + box.getWidth() - 1;

		if(startX <= placement.getSpace().getX() && placement.getSpace().getX() <= endX) {
			return true;
		}
		
		if(startX <= placement.getSpace().getX() + placement.getBox().getWidth() - 1 && placement.getSpace().getX() + placement.getBox().getWidth() - 1  <= endX) {
			return true;
		}
		
		return false;
	}
}
