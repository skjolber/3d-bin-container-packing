package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;

/**
 * Compares box items, for example to choose which box to place next.
 */
@FunctionalInterface
public interface BoxItemComparator {

	/**
	 * @param reference a box item
	 * @param candidate another box item
	 * @return a negative number if the candidate is better (placed first), zero if they are equally good, a positive
	 *         number if the reference is better
	 */
	int compare(BoxItem reference, BoxItem candidate);
}
