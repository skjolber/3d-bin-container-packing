package com.github.skjolber.packing.api;

import java.util.List;

public class BoxStackValue extends StackValue {

	public BoxStackValue(int dx, int dy, int dz, StackConstraint constraint, List<Surface> sides) {
		super(dx, dy, dz, constraint, sides);
	}

}
