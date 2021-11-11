package com.github.skjolber.packing.api;

import java.util.Comparator;

@FunctionalInterface
public interface StackableComparator {

	/**
	 * See {@linkplain Comparator#compare(Object, Object)}.<br>
	 * <br>
	 * Use so that if comparator.compare(currentBest, candidate) > 0 then currentBest = candidate.
	 * 
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer if o1 < o2. A positive integer if o1 > o2. Otherwise zero.
     * @throws NullPointerException if an argument is null and this
     *         comparator does not permit null arguments
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this comparator.
     */
	
	int compare(Stackable s1, Stackable s2);
	
}
