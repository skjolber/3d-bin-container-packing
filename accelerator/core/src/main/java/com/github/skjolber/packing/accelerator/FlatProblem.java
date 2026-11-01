package com.github.skjolber.packing.accelerator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;

/**
 * The boxes of one container and their rotations, as primitive arrays which the flat search reads and never changes. A type is a box item of the
 * iterator; the permutations are of types, as for the iterator.
 * <p>
 * Ints: the header ({@link #N}, {@link #TYPES}, the load size, the number of rotations, the limit, whether all boxes fit the container by volume and
 * weight, whether any arrangement of the best value will do, and the number of keys of the bound table), then the number of boxes of each type, then
 * the first rotation of each type (and the end of the last one), then the dx, dy and dz of each rotation, then the types in the order in which the
 * tree search tries them ({@link #typeOrder}), then the radix of each type in the key of a multiset of remaining boxes ({@link #radix}).
 * Longs: the volume of each type, then the smallest rotation area of each type, then the weight of each type, then the max load volume and the max load
 * weight of the container, then the bound table ({@link #bounds}, see {@link FlatBound}).
 */
public final class FlatProblem {

	/** the number of boxes in a permutation */
	public static final int N = 0;
	public static final int TYPES = 1;
	public static final int CX = 2;
	public static final int CY = 3;
	public static final int CZ = 4;
	public static final int ROTATIONS = 5;
	/** the number of leading boxes of a permutation which may be placed */
	public static final int LIMIT = 6;
	/** 1 if all boxes fit the container by volume and weight (and the limit allows all), otherwise 0 */
	public static final int ALL_FIT = 7;
	/** 1 if any arrangement of the best value will do (the tree search then tries the largest boxes first and prunes ties), 0 for the loop's winner */
	public static final int ANY_BEST = 8;
	/** the number of keys of the bound table, or 0 if the table is left out (too many multisets of remaining boxes) */
	public static final int KEYS = 9;
	/** 1 if a box is tried at every free point where it fits, as {@code BruteForcePackager} does; 0 at the best point of each rotation, as {@code FastBruteForcePackager} does */
	public static final int ALL_POINTS = 10;
	public static final int HEADER = 11;
	/** the most multisets of remaining boxes for which the bound table is computed */
	public static final int MAX_KEYS = 1 << 12;

	/** @return the offset of the table with the first rotation of each type (one more entry for the end of the last type), in the ints */
	public static int rotationOffsets(int types) {
		return HEADER + types;
	}

	/** @return the offset of the dx of the first rotation of the first type, in the ints; each rotation is its dx, dy and dz */
	public static int rotationValues(int types) {
		return HEADER + 2 * types + 1;
	}

	/** @return the offset of the smallest rotation area of the first type, in the longs */
	public static int minAreas(int types) {
		return types;
	}

	/** @return the offset of the weight of the first type, in the longs */
	public static int weights(int types) {
		return 2 * types;
	}

	/** @return the offset of the max load volume of the container, in the longs (the max load weight follows) */
	public static int maxLoad(int types) {
		return 3 * types;
	}

	/** @return the offset of the types in the order the tree search tries them at a node, in the ints */
	public static int typeOrder(int types, int rotations) {
		return rotationValues(types) + 3 * rotations;
	}

	/** @return the offset of the radix of each type in the key of a multiset of remaining boxes, in the ints: the key is the sum of count times radix */
	public static int radix(int types, int rotations) {
		return typeOrder(types, rotations) + types;
	}

	/** @return the offset of the bound table in the longs: for each key the volume, the weight and the number of boxes of the best sub-multiset within the load left */
	public static int bounds(int types) {
		return maxLoad(types) + 2;
	}

	private final int[] ints;
	private final long[] longs;

	private FlatProblem(int[] ints, long[] longs) {
		this.ints = ints;
		this.longs = longs;
	}

	public int[] ints() {
		return ints;
	}

	public long[] longs() {
		return longs;
	}

	public int n() {
		return ints[N];
	}

	/** @return true if a box is tried at every free point where it fits (the search of {@code BruteForcePackager}), which only the tree search does */
	public boolean isAllPoints() {
		return ints[ALL_POINTS] != 0;
	}

	public int types() {
		return ints[TYPES];
	}

	/**
	 * A problem for a container without load limits, where all boxes may be placed.
	 *
	 * @param iterator an iterator, at any permutation
	 * @param cx the load size of the container
	 */
	public static FlatProblem of(BoxItemPermutationRotationIterator iterator, int cx, int cy, int cz) {
		return of(iterator, cx, cy, cz, Long.MAX_VALUE, Long.MAX_VALUE, iterator.length());
	}

	/**
	 * @param iterator an iterator, at any permutation
	 * @param cx the load size of the container
	 * @param maxLoadVolume the max load volume of the container
	 * @param maxLoadWeight the max load weight of the container
	 * @param limit the number of leading boxes of a permutation which may be placed
	 */
	public static FlatProblem of(BoxItemPermutationRotationIterator iterator, int cx, int cy, int cz, long maxLoadVolume, long maxLoadWeight, int limit) {
		return of(iterator, cx, cy, cz, maxLoadVolume, maxLoadWeight, limit, false);
	}

	/**
	 * @param iterator an iterator, at any permutation
	 * @param cx the load size of the container
	 * @param maxLoadVolume the max load volume of the container
	 * @param maxLoadWeight the max load weight of the container
	 * @param limit the number of leading boxes of a permutation which may be placed
	 * @param anyBest true if any arrangement of the best value will do, false for the loop's winner (see {@link #ANY_BEST})
	 */
	public static FlatProblem of(BoxItemPermutationRotationIterator iterator, int cx, int cy, int cz, long maxLoadVolume, long maxLoadWeight, int limit, boolean anyBest) {
		return of(iterator, cx, cy, cz, maxLoadVolume, maxLoadWeight, limit, anyBest, false);
	}

	/**
	 * @param allPoints true to try a box at every free point where it fits (the search of {@code BruteForcePackager}), false at the best point of each
	 *        rotation (the search of {@code FastBruteForcePackager}); only the tree search ({@code FlatTree}) takes all points
	 */
	public static FlatProblem of(BoxItemPermutationRotationIterator iterator, int cx, int cy, int cz, long maxLoadVolume, long maxLoadWeight, int limit, boolean anyBest, boolean allPoints) {
		BoxStackValue[][] stackValues = iterator.getBoxItemStackValues();
		int types = stackValues.length;
		int n = iterator.length();
		int[] permutations = iterator.getPermutations();

		int rotations = 0;
		for (int t = 0; t < types; t++) {
			if(stackValues[t] != null) {
				rotations += stackValues[t].length;
			}
		}

		int[] ints = new int[radix(types, rotations) + types];
		long[] longs = new long[bounds(types)];
		ints[N] = n;
		ints[ANY_BEST] = anyBest ? 1 : 0;
		ints[ALL_POINTS] = allPoints ? 1 : 0;
		ints[TYPES] = types;
		ints[CX] = cx;
		ints[CY] = cy;
		ints[CZ] = cz;
		ints[ROTATIONS] = rotations;
		ints[LIMIT] = limit;
		longs[maxLoad(types)] = maxLoadVolume;
		longs[maxLoad(types) + 1] = maxLoadWeight;

		for (int i = 0; i < n; i++) {
			ints[HEADER + permutations[i]]++;
		}

		int offsets = rotationOffsets(types);
		int values = rotationValues(types);
		int rotation = 0;
		for (int t = 0; t < types; t++) {
			ints[offsets + t] = rotation;
			longs[minAreas(types) + t] = Long.MAX_VALUE;
			BoxStackValue[] typeRotations = stackValues[t];
			if(typeRotations == null) {
				continue;
			}
			for (BoxStackValue stackValue : typeRotations) {
				longs[weights(types) + t] = stackValue.getBox().getWeight();
				ints[values + 3 * rotation] = stackValue.getDx();
				ints[values + 3 * rotation + 1] = stackValue.getDy();
				ints[values + 3 * rotation + 2] = stackValue.getDz();
				longs[t] = stackValue.getVolume();
				if(stackValue.getArea() < longs[minAreas(types) + t]) {
					longs[minAreas(types) + t] = stackValue.getArea();
				}
				rotation++;
			}
		}
		ints[offsets + types] = rotation;

		// as canPackAll(..): the volume and the weight of all boxes fit the container, in the order of the iterator
		boolean allFit = limit >= n;
		long loadVolume = 0L;
		long loadWeight = 0L;
		for (int i = 0; i < n && allFit; i++) {
			long volume = longs[permutations[i]];
			long weight = longs[weights(types) + permutations[i]];
			if(volume > maxLoadVolume - loadVolume || weight > maxLoadWeight - loadWeight) {
				allFit = false;
			}
			loadVolume += volume;
			loadWeight += weight;
		}
		ints[ALL_FIT] = allFit ? 1 : 0;

		// the tree search tries the types largest first when any best arrangement will do, and in their order for the loop's winner
		int order = typeOrder(types, rotations);
		for (int t = 0; t < types; t++) {
			ints[order + t] = t;
		}
		if(anyBest) {
			for (int i = 1; i < types; i++) {
				int t = ints[order + i];
				int j = i - 1;
				while (j >= 0 && longs[ints[order + j]] < longs[t]) {
					ints[order + j + 1] = ints[order + j];
					j--;
				}
				ints[order + j + 1] = t;
			}
		}
		return new FlatProblem(ints, FlatBound.fill(ints, longs));
	}
}
