package com.github.skjolber.packing.packer.bruteforce;

import java.util.Arrays;

/**
 * The state of a thread's search which other threads can steal work from: at the shallow levels, the types which the node still has to try.
 * A thief takes the types after the current one of the shallowest such node (the largest subtree left, in the order the tree searches), as a
 * {@link BruteForceTreeWork} with the node's prefix of placements, which it replays on its own point calculator. The thread updates the state under the
 * lock at those levels only, so the deeper levels, where most of the nodes are, cost nothing.
 */
final class BruteForceTreeStealableState {

	/** the levels at which work is stolen: the subtrees below are small */
	static final int STEAL_DEPTH = 6;
	/** the nodes a part searches before the thieves take from it: a short search (all boxes fit, and the first arrangements place them) runs alone */
	static final int STEAL_AFTER = 4096;
	/** the nodes of the part searched so far, published every so often */
	volatile int nodes;
	/** starts the thieves when the part has searched {@link #STEAL_AFTER} nodes, or null */
	Runnable thieves;

	final Object lock = new Object();
	/** the type order of the search, for the ranks */
	int[] typeOrder;
	/** the levels published: the ones below the depth are on the stack with their types started (read without the lock to see if there is anything) */
	volatile int depth;
	/** the levels of the work's prefix, which have one type each */
	int prefixLength;
	final int[] placedType = new int[STEAL_DEPTH];
	final int[] placedOrdinal = new int[STEAL_DEPTH];
	/** per level, the rank of the type being tried, or -1 */
	final int[] typeRank = new int[STEAL_DEPTH];
	/** per level, the end of the ranks of the types to try */
	final int[] typeLimit = new int[STEAL_DEPTH];
	/** whether the thread is searching */
	boolean active;

	/**
	 * @param active the count of the parts being searched, or stolen: counted up under the lock, so that the victim cannot finish (and the
	 *        count reach zero) before the thief starts
	 * @return the types after the current one at the shallowest level with any, as work for the thief, or null
	 */
	BruteForceTreeWork steal(int[] counts, int[] groupEnd, java.util.concurrent.atomic.AtomicInteger active) {
		if(nodes < STEAL_AFTER || depth <= prefixLength) {
			// too early, or nothing to steal: without the lock
			return null;
		}
		synchronized (lock) {
			if(!this.active) {
				return null;
			}
			for (int level = prefixLength; level < depth; level++) {
				int rank = typeRank[level];
				if(rank == -1 || rank + 1 >= typeLimit[level]) {
					continue;
				}
				// a type of the range with boxes remaining, of the first group with boxes remaining
				int[] remaining = counts.clone();
				for (int i = 0; i < level; i++) {
					remaining[placedType[i]]--;
				}
				int end = -1;
				for (int t = 0; t < remaining.length; t++) {
					if(remaining[t] > 0) {
						end = groupEnd[t];
						break;
					}
				}
				boolean any = false;
				for (int i = rank + 1; i < typeLimit[level]; i++) {
					int t = typeOrder[i];
					if(remaining[t] > 0 && t < end) {
						any = true;
						break;
					}
				}
				if(!any) {
					continue;
				}
				BruteForceTreeWork work = new BruteForceTreeWork(Arrays.copyOf(placedType, level), Arrays.copyOf(placedOrdinal, level), rank + 1, typeLimit[level]);
				typeLimit[level] = rank + 1;
				active.incrementAndGet();
				return work;
			}
			return null;
		}
	}
}
