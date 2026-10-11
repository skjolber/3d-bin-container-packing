package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * Compares box item groups, for example to choose which group to place next.
 * <p>
 * Convention: {@code compare(a, b) > 0} means {@code a} is the better box item group (placed first).
 * <p>
 * Packagers run concurrently, for example with a parallel container packing strategy or a parallel brute-force packager:
 * implementations must be safe for concurrent use; stateless implementations are.
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
