package com.github.skjolber.packing.api;

public class DefaultStackValueConstraint implements StackConstraint {

	// structural integrity for this orientation
	protected final int maxSupportedWeight;

	public DefaultStackValueConstraint(int maxSupportedWeight) {
		this.maxSupportedWeight = maxSupportedWeight;
	}

	@Override
	public boolean supports(Stack stack, Box stackable, BoxStackValue value, int x, int y, int z) {
		return accepts(stack, stackable);
	}

	@Override
	public boolean accepts(Stack stack, Box stackable) {
		return stack.getWeight() + stackable.getWeight() <= maxSupportedWeight;
	}

	@Override
	public boolean canAccept(Box stackable) {
		return stackable.getWeight() <= maxSupportedWeight;
	}
}
