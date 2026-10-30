package com.github.skjolber.packing.packer.bruteforce.tree;

import java.util.Arrays;

/**
 * The orders settled so far, by type prefix: the loop stops an order at its first complete path, and the orders through a prefix of types (a
 * block of the lexicographic order of the permutations) are settled together when the prefix ends every arrangement (a leaf, a box over the
 * load limits, a hopeless prefix). Each prefix of a depth has a rank among the prefixes of that depth (the sequences of that length from the
 * multiset of boxes, in lexicographic order), and a count of the orders through it which are settled; a prefix is settled when the count is the
 * number of orders through it.
 * <p>
 * The counts take an int per prefix of every depth. When that is more than {@value #MAX_PREFIXES}, nothing is kept ({@link #exact} is false)
 * and the search falls back to settling the other candidates of a box at a node only when every order through one candidate settled.
 */
final class BruteForceTreeSettledOrders {

	static final int MAX_PREFIXES = 1 << 22;
	/** the most entries of the table of sequence counts (multisets times lengths) */
	static final long MAX_SEQUENCES = 1L << 20;

	/** the most prefixes to keep counts for; tests lower it to cover the fallback */
	static volatile int maxPrefixes = MAX_PREFIXES;

	final boolean exact;
	private final int n;
	private final int types;
	private final BruteForceTreeMultisets multisets;
	/** the number of sequences of a length from a multiset, by multiset key and length; -1 until computed */
	private final long[] sequences;
	/** the settled orders per depth and prefix rank */
	private final int[][] settled;
	/** the number of prefixes per depth: the size of the settled counts of the depth */
	private final int[] prefixSizes;
	/**
	 * per level, the number of sequences of each length from the multisets which remain after a type before the level's type: the rank of the
	 * level's prefix among longer prefixes is the sum over the levels before it
	 */
	private final long[][] before;
	/** the end of each type's group's range of types: a sequence takes from the first group with boxes remaining */
	private final int[] groupEnd;

	/**
	 * @param multisets the keys of the multisets
	 * @param groupEnd per type, the end of its group's range of types
	 */
	BruteForceTreeSettledOrders(int[] counts, int n, BruteForceTreeMultisets multisets, int[] groupEnd) {
		this.n = n;
		this.types = counts.length;
		this.multisets = multisets;
		this.groupEnd = groupEnd;
		long keys = multisets.keys;
		// the number of orders (the sequences of all boxes) first, without the table: the multinomial coefficient
		long orders = 1L;
		int sum = 0;
		for (int t = 0; t < types && orders <= maxPrefixes; t++) {
			for (int i = 1; i <= counts[t] && orders <= maxPrefixes; i++) {
				sum++;
				orders = orders * sum / i;
			}
		}
		boolean fits = orders <= maxPrefixes && keys * (n + 1) <= MAX_SEQUENCES;
		long[] sequences = null;
		int[][] settled = null;
		int[] prefixSizes = null;
		if(fits) {
			sequences = new long[(int)keys * (n + 1)];
			Arrays.fill(sequences, -1L);
			long total = 0;
			settled = new int[n + 1][];
			prefixSizes = new int[n + 1];
			int rootKey = multisets.fullKey;
			for (int d = 0; d <= n && fits; d++) {
				long prefixes = sequences(sequences, counts, rootKey, d);
				total += prefixes;
				if(total > maxPrefixes) {
					fits = false;
				} else {
					// allocated when first used: a search often reaches a few depths only
					prefixSizes[d] = (int)prefixes;
				}
			}
		}
		this.exact = fits;
		this.sequences = fits ? sequences : null;
		this.settled = fits ? settled : null;
		this.prefixSizes = fits ? prefixSizes : null;
		this.before = fits ? new long[n + 1][] : null;
	}

	/** a new search: no orders settled */
	void reset() {
		if(!exact) {
			return;
		}
		for (int[] counts : settled) {
			if(counts != null) {
				Arrays.fill(counts, 0);
			}
		}
	}

	/** the number of distinct sequences of the length from the remaining boxes (with the key) */
	private long sequences(long[] sequences, int[] remaining, int key, int length) {
		if(length == 0) {
			return 1L;
		}
		int index = key * (n + 1) + length;
		long count = sequences[index];
		if(count == -1L) {
			count = 0L;
			int end = types;
			for (int t = 0; t < end; t++) {
				if(remaining[t] > 0) {
					// the first group with boxes remaining
					end = groupEnd[t];
					int childKey = multisets.remove(key, remaining, t);
					remaining[t]--;
					count += sequences(sequences, remaining, childKey, length - 1);
					remaining[t]++;
				}
			}
			sequences[index] = count;
		}
		return count;
	}

	/** the number of orders through the prefix of a level, i.e. the sequences of the remaining boxes (with the key) */
	private long orders(int level, int[] remaining, int key) {
		return sequences(sequences, remaining, key, n - level);
	}

	/** @return whether every order through the level's prefix is settled */
	boolean isSettled(int level, int rank, int[] remaining, int key) {
		return exact && counts(level)[rank] == orders(level, remaining, key);
	}

	/** the rank of the first child of the level's prefix among the prefixes of the next depth */
	int firstChildRank(int level) {
		if(!exact) {
			return 0;
		}
		long rank = 0;
		for (int i = 0; i < level; i++) {
			rank += before[i][level - i];
		}
		return (int)rank;
	}

	/** the rank of the child with the type: the children are in type order */
	int childRank(int firstChildRank, int[] remaining, int type) {
		if(!exact) {
			return 0;
		}
		int rank = firstChildRank;
		for (int t = 0; t < type; t++) {
			if(remaining[t] > 0) {
				rank++;
			}
		}
		return rank;
	}

	/** keep, for the level's type, the sequences of each length from the multisets remaining after the types before it */
	void startType(int level, int[] remaining, int type, int key) {
		if(!exact) {
			return;
		}
		long[] counts = before[level];
		if(counts == null) {
			counts = before[level] = new long[n + 1];
		}
		Arrays.fill(counts, 0L);
		for (int t = 0; t < type; t++) {
			if(remaining[t] > 0) {
				int childKey = multisets.remove(key, remaining, t);
				remaining[t]--;
				for (int length = 0; length <= n - level - 1; length++) {
					counts[length] += sequences(sequences, remaining, childKey, length);
				}
				remaining[t]++;
			}
		}
	}

	/** @return whether every order with the type next at the level is settled */
	boolean isChildSettled(int level, int rank, int[] remaining, int type, int childKey) {
		if(!exact) {
			return false;
		}
		remaining[type]--;
		boolean settled = counts(level + 1)[rank] == orders(level + 1, remaining, childKey);
		remaining[type]++;
		return settled;
	}

	/** settle every order through the level's prefix (the ranks of the levels' prefixes are the path to it) */
	void settle(int level, int[] rankAt, int[] remaining, int key) {
		if(!exact) {
			return;
		}
		int[] counts = counts(level);
		long delta = orders(level, remaining, key) - counts[rankAt[level]];
		if(delta == 0) {
			return;
		}
		counts[rankAt[level]] += (int)delta;
		for (int i = 0; i < level; i++) {
			counts(i)[rankAt[i]] += (int)delta;
		}
	}

	/** the settled counts of a depth, allocated when first used */
	private int[] counts(int depth) {
		int[] counts = settled[depth];
		if(counts == null) {
			counts = settled[depth] = new int[prefixSizes[depth]];
		}
		return counts;
	}

	/** settle every order with the type next at the level */
	void settleChild(int level, int rank, int[] rankAt, int[] remaining, int type, int childKey) {
		if(!exact) {
			return;
		}
		remaining[type]--;
		int[] childCounts = counts(level + 1);
		long delta = orders(level + 1, remaining, childKey) - childCounts[rank];
		remaining[type]++;
		if(delta == 0) {
			return;
		}
		childCounts[rank] += (int)delta;
		for (int i = 0; i <= level; i++) {
			counts(i)[rankAt[i]] += (int)delta;
		}
	}
}
