package com.github.skjolber.packing.api;

import java.util.List;

public class BoxStackValue extends StackValue {

	private static final long serialVersionUID = 1L;
	
	private Box stackable;

	public BoxStackValue(int dx, int dy, int dz, StackValueConstraint constraint, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, surfaces);
	}
	
	public BoxStackValue(BoxStackValue boxStackValue) {
		super(boxStackValue);
		
		this.stackable = boxStackValue.stackable;
	}

	@Override
	public BoxStackValue clone() {
		return new BoxStackValue(this);
	}
	
	public void setStackable(Box stackable) {
		this.stackable = stackable;
	}
	
	@Override
	public Box getStackable() {
		return stackable;
	}

}
