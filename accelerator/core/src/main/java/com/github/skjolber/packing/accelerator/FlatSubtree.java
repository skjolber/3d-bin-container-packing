package com.github.skjolber.packing.accelerator;

/**
 * The work of one item: all permutations which start with a prefix, searched in the order and with the skipping of the permutation loop of {@code
 * FastBruteForcePackager.pack(..)}, keeping the best arrangement. The method is static and uses primitive arrays only, so that many items can run at once.
 * <p>
 * An item can pause after a number of nodes and be run again to continue, so that no item keeps a device busy for long and the interrupt can be polled
 * between the runs.
 * <p>
 * The state of an item is in an output array of ints ({@link #outInts(int)}) and longs ({@link #OUT_LONGS}), as laid out by the constants below.
 */
public final class FlatSubtree {

	/** the item has not been run */
	public static final int NOT_STARTED = 0;
	/** all permutations of the item have been searched, or one of them places all boxes */
	public static final int DONE = 1;
	/** the item stopped after its node budget, and can be run again */
	public static final int PAUSED = 2;
	/** the search needed more free points than the layout holds */
	public static final int OVERFLOW = 3;

	public static final int OUT_STATUS = 0;
	/** the number of boxes placed by the best arrangement */
	public static final int OUT_COUNT = 1;
	/** 1 if the best arrangement places all boxes */
	public static final int OUT_FULL = 2;
	/** the next permutation to search (n values), starting at this index */
	public static final int OUT_CURSOR = 3;

	/** the load volume of the best arrangement */
	public static final int OUT_VOLUME = 0;
	/** the load weight of the best arrangement */
	public static final int OUT_WEIGHT = 1;
	/** the number of nodes searched */
	public static final int OUT_NODES = 2;
	public static final int OUT_LONGS = 3;

	private FlatSubtree() {
	}

	/** @return the number of ints of the output of an item: the header, the next permutation, the permutation of the best arrangement and its rotations */
	public static int outInts(int n) {
		return OUT_CURSOR + 3 * n;
	}

	/** @return the offset of the permutation of the best arrangement, relative to the item's output */
	public static int outBestPermutation(int n) {
		return OUT_CURSOR + n;
	}

	/** @return the offset of the rotations of the best arrangement, relative to the item's output */
	public static int outBestRotations(int n) {
		return OUT_CURSOR + 2 * n;
	}

	/**
	 * Search the permutations of an item, from its first or from where it paused.
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param problemLongs the longs of the {@linkplain FlatProblem}
	 * @param depth the length of the prefixes
	 * @param prefixes the first permutation of each item, see {@link FlatPermutation#prefixes(int[], int, int)}
	 * @param item the item
	 * @param s the scratch of the work item
	 * @param sl the long scratch of the work item
	 * @param base the offset of the work item in the ints of the scratch
	 * @param longBase the offset of the work item in the longs of the scratch
	 * @param out the output ints, with the invariants of a zeroed array for an item which has not run
	 * @param outBase the offset of the item in the output ints
	 * @param outLongs the output longs
	 * @param outLongBase the offset of the item in the output longs
	 * @param boundVolume a load volume which has been reached (by another item, or by an earlier container): a permutation which cannot load more is
	 *        not searched
	 * @param budgetNodes the number of nodes after which to pause
	 * @return the status of the item
	 */
	public static int run(FlatLayout layout, int[] problem, long[] problemLongs, int depth, int[] prefixes, int item, int[] s, long[] sl, int base, int longBase,
			int[] out, int outBase, long[] outLongs, int outLongBase, long boundVolume, int budgetNodes) {
		final int n = problem[FlatProblem.N];
		final int types = problem[FlatProblem.TYPES];
		final int limit = problem[FlatProblem.LIMIT];
		final boolean allFit = problem[FlatProblem.ALL_FIT] != 0;
		final long maxLoadVolume = problemLongs[FlatProblem.maxLoad(types)];
		final long maxLoadWeight = problemLongs[FlatProblem.maxLoad(types) + 1];
		final int weights = FlatProblem.weights(types);

		final int cursor = outBase + OUT_CURSOR;
		final int bestPermutation = outBase + outBestPermutation(n);
		final int bestRotations = outBase + outBestRotations(n);
		final int permBase = base + layout.perm;
		final int control = base + layout.control;

		int status = out[outBase + OUT_STATUS];
		// one exit and no return in a loop: the OpenCL backend does not lower them correctly
		int result = status;
		boolean running = status != DONE && status != OVERFLOW;
		if(running && status == NOT_STARTED) {
			for (int i = 0; i < n; i++) {
				out[cursor + i] = prefixes[item * n + i];
			}
			out[outBase + OUT_COUNT] = 0;
			outLongs[outLongBase + OUT_VOLUME] = 0L;
			outLongs[outLongBase + OUT_WEIGHT] = 0L;
		}

		int nodes = 0;
		while (running) {
			// the volume which the first boxes of the permutation can load at most
			int maxPackableCount;
			long firstVolume = 0L;
			if(allFit) {
				maxPackableCount = n;
			} else {
				long loadVolume = 0L;
				long loadWeight = 0L;
				int count = 0;
				while (count < n) {
					int type = out[cursor + count];
					long volume = problemLongs[type];
					long weight = problemLongs[weights + type];
					if(volume > maxLoadVolume - loadVolume || weight > maxLoadWeight - loadWeight) {
						break;
					}
					loadVolume += volume;
					loadWeight += weight;
					count++;
				}
				maxPackableCount = Math.min(limit, count);
				for (int i = 0; i < maxPackableCount; i++) {
					firstVolume += problemLongs[out[cursor + i]];
				}
			}

			int size;
			if(!allFit && firstVolume < Math.max(boundVolume, outLongs[outLongBase + OUT_VOLUME])) {
				// no rotation of this permutation can load more than the best arrangement
				size = maxPackableCount;
			} else {
				for (int i = 0; i < n; i++) {
					s[permBase + i] = out[cursor + i];
				}
				s[control + FlatLayout.C_NODES] = 0;
				size = FlatSearch.searchPermutation(layout, problem, problemLongs, s, sl, base, longBase, maxPackableCount);
				if(size < 0) {
					out[outBase + OUT_STATUS] = OVERFLOW;
					result = OVERFLOW;
					running = false;
				} else {
					nodes += s[control + FlatLayout.C_NODES];
					outLongs[outLongBase + OUT_NODES] += s[control + FlatLayout.C_NODES];

					if(size > 0) {
						long volume = sl[longBase + layout.bestVolume];
						long weight = 0L;
						for (int i = 0; i < size; i++) {
							weight += problemLongs[weights + out[cursor + i]];
						}
						int count = out[outBase + OUT_COUNT];
						long bestVolume = outLongs[outLongBase + OUT_VOLUME];
						long bestWeight = outLongs[outLongBase + OUT_WEIGHT];
						// the earlier arrangement wins ties
						if(count == 0 || volume > bestVolume || (volume == bestVolume && (weight > bestWeight || (weight == bestWeight && size > count)))) {
							out[outBase + OUT_COUNT] = size;
							outLongs[outLongBase + OUT_VOLUME] = volume;
							outLongs[outLongBase + OUT_WEIGHT] = weight;
							for (int i = 0; i < n; i++) {
								out[bestPermutation + i] = out[cursor + i];
							}
							for (int i = 0; i < size; i++) {
								out[bestRotations + i] = s[base + layout.bestRotation + i];
							}
						}
						if(size == n) {
							out[outBase + OUT_FULL] = 1;
							out[outBase + OUT_STATUS] = DONE;
							result = DONE;
							running = false;
						}
					}
				}
			}

			if(running) {
				// the next permutation which changes the boxes up to the first which could not be placed
				int next = FlatPermutation.next(out, cursor, n, size);
				if(next == -1 || next < depth) {
					out[outBase + OUT_STATUS] = DONE;
					result = DONE;
					running = false;
				} else if(nodes >= budgetNodes) {
					out[outBase + OUT_STATUS] = PAUSED;
					result = PAUSED;
					running = false;
				}
			}
		}
		return result;
	}
}
