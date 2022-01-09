package com.github.skjolber.packing.api;

public interface StackConstraint {

	boolean canAccept(Stackable stackable);

	boolean accepts(Stack stack, Stackable stackable);
	
	boolean supports(Stack stack, Stackable stackable, StackValue value, int x, int y, int z);
	
}
