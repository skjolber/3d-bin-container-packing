package com.github.skjolber.packing.api;

import java.util.Comparator;

@FunctionalInterface
public interface StackValueComparator<T> {

	/**
	 * See {@linkplain Comparator#compare(Object, Object)}.<br>
	 * <br>
	 * Use so that if comparator.compare(point1, stackValue1, candidatePoint1, candidateStackValue1) > 0 then candidate is the best.
	 * 
	 * @param point1
	 * @param stackValue1
	 * @param point2
	 * @param stackValue2
     * @return a negative integer if point1/stackValue1 < point2/stackValue2. A positive integer if point1/stackValue1 > point2/stackValue2. Otherwise zero.
     *         comparator does not permit null arguments
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this comparator.
     */
	
	int compare(T point1, StackValue stackValue1, T point2, StackValue stackValue2);
	
}
