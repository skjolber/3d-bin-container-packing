package com.github.skjolber.packing.packer.bruteforce.flat;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;

/**
 * The boxes of one container and their rotations, as primitive arrays which the flat search reads and never changes. A type is a box item of the
 * iterator; the permutations are of types, as for the iterator.
 * <p>
 * Ints: the header ({@link #N}, {@link #TYPES}, the load size, the number of rotations, the limit and whether all boxes fit the container by volume and
 * weight), then the number of boxes of each type, then the first rotation of each type (and the end of the last one), then the dx, dy and dz of each
 * rotation.
 * Longs: the volume of each type, then the smallest rotation area of each type, then the weight of each type, then the max load volume and the max load
 * weight of the container.
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
	public static final int HEADER = 8;

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

		int[] ints = new int[rotationValues(types) + 3 * rotations];
		long[] longs = new long[maxLoad(types) + 2];
		ints[N] = n;
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
		return new FlatProblem(ints, longs);
	}
}
