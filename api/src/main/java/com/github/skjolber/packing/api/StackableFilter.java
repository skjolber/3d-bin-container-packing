package com.github.skjolber.packing.api;

@FunctionalInterface
public interface StackableFilter {

	/**
	 * @param s1 stackable 1
	 * @param s2 stackable 2
	 * @return true if 2 is superior to 1
	 */

	boolean filter(Stackable s1, Stackable s2);

}
