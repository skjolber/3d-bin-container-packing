package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
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
 */
final class BruteForceTreeSearch {

	/** the most multisets of the boxes (the product of the counts plus one over the types) for the tree search: the bounds are kept per multiset */
	static final long MAX_MULTISETS = 1L << 18;

	private BruteForceTreeSearch() {
	}

	/** @return whether the boxes of the iterator can be searched as a tree: not too many multisets of them */
	static boolean isSupported(DefaultBoxItemPermutationRotationIterator iterator) {
		int[] counts = new int[iterator.getBoxItemStackValues().length];
		for (int value : iterator.getPermutations()) {
			counts[value]++;
		}
		long keys = 1L;
		for (int count : counts) {
			keys *= count + 1;
			if(keys > MAX_MULTISETS) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the result (empty if no arrangement places a box), which refers to reused state until materialized
	 */
	static BruteForceIntermediatePackagerResult search(PointCalculator3DStack pointCalculator, Placement[] placements, int placementCount, ContainerItem containerItem, int containerIndex,
			DefaultBoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, boolean checkExtraction,
			LoadPlacementUtility utility, Container holder, Stack stack, int limit, long minLoadVolume) throws PackagerInterruptedException {
		final int n = iterator.length();
		final int depthLimit = Math.min(limit, n);
		final BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(n), containerIndex, iterator, utility != null);
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
		final long maxLoadVolume = holder.getMaxLoadVolume();
		final long maxLoadWeight = holder.getMaxLoadWeight();
		// the remaining boxes as a multiset, keyed by the counts (mixed radix): the bound and the settled orders are kept per multiset
		final int[] radix = new int[types];
		long keys = 1L;
		for (int t = 0; t < types; t++) {
			radix[t] = (int)Math.min(keys, Integer.MAX_VALUE);
			keys *= counts[t] + 1;
		}
		final Bound bounds = new Bound(counts, volumes, weights, maxLoadVolume, maxLoadWeight, radix, keys);
		final SettledOrders settled = new SettledOrders(counts, n, radix, keys);
		int remainingKey = key(counts, radix);

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

		final BruteForceSearchFrames frames = pointCalculator.getSearchFrames();
		final List<Placement> obstacles = holder.getObstacles();
		final ContainerAccess access = holder.getAccess();
		final boolean checkObstacles = obstacles != null && !obstacles.isEmpty();

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

		start(pointCalculator, holder, containerItem, utility, n, remaining, remainingKey, minAreas, bounds);
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
					} else if((haveBest || minLoadVolume > 0L)
							&& bounds.isHopeless(remaining, remainingKey, volume[level], weight[level], level, minLoadVolume, haveBest, bestVolume, bestWeight, bestCount)) {
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
						System.arraycopy(placedRotation, 0, bestRotations, 0, level);
						for (int i = 0; i < level; i++) {
							bestPoints[i] = pointCalculator.stackItems[i + 1].point;
						}
						if(bestCount == n && phase == 0) {
							// all boxes placed: only an earlier order can still win, so search again in the lexicographic order, where the first full leaf
							// settles everything after it; the orders settled so far stay settled
							settled.settle(level, rankAt, remaining, remainingKey);
							phase = 1;
							typeOrder = lexOrder;
							typeRank = lexOrder;
							for (int l = level - 1; l >= 0; l--) {
								unplace(pointCalculator, placements[l], stack, utility, pushed[l]);
							}
							System.arraycopy(counts, 0, remaining, 0, types);
							remainingKey = key(counts, radix);
							start(pointCalculator, holder, containerItem, utility, n, remaining, remainingKey, minAreas, bounds);
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
					typeAt[level] = nextType(remaining, typeOrder, typeRank, -1);
					typeStarted[level] = false;
					fit[level] = false;
					nodeSolved[level] = true;
					typeSolved[level] = false;
					// save the free points, so that each candidate of this level starts from them
					pointCalculator.push();
					pushed[level] = true;
				}
			}

			boolean placed = false;
			while (typeAt[level] != -1 && !placed) {
				final int t = typeAt[level];
				final int childKey = remainingKey - radix[t];
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
							System.arraycopy(placedRotation, 0, bestRotations, 0, level);
							for (int i = 0; i < level; i++) {
								bestPoints[i] = pointCalculator.stackItems[i + 1].point;
							}
						}
						settled.settleChild(level, rank, rankAt, remaining, t, childKey);
					} else if(haveBest || minLoadVolume > 0L) {
						remaining[t]--;
						boolean hopeless = bounds.isHopeless(remaining, childKey, volume[level] + volumes[t], weight[level] + weights[t], level + 1, minLoadVolume, haveBest, bestVolume,
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
						typeAt[level] = nextType(remaining, typeOrder, typeRank, t);
						typeStarted[level] = false;
						fit[level] = false;
						typeSolved[level] = false;
						continue;
					}
					// the candidates of the box, in its first rotation, from the free points of the level; the limits of the boxes still to place
					typeStarted[level] = true;
					childRank[level] = rank;
					settled.startType(level, remaining, t, remainingKey);
					pointCalculator.setMinimumAreaAndVolumeLimit(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
					Placement placement = placements[level];
					BoxStackValue stackValue = stackValues[t][0];
					frames.rotationIndexes[level] = 0;
					placement.setStackValue(iterator.getBoxItemByIndex(t), stackValue);
					AbstractBruteForcePackager.startCandidates(pointCalculator, pointFilter, fullSupport, frames, level, stack, stackValue);
				}

				Placement placement = placements[level];
				BoxItem item = iterator.getBoxItemByIndex(t);
				// no insert-after count: without box item groups the loop checks none
				int pointIndex = AbstractBruteForcePackager.nextPlacement(pointCalculator, pointFilter, fullSupport, frames, level, stack, item, stackValues[t], placement, 0, access,
						checkObstacles, obstacles, checkExtraction, utility);
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
							System.arraycopy(placedRotation, 0, bestRotations, 0, level);
							for (int i = 0; i < level; i++) {
								bestPoints[i] = pointCalculator.stackItems[i + 1].point;
							}
						}
					}
					typeAt[level] = nextType(remaining, typeOrder, typeRank, t);
					typeStarted[level] = false;
					fit[level] = false;
					typeSolved[level] = false;
				} else {
					// place the box and continue with the next level
					SimplePoint3D point = frames.candidatePoint;
					placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
					if(utility != null) {
						placement.clearLoad();
						placement.setIndex(stack.size());
						placement.setSupportedArea(frames.candidateSupportedArea);
					}
					int nextLevel = level + 1;
					remaining[t]--;
					remainingKey = childKey;
					volume[nextLevel] = volume[level] + volumes[t];
					weight[nextLevel] = weight[level] + weights[t];
					if(nextLevel == depthLimit || !canPlaceAny(remaining, types, volumes, weights, maxLoadVolume - volume[nextLevel], maxLoadWeight - weight[nextLevel])) {
						// no box can follow: keep the placement, but never search the free space after it
						pointCalculator.addLast(point, placement);
					} else {
						pointCalculator.add(pointIndex, placement, point);
						pointCalculator.setMinimumAreaAndVolumeLimit(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
					}
					stack.add(placement);
					if(utility != null) {
						utility.addSupportersLoad(placement);
					}
					fit[level] = true;
					placedType[level] = t;
					placedRotation[level] = frames.rotationIndexes[level];
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
					pointCalculator.pop();
				}
				level--;
				if(level >= 0) {
					int t = placedType[level];
					remaining[t]++;
					int placedKey = remainingKey;
					remainingKey += radix[t];
					unplace(pointCalculator, placements[level], stack, utility, false);
					// restore the free points to before this level's placement
					pointCalculator.redo();
					if(settled.exact ? settled.isChildSettled(level, childRank[level], remaining, t, placedKey) : solved) {
						// every order with the box next is settled: the loop stops an order at its first complete path, so the other candidates of the box
						// can only repeat arrangements of settled orders
						typeSolved[level] = true;
						typeAt[level] = nextType(remaining, typeOrder, typeRank, t);
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
		iterator.setPermutation(bestPermutation);
		List<Point> points = new ArrayList<>(bestCount);
		for (int i = 0; i < bestCount; i++) {
			points.add(bestPoints[i]);
		}
		result.setStateFromReusablePoints(points, AbstractBruteForcePackager.getState(iterator, bestRotations, bestCount), placements, placementCount);
		return result;
	}

	/** the calculator, the stack and the load utility at the start of a search: the container's free points, and the limits of all boxes */
	private static void start(PointCalculator3DStack pointCalculator, Container holder, ContainerItem containerItem, LoadPlacementUtility utility, int n, int[] remaining, int remainingKey,
			long[] minAreas, Bound bounds) {
		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.getInitialPoints() != null) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		if(utility != null) {
			utility.initialize(n);
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(bounds.minArea(remaining, remainingKey, minAreas), bounds.minVolume(remaining, remainingKey));
	}

	/** remove the placement of a level from the stack and the loads, and the level's push */
	private static void unplace(PointCalculator3DStack pointCalculator, Placement placement, Stack stack, LoadPlacementUtility utility, boolean pop) {
		if(utility != null) {
			placement.removeSupporteesAbove();
			for (PlacementLoad placementLoad : placement.getSupporters()) {
				placementLoad.getPlacement().removeLastSupportee();
			}
			placement.clearLoad();
		}
		stack.remove(stack.size() - 1);
		if(pop) {
			pointCalculator.pop();
		}
	}

	/** the key of a multiset of boxes: the counts in the mixed radix */
	private static int key(int[] remaining, int[] radix) {
		int key = 0;
		for (int t = 0; t < remaining.length; t++) {
			key += remaining[t] * radix[t];
		}
		return key;
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

	/** the next type with boxes remaining, in the order of the types, after the given type (or the first, for -1) */
	private static int nextType(int[] remaining, int[] typeOrder, int[] typeRank, int after) {
		for (int i = after == -1 ? 0 : typeRank[after] + 1; i < typeOrder.length; i++) {
			int t = typeOrder[i];
			if(remaining[t] > 0) {
				return t;
			}
		}
		return -1;
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
		private final int[] radix;
		/** the number of sequences of a length from a multiset, by multiset key and length; -1 until computed */
		private final long[] sequences;
		/** the settled orders per depth and prefix rank */
		private final int[][] settled;
		/**
		 * per level, the number of sequences of each length from the multisets which remain after a type before the level's type: the rank of the
		 * level's prefix among longer prefixes is the sum over the levels before it
		 */
		private final long[][] before;

		/**
		 * @param radix the radix of the multiset keys, by type
		 * @param keys the number of multiset keys
		 */
		SettledOrders(int[] counts, int n, int[] radix, long keys) {
			this.n = n;
			this.types = counts.length;
			this.radix = radix;
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
				int rootKey = key(counts, radix);
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
				for (int t = 0; t < types; t++) {
					if(remaining[t] > 0) {
						remaining[t]--;
						count += sequences(sequences, remaining, key - radix[t], length - 1);
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
					remaining[t]--;
					int childKey = key - radix[t];
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
		 * @param radix the radix of the multiset keys, by type
		 * @param keys the number of multiset keys
		 */
		Bound(int[] counts, long[] volumes, long[] weights, long maxLoadVolume, long maxLoadWeight, int[] radix, long keys) {
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
			int size = (int)keys;
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

		private static int compare(long volume, long weight, int count, long otherVolume, long otherWeight, int otherCount) {
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
