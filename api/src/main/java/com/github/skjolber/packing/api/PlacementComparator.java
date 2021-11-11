package com.github.skjolber.packing.api;

import java.util.Comparator;

@FunctionalInterface
public interface PlacementComparator<L> {

	/**
	 * See {@linkplain Comparator#compare(Object, Object)}.<br>
	 * <br>
	 * 
     * @param s1 the first object to be compared.
	 * @param sv1
	 * @param l1
     * @param s2 the second object to be compared.
	 * @param sv2
	 * @param l2
     * @return a negative integer if o1 < o2. A positive integer if o1 > o2. Otherwise zero.
     * @throws NullPointerException if an argument is null and this
     *         comparator does not permit null arguments
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this comparator.
     */
	
	int compare(Stackable s1, StackValue sv1, L l1, Stackable s2, StackValue sv2, L l2);
	
}
