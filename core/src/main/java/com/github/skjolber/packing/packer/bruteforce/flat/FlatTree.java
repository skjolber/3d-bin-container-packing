package com.github.skjolber.packing.packer.bruteforce.flat;

import static com.github.skjolber.packing.packer.bruteforce.flat.FlatLayout.POINT_INTS;

/**
 * The work of one item of the tree search: all orders which start with a prefix of types, searched as one tree, where a node is a prefix of placed
 * boxes (each in a rotation, at the best free point of the rotation) and its children are the remaining types in each rotation, as {@code
 * BruteForceTreeSearch} does for the fast brute force packager. Each prefix of placements is placed once, where {@link FlatSubtree} places it again
 * for every order which starts with it. The method is static and uses primitive arrays only, so that many items can run at once.
 * <p>
 * The leaves are the arrangements of the loop over the orders: the boxes of an order, each in a rotation at a position, until a box has no position
 * or is over the load limits, or the limit of boxes. The best is the most load volume, then weight, then boxes; among equal ones, for the loop's
 * winner ({@link FlatProblem#ANY_BEST} 0), the lexicographically first order (the order of a leaf is its prefix, the box which could not be placed,
 * and the rest in ascending order), and the first found. When any arrangement of the best value will do, the types are tried largest first, ties are
 * pruned, and the first arrangement which places all boxes ends the item. For the loop's winner, the types are tried in their order, a prefix which
 * can at most tie is still searched, and after an arrangement which places all boxes only the orders before it are searched.
 * <p>
 * A node is not expanded, and a box is not placed, when the prefix (and the box) plus the best sub-multiset of the remaining boxes within the load
 * left ({@link FlatBound}) cannot beat the best; the bound of another item (or an earlier container) prunes likewise. Unlike the tree search of the
 * packager, the item keeps no table of settled orders: the search below a prefix is a depth first search which visits each prefix of types once.
 * With the free space bound on ({@link FlatSpace}), a node is also bounded by the box types which fit none of its free spaces.
 * <p>
 * An item pauses after a number of nodes and is run again on the same scratch to continue: its state is the scratch (the frames of free points of
 * the levels, the types and rotations of the levels, the remaining boxes) and the output ({@link FlatSubtree#outInts(int)} ints and
 * {@link FlatSubtree#OUT_LONGS} longs, laid out as for {@link FlatSubtree}, without the cursor).
 */
public final class FlatTree {

	/** the free space bounds a node only where this many more of the smallest remaining boxes fit the load left: a cut must save a subtree */
	static final int SPACE_BOXES = 3;
	/** the number of free space checks after which the bound is given up when fewer than one in {@link #SPACE_CUT_RATE} of them cut */
	static final int SPACE_CHECKS = 2048;
	static final int SPACE_CUT_RATE = 16;

	private FlatTree() {
	}

	/**
	 * Search the orders of an item, from its first node or from where it paused.
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param problemLongs the longs of the {@linkplain FlatProblem}
	 * @param depth the length of the prefixes
	 * @param prefixes the first permutation of each item, see {@link FlatPermutation#prefixes(int[], int, int)}
	 * @param item the item
	 * @param s the scratch of the work item, which must be the same scratch for all the runs of the item
	 * @param sl the long scratch of the work item
	 * @param base the offset of the work item in the ints of the scratch
	 * @param longBase the offset of the work item in the longs of the scratch
	 * @param out the output ints, with the invariants of a zeroed array for an item which has not run
	 * @param outBase the offset of the item in the output ints
	 * @param outLongs the output longs
	 * @param outLongBase the offset of the item in the output longs
	 * @param boundVolume a load volume which has been reached by an earlier container: a prefix which cannot load more is not searched
	 * @param sharedVolume the best arrangement of the other items (its volume, weight and count), which bounds this item as its own best does
	 * @param freeSpace whether to bound the nodes by their free space ({@link FlatSpace})
	 * @param budgetNodes the number of nodes after which to pause
	 * @return the status of the item, see {@link FlatSubtree}
	 */
	public static int run(FlatLayout layout, int[] problem, long[] problemLongs, int depth, int[] prefixes, int item, int[] s, long[] sl, int base, int longBase,
			int[] out, int outBase, long[] outLongs, int outLongBase, long boundVolume, long sharedVolume, long sharedWeight, int sharedCount, boolean freeSpace, int budgetNodes) {
		final int n = problem[FlatProblem.N];
		final int types = problem[FlatProblem.TYPES];
		final int rotations = problem[FlatProblem.ROTATIONS];
		final int limit = problem[FlatProblem.LIMIT];
		final boolean allFit = problem[FlatProblem.ALL_FIT] != 0;
		final boolean anyBest = problem[FlatProblem.ANY_BEST] != 0;
		final int keys = problem[FlatProblem.KEYS];
		final int counts = FlatProblem.HEADER;
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int typeOrder = FlatProblem.typeOrder(types, rotations);
		final int radix = FlatProblem.radix(types, rotations);
		final int minAreas = FlatProblem.minAreas(types);
		final int weights = FlatProblem.weights(types);
		final long maxLoadVolume = problemLongs[FlatProblem.maxLoad(types)];
		final long maxLoadWeight = problemLongs[FlatProblem.maxLoad(types) + 1];
		final int bounds = FlatProblem.bounds(types);
		final int depthLimit = limit < n ? limit : n;

		final int control = base + layout.control;
		final int typeAtBase = base + layout.typeAt;
		final int fitBase = base + layout.fit;
		final int placedTypeBase = base + layout.placedType;
		final int remainingBase = base + layout.remaining;
		final int spaceCheckedBase = base + layout.spaceChecked;
		final int rotationBase = base + layout.rotation;
		final int placedRotationBase = base + layout.placedRotation;
		final int minAreaBase = longBase + layout.minArea;
		final int minVolumeBase = longBase + layout.minVolume;
		final int placedVolumeBase = longBase + layout.placedVolume;
		final int placedWeightBase = longBase + layout.placedWeight;
		final int bestPermutation = outBase + FlatSubtree.outBestPermutation(n);
		final int bestRotations = outBase + FlatSubtree.outBestRotations(n);
		final int prefix = item * n;

		int status = out[outBase + FlatSubtree.OUT_STATUS];
		// one exit and no return in a loop: the OpenCL backend does not lower them correctly
		int result = status;
		boolean running = status != FlatSubtree.DONE && status != FlatSubtree.OVERFLOW;
		if(running && status == FlatSubtree.NOT_STARTED) {
			layout.reset(s, base);
			for (int t = 0; t < types; t++) {
				s[remainingBase + t] = problem[counts + t];
			}
			s[control + FlatLayout.C_TREE_LEVEL] = 0;
			s[control + FlatLayout.C_TREE_DESCEND] = 1;
			s[control + FlatLayout.C_TREE_KEY] = keys - 1;
			sl[placedVolumeBase] = 0L;
			sl[placedWeightBase] = 0L;
			out[outBase + FlatSubtree.OUT_COUNT] = 0;
			out[outBase + FlatSubtree.OUT_FULL] = 0;
			outLongs[outLongBase + FlatSubtree.OUT_VOLUME] = 0L;
			outLongs[outLongBase + FlatSubtree.OUT_WEIGHT] = 0L;
			FlatPoints.clear(layout, s, base, problem[FlatProblem.CX], problem[FlatProblem.CY], problem[FlatProblem.CZ]);
			FlatSpace.clear(layout, problem, s, base, problem[FlatProblem.CX], problem[FlatProblem.CY], problem[FlatProblem.CZ]);
			minimums(problemLongs, s, sl, remainingBase, minAreaBase, minVolumeBase, types, minAreas, 0);
			if(sl[minAreaBase] != 0L || sl[minVolumeBase] != 0L) {
				FlatPoints.filterMinimums(layout, s, base, 0, sl[minAreaBase], sl[minVolumeBase]);
			}
		}

		int level = s[control + FlatLayout.C_TREE_LEVEL];
		boolean descend = s[control + FlatLayout.C_TREE_DESCEND] != 0;
		int key = s[control + FlatLayout.C_TREE_KEY];
		int bestCount = out[outBase + FlatSubtree.OUT_COUNT];
		long bestVolume = outLongs[outLongBase + FlatSubtree.OUT_VOLUME];
		long bestWeight = outLongs[outLongBase + FlatSubtree.OUT_WEIGHT];
		// the best to beat for the bound: the best of the other items when it is better than this item's own
		int beatCount = bestCount;
		long beatVolume = bestVolume;
		long beatWeight = bestWeight;
		if(sharedCount > 0 && (bestCount == 0 || sharedVolume > bestVolume || (sharedVolume == bestVolume && (sharedWeight > bestWeight || (sharedWeight == bestWeight && sharedCount > bestCount))))) {
			beatCount = sharedCount;
			beatVolume = sharedVolume;
			beatWeight = sharedWeight;
		}
		int nodes = 0;
		while (running && level >= 0) {
			if(descend) {
				// a node: the prefix of the placed boxes of the levels below
				descend = false;
				if(nodes >= budgetNodes) {
					descend = true;
					result = FlatSubtree.PAUSED;
					running = false;
				} else {
					nodes++;
					long volume = sl[placedVolumeBase + level];
					long weight = sl[placedWeightBase + level];
					boolean expand = level < depthLimit && canPlaceAny(problem, problemLongs, s, remainingBase, types, weights, maxLoadVolume - volume, maxLoadWeight - weight);
					if(expand && (beatCount > 0 || boundVolume > 0L)
							&& isHopeless(problem, problemLongs, s, remainingBase, types, weights, allFit, keys, bounds, key, volume, weight, level, maxLoadVolume, maxLoadWeight, boundVolume,
									beatCount, beatVolume, beatWeight, anyBest)) {
						// no completion of this prefix can beat the best
						expand = false;
					} else if(!expand && level > 0) {
						// every box which may be placed is placed, or none of the remaining ones fits the load left: a leaf
						if(isBetter(problem, s, out, placedTypeBase, remainingBase, bestPermutation, types, n, level, -1, volume, weight, bestCount, bestVolume, bestWeight, anyBest)) {
							bestCount = level;
							bestVolume = volume;
							bestWeight = weight;
							if(beatCount == 0 || volume > beatVolume || (volume == beatVolume && (weight > beatWeight || (weight == beatWeight && level > beatCount)))) {
								beatCount = level;
								beatVolume = volume;
								beatWeight = weight;
							}
							offer(layout, s, out, outLongs, outBase, outLongBase, base, placedTypeBase, remainingBase, types, level, -1, volume, weight);
							if(level == n) {
								out[outBase + FlatSubtree.OUT_FULL] = 1;
								if(anyBest) {
									result = FlatSubtree.DONE;
									running = false;
								}
							}
						}
					}
					if(!expand) {
						s[typeAtBase + level] = -1;
					} else {
						s[typeAtBase + level] = level < depth ? prefixes[prefix + level] : nextType(problem, s, remainingBase, typeOrder, types, -1);
						s[rotationBase + level] = 0;
						s[fitBase + level] = 0;
						s[spaceCheckedBase + level] = 0;
					}
				}
			}

			if(running) {
				final int t = s[typeAtBase + level];
				if(t == -1) {
					// the node is done: back to the level below, where the next rotation of its type follows
					level--;
					if(level >= 0) {
						int placed = s[placedTypeBase + level];
						s[remainingBase + placed]++;
						key += problem[radix + placed];
					}
				} else {
					boolean skip = false;
					if(s[rotationBase + level] == 0 && s[fitBase + level] == 0) {
						// the box is new at this node: a box over the load limits ends the arrangement; after a full arrangement only earlier orders can
						// win; a hopeless box is not placed
						long volume = sl[placedVolumeBase + level];
						long weight = sl[placedWeightBase + level];
						if(problemLongs[t] > maxLoadVolume - volume || problemLongs[weights + t] > maxLoadWeight - weight) {
							if(level > 0 && isBetter(problem, s, out, placedTypeBase, remainingBase, bestPermutation, types, n, level, t, volume, weight, bestCount, bestVolume, bestWeight, anyBest)) {
								bestCount = level;
								bestVolume = volume;
								bestWeight = weight;
								if(beatCount == 0 || volume > beatVolume || (volume == beatVolume && (weight > beatWeight || (weight == beatWeight && level > beatCount)))) {
									beatCount = level;
									beatVolume = volume;
									beatWeight = weight;
								}
								offer(layout, s, out, outLongs, outBase, outLongBase, base, placedTypeBase, remainingBase, types, level, t, volume, weight);
							}
							skip = true;
						} else if(bestCount == n && !anyBest && comparePrefix(s, out, placedTypeBase, bestPermutation, level, t) > 0) {
							skip = true;
						} else if(beatCount > 0 || boundVolume > 0L) {
							s[remainingBase + t]--;
							skip = isHopeless(problem, problemLongs, s, remainingBase, types, weights, allFit, keys, bounds, key - problem[radix + t], volume + problemLongs[t],
									weight + problemLongs[weights + t], level + 1, maxLoadVolume, maxLoadWeight, boundVolume, beatCount, beatVolume, beatWeight, anyBest);
							s[remainingBase + t]++;
						}
					}
					if(skip) {
						s[typeAtBase + level] = level < depth ? -1 : nextType(problem, s, remainingBase, typeOrder, types, t);
						s[rotationBase + level] = 0;
						s[fitBase + level] = 0;
					} else {
						// the next rotation of the box which has a position
						final int firstRotation = problem[rotationOffsets + t];
						final int rotationCount = problem[rotationOffsets + t + 1] - firstRotation;
						final int frame = base + layout.frameOffset(level);
						int rotationIndex = s[rotationBase + level];
						int pointIndex = -1;
						int dx = 0;
						int dy = 0;
						int dz = 0;
						while (pointIndex == -1 && rotationIndex < rotationCount) {
							int rotation = rotationValues + 3 * (firstRotation + rotationIndex);
							dx = problem[rotation];
							dy = problem[rotation + 1];
							dz = problem[rotation + 2];
							rotationIndex++;
							pointIndex = FlatSearch.bestPoint(s, frame, dx, dy, dz);
						}
						s[rotationBase + level] = rotationIndex;

						if(pointIndex != -1 && freeSpace && s[spaceCheckedBase + level] == 0 && s[control + FlatLayout.C_SPACE_OFF] == 0 && level > 0 && (beatCount > 0 || boundVolume > 0L)
								&& maxLoadVolume - sl[placedVolumeBase + level] >= SPACE_BOXES * sl[minVolumeBase + level]) {
							// the node has a child to place: bounded by its free space once, when it is not a dead end; the bound is given up when
							// it cuts too few of the nodes checked for its cost
							s[spaceCheckedBase + level] = 1;
							s[control + FlatLayout.C_SPACE_CHECKS]++;
							if(FlatSpace.isHopeless(layout, problem, problemLongs, s, base, level, remainingBase, sl[placedVolumeBase + level], sl[placedWeightBase + level], maxLoadVolume,
									maxLoadWeight, boundVolume, beatCount, beatVolume, beatWeight, anyBest)) {
								// no completion of the node can beat the best: the level ends without its other candidates
								s[control + FlatLayout.C_SPACE_CUTS]++;
								pointIndex = -1;
								s[fitBase + level] = 1;
								s[typeAtBase + level] = -1;
							} else if((s[control + FlatLayout.C_SPACE_CHECKS] & (SPACE_CHECKS - 1)) == 0 && s[control + FlatLayout.C_SPACE_CUTS] * SPACE_CUT_RATE < s[control + FlatLayout.C_SPACE_CHECKS]) {
								s[control + FlatLayout.C_SPACE_OFF] = 1;
							}
						}
						if(pointIndex != -1) {
							// place the box and continue with the next level
							int point = frame + 1 + pointIndex * POINT_INTS;
							int x = s[point];
							int y = s[point + 1];
							int z = s[point + 2];
							int nextLevel = level + 1;
							s[remainingBase + t]--;
							key -= problem[radix + t];
							sl[placedVolumeBase + nextLevel] = sl[placedVolumeBase + level] + problemLongs[t];
							sl[placedWeightBase + nextLevel] = sl[placedWeightBase + level] + problemLongs[weights + t];
							s[placedTypeBase + level] = t;
							s[placedRotationBase + level] = rotationIndex - 1;
							s[fitBase + level] = 1;
							// when no box can follow, the free space after the box is never searched
							boolean last = nextLevel == depthLimit || !canPlaceAny(problem, problemLongs, s, remainingBase, types, weights, maxLoadVolume - sl[placedVolumeBase + nextLevel],
									maxLoadWeight - sl[placedWeightBase + nextLevel]);
							if(!last) {
								FlatPoints.setPlacement(layout, s, base, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
								FlatSpace.place(layout, s, base, level);
								int count = FlatPoints.add(layout, s, base, level, pointIndex, level, sl[minAreaBase + level], sl[minVolumeBase + level]);
								if(count < 0) {
									result = FlatSubtree.OVERFLOW;
									running = false;
								} else {
									minimums(problemLongs, s, sl, remainingBase, minAreaBase, minVolumeBase, types, minAreas, nextLevel);
									if(sl[minAreaBase + nextLevel] != sl[minAreaBase + level] || sl[minVolumeBase + nextLevel] != sl[minVolumeBase + level]) {
										FlatPoints.filterMinimums(layout, s, base, nextLevel, sl[minAreaBase + nextLevel], sl[minVolumeBase + nextLevel]);
									}
								}
							}
							level = nextLevel;
							descend = true;
						} else if(s[typeAtBase + level] != -1) {
							if(s[fitBase + level] == 0 && level > 0) {
								// no rotation of the box has a position: the arrangement ends at this box
								long volume = sl[placedVolumeBase + level];
								long weight = sl[placedWeightBase + level];
								if(isBetter(problem, s, out, placedTypeBase, remainingBase, bestPermutation, types, n, level, t, volume, weight, bestCount, bestVolume, bestWeight, anyBest)) {
									bestCount = level;
									bestVolume = volume;
									bestWeight = weight;
									if(beatCount == 0 || volume > beatVolume || (volume == beatVolume && (weight > beatWeight || (weight == beatWeight && level > beatCount)))) {
										beatCount = level;
										beatVolume = volume;
										beatWeight = weight;
									}
									offer(layout, s, out, outLongs, outBase, outLongBase, base, placedTypeBase, remainingBase, types, level, t, volume, weight);
								}
							}
							s[typeAtBase + level] = level < depth ? -1 : nextType(problem, s, remainingBase, typeOrder, types, t);
							s[rotationBase + level] = 0;
							s[fitBase + level] = 0;
						}
					}
				}
			}
		}

		if(running) {
			// every node of the item is searched
			result = FlatSubtree.DONE;
		}
		if(result == FlatSubtree.OVERFLOW) {
			layout.reset(s, base);
			s[control + FlatLayout.C_OVERFLOW] = 1;
		}
		s[control + FlatLayout.C_TREE_LEVEL] = level;
		s[control + FlatLayout.C_TREE_DESCEND] = descend ? 1 : 0;
		s[control + FlatLayout.C_TREE_KEY] = key;
		out[outBase + FlatSubtree.OUT_STATUS] = result;
		outLongs[outLongBase + FlatSubtree.OUT_NODES] += nodes;
		return result;
	}

	/** The smallest area and volume of the remaining boxes, for the level: the free points below them can hold none of the remaining boxes. */
	private static void minimums(long[] problemLongs, int[] s, long[] sl, int remainingBase, int minAreaBase, int minVolumeBase, int types, int minAreas, int level) {
		long minArea = Long.MAX_VALUE;
		long minVolume = Long.MAX_VALUE;
		for (int t = 0; t < types; t++) {
			if(s[remainingBase + t] > 0) {
				if(problemLongs[minAreas + t] < minArea) {
					minArea = problemLongs[minAreas + t];
				}
				if(problemLongs[t] < minVolume) {
					minVolume = problemLongs[t];
				}
			}
		}
		sl[minAreaBase + level] = minArea;
		sl[minVolumeBase + level] = minVolume;
	}

	/** @return the next type with boxes remaining after the given one in the type order (or the first, for -1), or -1 */
	private static int nextType(int[] problem, int[] s, int remainingBase, int typeOrder, int types, int after) {
		int index = 0;
		if(after != -1) {
			while (problem[typeOrder + index] != after) {
				index++;
			}
			index++;
		}
		int result = -1;
		while (result == -1 && index < types) {
			int t = problem[typeOrder + index];
			if(s[remainingBase + t] > 0) {
				result = t;
			}
			index++;
		}
		return result;
	}

	/** @return true if some remaining box fits the load left */
	private static boolean canPlaceAny(int[] problem, long[] problemLongs, int[] s, int remainingBase, int types, int weights, long volumeLeft, long weightLeft) {
		boolean result = false;
		for (int t = 0; t < types && !result; t++) {
			if(s[remainingBase + t] > 0 && problemLongs[t] <= volumeLeft && problemLongs[weights + t] <= weightLeft) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Whether the prefix (count boxes, with the given volume and weight, and the key of the remaining boxes) plus the most the remaining boxes can
	 * add can beat neither the bound volume nor the best: by the bound table, or without one by the whole of the remaining boxes within the load left.
	 */
	private static boolean isHopeless(int[] problem, long[] problemLongs, int[] s, int remainingBase, int types, int weights, boolean allFit, int keys, int bounds, int key, long volume,
			long weight, int count, long maxLoadVolume, long maxLoadWeight, long boundVolume, int bestCount, long bestVolume, long bestWeight, boolean anyBest) {
		long v;
		long w;
		int c;
		if(keys > 0 && !allFit) {
			v = volume + problemLongs[bounds + 3 * key];
			w = weight + problemLongs[bounds + 3 * key + 1];
			c = count + (int)problemLongs[bounds + 3 * key + 2];
		} else {
			long remainingVolume = 0L;
			long remainingWeight = 0L;
			c = count;
			for (int t = 0; t < types; t++) {
				int k = s[remainingBase + t];
				remainingVolume += k * problemLongs[t];
				remainingWeight += k * problemLongs[weights + t];
				c += k;
			}
			v = volume + (remainingVolume < maxLoadVolume - volume ? remainingVolume : maxLoadVolume - volume);
			w = weight + (remainingWeight < maxLoadWeight - weight ? remainingWeight : maxLoadWeight - weight);
		}
		boolean result = v < boundVolume;
		if(!result && bestCount > 0) {
			int compare;
			if(v != bestVolume) {
				compare = v < bestVolume ? -1 : 1;
			} else if(w != bestWeight) {
				compare = w < bestWeight ? -1 : 1;
			} else if(c != bestCount) {
				compare = c < bestCount ? -1 : 1;
			} else {
				compare = 0;
			}
			result = compare < (anyBest ? 1 : 0);
		}
		return result;
	}

	/** @return 1 if the prefix of the placed types followed by the type is after the first level + 1 boxes of the permutation, -1 if before, 0 if equal */
	private static int comparePrefix(int[] s, int[] out, int placedTypeBase, int permutation, int level, int type) {
		int result = 0;
		for (int i = 0; i < level && result == 0; i++) {
			if(s[placedTypeBase + i] != out[permutation + i]) {
				result = s[placedTypeBase + i] < out[permutation + i] ? -1 : 1;
			}
		}
		if(result == 0 && type != out[permutation + level]) {
			result = type < out[permutation + level] ? -1 : 1;
		}
		return result;
	}

	/**
	 * Whether the arrangement of the prefix (count boxes, with the given volume and weight) beats the best: more volume, then weight, then boxes;
	 * among equal ones, for the loop's winner, the lexicographically first order, which for the arrangement is the prefix, the failing type (or
	 * none), and the remaining boxes in ascending order. When any best arrangement will do, an equal one is not better.
	 */
	private static boolean isBetter(int[] problem, int[] s, int[] out, int placedTypeBase, int remainingBase, int bestPermutation, int types, int n, int count, int failingType,
			long volume, long weight, int bestCount, long bestVolume, long bestWeight, boolean anyBest) {
		boolean result;
		boolean equal = false;
		if(bestCount == 0) {
			result = true;
		} else if(volume != bestVolume) {
			result = volume > bestVolume;
		} else if(weight != bestWeight) {
			result = weight > bestWeight;
		} else if(count != bestCount) {
			result = count > bestCount;
		} else {
			equal = true;
			result = !anyBest;
		}
		if(equal && result) {
			// the order of the arrangement against the order of the best, lexicographically, without writing it
			int compare = 0;
			int index = 0;
			while (compare == 0 && index < count) {
				compare = compareTypes(s[placedTypeBase + index], out[bestPermutation + index]);
				index++;
			}
			if(compare == 0 && failingType != -1 && index < n) {
				compare = compareTypes(failingType, out[bestPermutation + index]);
				index++;
			}
			// the remaining boxes in ascending order: taken of the type so far, of those remaining (less the failing one)
			int t = 0;
			int taken = 0;
			while (compare == 0 && index < n) {
				while (taken == s[remainingBase + t] - (t == failingType ? 1 : 0)) {
					t++;
					taken = 0;
				}
				compare = compareTypes(t, out[bestPermutation + index]);
				taken++;
				index++;
			}
			result = compare < 0;
		}
		return result;
	}

	private static int compareTypes(int a, int b) {
		return a == b ? 0 : (a < b ? -1 : 1);
	}

	/**
	 * Keep the arrangement of the prefix as the best: its order (the prefix, the failing type if any, and the remaining boxes in ascending order),
	 * its rotations, count, volume and weight.
	 */
	private static void offer(FlatLayout layout, int[] s, int[] out, long[] outLongs, int outBase, int outLongBase, int base, int placedTypeBase, int remainingBase, int types,
			int count, int failingType, long volume, long weight) {
		final int n = layout.n;
		out[outBase + FlatSubtree.OUT_COUNT] = count;
		outLongs[outLongBase + FlatSubtree.OUT_VOLUME] = volume;
		outLongs[outLongBase + FlatSubtree.OUT_WEIGHT] = weight;
		int from = base + layout.placedRotation;
		int to = outBase + FlatSubtree.outBestRotations(n);
		for (int i = 0; i < count; i++) {
			out[to + i] = s[from + i];
		}
		int order = outBase + FlatSubtree.outBestPermutation(n);
		int index = 0;
		for (int i = 0; i < count; i++) {
			out[order + index] = s[placedTypeBase + i];
			index++;
		}
		if(failingType != -1) {
			out[order + index] = failingType;
			index++;
		}
		for (int t = 0; t < types; t++) {
			int k = s[remainingBase + t] - (t == failingType ? 1 : 0);
			for (int i = 0; i < k; i++) {
				out[order + index] = t;
				index++;
			}
		}
	}
}
