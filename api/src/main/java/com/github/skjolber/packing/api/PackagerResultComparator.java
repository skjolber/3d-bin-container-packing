package com.github.skjolber.packing.api;

/**
 * Compares packaging results, for selecting the best of several packagers' results.
 * <p>
 * Convention: {@code compare(a, b) > 0} means {@code a} is the better result.
 */
@FunctionalInterface
public interface PackagerResultComparator {

	int compare(PackagerResult a, PackagerResult b);

	/**
	 * Whether a successful result with more containers never compares better, all else being equal, when the
	 * containers have no costs. When true, a packager can limit a search to the containers of an earlier result.
	 *
	 * @return true if fewer containers are preferred; the default is false, which is always safe
	 */
	default boolean prefersFewerContainers() {
		return false;
	}
}
