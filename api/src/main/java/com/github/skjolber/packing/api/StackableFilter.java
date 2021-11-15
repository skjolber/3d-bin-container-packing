package com.github.skjolber.packing.api;

@FunctionalInterface
public interface StackableFilter {

	boolean accept(Stackable s1, Stackable s2);
	
}
