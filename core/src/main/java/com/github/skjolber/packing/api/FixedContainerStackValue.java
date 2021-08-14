package com.github.skjolber.packing.api;

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
			int stackWeight,
			int loadDx, int loadDy, int loadDz, int maxLoadWeight, Container container) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, container);
					
		this.weight = stackWeight + container.getEmptyWeight();
	}
	
	public int getWeight() {
		return weight;
	}
	
}
