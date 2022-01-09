package com.github.skjolber.packing.api;

public class DefaultStackValueConstraint implements StackConstraint {

	// structural integrity for this orientation
	protected final int maxSupportedWeight;
	
	public DefaultStackValueConstraint(int maxSupportedWeight) {
		this.maxSupportedWeight = maxSupportedWeight;
	}

	@Override
	public boolean supports(Stack stack, Stackable stackable, StackValue value, int x, int y, int z) {
		return accepts(stack, stackable);
	}
	
	@Override
	public boolean accepts(Stack stack, Stackable stackable) {
		return stack.getWeight() + stackable.getWeight() <= maxSupportedWeight;
	}

	@Override
	public boolean canAccept(Stackable stackable) {
		return stackable.getWeight() <= maxSupportedWeight;
	}
}
