package com.github.skjolber.packing.api;

public class BoxStackValue extends StackValue {

	protected final int weight;

	public BoxStackValue(int dx, int dy, int dz, int weight, StackConstraint constraint) {
		super(dx, dy, dz,  constraint);
		
		this.weight = weight;
	}

	public int getWeight() {
		return weight;
	}
}
