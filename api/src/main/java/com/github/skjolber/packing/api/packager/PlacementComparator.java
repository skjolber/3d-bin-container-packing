package com.github.skjolber.packing.api.packager;

import java.util.Comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.Point3D;

@FunctionalInterface
public interface PlacementComparator {

	/**
	 * See {@linkplain Comparator#compare(Object, Object)}.<br>
	 * <br>
	 * 
	 * @param s1  the first object to be compared.
	 * @param sv1 the first stack value
	 * @param l1  TODO
	 * @param s2  the second object to be compared.
	 * @param sv2 the second stack value
	 * @param l2  TODO
	 * @return a negative integer if o1 less than o2. A positive integer if o1 more than o2. Otherwise zero.
	 * @throws NullPointerException if an argument is null and this
	 *                              comparator does not permit null arguments
	 * @throws ClassCastException   if the arguments' types prevent them from
	 *                              being compared by this comparator.
	 */

	int compare(Box s1, BoxStackValue sv1, Point3D l1, Box s2, BoxStackValue sv2, Point3D l2);

}
