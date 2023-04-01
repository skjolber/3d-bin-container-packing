package com.github.skjolber.packing.api;

public interface StackConstraint {

	boolean supports(Stack stack, int weight, long area);
	
}
