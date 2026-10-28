package com.github.skjolber.packing.packer.bruteforce.flat;

import static com.github.skjolber.packing.packer.bruteforce.flat.FlatLayout.POINT_INTS;

/**
 * The search of all box orders as one tree: a node is a prefix of placed boxes (each in a rotation, at its best point) and its children are the
 * remaining box types, in ascending order, in each of their rotations. Every prefix is placed once, where the loop over the permutations of
 * {@linkplain FlatSubtree} places it again for every permutation which starts with it.
 * <p>
 * The leaves are the same arrangements as the permutation loop finds: for a permutation, its boxes in order, each in a rotation, until a box fits no
 * point, the load limit or the limit of boxes. The winner is the arrangement with the most load volume, then load weight, then boxes, and among equal
 * ones the lexicographically first permutation (the permutation of a leaf is its prefix, the box which could not be placed, and the rest in ascending
 * order, which is the first permutation of the loop with that arrangement); among equal permutations the first rotations found. This is the winner of
 * the loop, which skips the permutations it can prove not to win and takes the first one on ties.
 * <p>
 * Pruning: a box is not placed when no completion with it next can load more than the best so far (the volume of the prefix and the box, plus the most
 * the remaining boxes can add within the load limits, which is the volume of their best sub-multiset, computed once per multiset), and once a permutation
 * places all boxes, prefixes after it in the order are not expanded.
 */
public final class FlatJointSearch {

	private FlatJointSearch() {
	}

	/**
	 * Search all box orders.
	 *
	 * @param problem the boxes of one container
	 * @param boundVolume a load volume which a better result must exceed, or zero
	 * @param maxPoints the most free points kept per level
	 * @return the winner (which places no box if none of the arrangements is worth placing), or null if a level needed more free points than maxPoints
	 */
	public static AcceleratorWinner search(FlatProblem problem, long boundVolume, int maxPoints) {
		final int[] p = problem.ints();
		final long[] pl = problem.longs();
		final int n = p[FlatProblem.N];
		final int types = p[FlatProblem.TYPES];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int minAreas = FlatProblem.minAreas(types);
		final int weights = FlatProblem.weights(types);
		final long maxLoadVolume = pl[FlatProblem.maxLoad(types)];
		final long maxLoadWeight = pl[FlatProblem.maxLoad(types) + 1];
		final int depthLimit = Math.min(p[FlatProblem.LIMIT], n);

		final FlatLayout layout = new FlatLayout(n, maxPoints);
		final int[] s = layout.newScratch();

		final int[] remaining = new int[types];
		for (int t = 0; t < types; t++) {
			remaining[t] = p[FlatProblem.HEADER + t];
		}
		final Bound bounds = new Bound(p, pl, types, maxLoadVolume, maxLoadWeight);

		// the state of the levels: the type and the rotation placed, the next type being tried and its next rotation, whether any rotation of it fit
		final int[] placedType = new int[n + 1];
		final int[] placedRotation = new int[n + 1];
		final int[] typeAt = new int[n + 1];
		final int[] rotationIndex = new int[n + 1];
		final boolean[] fitAt = new boolean[n + 1];
		final long[] volume = new long[n + 1];
		final long[] weight = new long[n + 1];
		final long[] minArea = new long[n + 1];
		final long[] minVolume = new long[n + 1];

		// the best arrangement
		boolean haveBest = false;
		int bestCount = 0;
		long bestVolume = 0L;
		long bestWeight = 0L;
		final int[] bestPermutation = new int[n];
		final int[] bestRotations = new int[n];
		final int[] candidate = new int[n];
		long nodes = 0L;

		minima(remaining, types, pl, minAreas, minArea, minVolume, 0);
		FlatPoints.clear(layout, s, 0, p[FlatProblem.CX], p[FlatProblem.CY], p[FlatProblem.CZ]);
		if(minArea[0] != 0L || minVolume[0] != 0L) {
			FlatPoints.filterMinimums(layout, s, 0, 0, minArea[0], minVolume[0]);
		}

		int level = 0;
		boolean descend = true;
		while (level >= 0) {
			if(descend) {
				descend = false;
				boolean expand = level < depthLimit;
				if(expand) {
					// no completion of this prefix can load more than the best
					long bound = Math.max(boundVolume, bestVolume);
					if(bound > 0L && volume[level] + bounds.maxFit(remaining) < bound) {
						expand = false;
					}
				} else if(level > 0) {
					// every box which may be placed is placed
					boolean better = isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, -1, remaining, types, volume[level], weight[level], candidate, n);
					if(better) {
						haveBest = true;
						bestCount = level;
						bestVolume = volume[level];
						bestWeight = weight[level];
						System.arraycopy(candidate, 0, bestPermutation, 0, n);
						System.arraycopy(placedRotation, 0, bestRotations, 0, level);
					}
				}
				if(!expand) {
					typeAt[level] = -1;
				} else {
					typeAt[level] = nextType(remaining, types, -1);
					rotationIndex[level] = 0;
					fitAt[level] = false;
				}
			}

			boolean placed = false;
			while (typeAt[level] != -1 && !placed) {
				final int t = typeAt[level];
				boolean skip = false;
				if(rotationIndex[level] == 0 && !fitAt[level]) {
					// the first rotation of the type: after a full arrangement only earlier permutations can win; a box over the load limits ends the arrangement
					if(bestCount == n && comparePrefix(placedType, level, t, bestPermutation) > 0) {
						skip = true;
					} else if(pl[t] > maxLoadVolume - volume[level] || pl[weights + t] > maxLoadWeight - weight[level]) {
						if(level > 0 && isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, t, remaining, types, volume[level], weight[level], candidate, n)) {
							haveBest = true;
							bestCount = level;
							bestVolume = volume[level];
							bestWeight = weight[level];
							System.arraycopy(candidate, 0, bestPermutation, 0, n);
							System.arraycopy(placedRotation, 0, bestRotations, 0, level);
						}
						skip = true;
					} else {
						// no completion with this box next can load more than the best: skip it before placing it, as the loop skips the order
						long bound = Math.max(boundVolume, bestVolume);
						if(bound > 0L) {
							remaining[t]--;
							long fit = bounds.maxFit(remaining);
							remaining[t]++;
							if(volume[level] + pl[t] + fit < bound) {
								skip = true;
							}
						}
					}
				}
				if(skip) {
					typeAt[level] = nextType(remaining, types, t);
					rotationIndex[level] = 0;
					fitAt[level] = false;
					continue;
				}

				final int firstRotation = p[rotationOffsets + t];
				final int rotationCount = p[rotationOffsets + t + 1] - firstRotation;
				final int frame = layout.frameOffset(level);
				int r = rotationIndex[level];
				int pointIndex = -1;
				int dx = 0;
				int dy = 0;
				int dz = 0;
				while (pointIndex == -1 && r < rotationCount) {
					int rotation = rotationValues + 3 * (firstRotation + r);
					dx = p[rotation];
					dy = p[rotation + 1];
					dz = p[rotation + 2];
					r++;
					pointIndex = FlatSearch.bestPoint(s, frame, dx, dy, dz);
				}
				rotationIndex[level] = r;
				if(pointIndex == -1) {
					if(!fitAt[level] && level > 0) {
						// no rotation of the box has a position: the arrangement ends at this box
						if(isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, t, remaining, types, volume[level], weight[level], candidate, n)) {
							haveBest = true;
							bestCount = level;
							bestVolume = volume[level];
							bestWeight = weight[level];
							System.arraycopy(candidate, 0, bestPermutation, 0, n);
							System.arraycopy(placedRotation, 0, bestRotations, 0, level);
						}
					}
					typeAt[level] = nextType(remaining, types, t);
					rotationIndex[level] = 0;
					fitAt[level] = false;
				} else {
					int point = frame + 1 + pointIndex * POINT_INTS;
					int x = s[point];
					int y = s[point + 1];
					int z = s[point + 2];
					FlatPoints.setPlacement(layout, s, 0, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
					int count = FlatPoints.add(layout, s, 0, level, pointIndex, level, minArea[level], minVolume[level]);
					if(count < 0) {
						return null;
					}
					nodes++;
					fitAt[level] = true;
					placedType[level] = t;
					placedRotation[level] = r - 1;
					remaining[t]--;
					int nextLevel = level + 1;
					volume[nextLevel] = volume[level] + pl[t];
					weight[nextLevel] = weight[level] + pl[weights + t];
					minima(remaining, types, pl, minAreas, minArea, minVolume, nextLevel);
					if(nextLevel < depthLimit && (minArea[nextLevel] != minArea[level] || minVolume[nextLevel] != minVolume[level])) {
						FlatPoints.filterMinimums(layout, s, 0, nextLevel, minArea[nextLevel], minVolume[nextLevel]);
					}
					level = nextLevel;
					descend = true;
					placed = true;
				}
			}
			if(!placed) {
				level--;
				if(level >= 0) {
					int t = placedType[level];
					remaining[t]++;
				}
			}
		}

		if(!haveBest) {
			return new AcceleratorWinner(new int[n], new int[n], 0, 0L, 0L, nodes, false);
		}
		return new AcceleratorWinner(bestPermutation, bestRotations, bestCount, bestVolume, bestWeight, nodes, bestCount == n);
	}

	/** the smallest area (in any rotation) and the smallest volume of the remaining boxes, for the level */
	private static void minima(int[] remaining, int types, long[] pl, int minAreas, long[] minArea, long[] minVolume, int level) {
		long area = Long.MAX_VALUE;
		long volume = Long.MAX_VALUE;
		for (int t = 0; t < types; t++) {
			if(remaining[t] > 0) {
				if(pl[minAreas + t] < area) {
					area = pl[minAreas + t];
				}
				if(pl[t] < volume) {
					volume = pl[t];
				}
			}
		}
		minArea[level] = area;
		minVolume[level] = volume;
	}

	private static int nextType(int[] remaining, int types, int after) {
		for (int t = after + 1; t < types; t++) {
			if(remaining[t] > 0) {
				return t;
			}
		}
		return -1;
	}

	/**
	 * The most load volume the remaining boxes can add: the largest volume of a sub-multiset of them within the load volume and weight left, which is
	 * what the first boxes of the best order of them can load. It depends only on the remaining multiset (the placed boxes are the rest), so it is
	 * computed once per multiset, by enumerating its sub-multisets.
	 */
	private static final class Bound {

		private final long[] pl;
		private final int types;
		private final int weights;
		private final long maxLoadVolume;
		private final long maxLoadWeight;
		private final long totalVolume;
		private final long totalWeight;
		private final int[] radix;
		private final long[] memo;
		private final int[] counts;

		Bound(int[] p, long[] pl, int types, long maxLoadVolume, long maxLoadWeight) {
			this.pl = pl;
			this.types = types;
			this.weights = FlatProblem.weights(types);
			this.maxLoadVolume = maxLoadVolume;
			this.maxLoadWeight = maxLoadWeight;
			radix = new int[types];
			counts = new int[types];
			int size = 1;
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				radix[t] = size;
				size *= p[FlatProblem.HEADER + t] + 1;
				volume += p[FlatProblem.HEADER + t] * pl[t];
				weight += p[FlatProblem.HEADER + t] * pl[weights + t];
			}
			totalVolume = volume;
			totalWeight = weight;
			memo = new long[size];
			java.util.Arrays.fill(memo, -1L);
		}

		long maxFit(int[] remaining) {
			int key = 0;
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				key += remaining[t] * radix[t];
				volume += remaining[t] * pl[t];
				weight += remaining[t] * pl[weights + t];
			}
			long result = memo[key];
			if(result == -1L) {
				// the load left for the remaining boxes: the placed ones are the rest
				long volumeLeft = maxLoadVolume - (totalVolume - volume);
				long weightLeft = maxLoadWeight - (totalWeight - weight);
				result = volumeLeft < 0L || weightLeft < 0L ? 0L : best(remaining, 0, volumeLeft, weightLeft);
				memo[key] = result;
			}
			return result;
		}

		/** the largest volume of a sub-multiset of the remaining boxes of the types from t on, within the limits */
		private long best(int[] remaining, int t, long volumeLeft, long weightLeft) {
			if(t == types) {
				return 0L;
			}
			long result = 0L;
			long v = pl[t];
			long w = pl[weights + t];
			for (int k = 0; k <= remaining[t]; k++) {
				long volume = k * v;
				long weight = k * w;
				if(volume > volumeLeft || weight > weightLeft) {
					break;
				}
				long candidate = volume + best(remaining, t + 1, volumeLeft - volume, weightLeft - weight);
				if(candidate > result) {
					result = candidate;
				}
			}
			return result;
		}
	}

	/** compare the prefix of the placed types followed by the type with the first level + 1 boxes of the permutation, lexicographically */
	private static int comparePrefix(int[] placedType, int level, int type, int[] permutation) {
		for (int i = 0; i < level; i++) {
			if(placedType[i] != permutation[i]) {
				return placedType[i] < permutation[i] ? -1 : 1;
			}
		}
		return Integer.compare(type, permutation[level]);
	}

	/**
	 * Whether the arrangement of the prefix (count boxes, with the given volume and weight) beats the best: more volume, then weight, then boxes; among
	 * equal ones the lexicographically first permutation, which for the arrangement is the prefix, the failing type (or none), and the remaining boxes in
	 * ascending order. That permutation is written to the candidate.
	 */
	private static boolean isBetter(boolean haveBest, int bestCount, long bestVolume, long bestWeight, int[] bestPermutation, int[] placedType, int count, int failingType, int[] remaining,
			int types, long volume, long weight, int[] candidate, int n) {
		// the permutation of the arrangement
		System.arraycopy(placedType, 0, candidate, 0, count);
		int index = count;
		if(failingType != -1) {
			candidate[index++] = failingType;
		}
		for (int t = 0; t < types; t++) {
			int k = remaining[t] - (t == failingType ? 1 : 0);
			for (int i = 0; i < k; i++) {
				candidate[index++] = t;
			}
		}
		if(!haveBest) {
			return true;
		}
		if(volume != bestVolume) {
			return volume > bestVolume;
		}
		if(weight != bestWeight) {
			return weight > bestWeight;
		}
		if(count != bestCount) {
			return count > bestCount;
		}
		for (int i = 0; i < n; i++) {
			if(candidate[i] != bestPermutation[i]) {
				return candidate[i] < bestPermutation[i];
			}
		}
		return false;
	}
}
