package com.github.skjolber.packing.api;

@FunctionalInterface
public interface StackableFilter {

	/**
	 * @param s1
	 * @param s2
	 * @return true if 2 is superior to 1
	 */
	
	boolean filter(Stackable s1, Stackable s2);
	
}
