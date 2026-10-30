package com.github.skjolber.packing.packer.bruteforce.tree;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The best arrangement of the parts of a search split between threads (by its load volume, weight and boxes): a part does not expand prefixes
 * which cannot beat it. Equal arrangements are not pruned, so the parts' results compare as on one thread.
 */
public final class BruteForceTreeSharedBest {

	private final AtomicReference<long[]> best = new AtomicReference<>();

	/** @return the volume, weight and count of the best arrangement so far, or null */
	long[] get() {
		return best.get();
	}

	void offer(long volume, long weight, int count) {
		long[] candidate = null;
		while (true) {
			long[] current = best.get();
			if(current != null && BruteForceTreeBound.compare(volume, weight, count, current[0], current[1], (int)current[2]) <= 0) {
				return;
			}
			if(candidate == null) {
				candidate = new long[] { volume, weight, count };
			}
			if(best.compareAndSet(current, candidate)) {
				return;
			}
		}
	}
}
