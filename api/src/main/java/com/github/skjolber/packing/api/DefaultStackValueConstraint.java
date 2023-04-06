package com.github.skjolber.packing.api;

import java.math.BigDecimal;

public class DefaultStackValueConstraint implements StackConstraint {

	// structural integrity for this orientation
	protected final BigDecimal maxSupportedWeight;

	public DefaultStackValueConstraint(BigDecimal maxSupportedWeight) {
		this.maxSupportedWeight = maxSupportedWeight;
	}

	@Override
	public boolean supports(Stack stack, Stackable stackable, StackValue value, BigDecimal x, BigDecimal y, BigDecimal z) {
		return accepts(stack, stackable);
	}

	@Override
	public boolean accepts(Stack stack, Stackable stackable) {
		return stack.getWeight().add(stackable.getWeight()).compareTo(maxSupportedWeight) <= 0;
	}

	@Override
	public boolean canAccept(Stackable stackable) {
		return stackable.getWeight().compareTo(maxSupportedWeight) <= 0;
	}
}
