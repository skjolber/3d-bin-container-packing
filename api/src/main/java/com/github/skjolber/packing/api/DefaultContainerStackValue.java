package com.github.skjolber.packing.api;

import java.util.List;

public class DefaultContainerStackValue extends ContainerStackValue {

	private static final long serialVersionUID = 1L;
	
	public DefaultContainerStackValue(
			int dx, int dy, int dz,
			StackConstraint constraint,
			int loadDx, int loadDy, int loadDz,
			int maxLoadWeight, List<Surface> sides) {
		super(dx, dy, dz, constraint, loadDx, loadDy, loadDz, maxLoadWeight, sides);
	}

	protected DefaultContainerStackValue(DefaultContainerStackValue other) {
		super(other);
	}

	@Override
	public DefaultContainerStackValue clone() {
		return new DefaultContainerStackValue(this);
	}

}
