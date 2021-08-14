package com.github.skjolber.packing.api;

public class DefaultContainerStackValue extends ContainerStackValue {

	protected final Stack stack;

	public DefaultContainerStackValue(
			int dx, int dy, int dz, 
			StackConstraint constraint,
			int loadDx, int loadDy, int loadDz, 
			int maxLoadWeight,
			Container container, 
			Stack stack) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, container);
		
		this.stack = stack;
	}

	@Override
	public int getWeight() {
		return container.getEmptyWeight() + stack.getWeight();
	}
	
}
