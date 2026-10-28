package com.github.skjolber.packing.packer.bruteforce.flat;

import static com.github.skjolber.packing.packer.bruteforce.flat.FlatLayout.POINT_INTS;

/**
 * The work of one item of the joint tree search: all box orders which start with a prefix, searched as one tree, where a node is a prefix of placed
 * boxes (each in a rotation, at its best point) and its children are the remaining box types in each rotation. Every prefix is placed once, where the
 * permutation loop of {@linkplain FlatSubtree} places it again for every order which starts with it.
 * <p>
 * The leaves are the same arrangements as the loop finds: the boxes of an order, each in a rotation, until a box fits no point, the load limits or the
 * limit of boxes. The best arrangement is compared as the loop compares: the most load volume, then load weight, then boxes, and among equal ones the
 * lexicographically first permutation (the permutation of a leaf is its prefix, the box which could not be placed, and the rest in ascending order,
 * which is the first permutation of the loop with that arrangement); among equal permutations the first rotations found. So the winner of the items,
 * reduced as for the loop ({@link FlatItems#reduce()}), is the winner of the loop.
 * <p>
 * Pruning, none of which changes the winner: a box is not placed when no completion with it next can beat the best (the volume, weight and boxes of the
 * prefix and the box, plus the best sub-multiset of the remaining boxes within the load limits, computed once per multiset); when the subtree of a
 * rotation settles every order through it (each ends at the load limits, the limit of boxes or a hopeless prefix), the other rotations of the box at that
 * node are skipped, since the loop stops an order at its first complete rotation path and they could only repeat arrangements of settled orders; after
 * an arrangement places all boxes, prefixes after it in the order are not expanded; and the types are tried largest first, so that a strong arrangement
 * is found early (ties are decided by comparing permutations, so the order of visiting does not change the winner). Once an arrangement places all boxes,
 * only an earlier order can still win, and the search starts again in the lexicographic order: there the first full leaf settles everything after it, so
 * the search ends at the first order which places all boxes, as the loop does.
 * <p>
 * An item can pause after a number of nodes and be run again: the path of the search is in the output ints, and the frames of the path are made again
 * by placing it again. The output has the layout of {@linkplain FlatSubtree} ({@link FlatSubtree#OUT_STATUS} to the best rotations), followed by the state
 * of the search; {@link #outInts(int)} is its size.
 */
public final class FlatTreeItem {

	/** the state of the search, after the output of FlatSubtree: the depth of the path, whether there is a best, the phase, then {@link #LEVEL_INTS} ints per level */
	private static final int LEVEL_INTS = 7;
	private static final int L_TYPE = 0;
	private static final int L_ROTATION = 1;
	private static final int L_PLACED_TYPE = 2;
	private static final int L_PLACED_ROTATION = 3;
	private static final int L_FIT = 4;
	private static final int L_TYPE_SOLVED = 5;
	private static final int L_NODE_SOLVED = 6;

	private FlatTreeItem() {
	}

	/** @return the number of ints of the output of an item: the output of {@linkplain FlatSubtree}, then the state of the search */
	public static int outInts(int n) {
		return FlatSubtree.outInts(n) + 3 + LEVEL_INTS * (n + 1);
	}

	/**
	 * Search the orders of an item, from the start or from where it paused.
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param problemLongs the longs of the {@linkplain FlatProblem}
	 * @param depth the length of the prefixes
	 * @param prefixes the first permutation of each item, see {@link FlatPermutation#prefixes(int[], int, int)}
	 * @param item the item
	 * @param s the scratch of the work item
	 * @param base the offset of the work item in the ints of the scratch
	 * @param out the output ints, with the invariants of a zeroed array for an item which has not run
	 * @param outBase the offset of the item in the output ints
	 * @param outLongs the output longs
	 * @param outLongBase the offset of the item in the output longs
	 * @param boundVolume the load volume of the best arrangement of the other items (or of an earlier container): an arrangement which cannot beat it is
	 *        not searched for
	 * @param boundWeight the load weight of that arrangement
	 * @param boundCount the boxes of that arrangement
	 * @param boundItem the item which found it, so that an arrangement which can only equal it is not searched for by a later item (the earlier wins the
	 *        tie); {@link Integer#MAX_VALUE} for a bound which only a better arrangement beats
	 * @param budgetNodes the number of nodes after which to pause
	 * @param bound the bound of the problem ({@link #newBound(int[], long[])}), reused by the items and the runs of a thread, or null to make one for this run
	 * @return the status of the item
	 */
	public static int run(FlatLayout layout, int[] problem, long[] problemLongs, int depth, int[] prefixes, int item, int[] s, int base, int[] out, int outBase, long[] outLongs,
			int outLongBase, long boundVolume, long boundWeight, int boundCount, int boundItem, int budgetNodes, Bound bound) {
		final int[] p = problem;
		final long[] pl = problemLongs;
		final int n = p[FlatProblem.N];
		final int types = p[FlatProblem.TYPES];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int minAreas = FlatProblem.minAreas(types);
		final int weights = FlatProblem.weights(types);
		final long maxLoadVolume = pl[FlatProblem.maxLoad(types)];
		final long maxLoadWeight = pl[FlatProblem.maxLoad(types) + 1];
		final int depthLimit = Math.min(p[FlatProblem.LIMIT], n);
		final int prefix = item * n;

		int status = out[outBase + FlatSubtree.OUT_STATUS];
		if(status == FlatSubtree.DONE || status == FlatSubtree.OVERFLOW) {
			return status;
		}
		final int bestPermutation = outBase + FlatSubtree.outBestPermutation(n);
		final int bestRotations = outBase + FlatSubtree.outBestRotations(n);
		final int state = outBase + FlatSubtree.outInts(n);
		final int levels = state + 3;

		final Bound bounds = bound != null ? bound : newBound(p, pl);
		final boolean boundWinsTies = boundItem < item;
		// the order in which the types are tried at a node: by volume, the largest first
		final int[] volumeOrder = new int[types];
		for (int i = 0; i < types; i++) {
			volumeOrder[i] = i;
		}
		for (int i = 1; i < types; i++) {
			int t = volumeOrder[i];
			int j = i - 1;
			while (j >= 0 && pl[volumeOrder[j]] < pl[t]) {
				volumeOrder[j + 1] = volumeOrder[j];
				j--;
			}
			volumeOrder[j + 1] = t;
		}
		final int[] volumeRank = new int[types];
		for (int i = 0; i < types; i++) {
			volumeRank[volumeOrder[i]] = i;
		}
		// the lexicographic order, for the phase after a full arrangement: the loop stops at the first order which places all boxes, and in this order
		// the first full leaf settles everything after it, so the search ends there as the loop does
		final int[] lexOrder = new int[types];
		for (int i = 0; i < types; i++) {
			lexOrder[i] = i;
		}

		final int[] remaining = new int[types];
		for (int t = 0; t < types; t++) {
			remaining[t] = p[FlatProblem.HEADER + t];
		}
		final long[] volume = new long[n + 1];
		final long[] weight = new long[n + 1];
		final long[] minArea = new long[n + 1];
		final long[] minVolume = new long[n + 1];
		final int[] candidate = new int[n];

		minima(remaining, types, pl, minAreas, minArea, minVolume, 0);
		FlatPoints.clear(layout, s, base, p[FlatProblem.CX], p[FlatProblem.CY], p[FlatProblem.CZ]);
		if(minArea[0] != 0L || minVolume[0] != 0L) {
			FlatPoints.filterMinimums(layout, s, base, 0, minArea[0], minVolume[0]);
		}

		int level = 0;
		boolean descend = true;
		boolean haveBest;
		int bestCount;
		long bestVolume;
		long bestWeight;
		int phase;
		if(status == FlatSubtree.NOT_STARTED) {
			haveBest = false;
			bestCount = 0;
			bestVolume = 0L;
			bestWeight = 0L;
			out[outBase + FlatSubtree.OUT_COUNT] = 0;
			outLongs[outLongBase + FlatSubtree.OUT_VOLUME] = 0L;
			outLongs[outLongBase + FlatSubtree.OUT_WEIGHT] = 0L;
			// the largest boxes first, unless an arrangement which places all boxes is known already
			phase = boundCount == n ? 1 : 0;
			out[state + 2] = phase;
		} else {
			// paused: place the path again, which makes its frames
			phase = out[state + 2];
			haveBest = out[state + 1] != 0;
			bestCount = out[outBase + FlatSubtree.OUT_COUNT];
			bestVolume = outLongs[outLongBase + FlatSubtree.OUT_VOLUME];
			bestWeight = outLongs[outLongBase + FlatSubtree.OUT_WEIGHT];
			int pathDepth = out[state];
			for (int l = 0; l < pathDepth; l++) {
				int o = levels + l * LEVEL_INTS;
				int t = out[o + L_PLACED_TYPE];
				int r = out[o + L_PLACED_ROTATION];
				int rotation = rotationValues + 3 * (p[rotationOffsets + t] + r);
				int dx = p[rotation];
				int dy = p[rotation + 1];
				int dz = p[rotation + 2];
				int frame = base + layout.frameOffset(l);
				int pointIndex = FlatSearch.bestPoint(s, frame, dx, dy, dz);
				int point = frame + 1 + pointIndex * POINT_INTS;
				int x = s[point];
				int y = s[point + 1];
				int z = s[point + 2];
				FlatPoints.setPlacement(layout, s, base, l, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
				if(FlatPoints.add(layout, s, base, l, pointIndex, l, minArea[l], minVolume[l]) < 0) {
					out[outBase + FlatSubtree.OUT_STATUS] = FlatSubtree.OVERFLOW;
					return FlatSubtree.OVERFLOW;
				}
				remaining[t]--;
				volume[l + 1] = volume[l] + pl[t];
				weight[l + 1] = weight[l] + pl[weights + t];
				minima(remaining, types, pl, minAreas, minArea, minVolume, l + 1);
				if(l + 1 < depthLimit && (minArea[l + 1] != minArea[l] || minVolume[l + 1] != minVolume[l])) {
					FlatPoints.filterMinimums(layout, s, base, l + 1, minArea[l + 1], minVolume[l + 1]);
				}
			}
			level = pathDepth;
		}

		int[] typeOrder = phase == 0 ? volumeOrder : lexOrder;
		int[] typeRank = phase == 0 ? volumeRank : lexOrder;

		int nodes = 0;
		int result = -1;
		while (level >= 0) {
			final int o = levels + level * LEVEL_INTS;
			if(descend) {
				descend = false;
				boolean expand = level < depthLimit;
				if(expand) {
					// no completion of this prefix can beat the best
					if(haveBest || boundVolume > 0L) {
						if(bounds.isHopeless(remaining, volume[level], weight[level], level, boundVolume, boundWeight, boundCount, boundWinsTies, haveBest, bestVolume, bestWeight, bestCount)) {
							expand = false;
						}
					}
				} else if(level > 0) {
					// every box which may be placed is placed
					if(isBetter(haveBest, bestCount, bestVolume, bestWeight, out, bestPermutation, out, levels, level, -1, remaining, types, volume[level], weight[level], candidate, n)) {
						haveBest = true;
						bestCount = level;
						bestVolume = volume[level];
						bestWeight = weight[level];
						System.arraycopy(candidate, 0, out, bestPermutation, n);
						copyRotations(out, levels, level, bestRotations);
						if(bestCount == n && phase == 0) {
							// all boxes placed: only an earlier order can still win, so search again in the lexicographic order, where the first full leaf
							// settles everything after it
							phase = 1;
							out[state + 2] = 1;
							typeOrder = lexOrder;
							typeRank = lexOrder;
							for (int t = 0; t < types; t++) {
								remaining[t] = p[FlatProblem.HEADER + t];
							}
							minima(remaining, types, pl, minAreas, minArea, minVolume, 0);
							FlatPoints.clear(layout, s, base, p[FlatProblem.CX], p[FlatProblem.CY], p[FlatProblem.CZ]);
							if(minArea[0] != 0L || minVolume[0] != 0L) {
								FlatPoints.filterMinimums(layout, s, base, 0, minArea[0], minVolume[0]);
							}
							level = 0;
							descend = true;
							continue;
						}
					}
				}
				if(!expand) {
					// a leaf, or a hopeless prefix: every order through it is settled
					out[o + L_TYPE] = -1;
					out[o + L_NODE_SOLVED] = 1;
				} else {
					out[o + L_TYPE] = nextType(remaining, typeOrder, typeRank, prefixes, prefix, depth, level, -1);
					out[o + L_ROTATION] = 0;
					out[o + L_FIT] = 0;
					out[o + L_NODE_SOLVED] = 1;
					out[o + L_TYPE_SOLVED] = 0;
				}
			}

			boolean placed = false;
			while (out[o + L_TYPE] != -1 && !placed) {
				final int t = out[o + L_TYPE];
				boolean skip = false;
				if(out[o + L_ROTATION] == 0 && out[o + L_FIT] == 0) {
					// the first rotation of the type: after a full arrangement only earlier permutations can win; a box over the load limits ends the arrangement
					if(bestCount == n && comparePrefix(out, levels, level, t, out, bestPermutation) > 0) {
						skip = true;
					} else if(pl[t] > maxLoadVolume - volume[level] || pl[weights + t] > maxLoadWeight - weight[level]) {
						if(level > 0 && isBetter(haveBest, bestCount, bestVolume, bestWeight, out, bestPermutation, out, levels, level, t, remaining, types, volume[level], weight[level], candidate, n)) {
							haveBest = true;
							bestCount = level;
							bestVolume = volume[level];
							bestWeight = weight[level];
							System.arraycopy(candidate, 0, out, bestPermutation, n);
							copyRotations(out, levels, level, bestRotations);
						}
						skip = true;
					} else if(haveBest || boundVolume > 0L) {
						// no completion with this box next can beat the best: skip it before placing it, as the loop skips the order
						remaining[t]--;
						boolean hopeless = bounds.isHopeless(remaining, volume[level] + pl[t], weight[level] + pl[weights + t], level + 1, boundVolume, boundWeight, boundCount, boundWinsTies,
								haveBest, bestVolume, bestWeight, bestCount);
						remaining[t]++;
						if(hopeless) {
							skip = true;
						}
					}
				}
				if(skip) {
					// over the load limits, hopeless, or after the full arrangement: the orders through the box are settled
					out[o + L_TYPE] = nextType(remaining, typeOrder, typeRank, prefixes, prefix, depth, level, t);
					out[o + L_ROTATION] = 0;
					out[o + L_FIT] = 0;
					out[o + L_TYPE_SOLVED] = 0;
					continue;
				}

				final int firstRotation = p[rotationOffsets + t];
				final int rotationCount = p[rotationOffsets + t + 1] - firstRotation;
				final int frame = base + layout.frameOffset(level);
				int r = out[o + L_ROTATION];
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
				out[o + L_ROTATION] = r;
				if(pointIndex == -1) {
					if(out[o + L_FIT] == 0 || out[o + L_TYPE_SOLVED] == 0) {
						// no rotation of the box has a position, or none of them settled every order with it next: those orders are not complete on this
						// path, so the prefix must be tried on its other paths
						out[o + L_NODE_SOLVED] = 0;
					}
					if(out[o + L_FIT] == 0 && level > 0) {
						// no rotation of the box has a position: the arrangement ends at this box
						if(isBetter(haveBest, bestCount, bestVolume, bestWeight, out, bestPermutation, out, levels, level, t, remaining, types, volume[level], weight[level], candidate, n)) {
							haveBest = true;
							bestCount = level;
							bestVolume = volume[level];
							bestWeight = weight[level];
							System.arraycopy(candidate, 0, out, bestPermutation, n);
							copyRotations(out, levels, level, bestRotations);
						}
					}
					out[o + L_TYPE] = nextType(remaining, typeOrder, typeRank, prefixes, prefix, depth, level, t);
					out[o + L_ROTATION] = 0;
					out[o + L_FIT] = 0;
					out[o + L_TYPE_SOLVED] = 0;
				} else {
					int point = frame + 1 + pointIndex * POINT_INTS;
					int x = s[point];
					int y = s[point + 1];
					int z = s[point + 2];
					FlatPoints.setPlacement(layout, s, base, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
					int count = FlatPoints.add(layout, s, base, level, pointIndex, level, minArea[level], minVolume[level]);
					if(count < 0) {
						out[outBase + FlatSubtree.OUT_STATUS] = FlatSubtree.OVERFLOW;
						return FlatSubtree.OVERFLOW;
					}
					nodes++;
					out[o + L_FIT] = 1;
					out[o + L_PLACED_TYPE] = t;
					out[o + L_PLACED_ROTATION] = r - 1;
					remaining[t]--;
					int nextLevel = level + 1;
					volume[nextLevel] = volume[level] + pl[t];
					weight[nextLevel] = weight[level] + pl[weights + t];
					minima(remaining, types, pl, minAreas, minArea, minVolume, nextLevel);
					if(nextLevel < depthLimit && (minArea[nextLevel] != minArea[level] || minVolume[nextLevel] != minVolume[level])) {
						FlatPoints.filterMinimums(layout, s, base, nextLevel, minArea[nextLevel], minVolume[nextLevel]);
					}
					level = nextLevel;
					descend = true;
					placed = true;
					if(nodes >= budgetNodes) {
						// pause before entering the new level: the path so far is in the state
						out[state] = level;
						result = FlatSubtree.PAUSED;
						break;
					}
				}
			}
			if(result != -1) {
				break;
			}
			if(!placed) {
				boolean solved = out[o + L_NODE_SOLVED] != 0;
				level--;
				if(level >= 0) {
					int t = out[levels + level * LEVEL_INTS + L_PLACED_TYPE];
					remaining[t]++;
					if(solved) {
						// the subtree of the rotation settled every order with the box next: the loop stops an order at its first complete path, so the
						// other rotations of the box can only repeat arrangements of settled orders
						out[levels + level * LEVEL_INTS + L_ROTATION] = p[rotationOffsets + t + 1] - p[rotationOffsets + t];
						out[levels + level * LEVEL_INTS + L_TYPE_SOLVED] = 1;
					}
				}
			}
		}
		if(result == -1) {
			result = FlatSubtree.DONE;
		}

		out[state + 1] = haveBest ? 1 : 0;
		out[outBase + FlatSubtree.OUT_COUNT] = bestCount;
		out[outBase + FlatSubtree.OUT_FULL] = bestCount == n && haveBest ? 1 : 0;
		outLongs[outLongBase + FlatSubtree.OUT_VOLUME] = bestVolume;
		outLongs[outLongBase + FlatSubtree.OUT_WEIGHT] = bestWeight;
		outLongs[outLongBase + FlatSubtree.OUT_NODES] += nodes;
		out[outBase + FlatSubtree.OUT_STATUS] = result;
		return result;
	}

	private static void copyRotations(int[] out, int levels, int count, int bestRotations) {
		for (int l = 0; l < count; l++) {
			out[bestRotations + l] = out[levels + l * LEVEL_INTS + L_PLACED_ROTATION];
		}
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

	/**
	 * the next type with boxes remaining, in the order of the types, after the given type (or the first, for -1); at the levels of the prefix of the item
	 * the only type is the one of the prefix
	 */
	private static int nextType(int[] remaining, int[] typeOrder, int[] typeRank, int[] prefixes, int prefix, int depth, int level, int after) {
		if(level < depth) {
			return after == -1 ? prefixes[prefix + level] : -1;
		}
		for (int i = after == -1 ? 0 : typeRank[after] + 1; i < typeOrder.length; i++) {
			int t = typeOrder[i];
			if(remaining[t] > 0) {
				return t;
			}
		}
		return -1;
	}

	/** compare the prefix of the placed types followed by the type with the first level + 1 boxes of the permutation, lexicographically */
	private static int comparePrefix(int[] out, int levels, int level, int type, int[] permutation, int permutationBase) {
		for (int i = 0; i < level; i++) {
			int placed = out[levels + i * LEVEL_INTS + L_PLACED_TYPE];
			if(placed != permutation[permutationBase + i]) {
				return placed < permutation[permutationBase + i] ? -1 : 1;
			}
		}
		return Integer.compare(type, permutation[permutationBase + level]);
	}

	/**
	 * Whether the arrangement of the prefix (count boxes, with the given volume and weight) beats the best: more volume, then weight, then boxes; among
	 * equal ones the lexicographically first permutation, which for the arrangement is the prefix, the failing type (or none), and the remaining boxes in
	 * ascending order. That permutation is written to the candidate.
	 */
	private static boolean isBetter(boolean haveBest, int bestCount, long bestVolume, long bestWeight, int[] best, int bestPermutation, int[] out, int levels, int count, int failingType,
			int[] remaining, int types, long volume, long weight, int[] candidate, int n) {
		boolean equal = false;
		if(haveBest) {
			// the values first: the permutation is only needed when the arrangement is at least as good
			if(volume != bestVolume) {
				if(volume < bestVolume) {
					return false;
				}
			} else if(weight != bestWeight) {
				if(weight < bestWeight) {
					return false;
				}
			} else if(count != bestCount) {
				if(count < bestCount) {
					return false;
				}
			} else {
				equal = true;
			}
		}
		for (int i = 0; i < count; i++) {
			candidate[i] = out[levels + i * LEVEL_INTS + L_PLACED_TYPE];
		}
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
		if(!equal) {
			return true;
		}
		for (int i = 0; i < n; i++) {
			if(candidate[i] != best[bestPermutation + i]) {
				return candidate[i] < best[bestPermutation + i];
			}
		}
		return false;
	}

	/** @return the bound of a problem, for {@link #run}: the memo of the best fits of the remaining multisets, for one thread */
	public static Bound newBound(int[] problem, long[] problemLongs) {
		return new Bound(problem, problemLongs);
	}

	/**
	 * The most the remaining boxes can add: the sub-multiset of them within the load volume and weight left with the most volume, then weight, then
	 * boxes, which is what the first boxes of the best order of them can load. It depends only on the remaining multiset (the placed boxes are the
	 * rest), so it is computed once per multiset, by enumerating its sub-multisets. When all boxes fit the load limits there is nothing to compute:
	 * every prefix can be completed with all the remaining boxes.
	 */
	public static final class Bound {

		private final long[] pl;
		private final int types;
		private final int weights;
		private final boolean allFit;
		private final long maxLoadVolume;
		private final long maxLoadWeight;
		private final long totalVolume;
		private final long totalWeight;
		private final int[] radix;
		private final long[] fitVolume;
		private final long[] fitWeight;
		private final int[] fitCount;
		private long bestVolume;
		private long bestWeight;
		private int bestCount;

		private Bound(int[] p, long[] pl) {
			this.pl = pl;
			this.types = p[FlatProblem.TYPES];
			this.weights = FlatProblem.weights(types);
			this.allFit = p[FlatProblem.ALL_FIT] != 0;
			this.maxLoadVolume = pl[FlatProblem.maxLoad(types)];
			this.maxLoadWeight = pl[FlatProblem.maxLoad(types) + 1];
			radix = new int[types];
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
			if(allFit) {
				fitVolume = null;
				fitWeight = null;
				fitCount = null;
			} else {
				fitVolume = new long[size];
				fitWeight = new long[size];
				fitCount = new int[size];
				java.util.Arrays.fill(fitVolume, -1L);
			}
		}

		/**
		 * Whether a prefix with the volume, weight and count, plus the best fit of the remaining boxes, can beat neither the bound (or equal it, if the
		 * bound wins ties) nor the best arrangement of the item.
		 */
		boolean isHopeless(int[] remaining, long volume, long weight, int count, long boundVolume, long boundWeight, int boundCount, boolean boundWinsTies, boolean haveBest,
				long bestVolume, long bestWeight, int bestCount) {
			long v;
			long w;
			int c;
			if(allFit) {
				// every remaining box can be added
				v = totalVolume;
				w = totalWeight;
				c = count;
				for (int t = 0; t < types; t++) {
					c += remaining[t];
				}
			} else {
				int key = maxFit(remaining);
				v = volume + fitVolume[key];
				w = weight + fitWeight[key];
				c = count + fitCount[key];
			}
			int bound = compare(v, w, c, boundVolume, boundWeight, boundCount);
			if(bound < 0 || (bound == 0 && boundWinsTies)) {
				return true;
			}
			return haveBest && compare(v, w, c, bestVolume, bestWeight, bestCount) < 0;
		}

		/** @return the key of the best fit of the remaining multiset, computed if not known */
		private int maxFit(int[] remaining) {
			int key = 0;
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				key += remaining[t] * radix[t];
				volume += remaining[t] * pl[t];
				weight += remaining[t] * pl[weights + t];
			}
			if(fitVolume[key] == -1L) {
				// the load left for the remaining boxes: the placed ones are the rest
				long volumeLeft = maxLoadVolume - (totalVolume - volume);
				long weightLeft = maxLoadWeight - (totalWeight - weight);
				bestVolume = 0L;
				bestWeight = 0L;
				bestCount = 0;
				if(volumeLeft >= 0L && weightLeft >= 0L) {
					best(remaining, 0, volumeLeft, weightLeft, 0L, 0L, 0);
				}
				fitVolume[key] = bestVolume;
				fitWeight[key] = bestWeight;
				fitCount[key] = bestCount;
			}
			return key;
		}

		private static int compare(long volume, long weight, int count, long otherVolume, long otherWeight, int otherCount) {
			if(volume != otherVolume) {
				return volume < otherVolume ? -1 : 1;
			}
			if(weight != otherWeight) {
				return weight < otherWeight ? -1 : 1;
			}
			return Integer.compare(count, otherCount);
		}

		/** the sub-multisets of the remaining boxes of the types from t on, within the limits, keeping the best by volume, weight and count */
		private void best(int[] remaining, int t, long volumeLeft, long weightLeft, long volume, long weight, int count) {
			if(t == types) {
				if(volume > bestVolume || (volume == bestVolume && (weight > bestWeight || (weight == bestWeight && count > bestCount)))) {
					bestVolume = volume;
					bestWeight = weight;
					bestCount = count;
				}
				return;
			}
			long v = pl[t];
			long w = pl[weights + t];
			for (int k = 0; k <= remaining[t]; k++) {
				long kv = k * v;
				long kw = k * w;
				if(kv > volumeLeft || kw > weightLeft) {
					break;
				}
				best(remaining, t + 1, volumeLeft - kv, weightLeft - kw, volume + kv, weight + kw, count + k);
			}
		}
	}
}
