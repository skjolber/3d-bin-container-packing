package com.github.skjolber.packing.api;

/**
 * Compares packaging results, for selecting the best of several packagers' results.
 * <p>
 * Convention: {@code compare(a, b) > 0} means {@code a} is the better result.
 */
@FunctionalInterface
public interface PackagerResultComparator {

	int compare(PackagerResult a, PackagerResult b);
}
