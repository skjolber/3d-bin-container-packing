package com.github.skjolber.packing.api;

public class DefaultStackScopeConstraintBuilder extends AbstractStackScopeConstraintBuilder<DefaultStackScopeConstraintBuilder> {

	@Override
	public StackScopeConstraint build() {
		return new DefaultStackScopeConstraint(stackables, container, containerStackValue, stack);
	}

}
