package com.github.skjolber.packing.packer.bruteforce.tree;

import java.util.Arrays;

/**
 * The tables of a search, per multiset of remaining boxes and per type prefix: the parts of a search split between threads, which search the
 * same boxes for the same container, can share them on a thread (the orders of the parts are disjoint, so the settled orders add up).
 */
public final class BruteForceTreeTables {

	final BruteForceTreeMultisets multisets;
	final BruteForceTreeBound bounds;
	final BruteForceTreeSettledOrders settled;
	/** the free space of the nodes of the holder's last search, reused by the next (see {@link BruteForceTreeFreeSpace#reset}), or null */
	BruteForceTreeFreeSpace freeSpace;
	// the problem the tables are for
	private final int[] counts;
	private final int[] groupEnd;
	private final int n;
	private final long maxLoadVolume;
	private final long maxLoadWeight;
	private long[] volumes;
	private long[] weights;
	private long[] minAreas;
	/** the split search the tables were last used by, or null */
	private BruteForceTreeSharedBest sharedBest;

	private BruteForceTreeTables(BruteForceTreeMultisets multisets, BruteForceTreeBound bounds, BruteForceTreeSettledOrders settled, int[] counts, int[] groupEnd, int n, long maxLoadVolume,
			long maxLoadWeight, long[] volumes, long[] weights, long[] minAreas) {
		this.multisets = multisets;
		this.bounds = bounds;
		this.settled = settled;
		this.counts = counts;
		this.groupEnd = groupEnd;
		this.n = n;
		this.maxLoadVolume = maxLoadVolume;
		this.maxLoadWeight = maxLoadWeight;
		this.volumes = volumes;
		this.weights = weights;
		this.minAreas = minAreas;
	}

	/**
	 * The tables for a problem: the previous ones when they are for the same boxes (another part of the search, or a new search of it), the
	 * previous ones reset when they are for other boxes of the same structure (the keys and the sequence counts hold: box item groups in
	 * another order), or new ones.
	 *
	 * @param previous the tables of the previous search of the holder, or null
	 * @param sharedBest the split search this search is a part of, or null: a part of the search the previous tables were used by keeps their
	 *        settled orders (the parts' orders are disjoint), any other search starts with none settled
	 */
	static BruteForceTreeTables of(BruteForceTreeTables previous, int[] counts, long[] volumes, long[] weights, long[] minAreas, long maxLoadVolume, long maxLoadWeight, int[] groupEnd, int n,
			BruteForceTreeSharedBest sharedBest) {
		if(previous != null && previous.n == n && previous.maxLoadVolume == maxLoadVolume && previous.maxLoadWeight == maxLoadWeight && Arrays.equals(previous.counts, counts)
				&& Arrays.equals(previous.groupEnd, groupEnd)) {
			boolean same = Arrays.equals(previous.volumes, volumes) && Arrays.equals(previous.weights, weights) && Arrays.equals(previous.minAreas, minAreas);
			if(same || previous.bounds.reset(volumes, weights)) {
				if(!same) {
					previous.volumes = volumes;
					previous.weights = weights;
					previous.minAreas = minAreas;
				}
				if(sharedBest == null || previous.sharedBest != sharedBest) {
					previous.settled.reset();
				}
				previous.sharedBest = sharedBest;
				return previous;
			}
		}
		BruteForceTreeMultisets multisets = new BruteForceTreeMultisets(counts, groupEnd);
		BruteForceTreeTables tables = new BruteForceTreeTables(multisets, new BruteForceTreeBound(counts, volumes, weights, maxLoadVolume, maxLoadWeight, multisets.keys, groupEnd),
				new BruteForceTreeSettledOrders(counts, n, multisets, groupEnd), counts, groupEnd, n, maxLoadVolume, maxLoadWeight, volumes, weights, minAreas);
		tables.sharedBest = sharedBest;
		if(previous != null) {
			tables.freeSpace = previous.freeSpace;
		}
		return tables;
	}
}
