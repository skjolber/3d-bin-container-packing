package com.github.skjolber.packing.packer.bruteforce;


/**
 * The keys of the multisets of remaining boxes which a search meets: the boxes of the first group with boxes remaining (any counts of its
 * types, in a mixed radix), while the groups before it are placed and the groups after it are whole. Without groups, all multisets. The empty
 * multiset has the last key.
 */
final class BruteForceTreeMultisets {

	private final int[] group;
	private final int[] radix;
	private final int[] offsets;
	private final int[] fullKeys;
	private final int[] groupRemaining;
	private final int groups;
	/** the number of keys, including the empty multiset's */
	final int keys;
	/** the key of all boxes */
	final int fullKey;

	BruteForceTreeMultisets(int[] counts, int[] groupEnd) {
		int types = counts.length;
		group = new int[types];
		radix = new int[types];
		int[] groupStart = new int[types];
		int g = 0;
		for (int t = 0; t < types; t++) {
			if(counts[t] == 0) {
				group[t] = -1;
				continue;
			}
			groupStart[g] = t;
			int end = groupEnd[t];
			for (int k = t; k < end; k++) {
				group[k] = g;
			}
			g++;
			t = end - 1;
		}
		groups = g;
		offsets = new int[groups + 1];
		fullKeys = new int[groups + 1];
		groupRemaining = new int[groups];
		long offset = 0L;
		for (int i = 0; i < groups; i++) {
			offsets[i] = (int)offset;
			long size = 1L;
			long full = offset;
			int end = groupEnd[groupStart[i]];
			for (int t = groupStart[i]; t < end; t++) {
				radix[t] = (int)Math.min(size, Integer.MAX_VALUE);
				full += (long)counts[t] * radix[t];
				size *= counts[t] + 1;
				groupRemaining[i] += counts[t];
			}
			fullKeys[i] = (int)full;
			offset += size;
		}
		// the empty multiset
		offsets[groups] = (int)offset;
		fullKeys[groups] = (int)offset;
		keys = (int)offset + 1;
		fullKey = fullKeys[0];
	}

	/** @return the number of keys, i.e. of multisets a search meets (and the empty one) */
	static long count(int[] counts, int[] groupEnd) {
		long total = 1L;
		for (int t = 0; t < counts.length; t++) {
			if(counts[t] == 0) {
				continue;
			}
			long size = 1L;
			int end = groupEnd[t];
			for (int k = t; k < end; k++) {
				size *= counts[k] + 1;
				if(size > Integer.MAX_VALUE) {
					return Long.MAX_VALUE;
				}
			}
			total += size;
			t = end - 1;
		}
		return total;
	}

	/**
	 * @param key the key of the remaining boxes, of which the type is one
	 * @param remaining the remaining boxes
	 * @return the key of the remaining boxes without one box of the type
	 */
	int remove(int key, int[] remaining, int type) {
		int g = group[type];
		int left = 0;
		// the group's remaining boxes: the ones of the first group with boxes remaining are counted in the key
		for (int t = type; t >= 0 && group[t] == g; t--) {
			left += remaining[t];
		}
		for (int t = type + 1; t < group.length && group[t] == g; t++) {
			left += remaining[t];
		}
		if(left > 1) {
			return key - radix[type];
		}
		// the group's last box: the next group, whole
		return fullKeys[g + 1];
	}

	/**
	 * @param key the key of the remaining boxes
	 * @return the key of the remaining boxes with one more box of the type (the last placed)
	 */
	int add(int key, int type) {
		int g = group[type];
		if(key < offsets[g + 1]) {
			// the group has boxes remaining
			return key + radix[type];
		}
		// the group was placed whole: this box is its only one remaining
		return offsets[g] + radix[type];
	}
}
