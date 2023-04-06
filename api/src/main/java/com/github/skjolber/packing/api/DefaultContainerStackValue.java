package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.List;

public class DefaultContainerStackValue extends ContainerStackValue {

	private static final long serialVersionUID = 1L;

	public DefaultContainerStackValue(
			BigDecimal dx, BigDecimal dy, BigDecimal dz,
			StackConstraint constraint,
			BigDecimal loadDx, BigDecimal loadDy, BigDecimal loadDz,
			BigDecimal maxLoadWeight, List<Surface> sides) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, sides);
	}

}
