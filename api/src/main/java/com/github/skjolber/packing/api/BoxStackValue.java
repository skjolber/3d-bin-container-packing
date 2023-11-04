package com.github.skjolber.packing.api;

import java.util.List;

public class BoxStackValue extends StackValue {

	private static final long serialVersionUID = 1L;

	public BoxStackValue(int dx, int dy, int dz, StackConstraint constraint, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, surfaces);
	}
	
	public BoxStackValue(BoxStackValue boxStackValue) {
		super(boxStackValue);
	}

	@Override
	public BoxStackValue clone() {
		return new BoxStackValue(this);
	}

}
