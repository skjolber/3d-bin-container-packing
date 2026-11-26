package com.github.skjolber.packing.accelerator;

import static com.github.skjolber.packing.accelerator.FlatLayout.POINT_INTS;

import java.util.Arrays;

/**
 * Experiment, off by default, for one thread only: two ways for the tree search ({@link FlatTree}) to search fewer nodes.
 * <ul>
 * <li>{@link #cache}: the states which were searched. The search below a node is a function of its free points (with the placements of their
 * planes) and of the boxes which remain, so a node with the state of a node which was searched before can add nothing better than the best: the
 * first search found it, or cut what could not beat the best of its time, and the best only improves. This holds for the value of the winner (its
 * volume, weight and count); among arrangements of the same value the search with the cache may keep another one. Only nodes past the prefix of the
 * item are compared, as the others have the types of the prefix as their only children. A state is a hash of 64 bits: two states with the same hash
 * are taken as the same.</li>
 * <li>{@link #union}: a box is placed at a free point, within it, and the free points of the nodes below a node are within its free points. So the
 * boxes below a node add at most the volume of the union of its free points, which can be less than the load left.</li>
 * </ul>
 */
final class FlatPruning {

	/** whether nodes with the state of a searched node are skipped; {@link #clear()} before each search */
	static boolean cache;
	/** only nodes with at least this many levels below them are looked up: the search below a node near the leaves costs less than its state */
	static int cacheMinRemaining = Integer.getInteger("flat.pruning.minRemaining", 1);
	/** whether the volume which the boxes below a node can add is bounded by the union of its free points */
	static boolean union;

	static long lookups;
	static long hits;
	static long unionChecks;
	static long unionCuts;

	private static long[] table = new long[1 << 16];
	private static int size;
	/** the state of the node of each level (zero for none), and whether it was found in the cache */
	static final long[] LEVEL_STATE = new long[FlatLayout.MAX_PLACEMENTS + 2];

	private static final int[] XS = new int[2 * 1024];
	private static final int[] YS = new int[2 * 1024];
	private static final int[] ZS = new int[2 * 1024];

	private FlatPruning() {
	}

	static void clear() {
		Arrays.fill(table, 0L);
		size = 0;
		Arrays.fill(LEVEL_STATE, 0L);
		lookups = 0L;
		hits = 0L;
		unionChecks = 0L;
		unionCuts = 0L;
	}

	private static long mix(long hash, long value) {
		hash ^= value + 0x9E3779B97F4A7C15L + (hash << 6) + (hash >>> 2);
		hash *= 0xBF58476D1CE4E5B9L;
		return hash ^ (hash >>> 29);
	}

	/** @return the state of the node of a level: its free points, the placements of their planes and the boxes which remain; not zero */
	static long state(FlatLayout layout, int[] s, int base, int level, int remainingBase, int types) {
		long hash = 0x1234567L;
		for (int t = 0; t < types; t++) {
			hash = mix(hash, s[remainingBase + t]);
		}
		final int frame = base + layout.frameOffset(level);
		final int count = s[frame];
		final int placements = base + layout.placements;
		hash = mix(hash, count);
		for (int k = 0; k < count; k++) {
			int point = frame + 1 + k * POINT_INTS;
			for (int c = 0; c < 6; c++) {
				hash = mix(hash, s[point + c]);
			}
			int planes = s[point + 6];
			for (int shift = 0; shift <= 20; shift += 10) {
				int id = (planes >>> shift) & 1023;
				if(id == 1023) {
					hash = mix(hash, -1L);
				} else {
					for (int c = 0; c < 6; c++) {
						hash = mix(hash, s[placements + 6 * id + c]);
					}
				}
			}
		}
		return hash == 0L ? 1L : hash;
	}

	static boolean contains(long state) {
		lookups++;
		int mask = table.length - 1;
		int index = (int)(state >>> 20) & mask;
		while (table[index] != 0L) {
			if(table[index] == state) {
				hits++;
				return true;
			}
			index = (index + 1) & mask;
		}
		return false;
	}

	static void add(long state) {
		if(2 * (size + 1) > table.length) {
			long[] old = table;
			table = new long[2 * old.length];
			size = 0;
			for (long value : old) {
				if(value != 0L) {
					add(value);
				}
			}
		}
		int mask = table.length - 1;
		int index = (int)(state >>> 20) & mask;
		while (table[index] != 0L && table[index] != state) {
			index = (index + 1) & mask;
		}
		if(table[index] == 0L) {
			table[index] = state;
			size++;
		}
	}

	private static int sortedUnique(int[] values, int count) {
		Arrays.sort(values, 0, count);
		int unique = 0;
		for (int i = 0; i < count; i++) {
			if(unique == 0 || values[unique - 1] != values[i]) {
				values[unique++] = values[i];
			}
		}
		return unique;
	}

	/** @return the volume of the union of the free points of the frame of a level, or at least the limit if it is not less */
	static long unionVolume(FlatLayout layout, int[] s, int base, int level, long limit) {
		final int frame = base + layout.frameOffset(level);
		final int count = s[frame];
		if(count > 1024) {
			return Long.MAX_VALUE;
		}
		long sum = 0L;
		for (int k = 0; k < count; k++) {
			int p = frame + 1 + k * POINT_INTS;
			XS[2 * k] = s[p];
			XS[2 * k + 1] = s[p + 3] + 1;
			YS[2 * k] = s[p + 1];
			YS[2 * k + 1] = s[p + 4] + 1;
			ZS[2 * k] = s[p + 2];
			ZS[2 * k + 1] = s[p + 5] + 1;
			sum += (long)(s[p + 3] + 1 - s[p]) * (s[p + 4] + 1 - s[p + 1]) * (s[p + 5] + 1 - s[p + 2]);
		}
		if(count <= 1) {
			return sum;
		}
		int xs = sortedUnique(XS, 2 * count);
		int ys = sortedUnique(YS, 2 * count);
		int zs = sortedUnique(ZS, 2 * count);
		long volume = 0L;
		for (int i = 0; i + 1 < xs && volume < limit; i++) {
			for (int j = 0; j + 1 < ys; j++) {
				for (int l = 0; l + 1 < zs; l++) {
					boolean covered = false;
					for (int k = 0; k < count && !covered; k++) {
						int p = frame + 1 + k * POINT_INTS;
						covered = s[p] <= XS[i] && XS[i] <= s[p + 3] && s[p + 1] <= YS[j] && YS[j] <= s[p + 4] && s[p + 2] <= ZS[l] && ZS[l] <= s[p + 5];
					}
					if(covered) {
						volume += (long)(XS[i + 1] - XS[i]) * (YS[j + 1] - YS[j]) * (ZS[l + 1] - ZS[l]);
					}
				}
			}
		}
		return volume;
	}
}
