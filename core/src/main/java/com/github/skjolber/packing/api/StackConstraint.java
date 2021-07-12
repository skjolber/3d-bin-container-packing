package com.github.skjolber.packing.api;

public interface StackConstraint {

	boolean supports(int weight, long area);
	
}
