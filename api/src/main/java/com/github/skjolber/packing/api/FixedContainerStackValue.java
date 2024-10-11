package com.github.skjolber.packing.api;

import java.util.List;

/**
 * 
 * Fixed as does not have any container stackables within; weight is constant.
 *
 */

public class FixedContainerStackValue extends ContainerStackValue {

	private static final long serialVersionUID = 1L;

	protected final int weight;

	public FixedContainerStackValue(
			int dx, int dy, int dz,
			StackValueConstraint constraint,
			int stackWeight, int emptyWeight,
			int loadDx, int loadDy, int loadDz, int maxLoadWeight, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, surfaces);

		this.weight = stackWeight + emptyWeight;
	}

	protected FixedContainerStackValue(FixedContainerStackValue other) {
		super(other);
		
		weight = other.weight;
	}

	public int getWeight() {
		return weight;
	}

	@Override
	public FixedContainerStackValue clone() {
		return new FixedContainerStackValue(this);
	}


}
