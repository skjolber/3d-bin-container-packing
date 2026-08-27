package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Fast brute-force packager which evaluates per-box load weight, pressure,
 * box-count, and identical-box constraints while selecting placements.
 */
public class LoadFastBruteForcePackager extends FastBruteForcePackager {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends FastBruteForcePackagerBuilder {

		@Override
		public Builder withComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = comparator;
			return this;
		}

		@Override
		public LoadFastBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			return new LoadFastBruteForcePackager(comparator);
		}
	}

	public LoadFastBruteForcePackager(Comparator<IntermediatePackagerResult> comparator) {
		super(comparator);
	}

	@Override
	protected boolean supportsLoad() {
		return true;
	}

	@Override
	public int packStackPlacement(FastPointCalculator3DStack pointCalculator, List<Placement> placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad) {
		LoadPlacementUtility utility = LoadBruteForcePackager.createLoadPlacementUtility(iterator, stack);
		if(utility == null) {
			return super.packStackPlacement(pointCalculator, placements, iterator, stack, container, placementIndex, interrupt, minStackableAreaIndex, freeWeightLoad);
		}

		utility.initialize(placements.size());
		rebuildLoad(stack, utility);

		while (placementIndex < iterator.length()) {
			if(interrupt.getAsBoolean()) {
				return Integer.MIN_VALUE;
			}

			BoxStackValue stackValue = iterator.getStackValue(placementIndex);
			Box box = stackValue.getBox();
			if(box.getWeight() > freeWeightLoad) {
				break;
			}

			int bestPointIndex = -1;
			long bestSupportedArea = -1L;
			int pointCount = pointCalculator.size();
			for (int k = 0; k < pointCount; k++) {
				Point point = pointCalculator.get(k);
				if(!point.fits3D(stackValue)) {
					continue;
				}

				utility.populatePointSupporters(point);
				utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
				long supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
				if(supportedArea == -1L) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point bestPoint = pointCalculator.get(bestPointIndex);
					if(bestPoint.getArea() < point.getArea()) {
						continue;
					}
					if(bestPoint.getArea() == point.getArea() && bestPoint.getVolume() < point.getVolume()) {
						continue;
					}
				}
				bestPointIndex = k;
				bestSupportedArea = supportedArea;
			}

			if(bestPointIndex == -1) {
				break;
			}

			Point point = pointCalculator.get(bestPointIndex);
			// Candidate evaluation leaves the utility primed for the last point checked,
			// so repopulate its caches for the selected point before adding the load.
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			bestSupportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);

			Placement placement = placements.get(placementIndex);
			placement.clearLoad();
			placement.setStackValue(stackValue);
			placement.setPoint(point);
			placement.setIndex(stack.size());
			placement.setSupportedArea(bestSupportedArea);

			pointCalculator.add(bestPointIndex, placement);
			stack.add(placement);
			utility.addSupportersLoad(placement);

			freeWeightLoad -= box.getWeight();
			placementIndex++;

			if(placementIndex < iterator.length()) {
				if(placementIndex == minStackableAreaIndex) {
					minStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex);
					pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(placementIndex));
				} else {
					pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(placementIndex));
				}
			}
		}

		return placementIndex;
	}

	private static void rebuildLoad(Stack stack, LoadPlacementUtility utility) {
		List<Placement> prefix = new ArrayList<>(stack.getPlacements());
		stack.clear();
		for (Placement placement : prefix) {
			placement.clearLoad();
		}
		for (Placement placement : prefix) {
			Point point = new DefaultPoint3D(placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(), placement.getAbsoluteEndX(), placement.getAbsoluteEndY(), placement.getAbsoluteEndZ());
			BoxStackValue stackValue = placement.getStackValue();
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			long supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
			placement.setIndex(stack.size());
			placement.setSupportedArea(supportedArea);
			stack.add(placement);
			utility.addSupportersLoad(placement);
		}
	}
}
