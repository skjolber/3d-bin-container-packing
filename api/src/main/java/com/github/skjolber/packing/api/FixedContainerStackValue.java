package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * 
 * Fixed as does not have any container stackables within; weight is constant.
 *
 */

public class FixedContainerStackValue extends ContainerStackValue {

	private static final long serialVersionUID = 1L;

	protected final BigDecimal weight;

	public FixedContainerStackValue(
			BigDecimal dx, BigDecimal dy, BigDecimal dz,
			StackConstraint constraint,
			BigDecimal stackWeight, BigDecimal emptyWeight,
			BigDecimal loadDx, BigDecimal loadDy, BigDecimal loadDz, BigDecimal maxLoadWeight, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, surfaces);

		this.weight = stackWeight.add(emptyWeight);
	}

	public BigDecimal getWeight() {
		return weight;
	}

}
