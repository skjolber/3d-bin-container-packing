package com.github.skjolber.packing.api;

@FunctionalInterface
public interface StackableFilter {

	boolean accepts(Stackable stackale);
	
}
