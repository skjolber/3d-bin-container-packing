package com.github.skjolber.packing.api.ep;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

@FunctionalInterface
public interface StackValuePointFilter<T> {

	/**
	 * Check whether to let 2 replace 1 as the best.
	 * 
	 * @param stackable1 first stackable
	 * @param point1 first point
	 * @param stackValue1 first stack value
	 * @param stackable2 second stackable
	 * @param point2 second point
	 * @param stackValue2 second stack value
     * @return true if point2 is superior to point1
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this filter.
     */

	
	boolean accept(Stackable stackable1, T point1, StackValue stackValue1, Stackable stackable2, T point2, StackValue stackValue2);
	
}
