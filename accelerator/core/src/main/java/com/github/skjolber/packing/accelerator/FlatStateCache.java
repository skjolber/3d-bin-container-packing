package com.github.skjolber.packing.accelerator;

import static com.github.skjolber.packing.accelerator.FlatLayout.PLACEMENT_INTS;
import static com.github.skjolber.packing.accelerator.FlatLayout.PLANE_NONE;
import static com.github.skjolber.packing.accelerator.FlatLayout.POINT_INTS;

/**
 * The states which the tree search ({@link FlatTree}) has searched, so that it does not search them again. Different orders of the same boxes often
 * lead to the same state: the same free points (with the placements of their planes) and the same boxes remaining. The search below a node is a
 * function of its state, so a node with the state of a node which was searched before can add nothing better than the best: the first search found
 * it, or cut what could not beat the best of its time, and the best only improves.
 * <p>
 * This holds for the value of the winner (its volume, weight and count), not for which arrangement of that value wins: the node which is skipped may
 * be the one with the winner of the loop over the orders, which is the first in the order of the boxes and not the first which the tree reaches. So
 * the cache is for a search where any best arrangement will do ({@link FlatProblem#ANY_BEST}), and the engine does not use it otherwise. Under any
 * best the rotations of the winner are those of the node which found it, which need not be those which a search of its order alone picks first.
 * <p>
 * A state is a hash of 128 bits, and two states with the same hash are taken as the same: the cache is exact up to that. The table starts small and
 * doubles when it holds as many states as it has entries, up to the entries which the engine allows, so a search which is soon over does not pay for
 * a large table and one which is not gets one. The states stay where they are when the table doubles, so half of them are found again. A state
 * which finds none of the few slots it may take free replaces the first of them: the cache forgets rather than grows without end.
 * <p>
 * The cache of a thread is two arrays, so that the ports to native code keep it the same way, value for value. The first has counters, and the state
 * of the node of each level which is being searched (two longs, zero for none). The second is the table: two longs for each entry, the first of them
 * zero for none. It is for one search: the state has neither the sizes of the boxes nor the container.
 */
public final class FlatStateCache {

	/** the number of nodes whose state was looked up */
	public static final int LOOKUPS = 0;
	/** the number of those which were found, and so not searched */
	public static final int HITS = 1;
	/** the number of entries of the table less one; the number of entries is a power of two */
	public static final int MASK = 2;
	/**
	 * not zero to check the cache instead of using it: a node which is found is searched all the same, and counted if its search finds what the cache
	 * says it cannot ({@link #VIOLATIONS}). For the tests of the Java code; the ports leave it zero
	 */
	public static final int VERIFY = 3;
	/** the number of nodes which were found and whose search found an arrangement better than the best to beat when they were entered */
	public static final int VIOLATIONS = 4;
	/** the number of entries which the table may grow to, less one */
	public static final int MAX_MASK = 5;
	/** the number of states in the table */
	public static final int COUNT = 6;
	/** the offset of the states of the levels */
	public static final int LEVELS = 8;

	/** the entries which a table starts with */
	public static final int FIRST_ENTRIES = 256;
	/** the entries which the table of a thread may grow to unless the engine is told otherwise: one megabyte */
	public static final int DEFAULT_ENTRIES = 1 << 16;

	/** the fewest and the most entries of a table */
	public static final int MIN_ENTRIES = 16;
	public static final int MAX_ENTRIES = 1 << 24;

	/** the slots which an entry may take, from the one of its hash on */
	public static final int PROBES = 4;

	/** only nodes with at least this many boxes still to place are looked up: the search below a node near the leaves costs less than its state */
	public static final int MIN_REMAINING = 2;

	private static final long SEED_0 = 0x243F6A8885A308D3L;
	private static final long SEED_1 = 0x13198A2E03707344L;
	private static final long SEED_2 = 0xA4093822299F31D0L;
	private static final long SEED_3 = 0x082EFA98EC4E6C89L;
	private static final long K_0 = 0x9E3779B97F4A7C15L;
	private static final long K_1 = 0xC2B2AE3D27D4EB4FL;
	private static final long K_2 = 0x165667B19E3779F9L;
	private static final long K_3 = 0xD6E8FEB86659FD93L;
	private static final long FINAL_1 = 0xFF51AFD7ED558CCDL;
	private static final long FINAL_2 = 0xC4CEB9FE1A85EC53L;

	private FlatStateCache() {
	}

	/** @return whether a table may have this many entries */
	public static boolean isValidEntries(int entries) {
		return entries >= MIN_ENTRIES && entries <= MAX_ENTRIES && Integer.bitCount(entries) == 1;
	}

	/** @return the number of longs of the first array of a cache for a problem of n boxes */
	public static int size(int n) {
		return LEVELS + 2 * (n + 1);
	}

	/**
	 * @param n the number of boxes of the problem
	 * @param entries the most entries of the table
	 * @return an empty cache for a search: the counters and the states of the levels, and the table
	 */
	public static long[][] newCache(int n, int entries) {
		int first = Math.min(entries, FIRST_ENTRIES);
		long[] cache = new long[size(n)];
		cache[MASK] = first - 1;
		cache[MAX_MASK] = entries - 1;
		return new long[][] { cache, new long[2 * first] };
	}

	/** the caches of the engines check themselves ({@link #VERIFY}) and add what they count here: for the tests, see {@code FlatStateCacheTest} */
	static volatile boolean verify;
	static final java.util.concurrent.atomic.AtomicLong VERIFIED = new java.util.concurrent.atomic.AtomicLong();
	static final java.util.concurrent.atomic.AtomicLong VIOLATED = new java.util.concurrent.atomic.AtomicLong();

	/** @return a cache which checks itself: after the states of the levels, for each level whether its node was found, and the best to beat when it was entered */
	static long[][] newVerifyingCache(int n, int entries) {
		long[][] cache = newCache(n, entries);
		cache[0] = java.util.Arrays.copyOf(cache[0], size(n) + 4 * (n + 1));
		cache[0][VERIFY] = 1L;
		return cache;
	}

	/** The node of the level was found: remember the best to beat, as it is searched all the same. */
	static void found(int n, int level, long[] cache, int beatCount, long beatVolume, long beatWeight) {
		int o = size(n) + 4 * level;
		cache[o] = 1L;
		cache[o + 1] = beatCount;
		cache[o + 2] = beatVolume;
		cache[o + 3] = beatWeight;
	}

	/** The node of the level is done: if it was found, its search must not have found an arrangement better than the best to beat when it was entered. */
	static void check(int n, int level, long[] cache, int bestCount, long bestVolume, long bestWeight) {
		int o = size(n) + 4 * level;
		if(cache[o] != 0L) {
			cache[o] = 0L;
			long count = cache[o + 1];
			long volume = cache[o + 2];
			long weight = cache[o + 3];
			if(bestCount > 0 && (count == 0L || bestVolume > volume || (bestVolume == volume && (bestWeight > weight || (bestWeight == weight && bestCount > count))))) {
				cache[VIOLATIONS]++;
			}
		}
	}

	private static long pair(int low, int high) {
		return (low & 0xFFFFFFFFL) | ((long)high << 32);
	}

	private static long step(long hash, long value, long k) {
		return (Long.rotateLeft(hash, 27) ^ value) * k;
	}

	private static long finish(long hash) {
		hash ^= hash >>> 33;
		hash *= FINAL_1;
		hash ^= hash >>> 33;
		hash *= FINAL_2;
		return hash ^ (hash >>> 33);
	}

	/**
	 * Compute the state of the node of a level and keep it as the state of the level: the boxes which remain of each type, and the free points of
	 * the frame of the level, each with the placements of its three planes. The values are taken two ints at a time and spread over four hashes in
	 * turn, so that a processor computes the four side by side.
	 */
	public static void state(FlatLayout layout, int[] s, int base, int level, int remainingBase, int types, long[] cache) {
		long h0 = SEED_0;
		long h1 = SEED_1;
		long h2 = SEED_2;
		long h3 = SEED_3;
		// the remaining boxes, two types at a time: the hashes 0 and 1
		int t = 0;
		for (; t + 3 < types; t += 4) {
			h0 = step(h0, pair(s[remainingBase + t], s[remainingBase + t + 1]), K_0);
			h1 = step(h1, pair(s[remainingBase + t + 2], s[remainingBase + t + 3]), K_1);
		}
		long rest0 = 0L;
		long rest1 = 0L;
		if(t < types) {
			rest0 = pair(s[remainingBase + t], t + 1 < types ? s[remainingBase + t + 1] : 0);
			if(t + 2 < types) {
				rest1 = pair(s[remainingBase + t + 2], 0);
			}
		}
		h0 = step(h0, rest0, K_0);
		h1 = step(h1, rest1, K_1);

		final int frame = base + layout.frameOffset(level);
		final int count = s[frame];
		final int placements = base + layout.placements;
		h2 = step(h2, count, K_2);
		h3 = step(h3, types, K_3);
		for (int k = 0; k < count; k++) {
			int point = frame + 1 + k * POINT_INTS;
			// the point: the hashes 0 to 2
			h0 = step(h0, pair(s[point], s[point + 1]), K_0);
			h1 = step(h1, pair(s[point + 2], s[point + 3]), K_1);
			h2 = step(h2, pair(s[point + 4], s[point + 5]), K_2);
			int planes = s[point + 6];
			// the placements of its planes, where no plane is one value: the hashes 3, then 0 to 2 and 1 to 3
			int yz = planes & PLANE_NONE;
			int xz = (planes >>> 10) & PLANE_NONE;
			int xy = (planes >>> 20) & PLANE_NONE;
			if(yz == PLANE_NONE) {
				h3 = step(h3, -1L, K_3);
			} else {
				int p = placements + yz * PLACEMENT_INTS;
				h3 = step(h3, pair(s[p], s[p + 1]), K_3);
				h0 = step(h0, pair(s[p + 2], s[p + 3]), K_0);
				h1 = step(h1, pair(s[p + 4], s[p + 5]), K_1);
			}
			if(xz == PLANE_NONE) {
				h2 = step(h2, -1L, K_2);
			} else {
				int p = placements + xz * PLACEMENT_INTS;
				h2 = step(h2, pair(s[p], s[p + 1]), K_2);
				h3 = step(h3, pair(s[p + 2], s[p + 3]), K_3);
				h0 = step(h0, pair(s[p + 4], s[p + 5]), K_0);
			}
			if(xy == PLANE_NONE) {
				h1 = step(h1, -1L, K_1);
			} else {
				int p = placements + xy * PLACEMENT_INTS;
				h1 = step(h1, pair(s[p], s[p + 1]), K_1);
				h2 = step(h2, pair(s[p + 2], s[p + 3]), K_2);
				h3 = step(h3, pair(s[p + 4], s[p + 5]), K_3);
			}
		}
		long x = h0 + Long.rotateLeft(h1, 21);
		long y = h2 + Long.rotateLeft(h3, 43);
		long first = finish(x ^ (y * K_1));
		long second = finish(y ^ (x * K_3));
		if(first == 0L) {
			// zero is no state
			first = 1L;
		}
		cache[LEVELS + 2 * level] = first;
		cache[LEVELS + 2 * level + 1] = second;
	}

	/** Forget the state of the level: its node is not looked up. */
	public static void none(int level, long[] cache) {
		cache[LEVELS + 2 * level] = 0L;
	}

	/**
	 * @param cache the counters and the states of the levels
	 * @param table the table
	 * @return whether the state of the level is in the table
	 */
	public static boolean contains(int level, long[] cache, long[] table) {
		final long first = cache[LEVELS + 2 * level];
		final long second = cache[LEVELS + 2 * level + 1];
		final int mask = (int)cache[MASK];
		final int index = (int)first & mask;
		cache[LOOKUPS]++;
		boolean found = false;
		for (int probe = 0; probe < PROBES && !found; probe++) {
			int slot = 2 * ((index + probe) & mask);
			found = table[slot] == first && table[slot + 1] == second;
		}
		if(found) {
			cache[HITS]++;
		}
		return found;
	}

	/**
	 * Add the state of the level to the table, if the level has one, and forget it: its node is searched. The table doubles when it then holds as
	 * many states as it has entries.
	 *
	 * @param cache the cache: the counters and the states of the levels, and the table, which may be replaced by a larger one
	 */
	public static void add(int level, long[][] cache) {
		final long[] levels = cache[0];
		final long first = levels[LEVELS + 2 * level];
		if(first != 0L) {
			final long second = levels[LEVELS + 2 * level + 1];
			levels[LEVELS + 2 * level] = 0L;
			long[] table = cache[1];
			final int mask = (int)levels[MASK];
			final int index = (int)first & mask;
			// the first slot which is free or has the state, else the first slot
			int target = 2 * index;
			boolean placed = false;
			for (int probe = 0; probe < PROBES && !placed; probe++) {
				int slot = 2 * ((index + probe) & mask);
				if(table[slot] == 0L || (table[slot] == first && table[slot + 1] == second)) {
					target = slot;
					placed = true;
				}
			}
			if(table[target] == 0L) {
				// one more state
				levels[COUNT]++;
				if(levels[COUNT] > mask && mask < levels[MAX_MASK]) {
					levels[MASK] = 2L * mask + 1L;
					table = java.util.Arrays.copyOf(table, 2 * table.length);
					cache[1] = table;
				}
			}
			table[target] = first;
			table[target + 1] = second;
		}
	}
}
