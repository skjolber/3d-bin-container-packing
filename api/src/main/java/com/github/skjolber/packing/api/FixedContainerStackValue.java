package com.github.skjolber.packing.api;

import java.util.List;

/**
 * 
 * Fixed as does not have any container stackables within.
 *
 */


public class FixedContainerStackValue extends ContainerStackValue {

	protected final int weight;

	public FixedContainerStackValue(
			int dx, int dy, int dz, 
			StackConstraint constraint,
			int stackWeight, int emptyWeight,
			int loadDx, int loadDy, int loadDz, int maxLoadWeight, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, surfaces);
					
		this.weight = stackWeight + emptyWeight;
	}
	
	public int getWeight() {
		return weight;
	}
	
}
