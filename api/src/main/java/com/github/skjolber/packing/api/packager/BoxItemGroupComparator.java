package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * Compares box item groups, for example to choose which group to place next.
 */
@FunctionalInterface
public interface BoxItemGroupComparator {

	/**
	 * @param reference a box item group
	 * @param candidate another box item group
	 * @return a negative number if the candidate is better (placed first), zero if they are equally good, a positive
	 *         number if the reference is better
	 */
	int compare(BoxItemGroup reference, BoxItemGroup candidate);
}
