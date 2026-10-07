package com.github.skjolber.packing.packer.bruteforce;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.AbstractBruteForcePackager.SkippingBest;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Recursive reference for the brute-force search ({@link AbstractBruteForcePackager#searchOrder}): the same choices
 * in the same order (each rotation, then each candidate position, then skipping), written as recursion, so that the
 * iterative search can be compared with it.
 */
public class RecursiveBruteForceSearch {

	/** Brute force with the recursive search instead of the iterative one. */
	public static class RecursiveBruteForcePackager extends BruteForcePackager {

		public RecursiveBruteForcePackager(BruteForcePointIteratorFilter pointFilter) {
			super(new BruteForceIntermediatePackagerResultComparator(), pointFilter, false);
		}

		@Override
		protected void searchOrder(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, Container container,
				PackagerInterruptSupplier interrupt, LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, boolean checkExtraction,
				int[] insertAfterCounts, int maxPackableCount, int[] skipEnds, int maxContainerPriority, SkippingBest skipping, int[] rotations) throws PackagerInterruptedException {
			Search search = new Search(pointCalculator, placements, iterator, stack, container, interrupt, utility, pointFilter, fullSupport, checkExtraction, insertAfterCounts,
					skipping != null ? iterator.length() : maxPackableCount, skipEnds, skipping, rotations);
			if(search.length == 0) {
				return;
			}
			pointCalculator.setMinimumAreaAndVolumeLimit(search.minAreas[0], iterator.getMinBoxVolume(0));
			search.search(0, 0, 0L, container.getMaxLoadWeight(), maxContainerPriority);
		}
	}

	private static class Search {

		private final PointCalculator3DStack pointCalculator;
		private final Placement[] placements;
		private final BoxItemPermutationRotationIterator iterator;
		private final Stack stack;
		private final Container container;
		private final PackagerInterruptSupplier interrupt;
		private final LoadPlacementUtility utility;
		private final BruteForcePointIteratorFilter pointFilter;
		private final boolean fullSupport;
		private final boolean checkExtraction;
		private final boolean checkObstacles;
		private final int[] insertAfterCounts;
		private final int length;
		private final int[] skipEnds;
		private final SkippingBest skipping;
		private final int[] rotations;

		private final Box[] boxes;
		private final long[] minAreas;
		private final long[] remainingVolumes;
		private final int[] placedPermutations;
		private final int[] placedRotations;

		Search(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, Container container,
				PackagerInterruptSupplier interrupt, LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, boolean checkExtraction,
				int[] insertAfterCounts, int length, int[] skipEnds, SkippingBest skipping, int[] rotations) {
			this.pointCalculator = pointCalculator;
			this.placements = placements;
			this.iterator = iterator;
			this.stack = stack;
			this.container = container;
			this.interrupt = interrupt;
			this.utility = utility;
			this.pointFilter = pointFilter;
			this.fullSupport = fullSupport;
			this.checkExtraction = checkExtraction;
			this.checkObstacles = !container.getObstacles().isEmpty();
			this.insertAfterCounts = insertAfterCounts;
			this.length = length;
			this.skipEnds = skipEnds;
			this.skipping = skipping;
			this.rotations = rotations;

			this.boxes = new Box[length];
			this.minAreas = new long[length + 1];
			this.remainingVolumes = new long[length + 1];
			this.placedPermutations = new int[length];
			this.placedRotations = new int[length];
			long minArea = Long.MAX_VALUE;
			for (int i = length - 1; i >= 0; i--) {
				boxes[i] = iterator.getStackValue(i).getBox();
				minArea = Math.min(minArea, boxes[i].getMinimumArea());
				minAreas[i] = minArea;
				remainingVolumes[i] = remainingVolumes[i + 1] + boxes[i].getVolume();
			}
		}

		/** @return true if the best arrangement places all boxes: no candidate can be better */
		private boolean isComplete() {
			return skipping != null ? skipping.count == length : pointCalculator.getBestStackIndex() >= length;
		}

		void search(int level, int placedCount, long placedVolume, int freeLoadWeight, int maxContainerPriority) throws PackagerInterruptedException {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			if(skipping == null) {
				int bestStackIndex = pointCalculator.getBestStackIndex();
				pointCalculator.updateBest();
				if(pointCalculator.getBestStackIndex() > bestStackIndex) {
					System.arraycopy(placedRotations, 0, rotations, 0, placedCount);
				}
			}
			if(level == length) {
				if(skipping != null && placedCount > 0 && skipping.canBeBetter(placedVolume)) {
					skipping.offer(pointCalculator.getPoints(), placedPermutations, placedRotations, placedCount);
				}
				return;
			}
			if(skipping != null && !skipping.canBeBetter(placedVolume + remainingVolumes[level])) {
				return;
			}
			Box box = boxes[level];
			boolean placeable = skipping == null || (box.getBoxItem().getContainerPriority() <= maxContainerPriority
					&& box.getWeight() <= freeLoadWeight && placedVolume + box.getVolume() <= container.getMaxLoadVolume());
			if(placeable) {
				pointCalculator.push();
				int insertAfterCount = insertAfterCounts == null ? placedCount : insertAfterCounts.length == 0 ? 0 : insertAfterCounts[level];
				ContainerAccess access = container.getAccess();
				BoxStackValue[] stackValues = box.getStackValues();
				for (int rotation = 0; rotation < stackValues.length; rotation++) {
					BoxStackValue stackValue = stackValues[rotation];
					// the candidate positions: point indexes, or with full support positions
					FullSupportCandidates candidates = null;
					IntIterator filtered = null;
					int count;
					if(fullSupport) {
						candidates = new FullSupportCandidates();
						candidates.populate(pointCalculator, pointFilter == null ? null : pointFilter.getPoints(pointCalculator, stackValue), stack.getPlacements(), stackValue);
						count = candidates.size();
					} else if(pointFilter != null) {
						filtered = pointFilter.getPoints(pointCalculator, stackValue);
						count = Integer.MAX_VALUE;
					} else {
						count = pointCalculator.size();
					}
					for (int k = 0; k < count; k++) {
						int candidate;
						if(filtered != null) {
							if(!filtered.hasNext()) {
								break;
							}
							candidate = filtered.next();
						} else {
							candidate = k;
							if(candidates == null && !pointCalculator.get(candidate).fits3D(stackValue)) {
								continue;
							}
						}
						SimplePoint3D point = candidates != null ? candidates.getPoint(candidate) : pointCalculator.get(candidate);
						if(insertAfterCount > 0 && !AbstractBruteForcePackager.isInsertableAfter(point, stackValue, stack, insertAfterCount, access)) {
							continue;
						}
						if(checkObstacles && !AbstractBruteForcePackager.isInsertable(point, stackValue, container.getObstacles(), access)) {
							continue;
						}
						if(checkExtraction && !AbstractBruteForcePackager.isExtractable(point, stackValue, stack, access)) {
							continue;
						}
						long supportedArea = 0L;
						if(utility != null) {
							utility.populatePointSupporters(point);
							utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
							supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
							if(supportedArea == -1L) {
								continue;
							}
						}
						int pointIndex = candidates != null ? candidates.getPointIndex(candidate) : candidate;

						Placement placement = placements[placedCount];
						placement.setStackValue(stackValue);
						placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
						if(utility != null) {
							placement.clearLoad();
							placement.setIndex(stack.size());
							placement.setSupportedArea(supportedArea);
						}
						pointCalculator.add(pointIndex, placement, point);
						stack.add(placement);
						if(utility != null) {
							utility.addSupportersLoad(placement);
						}
						if(level + 1 < length) {
							if(minAreas[level + 1] != minAreas[level]) {
								pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[level + 1], iterator.getMinBoxVolume(level + 1));
							} else {
								pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(level + 1));
							}
						}
						placedPermutations[placedCount] = box.getBoxItem().getLocalIndex();
						placedRotations[placedCount] = rotation;

						search(level + 1, placedCount + 1, placedVolume + box.getVolume(), freeLoadWeight - box.getWeight(), maxContainerPriority);

						if(utility != null) {
							placement.removeSupporteesAbove();
							for (PlacementLoad placementLoad : placement.getSupporters()) {
								placementLoad.getPlacement().removeLastSupportee();
							}
							placement.clearLoad();
						}
						stack.remove(stack.size() - 1);
						if(isComplete()) {
							pointCalculator.pop();
							return;
						}
						pointCalculator.redo();
					}
				}
				pointCalculator.pop();
			}
			if(skipping == null) {
				// without skipping, the arrangement ends at this box
				return;
			}
			int skipEnd = skipEnds != null ? skipEnds[level] : level + 1;
			if(skipEnd == -1) {
				return;
			}
			search(skipEnd, placedCount, placedVolume, freeLoadWeight, Math.min(maxContainerPriority, box.getBoxItem().getContainerPriority()));
		}
	}
}
