package com.github.skjolber.packing.packer.bruteforce.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.AbstractBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.AbstractBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResult;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager.FastBruteForceBoxStackValuePointComparator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * The search of all box orders of a container as one tree, with the result of the loop over the permutations of
 * {@link AbstractBruteForcePackager#pack}. The loop places every order from its first box; the tree places each prefix once, for all the orders
 * which start with it:
 *
 * <pre>
 *   loop:   a b c   a c b   b a c   b c a   c a b   c b a     box types a, b, c: each order is placed from its first box
 *
 *   tree:            root
 *                 /   |   \
 *                a    b    c          a node is a prefix of placed boxes, each in a rotation at a position; its children
 *               / \  / \  / \         are the remaining box types, in each rotation at each candidate position
 *              b   c a  c a  b
 *              |   | |  | |  |
 *              c   b c  a b  a        a leaf is an arrangement: the boxes of an order, until one has no position
 * </pre>
 *
 * The candidates of a box at a node are the ones the loop tries ({@link AbstractBruteForcePackager#nextPlacement}), on the same point calculator,
 * driven as the loop drives it, so the positions are the loop's.
 * <p>
 * The leaves are the same arrangements as the loop finds: the boxes of an order, each in a rotation at a position, until a box has no position, the
 * load limits or the limit of boxes. The best is compared as the loop compares ({@link com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator}):
 * the most load volume, then load weight, then boxes, and among equal ones the lexicographically first permutation (the permutation of a leaf is its
 * prefix, the box which could not be placed, and the rest in ascending order, which is the first permutation of the loop with that arrangement);
 * among equal permutations the first rotations and positions found. So the winner is the loop's.
 * <p>
 * The loop stops an order at its first complete path (every box which fits the load limits placed), and skips the orders which start with the boxes
 * of an arrangement which could not be continued. The orders through a prefix are a block of the lexicographic order, so the tree keeps the orders
 * settled that way per type prefix ({@link BruteForceTreeSettledOrders}), and expands a prefix only while some order through it is still open:
 *
 * <pre>
 *   orders:     a b c   a c b   b a c   b c a   c a b   c b a
 *   prefix a:   ^^^^^^^^^^^^^                                   the orders through a prefix are a block
 *
 *   a b c places every box, so the loop stops that order and skips a c b, which starts with the same boxes:
 *   settled:    x x x   x x x   . . .   . . .   . . .   . . .   a is settled, b and c are still open
 *
 *   b a cannot be continued (no position for a after b), so the loop skips every order which starts with b a:
 *   settled:    x x x   x x x   x x x   . . .   . . .   . . .   b a is settled, b c a is searched next
 * </pre>
 *
 * So each prefix of placements is visited once, for the orders the loop would still search there.
 * <p>
 * Pruning, none of which changes the winner: a box is not placed when no completion with it next can beat the best, by the volume, weight and
 * boxes of the prefix and the box plus the best sub-multiset of the remaining boxes within the load limits (computed once per multiset, see
 * {@link BruteForceTreeBound}):
 *
 * <pre>
 *   prefix a b:  volume 12   +   bound of the remaining boxes {c, d} within the load limits: 5   =   17,  below the best so far (18): not expanded
 * </pre>
 *
 * After an arrangement places all boxes, prefixes after it in the order are not expanded; the types are tried largest first when not all boxes fit,
 * so that a strong arrangement is found early (ties are decided by comparing permutations, so the order of visiting does not change the winner),
 * and once an arrangement places all boxes the search starts again in the lexicographic order, where the first full leaf settles everything after
 * it, so the search ends at the first order which places all boxes, as the loop does.
 * <p>
 * With box item groups the orders keep the groups in their sequence (each group's boxes are a block of the order, see
 * {@link DefaultBoxItemGroupPermutationRotationIterator}), so the children of a node are the remaining boxes of the first group with boxes
 * remaining, and a box must be insertable after the boxes of the groups before its own, as in the loop:
 *
 * <pre>
 *   groups {a, b} then {c}:      root
 *                               /    \
 *                              a      b         the first group first, in every order
 *                              |      |
 *                              b      a
 *                              |      |
 *                              c      c         then the next group
 * </pre>
 */
public final class BruteForceTreeSearch {

	/** the most multisets of the boxes (the product of the counts plus one over the types) for the tree search: the bounds are kept per multiset */
	static final long MAX_MULTISETS = 1L << 18;

	private BruteForceTreeSearch() {
	}

	/**
	 * @param insertAfterCounts the loop's, see {@link AbstractBruteForcePackager#getInsertAfterCounts}: with box item groups, per position the start
	 *        of its group's block
	 * @return whether the boxes of the iterator can be searched as a tree: not too many multisets of them, and the groups' box items in sequence
	 */
	public static boolean isSupported(AbstractBoxItemPermutationRotationIterator iterator, int[] insertAfterCounts) {
		int[] permutations = iterator.getPermutations();
		int[] counts = new int[iterator.getBoxItemStackValues().length];
		for (int value : permutations) {
			counts[value]++;
		}
		// the box item indexes must ascend through the sequence of the groups, so that the orders are lexicographic over the box item indexes
		int[] groupEnd = new int[counts.length];
		for (int t = 0; t < counts.length; t++) {
			groupEnd[t] = t + 1;
		}
		return groupEnds(permutations, insertAfterCounts, groupEnd) && BruteForceTreeMultisets.count(counts, groupEnd) <= MAX_MULTISETS;
	}

	/**
	 * @return per type, the end of its group's range of types (its own index plus one without boxes): the boxes of a group are a block of the
	 *         order, and the types ascend through the groups (see {@link #isSupported}), so the types of a group are a range
	 */
	static int[] groupEnds(int[] permutations, int[] insertAfterCounts, int types) {
		int[] groupEnd = new int[types];
		for (int t = 0; t < types; t++) {
			groupEnd[t] = t + 1;
		}
		groupEnds(permutations, insertAfterCounts, groupEnd);
		return groupEnd;
	}

	/**
	 * @param groupEnd per type, set to the end of its group's range of types (the types of a block of the permutation, in any order: the
	 *        permutation may be any order of its boxes)
	 * @return whether the ranges of the groups ascend through the sequence of the groups, so that the orders are lexicographic over the box item
	 *         indexes
	 */
	private static boolean groupEnds(int[] permutations, int[] insertAfterCounts, int[] groupEnd) {
		int n = permutations.length;
		int start = 0;
		int previousMax = -1;
		for (int i = 1; i <= n; i++) {
			if(i == n || (insertAfterCounts.length != 0 && insertAfterCounts[i] == i)) {
				int min = Integer.MAX_VALUE;
				int max = -1;
				for (int k = start; k < i; k++) {
					int t = permutations[k];
					if(t < min) {
						min = t;
					}
					if(t > max) {
						max = t;
					}
				}
				if(min <= previousMax) {
					return false;
				}
				previousMax = max;
				for (int t = min; t <= max; t++) {
					groupEnd[t] = max + 1;
				}
				start = i;
			}
		}
		return true;
	}

	/**
	 * @param candidates the candidate positions of a box at a node, and the free points: the packager's search
	 * @param insertAfterCounts with box item groups, per position the number of boxes of the groups before its own (empty otherwise)
	 * @return the result (empty if no arrangement places a box), which refers to the reused placements until materialized
	 */
	public static BruteForceIntermediatePackagerResult search(BruteForceTreeCandidates candidates, Placement[] placements, int placementCount, ContainerItem containerItem, int containerIndex,
			AbstractBoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, Container holder, int limit, long minLoadVolume, int[] insertAfterCounts)
			throws PackagerInterruptedException {
		return search(candidates, placements, placementCount, containerItem, containerIndex, iterator, interrupt, holder, limit, minLoadVolume, insertAfterCounts, BruteForceTreeWork.ROOT, null,
				null, null);
	}

	/**
	 * @param work the part of the search: the whole search, or a part stolen from another thread
	 * @param shared the best arrangement of the parts of a split search, for pruning, or null
	 * @param tables a holder of the tables of a thread's parts, or of a session's searches, or null: the tables are taken from it when it holds them
	 *        (see {@link BruteForceTreeTables#of}), and left in it otherwise
	 * @param stealable the state other threads steal work from, or null
	 * @see #search(BruteForceTreeCandidates, Placement[], int, ContainerItem, int, AbstractBoxItemPermutationRotationIterator, PackagerInterruptSupplier, Container, int, long, int[])
	 */
	public static BruteForceIntermediatePackagerResult search(BruteForceTreeCandidates candidates, Placement[] placements, int placementCount, ContainerItem containerItem, int containerIndex,
			AbstractBoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, Container holder, int limit, long minLoadVolume, int[] insertAfterCounts, BruteForceTreeWork work,
			BruteForceTreeSharedBest shared, BruteForceTreeTables[] tables, BruteForceTreeStealableState stealable) throws PackagerInterruptedException {
		final int n = iterator.length();
		final int depthLimit = Math.min(limit, n);
		final BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(n), containerIndex, iterator, candidates.utility != null);
		if(depthLimit == 0) {
			return result;
		}

		// the types: the box items, with their rotations, volume, weight and smallest area
		final BoxStackValue[][] stackValues = iterator.getBoxItemStackValues();
		final int types = stackValues.length;
		final int[] counts = new int[types];
		for (int value : iterator.getPermutations()) {
			counts[value]++;
		}
		final long[] volumes = new long[types];
		final long[] weights = new long[types];
		final long[] minAreas = new long[types];
		for (int t = 0; t < types; t++) {
			if(counts[t] > 0) {
				volumes[t] = stackValues[t][0].getBox().getVolume();
				weights[t] = stackValues[t][0].getBox().getWeight();
				long minArea = Long.MAX_VALUE;
				for (BoxStackValue stackValue : stackValues[t]) {
					if(stackValue.getArea() < minArea) {
						minArea = stackValue.getArea();
					}
				}
				minAreas[t] = minArea;
			}
		}
		// box item groups: the boxes of a group are a block of the order, so the types of a group are a range; the end of each type's range
		final int[] groupEnd = groupEnds(iterator.getPermutations(), insertAfterCounts, types);
		final long maxLoadVolume = holder.getMaxLoadVolume();
		final long maxLoadWeight = holder.getMaxLoadWeight();
		// the remaining boxes as a multiset, keyed compactly: the bound and the settled orders are kept per multiset
		BruteForceTreeTables sharedTables = BruteForceTreeTables.of(tables != null ? tables[0] : null, counts, volumes, weights, minAreas, maxLoadVolume, maxLoadWeight, groupEnd, n, shared);
		if(tables != null) {
			tables[0] = sharedTables;
		}
		final BruteForceTreeMultisets multisets = sharedTables.multisets;
		final BruteForceTreeBound bounds = sharedTables.bounds;
		final BruteForceTreeSettledOrders settled = sharedTables.settled;
		int remainingKey = multisets.fullKey;

		// the order of the types at a node: the largest first; the lexicographic order after a full arrangement
		final int[] volumeOrder = new int[types];
		for (int i = 0; i < types; i++) {
			volumeOrder[i] = i;
		}
		for (int i = 1; i < types; i++) {
			int t = volumeOrder[i];
			int j = i - 1;
			while (j >= 0 && volumes[volumeOrder[j]] < volumes[t]) {
				volumeOrder[j + 1] = volumeOrder[j];
				j--;
			}
			volumeOrder[j + 1] = t;
		}
		final int[] volumeRank = new int[types];
		for (int i = 0; i < types; i++) {
			volumeRank[volumeOrder[i]] = i;
		}
		final int[] lexOrder = new int[types];
		for (int i = 0; i < types; i++) {
			lexOrder[i] = i;
		}

		// the state of the levels
		final int[] remaining = counts.clone();
		final int[] typeAt = new int[n + 1];
		final boolean[] typeStarted = new boolean[n + 1];
		final int[] placedType = new int[n + 1];
		final int[] placedRotation = new int[n + 1];
		final boolean[] fit = new boolean[n + 1];
		final boolean[] typeSolved = new boolean[n + 1];
		final boolean[] nodeSolved = new boolean[n + 1];
		final boolean[] pushed = new boolean[n + 1];
		final long[] volume = new long[n + 1];
		final long[] weight = new long[n + 1];
		final int[] candidate = new int[n];
		// the rank of each level's prefix among the prefixes of its depth, of the children of the level's prefix, and of the level's box as a child
		final int[] rankAt = new int[n + 1];
		final int[] firstChildRank = new int[n + 1];
		final int[] childRank = new int[n + 1];
		// per level, the number of candidates of the type tried so far, and the end of the ranks of the types to try
		final int[] ordinal = new int[n + 1];
		final int[] typeLimit = new int[n + 1];
		final int[] prefix = work.prefixTypes;
		final int forced = prefix.length;
		final int[] prefixOrdinals = work.prefixOrdinals;

		// the best arrangement
		boolean haveBest = false;
		int bestCount = 0;
		long bestVolume = 0L;
		long bestWeight = 0L;
		final int[] bestPermutation = new int[n];
		final int[] bestRotations = new int[n];
		final SimplePoint3D[] bestPoints = new SimplePoint3D[n];

		// when all boxes fit the load limits the bound prunes nothing until an arrangement places them all, and the first such arrangement in
		// the lexicographic order wins (as for the loop): search in that order from the start
		final boolean anyBest = candidates.anyBest;
		int phase = bounds.allFit && minLoadVolume == 0L && !anyBest ? 1 : 0;
		int[] typeOrder = phase == 0 ? volumeOrder : lexOrder;
		int[] typeRank = phase == 0 ? volumeRank : lexOrder;
		if(stealable != null) {
			synchronized (stealable.lock) {
				stealable.typeOrder = typeOrder;
				stealable.depth = 0;
				stealable.nodes = 0;
				stealable.prefixLength = forced;
				stealable.active = true;
			}
		}

		candidates.start(holder, containerItem, n);
		candidates.setLimits(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
		int level = 0;
		int nodes = 0;
		boolean descend = true;
		while (level >= 0) {
			if(descend) {
				descend = false;
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				if(stealable != null && (++nodes & 255) == 0) {
					stealable.nodes = nodes;
					if(nodes == BruteForceTreeStealableState.STEAL_AFTER && stealable.thieves != null) {
						stealable.thieves.run();
						stealable.thieves = null;
					}
				}
				boolean expand = level < depthLimit;
				if(expand) {
					if(settled.isSettled(level, rankAt[level], remaining, remainingKey)) {
						// every order through this prefix is settled
						expand = false;
					} else if((haveBest || minLoadVolume > 0L || shared != null)
							&& isHopeless(bounds, shared, remaining, remainingKey, volume[level], weight[level], level, minLoadVolume, haveBest, bestVolume, bestWeight, bestCount, anyBest)) {
						// no completion of this prefix can beat the best
						settled.settle(level, rankAt, remaining, remainingKey);
						expand = false;
					}
				} else {
					// every box which may be placed is placed
					if(level > 0 && isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, -1, remaining, types, volume[level], weight[level], candidate, n)) {
						haveBest = true;
						bestCount = level;
						bestVolume = volume[level];
						bestWeight = weight[level];
						System.arraycopy(candidate, 0, bestPermutation, 0, n);
						if(shared != null) {
							shared.offer(bestVolume, bestWeight, bestCount);
						}
						System.arraycopy(placedRotation, 0, bestRotations, 0, level);
						for (int i = 0; i < level; i++) {
							bestPoints[i] = candidates.placedPoint(i);
						}
						if(bestCount == n && anyBest) {
							// all boxes placed: nothing beats it, and any such arrangement will do
							for (int l = level - 1; l >= 0; l--) {
								candidates.unplace(l, placements[l]);
								if(pushed[l]) {
									candidates.leave(l);
								}
							}
							break;
						}
						if(bestCount == n && phase == 0) {
							// all boxes placed: only an earlier order can still win, so search again in the lexicographic order, where the first full leaf
							// settles everything after it; the orders settled so far stay settled
							settled.settle(level, rankAt, remaining, remainingKey);
							phase = 1;
							typeOrder = lexOrder;
							typeRank = lexOrder;
							for (int l = level - 1; l >= 0; l--) {
								candidates.unplace(l, placements[l]);
								if(pushed[l]) {
									candidates.leave(l);
								}
							}
							System.arraycopy(counts, 0, remaining, 0, types);
							remainingKey = multisets.fullKey;
							candidates.start(holder, containerItem, n);
							candidates.setLimits(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
							level = 0;
							descend = true;
							continue;
						}
					}
					settled.settle(level, rankAt, remaining, remainingKey);
				}
				if(!expand) {
					// a leaf, or a settled prefix: every order through it is settled
					typeAt[level] = -1;
					nodeSolved[level] = true;
					pushed[level] = false;
				} else {
					firstChildRank[level] = settled.firstChildRank(level);
					typeLimit[level] = level == forced ? Math.min(work.endRank, types) : types;
					typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, -1, prefix, level, level == forced ? work.startRank : 0, typeLimit[level]);
					if(stealable != null && level < BruteForceTreeStealableState.STEAL_DEPTH) {
						synchronized (stealable.lock) {
							if(level > 0) {
								stealable.placedType[level - 1] = placedType[level - 1];
								stealable.placedOrdinal[level - 1] = ordinal[level - 1] - 1;
							}
							stealable.typeRank[level] = typeAt[level] == -1 ? -1 : typeRank[typeAt[level]];
							stealable.typeLimit[level] = level < forced ? -1 : typeLimit[level];
							stealable.depth = level + 1;
						}
					}
					typeStarted[level] = false;
					fit[level] = false;
					nodeSolved[level] = true;
					typeSolved[level] = false;
					// save the free points, so that each candidate of this level starts from them
					candidates.enter(level);
					pushed[level] = true;
				}
			}

			boolean placed = false;
			while (typeAt[level] != -1 && !placed) {
				final int t = typeAt[level];
				final int childKey = multisets.remove(remainingKey, remaining, t);
				if(!typeStarted[level]) {
					// the orders with the box next: settled already; after a full arrangement only earlier permutations can win; a box over the load
					// limits ends the arrangement; a hopeless box is not placed
					int rank = settled.childRank(firstChildRank[level], remaining, t);
					boolean skip = true;
					if(settled.isChildSettled(level, rank, remaining, t, childKey)) {
						// nothing
					} else if(bestCount == n && comparePrefix(placedType, level, t, bestPermutation) > 0) {
						settled.settleChild(level, rank, rankAt, remaining, t, childKey);
					} else if(volumes[t] > maxLoadVolume - volume[level] || weights[t] > maxLoadWeight - weight[level]) {
						if(level > 0 && isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, t, remaining, types, volume[level], weight[level], candidate, n)) {
							haveBest = true;
							bestCount = level;
							bestVolume = volume[level];
							bestWeight = weight[level];
							System.arraycopy(candidate, 0, bestPermutation, 0, n);
							if(shared != null) {
								shared.offer(bestVolume, bestWeight, bestCount);
							}
							System.arraycopy(placedRotation, 0, bestRotations, 0, level);
							for (int i = 0; i < level; i++) {
								bestPoints[i] = candidates.placedPoint(i);
							}
						}
						settled.settleChild(level, rank, rankAt, remaining, t, childKey);
					} else if(haveBest || minLoadVolume > 0L || shared != null) {
						remaining[t]--;
						boolean hopeless = isHopeless(bounds, shared, remaining, childKey, volume[level] + volumes[t], weight[level] + weights[t], level + 1, minLoadVolume, haveBest, bestVolume,
								bestWeight, bestCount, anyBest);
						remaining[t]++;
						if(hopeless) {
							settled.settleChild(level, rank, rankAt, remaining, t, childKey);
						} else {
							skip = false;
						}
					} else {
						skip = false;
					}
					if(skip) {
						typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, t, prefix, level, 0, typeLimit[level]);
						typeAt[level] = publishType(stealable, level, typeAt[level], typeRank, remaining, typeOrder, groupEnd, typeLimit);
						typeStarted[level] = false;
						fit[level] = false;
						typeSolved[level] = false;
						continue;
					}
					// the candidates of the box, in its first rotation, from the free points of the level; the limits of the boxes still to place
					typeStarted[level] = true;
					ordinal[level] = 0;
					childRank[level] = rank;
					settled.startType(level, remaining, t, remainingKey);
					candidates.setLimits(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
					candidates.startType(level, iterator.getBoxItemByIndex(t), stackValues[t], placements[level]);
				}

				Placement placement = placements[level];
				// with box item groups, the box must be insertable after the boxes of the groups before its own
				int insertAfterCount = insertAfterCounts.length == 0 ? 0 : insertAfterCounts[level];
				int pointIndex = candidates.next(level, iterator.getBoxItemByIndex(t), stackValues[t], placement, insertAfterCount);
				if(pointIndex == -1) {
					if(!fit[level] || !typeSolved[level]) {
						// no position for the box, or none of its candidates settled every order with it next: those orders are not complete on this path, so
						// the prefix must be tried on its other paths
						nodeSolved[level] = false;
					}
					if(!fit[level] && level > 0) {
						// no rotation of the box has a position: the arrangement ends at this box
						if(isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, t, remaining, types, volume[level], weight[level], candidate, n)) {
							haveBest = true;
							bestCount = level;
							bestVolume = volume[level];
							bestWeight = weight[level];
							System.arraycopy(candidate, 0, bestPermutation, 0, n);
							if(shared != null) {
								shared.offer(bestVolume, bestWeight, bestCount);
							}
							System.arraycopy(placedRotation, 0, bestRotations, 0, level);
							for (int i = 0; i < level; i++) {
								bestPoints[i] = candidates.placedPoint(i);
							}
						}
					}
					typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, t, prefix, level, 0, typeLimit[level]);
					typeAt[level] = publishType(stealable, level, typeAt[level], typeRank, remaining, typeOrder, groupEnd, typeLimit);
					typeStarted[level] = false;
					fit[level] = false;
					typeSolved[level] = false;
				} else if(level < forced && ordinal[level] < prefixOrdinals[level]) {
					// a stolen part: the candidates before the one placed at this level
					ordinal[level]++;
				} else {
					// place the box and continue with the next level
					int nextLevel = level + 1;
					remaining[t]--;
					remainingKey = childKey;
					volume[nextLevel] = volume[level] + volumes[t];
					weight[nextLevel] = weight[level] + weights[t];
					// when no box can follow, the placement is kept but the free space after it is never searched
					boolean last = nextLevel == depthLimit || !canPlaceAny(remaining, types, volumes, weights, maxLoadVolume - volume[nextLevel], maxLoadWeight - weight[nextLevel]);
					candidates.place(level, pointIndex, placement, last);
					if(!last) {
						candidates.setLimits(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
					}
					fit[level] = true;
					placedType[level] = t;
					placedRotation[level] = candidates.rotation;
					ordinal[level]++;
					rankAt[nextLevel] = childRank[level];
					level = nextLevel;
					descend = true;
					placed = true;
				}
			}
			if(!placed) {
				// the level is done: undo its push, and back at the level below, its placement
				boolean solved = nodeSolved[level];
				if(pushed[level]) {
					candidates.leave(level);
				}
				if(stealable != null && level < BruteForceTreeStealableState.STEAL_DEPTH) {
					synchronized (stealable.lock) {
						stealable.depth = level;
					}
				}
				level--;
				if(level >= 0) {
					int t = placedType[level];
					remaining[t]++;
					int placedKey = remainingKey;
					remainingKey = multisets.add(remainingKey, t);
					// remove the placement, and restore the free points to before it
					candidates.unplace(level, placements[level]);
					if(level < forced) {
						// a stolen part: the one candidate of the level
						typeAt[level] = -1;
					} else if(settled.exact ? settled.isChildSettled(level, childRank[level], remaining, t, placedKey) : solved) {
						// every order with the box next is settled: the loop stops an order at its first complete path, so the other candidates of the box
						// can only repeat arrangements of settled orders
						typeSolved[level] = true;
						typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, t, prefix, level, 0, typeLimit[level]);
						typeAt[level] = publishType(stealable, level, typeAt[level], typeRank, remaining, typeOrder, groupEnd, typeLimit);
						typeStarted[level] = false;
						fit[level] = false;
						typeSolved[level] = false;
					}
				}
			}
		}

		if(stealable != null) {
			synchronized (stealable.lock) {
				stealable.active = false;
				stealable.depth = 0;
			}
		}
		if(!haveBest) {
			return result;
		}
		if(iterator instanceof DefaultBoxItemPermutationRotationIterator defaultIterator) {
			defaultIterator.setPermutation(bestPermutation);
		} else {
			((DefaultBoxItemGroupPermutationRotationIterator)iterator).setPermutation(bestPermutation);
		}
		List<Point> points = new ArrayList<>(bestCount);
		for (int i = 0; i < bestCount; i++) {
			points.add(bestPoints[i]);
		}
		result.setState(points, AbstractBruteForcePackager.getState(iterator, bestRotations, bestCount), placements, placementCount);
		return result;
	}

	/** whether any remaining box fits the load left */
	private static boolean canPlaceAny(int[] remaining, int types, long[] volumes, long[] weights, long volumeLeft, long weightLeft) {
		for (int t = 0; t < types; t++) {
			if(remaining[t] > 0 && volumes[t] <= volumeLeft && weights[t] <= weightLeft) {
				return true;
			}
		}
		return false;
	}

	/**
	 * the next type with boxes remaining of the first group with boxes remaining (the smallest remaining type's), in the order of the types, after
	 * the given type (or the first, for -1)
	 */
	private static int nextType(int[] remaining, int[] typeOrder, int[] typeRank, int[] groupEnd, int after, int[] prefix, int level, int from, int limit) {
		if(level < prefix.length) {
			// a part of a split search: the orders which start with the prefix
			return after == -1 ? prefix[level] : -1;
		}
		int end = -1;
		for (int t = 0; t < remaining.length; t++) {
			if(remaining[t] > 0) {
				end = groupEnd[t];
				break;
			}
		}
		for (int i = after == -1 ? from : typeRank[after] + 1; i < limit; i++) {
			int t = typeOrder[i];
			if(remaining[t] > 0 && t < end) {
				return t;
			}
		}
		return -1;
	}

	/**
	 * Publish the next type of a level to the thieves, and take their limit: the types after the limit were stolen.
	 *
	 * @return the next type, or -1 if it was stolen
	 */
	private static int publishType(BruteForceTreeStealableState stealable, int level, int next, int[] typeRank, int[] remaining, int[] typeOrder, int[] groupEnd, int[] typeLimit) {
		if(stealable == null || level >= BruteForceTreeStealableState.STEAL_DEPTH) {
			return next;
		}
		synchronized (stealable.lock) {
			int limit = stealable.typeLimit[level];
			if(limit != -1 && limit < typeLimit[level]) {
				typeLimit[level] = limit;
				if(next != -1 && typeRank[next] >= limit) {
					next = -1;
				}
			}
			stealable.typeRank[level] = next == -1 ? -1 : typeRank[next];
		}
		return next;
	}

	/** {@link BruteForceTreeBound#isHopeless} against the better of the search's own best arrangement and the shared one */
	private static boolean isHopeless(BruteForceTreeBound bounds, BruteForceTreeSharedBest shared, int[] remaining, int key, long volume, long weight, int count, long boundVolume, boolean haveBest,
			long bestVolume, long bestWeight, int bestCount, boolean anyBest) {
		if(shared != null) {
			long[] best = shared.get();
			if(best != null && (!haveBest || BruteForceTreeBound.compare(best[0], best[1], (int)best[2], bestVolume, bestWeight, bestCount) > 0)) {
				return bounds.isHopeless(remaining, key, volume, weight, count, boundVolume, true, best[0], best[1], (int)best[2], anyBest);
			}
		}
		return bounds.isHopeless(remaining, key, volume, weight, count, boundVolume, haveBest, bestVolume, bestWeight, bestCount, anyBest);
	}

	/** @return the first order with the prefix: the prefix, then the remaining boxes by type (which keeps the groups in sequence) */
	static int[] firstOrder(AbstractBoxItemPermutationRotationIterator iterator, int[] prefix) {
		int[] permutations = iterator.getPermutations();
		int types = iterator.getBoxItemStackValues().length;
		int[] counts = new int[types];
		for (int value : permutations) {
			counts[value]++;
		}
		int[] order = new int[permutations.length];
		int index = 0;
		for (int t : prefix) {
			counts[t]--;
			order[index++] = t;
		}
		for (int t = 0; t < types; t++) {
			for (int i = 0; i < counts[t]; i++) {
				order[index++] = t;
			}
		}
		return order;
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
	private static boolean isBetter(boolean haveBest, int bestCount, long bestVolume, long bestWeight, int[] bestPermutation, int[] placedType, int count, int failingType,
			int[] remaining, int types, long volume, long weight, int[] candidate, int n) {
		boolean equal = false;
		if(haveBest) {
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
		if(!equal) {
			return true;
		}
		for (int i = 0; i < n; i++) {
			if(candidate[i] != bestPermutation[i]) {
				return candidate[i] < bestPermutation[i];
			}
		}
		return false;
	}

}
