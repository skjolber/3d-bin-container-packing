package com.github.skjolber.packing.api;

public class DefaultContainerStackValue extends ContainerStackValue {

	public DefaultContainerStackValue(
			int dx, int dy, int dz, 
			StackConstraint constraint,
			int loadDx, int loadDy, int loadDz, 
			int maxLoadWeight) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight);
	}
	
	
}
