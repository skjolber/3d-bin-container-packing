package com.github.skjolber.packing.api;

import java.math.BigDecimal;

public interface StackConstraint {

	boolean canAccept(Stackable stackable);

	boolean accepts(Stack stack, Stackable stackable);

	boolean supports(Stack stack, Stackable stackable, StackValue value, BigDecimal x, BigDecimal y, BigDecimal z);

}
