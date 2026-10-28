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
 * Pruning, none of which changes the winner: a box is not placed when no completion with it next can beat the best (the volume, weight and boxes of
 * the prefix and the box, plus the best sub-multiset of the remaining boxes within the load limits, computed once per multiset); when the subtree of
 * a candidate settles every order through it (each ends at the load limits, the limit of boxes or a hopeless prefix), the other candidates of the box
 * at that node are skipped, since the loop stops an order at its first complete path and they could only repeat arrangements of settled orders;
 * after an arrangement places all boxes, prefixes after it in the order are not expanded; the types are tried largest first, so that a strong
 * arrangement is found early (ties are decided by comparing permutations, so the order of visiting does not change the winner), and once an
 * arrangement places all boxes the search starts again in the lexicographic order, where the first full leaf settles everything after it, so the
 * search ends at the first order which places all boxes, as the loop does.
 */
final class BruteForceTreeSearch {

	private BruteForceTreeSearch() {
	}

	/**
	 * @param insertAfterCounts null without box item groups
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
		final Bound bounds = new Bound(counts, volumes, weights, maxLoadVolume, maxLoadWeight);

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

		// the best arrangement
		boolean haveBest = false;
		int bestCount = 0;
		long bestVolume = 0L;
		long bestWeight = 0L;
		final int[] bestPermutation = new int[n];
		final int[] bestRotations = new int[n];
		final SimplePoint3D[] bestPoints = new SimplePoint3D[n];

		int phase = 0;
		int[] typeOrder = volumeOrder;
		int[] typeRank = volumeRank;

		start(pointCalculator, holder, containerItem, utility, n, remaining, types, minAreas, volumes);
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
					// no completion of this prefix can beat the best
					if((haveBest || minLoadVolume > 0L) && bounds.isHopeless(remaining, volume[level], weight[level], level, minLoadVolume, haveBest, bestVolume, bestWeight, bestCount)) {
						expand = false;
					}
				} else if(level > 0) {
					// every box which may be placed is placed
					if(isBetter(haveBest, bestCount, bestVolume, bestWeight, bestPermutation, placedType, level, -1, remaining, types, volume[level], weight[level], candidate, n)) {
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
							// settles everything after it
							phase = 1;
							typeOrder = lexOrder;
							typeRank = lexOrder;
							for (int l = level - 1; l >= 0; l--) {
								unplace(pointCalculator, placements[l], stack, utility, pushed[l]);
							}
							System.arraycopy(counts, 0, remaining, 0, types);
							start(pointCalculator, holder, containerItem, utility, n, remaining, types, minAreas, volumes);
							level = 0;
							descend = true;
							continue;
						}
					}
				}
				if(!expand) {
					// a leaf, or a hopeless prefix: every order through it is settled
					typeAt[level] = -1;
					nodeSolved[level] = true;
					pushed[level] = false;
				} else {
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
				if(!typeStarted[level]) {
					// after a full arrangement only earlier permutations can win; a box over the load limits ends the arrangement; a hopeless box is not placed
					boolean skip = false;
					if(bestCount == n && comparePrefix(placedType, level, t, bestPermutation) > 0) {
						skip = true;
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
						skip = true;
					} else if(haveBest || minLoadVolume > 0L) {
						remaining[t]--;
						boolean hopeless = bounds.isHopeless(remaining, volume[level] + volumes[t], weight[level] + weights[t], level + 1, minLoadVolume, haveBest, bestVolume, bestWeight,
								bestCount);
						remaining[t]++;
						skip = hopeless;
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
					pointCalculator.setMinimumAreaAndVolumeLimit(minArea(remaining, types, minAreas), minVolume(remaining, types, volumes));
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
					volume[nextLevel] = volume[level] + volumes[t];
					weight[nextLevel] = weight[level] + weights[t];
					if(nextLevel == depthLimit || !canPlaceAny(remaining, types, volumes, weights, maxLoadVolume - volume[nextLevel], maxLoadWeight - weight[nextLevel])) {
						// no box can follow: keep the placement, but never search the free space after it
						pointCalculator.addLast(point, placement);
					} else {
						pointCalculator.add(pointIndex, placement, point);
						pointCalculator.setMinimumAreaAndVolumeLimit(minArea(remaining, types, minAreas), minVolume(remaining, types, volumes));
					}
					stack.add(placement);
					if(utility != null) {
						utility.addSupportersLoad(placement);
					}
					fit[level] = true;
					placedType[level] = t;
					placedRotation[level] = frames.rotationIndexes[level];
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
					unplace(pointCalculator, placements[level], stack, utility, false);
					// restore the free points to before this level's placement
					pointCalculator.redo();
					if(solved) {
						// the subtree of the candidate settled every order with the box next: the loop stops an order at its first complete path, so the
						// other candidates of the box can only repeat arrangements of settled orders
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
	private static void start(PointCalculator3DStack pointCalculator, Container holder, ContainerItem containerItem, LoadPlacementUtility utility, int n, int[] remaining, int types,
			long[] minAreas, long[] volumes) {
		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.getInitialPoints() != null) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		if(utility != null) {
			utility.initialize(n);
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(minArea(remaining, types, minAreas), minVolume(remaining, types, volumes));
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

	private static long minArea(int[] remaining, int types, long[] minAreas) {
		long area = Long.MAX_VALUE;
		for (int t = 0; t < types; t++) {
			if(remaining[t] > 0 && minAreas[t] < area) {
				area = minAreas[t];
			}
		}
		return area;
	}

	private static long minVolume(int[] remaining, int types, long[] volumes) {
		long volume = Long.MAX_VALUE;
		for (int t = 0; t < types; t++) {
			if(remaining[t] > 0 && volumes[t] < volume) {
				volume = volumes[t];
			}
		}
		return volume;
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
	 * The most the remaining boxes can add: the sub-multiset of them within the load volume and weight left with the most volume, then weight, then
	 * boxes, which is what the first boxes of the best order of them can load. It depends only on the remaining multiset (the placed boxes are the
	 * rest), so it is computed once per multiset, by enumerating its sub-multisets. When all boxes fit the load limits there is nothing to compute.
	 */
	static final class Bound {

		private final long[] volumes;
		private final long[] weights;
		private final int types;
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

		Bound(int[] counts, long[] volumes, long[] weights, long maxLoadVolume, long maxLoadWeight) {
			this.volumes = volumes;
			this.weights = weights;
			this.types = counts.length;
			this.maxLoadVolume = maxLoadVolume;
			this.maxLoadWeight = maxLoadWeight;
			radix = new int[types];
			int size = 1;
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				radix[t] = size;
				size *= counts[t] + 1;
				volume += counts[t] * volumes[t];
				weight += counts[t] * weights[t];
			}
			totalVolume = volume;
			totalWeight = weight;
			allFit = volume <= maxLoadVolume && weight <= maxLoadWeight;
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
		}

		/** whether a prefix with the volume, weight and count, plus the best fit of the remaining boxes, can beat neither the bound nor the best arrangement */
		boolean isHopeless(int[] remaining, long volume, long weight, int count, long boundVolume, boolean haveBest, long bestVolume, long bestWeight, int bestCount) {
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
				int key = maxFit(remaining);
				v = volume + fitVolume[key];
				w = weight + fitWeight[key];
				c = count + fitCount[key];
			}
			if(v < boundVolume) {
				return true;
			}
			return haveBest && compare(v, w, c, bestVolume, bestWeight, bestCount) < 0;
		}

		private int maxFit(int[] remaining) {
			int key = 0;
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				key += remaining[t] * radix[t];
				volume += remaining[t] * volumes[t];
				weight += remaining[t] * weights[t];
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
