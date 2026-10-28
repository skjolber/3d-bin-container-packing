package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;

/**
 * Runs the search of the fast brute force packager for the current permutation of an iterator, so that other packages can compare to it (the
 * search methods are not public).
 */
public final class FlatSearchOracle {

	/** the best arrangement of a permutation: the number of boxes, their load volume and the rotation of each */
	public static final class Search {

		public int count;
		public long loadVolume;
		public int[] rotations;
	}

	private final FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();

	private Container cachedContainer;
	private int cachedLength = -1;
	private Container holder;
	private FastPointCalculator3DStack calculator;
	private Placement[] placements;
	private AbstractBruteForcePackager.PrefixBest prefix;

	/**
	 * The search state is reused between searches of the same container and length, as the packager does.
	 *
	 * @param maxPackableCount the number of leading boxes which may be placed
	 */
	public Search search(BoxItemPermutationRotationIterator iterator, Container container, int maxPackableCount) {
		int n = iterator.length();
		if(container != cachedContainer || n != cachedLength) {
			cachedContainer = container;
			cachedLength = n;
			holder = container.copy(n);
			calculator = new FastPointCalculator3DStack(n + 1);
			placements = AbstractBruteForcePackager.getPlacements(n);
			prefix = new AbstractBruteForcePackager.PrefixBest(n);
		}
		calculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		prefix.reset();
		try {
			packager.searchOrder(calculator, placements, iterator, holder.getStack(), holder, () -> false, null, FastBruteForcePackager.DEFAULT_POINT_COMPARATOR, false,
					AbstractBruteForcePackager.getInsertAfterCounts(iterator), maxPackableCount, null, Integer.MAX_VALUE, null, prefix);
		} catch (PackagerInterruptedException e) {
			throw new IllegalStateException(e);
		}

		Search search = new Search();
		search.count = prefix.count;
		search.loadVolume = prefix.loadVolume;
		search.rotations = java.util.Arrays.copyOf(prefix.rotations, prefix.count);
		return search;
	}

	/** @return the number of leading boxes of the permutation which fit the container by volume and weight, as the packager limits its search */
	public static int maxPackableCount(BoxItemPermutationRotationIterator iterator, Container container) {
		return AbstractBruteForcePackager.getMaxPackableCount(iterator, container.getMaxLoadVolume(), container.getMaxLoadWeight());
	}

	/** @return the index of the best free point of the calculator for the stack value, by the default point comparator, or -1 */
	public int bestPoint(FastPointCalculator3DStack calculator, BoxStackValue stackValue) {
		return packager.getBestPoint(calculator, null, stackValue, null, 0, null, false, false, FastBruteForcePackager.DEFAULT_POINT_COMPARATOR);
	}
}
