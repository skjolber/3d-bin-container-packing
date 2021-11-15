package com.github.skjolber.packing.api;

import java.util.Comparator;

@FunctionalInterface
public interface StackValuePointFilter<T> {

	/**
	 * Check whether to let 2 replace 1 as the best.
	 * 
	 * @param stackable1
	 * @param point1
	 * @param stackValue1
	 * @param stackable2
	 * @param point2
	 * @param stackValue2
     * @return true if point2 is superior to point1
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this comparator.
     */

	
	boolean accept(Stackable stackable1, T point1, StackValue stackValue1, Stackable stackable2, T point2, StackValue stackValue2);
	
}
