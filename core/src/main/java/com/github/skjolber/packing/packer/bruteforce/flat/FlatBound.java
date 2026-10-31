package com.github.skjolber.packing.packer.bruteforce.flat;

import java.util.Arrays;

/**
 * The bound table of a {@linkplain FlatProblem}: for each multiset of remaining boxes, the most the remaining boxes can add to the arrangement of
 * the placed ones: the sub-multiset within the load volume and weight left with the most volume, then weight, then boxes. The placed boxes are the
 * rest of the boxes, so the load left is a function of the remaining multiset, and the table is computed once per problem, as {@code
 * BruteForceTreeBound} computes it once per multiset met.
 * <p>
 * A multiset is keyed in a mixed radix: the sum over the types of the remaining count times the radix of the type ({@link FlatProblem#radix}). The
 * table is left out when there are more than {@link FlatProblem#MAX_KEYS} multisets ({@link FlatProblem#KEYS} is then 0), and the search bounds a
 * node by the whole of the remaining boxes within the load left instead, which is looser.
 */
public final class FlatBound {

	private FlatBound() {
	}

	/**
	 * Fill the radix of each type in the ints, the number of keys, and the bound table in the longs.
	 *
	 * @param ints the ints of the problem, with the header and the counts filled
	 * @param longs the longs of the problem, with the volumes, weights and max loads filled, of the length without the table
	 * @return the longs, with the table appended when it is computed
	 */
	static long[] fill(int[] ints, long[] longs) {
		int types = ints[FlatProblem.TYPES];
		int rotations = ints[FlatProblem.ROTATIONS];
		int radix = FlatProblem.radix(types, rotations);
		long keys = 1L;
		for (int t = 0; t < types; t++) {
			ints[radix + t] = (int)keys;
			keys *= ints[FlatProblem.HEADER + t] + 1;
			if(keys > FlatProblem.MAX_KEYS) {
				ints[FlatProblem.KEYS] = 0;
				return longs;
			}
		}
		ints[FlatProblem.KEYS] = (int)keys;
		int weights = FlatProblem.weights(types);
		long maxLoadVolume = longs[FlatProblem.maxLoad(types)];
		long maxLoadWeight = longs[FlatProblem.maxLoad(types) + 1];
		long totalVolume = 0L;
		long totalWeight = 0L;
		for (int t = 0; t < types; t++) {
			totalVolume += ints[FlatProblem.HEADER + t] * longs[t];
			totalWeight += ints[FlatProblem.HEADER + t] * longs[weights + t];
		}

		int bounds = FlatProblem.bounds(types);
		long[] result = Arrays.copyOf(longs, bounds + 3 * (int)keys);
		int[] remaining = new int[types];
		int[] subset = new int[types];
		for (int key = 0; key < keys; key++) {
			long remainingVolume = 0L;
			long remainingWeight = 0L;
			for (int t = 0; t < types; t++) {
				remaining[t] = (key / ints[radix + t]) % (ints[FlatProblem.HEADER + t] + 1);
				remainingVolume += remaining[t] * longs[t];
				remainingWeight += remaining[t] * longs[weights + t];
			}
			// the placed boxes are the rest: the load left for the remaining ones
			long volumeLeft = maxLoadVolume - (totalVolume - remainingVolume);
			long weightLeft = maxLoadWeight - (totalWeight - remainingWeight);
			long bestVolume = 0L;
			long bestWeight = 0L;
			int bestCount = 0;
			if(volumeLeft >= 0L && weightLeft >= 0L) {
				// the sub-multisets of the remaining boxes as an odometer, with running sums
				Arrays.fill(subset, 0);
				long volume = 0L;
				long weight = 0L;
				int count = 0;
				boolean more = true;
				while (more) {
					if(volume <= volumeLeft && weight <= weightLeft
							&& (volume > bestVolume || (volume == bestVolume && (weight > bestWeight || (weight == bestWeight && count > bestCount))))) {
						bestVolume = volume;
						bestWeight = weight;
						bestCount = count;
					}
					int t = 0;
					while (t < types && subset[t] == remaining[t]) {
						volume -= subset[t] * longs[t];
						weight -= subset[t] * longs[weights + t];
						count -= subset[t];
						subset[t] = 0;
						t++;
					}
					if(t == types) {
						more = false;
					} else {
						subset[t]++;
						volume += longs[t];
						weight += longs[weights + t];
						count++;
					}
				}
			}
			result[bounds + 3 * key] = bestVolume;
			result[bounds + 3 * key + 1] = bestWeight;
			result[bounds + 3 * key + 2] = bestCount;
		}
		return result;
	}
}
