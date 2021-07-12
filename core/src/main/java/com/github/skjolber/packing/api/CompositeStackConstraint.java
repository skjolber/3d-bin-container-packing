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
	public boolean supports(int weight, long area) {
		for (StackConstraint constraint : constraints) {
			if(!constraint.supports(weight, area)) {
				return false;
			}
		}
		return true;
	}
	
}
