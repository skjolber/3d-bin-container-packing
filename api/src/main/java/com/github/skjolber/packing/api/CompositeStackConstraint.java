package com.github.skjolber.packing.api;

import java.util.List;

public class CompositeStackConstraint implements StackConstraint {

	// structural integrity for this orientation
	protected final StackConstraint[] constraints;

	public CompositeStackConstraint(List<StackConstraint> constraints) {
		this.constraints = constraints.toArray(new StackConstraint[constraints.size()]);
	}

	public CompositeStackConstraint(StackConstraint[] constraints) {
		this.constraints = constraints;
	}

	@Override
	public boolean supports(Stack stack, Stackable stackable, StackValue value, int x, int y, int z) {
		for (StackConstraint constraint : constraints) {
			if(!constraint.supports(stack, stackable, value, x, y, z)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean accepts(Stack stack, Stackable stackable) {
		for (StackConstraint constraint : constraints) {
			if(!constraint.accepts(stack, stackable)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canAccept(Stackable stackable) {
		for (StackConstraint constraint : constraints) {
			if(!constraint.canAccept(stackable)) {
				return false;
			}
		}
		return true;
	}

}
