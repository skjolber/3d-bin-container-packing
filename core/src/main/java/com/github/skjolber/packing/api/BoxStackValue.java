package com.github.skjolber.packing.api;

public class BoxStackValue extends StackValue {

	protected final Box box;

	public BoxStackValue(int dx, int dy, int dz, Box box, StackConstraint constraint) {
		super(dx, dy, dz,  constraint);
		
		this.box = box;
	}

	public int getWeight() {
		return box.getWeight();
	}
	
	@Override
	public Stackable getStackable() {
		return box;
	}
}
