package com.github.skjolber.packing.packer.bruteforce;

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
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager.FastBruteForceBoxStackValuePointComparator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;
/**
 * The search of all box orders of a container as one tree, with the result of the loop over the permutations of
 * {@link AbstractBruteForcePackager#pack}: a node is a prefix of placed boxes (each in a rotation, at a position), its children are the remaining
 * box types in each rotation at each candidate position. Every prefix is placed once, where the loop places it again for every order which starts
 * with it. The candidates of a box at a node are the ones the loop tries ({@link AbstractBruteForcePackager#nextPlacement}), on the same point
 * calculator, driven as the loop drives it, so the positions are the loop's.
 * <p>
 * The leaves are the same arrangements as the loop finds: the boxes of an order, each in a rotation at a position, until a box has no position, the
 * load limits or the limit of boxes. The best is compared as the loop compares ({@link com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator}):
 * the most load volume, then load weight, then boxes, and among equal ones the lexicographically first permutation (the permutation of a leaf is its
 * prefix, the box which could not be placed, and the rest in ascending order, which is the first permutation of the loop with that arrangement);
 * among equal permutations the first rotations and positions found. So the winner is the loop's.
 * <p>
 * The loop stops an order at its first complete path (every box which fits the load limits placed), and skips the orders which start with the boxes
 * of an arrangement which could not be continued. The tree keeps the orders settled that way per type prefix ({@link SettledOrders}: the orders
 * through a prefix are a block of the lexicographic order), and expands a prefix only while some order through it is still open. So each prefix of
 * placements is visited once, for the orders the loop would still search there.
 * <p>
 * Pruning, none of which changes the winner: a box is not placed when no completion with it next can beat the best (the volume, weight and boxes of
 * the prefix and the box, plus the best sub-multiset of the remaining boxes within the load limits, computed once per multiset); after an
 * arrangement places all boxes, prefixes after it in the order are not expanded; the types are tried largest first when not all boxes fit, so that a
 * strong arrangement is found early (ties are decided by comparing permutations, so the order of visiting does not change the winner), and once an
 * arrangement places all boxes the search starts again in the lexicographic order, where the first full leaf settles everything after it, so the
 * search ends at the first order which places all boxes, as the loop does.
 * <p>
 * With box item groups the orders keep the groups in their sequence (each group's boxes are a block of the order, see
 * {@link DefaultBoxItemGroupPermutationRotationIterator}), so the children of a node are the remaining boxes of the first group with boxes
 * remaining, and a box must be insertable after the boxes of the groups before its own, as in the loop.
 */
final class BruteForceTreeSearch {

	/** the most multisets of the boxes (the product of the counts plus one over the types) for the tree search: the bounds are kept per multiset */
	static final long MAX_MULTISETS = 1L << 18;

	private BruteForceTreeSearch() {
	}

	/**
	 * @param insertAfterCounts the loop's, see {@link AbstractBruteForcePackager#getInsertAfterCounts}: with box item groups, per position the start
	 *        of its group's block
	 * @return whether the boxes of the iterator can be searched as a tree: not too many multisets of them, and the groups' box items in sequence
	 */
	static boolean isSupported(AbstractBoxItemPermutationRotationIterator iterator, int[] insertAfterCounts) {
		int[] permutations = iterator.getPermutations();
		int[] counts = new int[iterator.getBoxItemStackValues().length];
		for (int value : permutations) {
			counts[value]++;
		}
		if(insertAfterCounts.length != 0) {
			// the box item indexes must ascend through the sequence of the groups, so that the orders are lexicographic over the box item indexes
			// (the first permutation lists each group's box items in ascending order)
			int[] expected = permutations.clone();
			int start = 0;
			for (int i = 1; i <= permutations.length; i++) {
				if(i == permutations.length || insertAfterCounts[i] == i) {
					Arrays.sort(expected, start, i);
					start = i;
				}
			}
			for (int i = 1; i < expected.length; i++) {
				if(expected[i] < expected[i - 1]) {
					return false;
				}
			}
		}
		return Multisets.count(counts, groupEnds(permutations, insertAfterCounts, counts.length)) <= MAX_MULTISETS;
	}

	/**
	 * @return per type, the end of its group's range of types (its own index plus one without boxes): the boxes of a group are a block of the
	 *         order, and the types ascend through the groups (see {@link #isSupported}), so the types of a group are a range
	 */
	private static int[] groupEnds(int[] permutations, int[] insertAfterCounts, int types) {
		int n = permutations.length;
		int[] groupEnd = new int[types];
		for (int t = 0; t < types; t++) {
			groupEnd[t] = t + 1;
		}
		// the block start..i of the permutation holds a group's types (in any order: the permutation may be a unit's first order)
		int[] sorted = permutations.clone();
		int start = 0;
		for (int i = 1; i <= n; i++) {
			if(i == n || (insertAfterCounts.length != 0 && insertAfterCounts[i] == i)) {
				Arrays.sort(sorted, start, i);
				int end = sorted[i - 1] + 1;
				for (int k = start; k < i; k++) {
					groupEnd[sorted[k]] = end;
				}
				start = i;
			}
		}
		return groupEnd;
	}

	/**
	 * @param candidates the candidate positions of a box at a node, and the free points: the packager's search
	 * @param insertAfterCounts with box item groups, per position the number of boxes of the groups before its own (empty otherwise)
	 * @return the result (empty if no arrangement places a box), which refers to the reused placements until materialized
	 */
	static BruteForceIntermediatePackagerResult search(Candidates candidates, Placement[] placements, int placementCount, ContainerItem containerItem, int containerIndex,
			AbstractBoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, Container holder, int limit, long minLoadVolume, int[] insertAfterCounts)
			throws PackagerInterruptedException {
		return search(candidates, placements, placementCount, containerItem, containerIndex, iterator, interrupt, holder, limit, minLoadVolume, insertAfterCounts, NO_PREFIX, null);
	}

	private static final int[] NO_PREFIX = new int[0];

	/**
	 * @param prefix the types of the first boxes of the orders to search (empty for all orders): a unit of a search split between threads
	 * @param shared the best arrangement of the units of a split search, for pruning, or null
	 * @see #search(Candidates, Placement[], int, ContainerItem, int, AbstractBoxItemPermutationRotationIterator, PackagerInterruptSupplier, Container, int, long, int[])
	 */
	static BruteForceIntermediatePackagerResult search(Candidates candidates, Placement[] placements, int placementCount, ContainerItem containerItem, int containerIndex,
			AbstractBoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, Container holder, int limit, long minLoadVolume, int[] insertAfterCounts, int[] prefix,
			SharedBest shared) throws PackagerInterruptedException {
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
		final Multisets multisets = new Multisets(counts, groupEnd);
		final Bound bounds = new Bound(counts, volumes, weights, maxLoadVolume, maxLoadWeight, multisets.keys);
		final SettledOrders settled = new SettledOrders(counts, n, multisets, groupEnd);
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
		int phase = bounds.allFit && minLoadVolume == 0L ? 1 : 0;
		int[] typeOrder = phase == 0 ? volumeOrder : lexOrder;
		int[] typeRank = phase == 0 ? volumeRank : lexOrder;

		candidates.start(holder, containerItem, n);
		candidates.setLimits(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
		int level = 0;
		boolean descend = true;
		while (level >= 0) {
			if(descend) {
				descend = false;
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				boolean expand = level < depthLimit;
				if(expand) {
					if(settled.isSettled(level, rankAt[level], remaining, remainingKey)) {
						// every order through this prefix is settled
						expand = false;
					} else if((haveBest || minLoadVolume > 0L || shared != null)
							&& isHopeless(bounds, shared, remaining, remainingKey, volume[level], weight[level], level, minLoadVolume, haveBest, bestVolume, bestWeight, bestCount)) {
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
					typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, -1, prefix, level);
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
								bestWeight, bestCount);
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
						typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, t, prefix, level);
						typeStarted[level] = false;
						fit[level] = false;
						typeSolved[level] = false;
						continue;
					}
					// the candidates of the box, in its first rotation, from the free points of the level; the limits of the boxes still to place
					typeStarted[level] = true;
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
					typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, t, prefix, level);
					typeStarted[level] = false;
					fit[level] = false;
					typeSolved[level] = false;
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
				level--;
				if(level >= 0) {
					int t = placedType[level];
					remaining[t]++;
					int placedKey = remainingKey;
					remainingKey = multisets.add(remainingKey, t);
					// remove the placement, and restore the free points to before it
					candidates.unplace(level, placements[level]);
					if(settled.exact ? settled.isChildSettled(level, childRank[level], remaining, t, placedKey) : solved) {
						// every order with the box next is settled: the loop stops an order at its first complete path, so the other candidates of the box
						// can only repeat arrangements of settled orders
						typeSolved[level] = true;
						typeAt[level] = nextType(remaining, typeOrder, typeRank, groupEnd, t, prefix, level);
						typeStarted[level] = false;
						fit[level] = false;
						typeSolved[level] = false;
					}
				}
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

	/**
	 * The candidate positions of a box at a node, and the free points of the nodes: a packager's own search of an order, so that the tree places
	 * the boxes as the packager's loop does. The levels are the depths of the tree; a level's box is tried in each rotation, at the candidates of
	 * the rotation ({@link #next}); the placement of a level is removed when the tree returns to it ({@link #unplace}).
	 */
	abstract static class Candidates {

		protected final Stack stack;
		/** with box load limits, the loads of the placed boxes; otherwise null */
		protected final LoadPlacementUtility utility;
		/** the candidate found by {@link #next}: its point, its rotation (index of the box's stack values) and with load limits its supported area */
		protected SimplePoint3D point;
		protected int rotation;
		protected long supportedArea;

		protected Candidates(Stack stack, LoadPlacementUtility utility) {
			this.stack = stack;
			this.utility = utility;
		}

		/** the start of a search (or a restart): the container's free points, no placements */
		abstract void start(Container holder, ContainerItem containerItem, int n);

		/** the smallest area and volume of the boxes still to place: free points which cannot hold them are dropped */
		abstract void setLimits(long minArea, long minVolume);

		/** a node at the level is expanded: its free points are the ones each candidate starts from */
		abstract void enter(int level);

		/** the level's box is tried: its first rotation, and the placement for it */
		abstract void startType(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement);

		/**
		 * The next candidate of the level's box, in the current rotation or the next ones: the placement's stack value is the rotation, and
		 * {@link #point}, {@link #rotation} and {@link #supportedArea} describe the candidate.
		 *
		 * @param insertAfterCount the number of boxes placed before the box, which it must be insertable after
		 * @return the point index, or -1 if the box has no more candidates in any rotation
		 */
		abstract int next(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement, int insertAfterCount);

		/** place the candidate at the level; when last, the free space after it is never searched */
		void place(int level, int pointIndex, Placement placement, boolean last) {
			placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
			if(utility != null) {
				placement.clearLoad();
				placement.setIndex(stack.size());
				placement.setSupportedArea(supportedArea);
			}
			add(level, pointIndex, placement, last);
			stack.add(placement);
			if(utility != null) {
				utility.saveLoads(level);
				utility.addSupportersLoad(placement);
			}
		}

		/** the placement at its point, to the free points */
		abstract void add(int level, int pointIndex, Placement placement, boolean last);

		/** remove the placement of the level (the last placed), and restore the free points to before it */
		void unplace(int level, Placement placement) {
			if(utility != null) {
				placement.removeSupporteesAbove();
				for (PlacementLoad placementLoad : placement.getSupporters()) {
					placementLoad.getPlacement().removeLastSupportee();
				}
				placement.clearLoad();
				utility.restoreLoads(level);
			}
			stack.remove(stack.size() - 1);
			restore(level);
		}

		/** restore the free points to the ones the level's candidates start from */
		abstract void restore(int level);

		/** the node at the level is done (after {@link #enter}) */
		abstract void leave(int level);

		/** @return the point of the placement of the level */
		abstract SimplePoint3D placedPoint(int level);

		protected static void clearToSize(DefaultPointCalculator3D pointCalculator, Container holder, ContainerItem containerItem) {
			pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
			if(containerItem.getInitialPoints() != null) {
				pointCalculator.setPoints(containerItem.getInitialPoints());
				pointCalculator.clear();
			}
		}
	}

	/**
	 * The candidates of {@link BruteForcePackager}: all free points where the box fits (or the point filter's), in index order, the rotations in
	 * order, with the checks of the loop ({@link AbstractBruteForcePackager#nextPlacement}), on the loop's point calculator.
	 */
	static final class PointCandidates extends Candidates {

		private final PointCalculator3DStack pointCalculator;
		private final BruteForceSearchFrames frames;
		private final BruteForcePointIteratorFilter pointFilter;
		private final boolean fullSupport;
		private final boolean checkExtraction;
		private final List<Placement> obstacles;
		private final boolean checkObstacles;
		private final ContainerAccess access;

		PointCandidates(PointCalculator3DStack pointCalculator, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, boolean checkExtraction, LoadPlacementUtility utility,
				Container holder) {
			super(holder.getStack(), utility);
			this.pointCalculator = pointCalculator;
			this.frames = pointCalculator.getSearchFrames();
			this.pointFilter = pointFilter;
			this.fullSupport = fullSupport;
			this.checkExtraction = checkExtraction;
			this.obstacles = holder.getObstacles();
			this.checkObstacles = obstacles != null && !obstacles.isEmpty();
			this.access = holder.getAccess();
		}

		@Override
		void start(Container holder, ContainerItem containerItem, int n) {
			clearToSize(pointCalculator, holder, containerItem);
			if(utility != null) {
				utility.initialize(n);
			}
		}

		@Override
		void setLimits(long minArea, long minVolume) {
			pointCalculator.setMinimumAreaAndVolumeLimit(minArea, minVolume);
		}

		@Override
		void enter(int level) {
			pointCalculator.push();
		}

		@Override
		void startType(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement) {
			frames.rotationIndexes[level] = 0;
			placement.setStackValue(item, stackValues[0]);
			AbstractBruteForcePackager.startCandidates(pointCalculator, pointFilter, fullSupport, frames, level, stack, stackValues[0]);
		}

		@Override
		int next(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement, int insertAfterCount) {
			int pointIndex = AbstractBruteForcePackager.nextPlacement(pointCalculator, pointFilter, fullSupport, frames, level, stack, item, stackValues, placement, insertAfterCount, access,
					checkObstacles, obstacles, checkExtraction, utility);
			if(pointIndex != -1) {
				point = frames.candidatePoint;
				supportedArea = frames.candidateSupportedArea;
				rotation = frames.rotationIndexes[level];
			}
			return pointIndex;
		}

		@Override
		void add(int level, int pointIndex, Placement placement, boolean last) {
			if(last) {
				pointCalculator.addLast(point, placement);
			} else {
				pointCalculator.add(pointIndex, placement, point);
			}
		}

		@Override
		void restore(int level) {
			pointCalculator.redo();
		}

		@Override
		void leave(int level) {
			pointCalculator.pop();
		}

		@Override
		SimplePoint3D placedPoint(int level) {
			return pointCalculator.stackItems[level + 1].point;
		}
	}

	/**
	 * The candidates of {@link FastBruteForcePackager}: per rotation the best free point by its point comparator (where the box is insertable, and
	 * with full support the best fully supported position), on its point calculator.
	 */
	static final class FastCandidates extends Candidates {

		private final FastBruteForcePackager packager;
		private final FastPointCalculator3DStack pointCalculator;
		private final FastBruteForceBoxStackValuePointComparator pointComparator;
		private final FullSupportCandidates fullSupportCandidates;
		private final boolean checkExtraction;
		private final boolean checkObstacles;
		private final Container container;
		private final int[] rotationIndexes;

		FastCandidates(FastBruteForcePackager packager, FastPointCalculator3DStack pointCalculator, FastBruteForceBoxStackValuePointComparator pointComparator, boolean fullSupport,
				boolean checkExtraction, LoadPlacementUtility utility, Container holder, int n) {
			super(holder.getStack(), utility);
			this.packager = packager;
			this.pointCalculator = pointCalculator;
			this.pointComparator = pointComparator;
			this.fullSupportCandidates = fullSupport ? pointCalculator.getFullSupportCandidates() : null;
			this.checkExtraction = checkExtraction;
			this.checkObstacles = !holder.getObstacles().isEmpty();
			this.container = holder;
			this.rotationIndexes = new int[n];
		}

		@Override
		void start(Container holder, ContainerItem containerItem, int n) {
			clearToSize(pointCalculator, holder, containerItem);
			if(utility != null) {
				utility.initialize(n);
			}
		}

		@Override
		void setLimits(long minArea, long minVolume) {
			pointCalculator.setMinimumAreaAndVolumeLimit(minArea, minVolume);
		}

		@Override
		void enter(int level) {
			// the candidates do not change the free points
		}

		@Override
		void startType(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement) {
			rotationIndexes[level] = 0;
		}

		@Override
		int next(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement, int insertAfterCount) {
			while (rotationIndexes[level] < stackValues.length) {
				BoxStackValue stackValue = stackValues[rotationIndexes[level]];
				rotationIndexes[level]++;
				int pointIndex;
				SimplePoint3D candidatePoint;
				if(fullSupportCandidates != null) {
					fullSupportCandidates.populate(pointCalculator, null, stack.getPlacements(), stackValue);
					int candidate = packager.getBestFullySupported(fullSupportCandidates, item, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility,
							pointComparator);
					if(candidate == -1) {
						continue;
					}
					pointIndex = fullSupportCandidates.getPointIndex(candidate);
					candidatePoint = fullSupportCandidates.getPoint(candidate);
				} else {
					if(utility == null) {
						pointIndex = packager.getBestPoint(pointCalculator, item, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, pointComparator);
					} else {
						pointIndex = packager.getBestPointWithLoad(pointCalculator, item, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility, pointComparator);
					}
					if(pointIndex == -1) {
						continue;
					}
					candidatePoint = pointCalculator.get(pointIndex);
				}
				if(utility != null) {
					// the utility caches are primed for the last position checked: repopulate them for the selected one
					utility.populatePointSupporters(candidatePoint);
					utility.populatePointSupportees(candidatePoint, stackValue.getDz(), stackValue.getDz());
					supportedArea = utility.getSupportedAreaAtPoint(candidatePoint, stackValue, false);
				}
				placement.setStackValue(item, stackValue);
				point = candidatePoint;
				rotation = rotationIndexes[level] - 1;
				return pointIndex;
			}
			return -1;
		}

		@Override
		void add(int level, int pointIndex, Placement placement, boolean last) {
			if(last) {
				pointCalculator.addLast(point, placement);
			} else {
				pointCalculator.add(pointIndex, placement, point);
			}
		}

		@Override
		void restore(int level) {
			pointCalculator.setStackSize(level);
		}

		@Override
		void leave(int level) {
			// nothing saved
		}

		@Override
		SimplePoint3D placedPoint(int level) {
			return pointCalculator.getPoint(level);
		}
	}

	/**
	 * The keys of the multisets of remaining boxes which a search meets: the boxes of the first group with boxes remaining (any counts of its
	 * types, in a mixed radix), while the groups before it are placed and the groups after it are whole. Without groups, all multisets. The empty
	 * multiset has the last key.
	 */
	static final class Multisets {

		private final int[] group;
		private final int[] radix;
		private final int[] offsets;
		private final int[] fullKeys;
		private final int[] groupRemaining;
		private final int groups;
		/** the number of keys, including the empty multiset's */
		final int keys;
		/** the key of all boxes */
		final int fullKey;

		Multisets(int[] counts, int[] groupEnd) {
			int types = counts.length;
			group = new int[types];
			radix = new int[types];
			int[] groupStart = new int[types];
			int g = 0;
			for (int t = 0; t < types; t++) {
				if(counts[t] == 0) {
					group[t] = -1;
					continue;
				}
				groupStart[g] = t;
				int end = groupEnd[t];
				for (int k = t; k < end; k++) {
					group[k] = g;
				}
				g++;
				t = end - 1;
			}
			groups = g;
			offsets = new int[groups + 1];
			fullKeys = new int[groups + 1];
			groupRemaining = new int[groups];
			long offset = 0L;
			for (int i = 0; i < groups; i++) {
				offsets[i] = (int)offset;
				long size = 1L;
				long full = offset;
				int end = groupEnd[groupStart[i]];
				for (int t = groupStart[i]; t < end; t++) {
					radix[t] = (int)Math.min(size, Integer.MAX_VALUE);
					full += (long)counts[t] * radix[t];
					size *= counts[t] + 1;
					groupRemaining[i] += counts[t];
				}
				fullKeys[i] = (int)full;
				offset += size;
			}
			// the empty multiset
			offsets[groups] = (int)offset;
			fullKeys[groups] = (int)offset;
			keys = (int)offset + 1;
			fullKey = fullKeys[0];
		}

		/** @return the number of keys, i.e. of multisets a search meets (and the empty one) */
		static long count(int[] counts, int[] groupEnd) {
			long total = 1L;
			for (int t = 0; t < counts.length; t++) {
				if(counts[t] == 0) {
					continue;
				}
				long size = 1L;
				int end = groupEnd[t];
				for (int k = t; k < end; k++) {
					size *= counts[k] + 1;
					if(size > Integer.MAX_VALUE) {
						return Long.MAX_VALUE;
					}
				}
				total += size;
				t = end - 1;
			}
			return total;
		}

		/**
		 * @param key the key of the remaining boxes, of which the type is one
		 * @param remaining the remaining boxes
		 * @return the key of the remaining boxes without one box of the type
		 */
		int remove(int key, int[] remaining, int type) {
			int g = group[type];
			int left = 0;
			// the group's remaining boxes: the ones of the first group with boxes remaining are counted in the key
			for (int t = type; t >= 0 && group[t] == g; t--) {
				left += remaining[t];
			}
			for (int t = type + 1; t < group.length && group[t] == g; t++) {
				left += remaining[t];
			}
			if(left > 1) {
				return key - radix[type];
			}
			// the group's last box: the next group, whole
			return fullKeys[g + 1];
		}

		/**
		 * @param key the key of the remaining boxes
		 * @return the key of the remaining boxes with one more box of the type (the last placed)
		 */
		int add(int key, int type) {
			int g = group[type];
			if(key < offsets[g + 1]) {
				// the group has boxes remaining
				return key + radix[type];
			}
			// the group was placed whole: this box is its only one remaining
			return offsets[g] + radix[type];
		}
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
	private static int nextType(int[] remaining, int[] typeOrder, int[] typeRank, int[] groupEnd, int after, int[] prefix, int level) {
		if(level < prefix.length) {
			// a unit of a split search: the orders which start with the prefix
			return after == -1 ? prefix[level] : -1;
		}
		int end = -1;
		for (int t = 0; t < remaining.length; t++) {
			if(remaining[t] > 0) {
				end = groupEnd[t];
				break;
			}
		}
		for (int i = after == -1 ? 0 : typeRank[after] + 1; i < typeOrder.length; i++) {
			int t = typeOrder[i];
			if(remaining[t] > 0 && t < end) {
				return t;
			}
		}
		return -1;
	}

	/** {@link Bound#isHopeless} against the better of the search's own best arrangement and the shared one */
	private static boolean isHopeless(Bound bounds, SharedBest shared, int[] remaining, int key, long volume, long weight, int count, long boundVolume, boolean haveBest, long bestVolume,
			long bestWeight, int bestCount) {
		if(shared != null) {
			long[] best = shared.get();
			if(best != null && (!haveBest || Bound.compare(best[0], best[1], (int)best[2], bestVolume, bestWeight, bestCount) > 0)) {
				return bounds.isHopeless(remaining, key, volume, weight, count, boundVolume, true, best[0], best[1], (int)best[2]);
			}
		}
		return bounds.isHopeless(remaining, key, volume, weight, count, boundVolume, haveBest, bestVolume, bestWeight, bestCount);
	}

	/**
	 * The best arrangement of the units of a search split between threads (by its load volume, weight and boxes): a unit does not expand prefixes
	 * which cannot beat it. Equal arrangements are not pruned, so the units' results compare as on one thread.
	 */
	static final class SharedBest {

		private final AtomicReference<long[]> best = new AtomicReference<>();

		/** @return the volume, weight and count of the best arrangement so far, or null */
		long[] get() {
			return best.get();
		}

		void offer(long volume, long weight, int count) {
			long[] candidate = null;
			while (true) {
				long[] current = best.get();
				if(current != null && Bound.compare(volume, weight, count, current[0], current[1], (int)current[2]) <= 0) {
					return;
				}
				if(candidate == null) {
					candidate = new long[] { volume, weight, count };
				}
				if(best.compareAndSet(current, candidate)) {
					return;
				}
			}
		}
	}

	/**
	 * The prefixes of the orders which split a search between threads: the shortest prefixes (of types, keeping box item groups in sequence)
	 * which number at least the units wanted, at most one box shorter than the orders; in lexicographic order, so that the units' results compare
	 * in the order of the orders.
	 */
	static List<int[]> prefixes(AbstractBoxItemPermutationRotationIterator iterator, int[] insertAfterCounts, int minUnits) {
		int[] permutations = iterator.getPermutations();
		int n = permutations.length;
		int types = iterator.getBoxItemStackValues().length;
		int[] counts = new int[types];
		for (int value : permutations) {
			counts[value]++;
		}
		// the groups' ranges of types, as in search
		int[] groupEnd = new int[types];
		int[] sorted = permutations.clone();
		int start = 0;
		for (int i = 1; i <= n; i++) {
			if(i == n || (insertAfterCounts.length != 0 && insertAfterCounts[i] == i)) {
				Arrays.sort(sorted, start, i);
				int end = sorted[i - 1] + 1;
				for (int k = start; k < i; k++) {
					groupEnd[sorted[k]] = end;
				}
				start = i;
			}
		}
		List<int[]> prefixes = new ArrayList<>();
		for (int depth = 1; depth < n; depth++) {
			prefixes.clear();
			prefixes(counts, groupEnd, new int[depth], 0, prefixes);
			if(prefixes.size() >= minUnits) {
				break;
			}
		}
		return prefixes;
	}

	private static void prefixes(int[] remaining, int[] groupEnd, int[] prefix, int depth, List<int[]> prefixes) {
		if(depth == prefix.length) {
			prefixes.add(prefix.clone());
			return;
		}
		int end = remaining.length;
		for (int t = 0; t < end; t++) {
			if(remaining[t] > 0) {
				// the first group with boxes remaining
				end = groupEnd[t];
				remaining[t]--;
				prefix[depth] = t;
				prefixes(remaining, groupEnd, prefix, depth + 1, prefixes);
				remaining[t]++;
			}
		}
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
	static final class SettledOrders {

		static final int MAX_PREFIXES = 1 << 22;
		/** the most entries of the table of sequence counts (multisets times lengths) */
		static final long MAX_SEQUENCES = 1L << 20;

		/** the most prefixes to keep counts for; tests lower it to cover the fallback */
		static volatile int maxPrefixes = MAX_PREFIXES;

		final boolean exact;
		private final int n;
		private final int types;
		private final Multisets multisets;
		/** the number of sequences of a length from a multiset, by multiset key and length; -1 until computed */
		private final long[] sequences;
		/** the settled orders per depth and prefix rank */
		private final int[][] settled;
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
		SettledOrders(int[] counts, int n, Multisets multisets, int[] groupEnd) {
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
			if(fits) {
				sequences = new long[(int)keys * (n + 1)];
				Arrays.fill(sequences, -1L);
				long total = 0;
				settled = new int[n + 1][];
				int rootKey = multisets.fullKey;
				for (int d = 0; d <= n && fits; d++) {
					long prefixes = sequences(sequences, counts, rootKey, d);
					total += prefixes;
					if(total > maxPrefixes) {
						fits = false;
					} else {
						settled[d] = new int[(int)prefixes];
					}
				}
			}
			this.exact = fits;
			this.sequences = fits ? sequences : null;
			this.settled = fits ? settled : null;
			this.before = fits ? new long[n + 1][n + 1] : null;
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
			return exact && settled[level][rank] == orders(level, remaining, key);
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
			boolean settled = this.settled[level + 1][rank] == orders(level + 1, remaining, childKey);
			remaining[type]++;
			return settled;
		}

		/** settle every order through the level's prefix (the ranks of the levels' prefixes are the path to it) */
		void settle(int level, int[] rankAt, int[] remaining, int key) {
			if(!exact) {
				return;
			}
			int[] counts = settled[level];
			long delta = orders(level, remaining, key) - counts[rankAt[level]];
			if(delta == 0) {
				return;
			}
			counts[rankAt[level]] += (int)delta;
			for (int i = 0; i < level; i++) {
				settled[i][rankAt[i]] += (int)delta;
			}
		}

		/** settle every order with the type next at the level */
		void settleChild(int level, int rank, int[] rankAt, int[] remaining, int type, int childKey) {
			if(!exact) {
				return;
			}
			remaining[type]--;
			long delta = orders(level + 1, remaining, childKey) - settled[level + 1][rank];
			remaining[type]++;
			if(delta == 0) {
				return;
			}
			settled[level + 1][rank] += (int)delta;
			for (int i = 0; i <= level; i++) {
				settled[i][rankAt[i]] += (int)delta;
			}
		}
	}

	/**
	 * The most the remaining boxes can add: the sub-multiset of them within the load volume and weight left with the most volume, then weight, then
	 * boxes, which is what the first boxes of the best order of them can load. It depends only on the remaining multiset (the placed boxes are the
	 * rest), so it is computed once per multiset, by enumerating its sub-multisets. When all boxes fit the load limits there is nothing to compute.
	 */
	static final class Bound {

		private final long[] volumes;
		private final long[] weights;
		private final int types;
		final boolean allFit;
		private final long maxLoadVolume;
		private final long maxLoadWeight;
		private final long totalVolume;
		private final long totalWeight;
		private final long[] fitVolume;
		private final long[] fitWeight;
		private final int[] fitCount;
		/** the smallest area and volume of the remaining boxes, by multiset key (0 until computed); null for too many keys */
		private final long[] minArea;
		private final long[] minVolume;
		private long bestVolume;
		private long bestWeight;
		private int bestCount;

		/**
		 * @param keys the number of multiset keys
		 */
		Bound(int[] counts, long[] volumes, long[] weights, long maxLoadVolume, long maxLoadWeight, int keys) {
			this.volumes = volumes;
			this.weights = weights;
			this.types = counts.length;
			this.maxLoadVolume = maxLoadVolume;
			this.maxLoadWeight = maxLoadWeight;
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				volume += counts[t] * volumes[t];
				weight += counts[t] * weights[t];
			}
			totalVolume = volume;
			totalWeight = weight;
			allFit = volume <= maxLoadVolume && weight <= maxLoadWeight;
			int size = keys;
			if(allFit) {
				fitVolume = null;
				fitWeight = null;
				fitCount = null;
			} else {
				fitVolume = new long[size];
				fitWeight = new long[size];
				fitCount = new int[size];
				Arrays.fill(fitVolume, -1L);
			}
			minArea = new long[size];
			minVolume = new long[size];
		}

		/** the smallest area of the remaining boxes (with the key), over their rotations */
		long minArea(int[] remaining, int key, long[] minAreas) {
			long area = minArea[key];
			if(area == 0L) {
				area = Long.MAX_VALUE;
				for (int t = 0; t < types; t++) {
					if(remaining[t] > 0 && minAreas[t] < area) {
						area = minAreas[t];
					}
				}
				minArea[key] = area;
			}
			return area;
		}

		/** the smallest volume of the remaining boxes (with the key) */
		long minVolume(int[] remaining, int key) {
			long volume = minVolume[key];
			if(volume == 0L) {
				volume = Long.MAX_VALUE;
				for (int t = 0; t < types; t++) {
					if(remaining[t] > 0 && volumes[t] < volume) {
						volume = volumes[t];
					}
				}
				minVolume[key] = volume;
			}
			return volume;
		}

		/** whether a prefix with the volume, weight and count, plus the best fit of the remaining boxes, can beat neither the bound nor the best arrangement */
		boolean isHopeless(int[] remaining, int key, long volume, long weight, int count, long boundVolume, boolean haveBest, long bestVolume, long bestWeight, int bestCount) {
			long v;
			long w;
			int c;
			if(allFit) {
				v = totalVolume;
				w = totalWeight;
				c = count;
				for (int t = 0; t < types; t++) {
					c += remaining[t];
				}
			} else {
				maxFit(remaining, key);
				v = volume + fitVolume[key];
				w = weight + fitWeight[key];
				c = count + fitCount[key];
			}
			if(v < boundVolume) {
				return true;
			}
			return haveBest && compare(v, w, c, bestVolume, bestWeight, bestCount) < 0;
		}

		private void maxFit(int[] remaining, int key) {
			if(fitVolume[key] == -1L) {
				long volume = 0L;
				long weight = 0L;
				for (int t = 0; t < types; t++) {
					volume += remaining[t] * volumes[t];
					weight += remaining[t] * weights[t];
				}
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
		}

		static int compare(long volume, long weight, int count, long otherVolume, long otherWeight, int otherCount) {
			if(volume != otherVolume) {
				return volume < otherVolume ? -1 : 1;
			}
			if(weight != otherWeight) {
				return weight < otherWeight ? -1 : 1;
			}
			return Integer.compare(count, otherCount);
		}

		private void best(int[] remaining, int t, long volumeLeft, long weightLeft, long volume, long weight, int count) {
			if(t == types) {
				if(volume > bestVolume || (volume == bestVolume && (weight > bestWeight || (weight == bestWeight && count > bestCount)))) {
					bestVolume = volume;
					bestWeight = weight;
					bestCount = count;
				}
				return;
			}
			long v = volumes[t];
			long w = weights[t];
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
