package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.List;

public class BoxStackValue extends StackValue {

	private static final long serialVersionUID = 1L;

	public BoxStackValue(BigDecimal dx, BigDecimal dy, BigDecimal dz, StackConstraint constraint, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, surfaces);
	}

}
