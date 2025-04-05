package com.github.skjolber.packing.api;

public interface StackConstraint {

	boolean canAccept(Box stackable);

	boolean accepts(Stack stack, Box stackable);

	boolean supports(Stack stack, Box stackable, BoxStackValue value, int x, int y, int z);

}
