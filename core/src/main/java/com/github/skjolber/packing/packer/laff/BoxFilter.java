package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Box;

@FunctionalInterface
public interface BoxFilter {

	/**
	 * @param s1 stackable 1
	 * @param s2 stackable 2
	 * @return true if 2 is superior to 1
	 */

	boolean filter(Box s1, Box s2);

}
