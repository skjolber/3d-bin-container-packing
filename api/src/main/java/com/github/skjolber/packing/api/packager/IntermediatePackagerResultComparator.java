package com.github.skjolber.packing.api.packager;

/**
 * Compares the results of packing a container, to choose the best.
 * <p>
 * Convention: {@code compare(a, b) > 0} means {@code a} is the better result.
 * <br>
 * <br>
 * Compare the results by their load volume, load weight and box count ({@link IntermediatePackagerResult#getLoadVolume()},
 * {@link IntermediatePackagerResult#getLoadWeight()} and {@link IntermediatePackagerResult#getBoxCount()}) and their
 * container, not by their stacks: the brute-force packagers compare results whose stacks are not built (they share reused
 * placements until a result is accepted), so the stacks of the compared results may show the same placements.
 * <p>
 * Packagers run concurrently, for example with a parallel container packing strategy or a parallel brute-force packager:
 * implementations must be safe for concurrent use; stateless implementations are.
 */
@FunctionalInterface
public interface IntermediatePackagerResultComparator {

	static final int ARGUMENT_1_IS_BETTER = 1;
	static final int ARGUMENT_2_IS_BETTER = -1;

	/**
	 * 
	 * Returns {@link #ARGUMENT_2_IS_BETTER} if o1 is less / worse than o2.
	 * Returns {@link #ARGUMENT_1_IS_BETTER} if o1 is more / better than o2.
	 * 
	 * Return 0 otherwise.
	 */

	int compare(IntermediatePackagerResult o1, IntermediatePackagerResult o2);

	/**
	 * Whether a result with less load volume ({@link IntermediatePackagerResult#getLoadVolume()}) always
	 * compares worse. When true, packagers can skip searches which cannot load more than the best result
	 * so far.
	 *
	 * @return true if load volume is compared first; the default is false, which is always safe
	 */
	default boolean prefersHigherLoadVolume() {
		return false;
	}

}
