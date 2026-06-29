package com.github.skjolber.packing.packer.util;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;

/**
 * Utility for {@code WeightPressureCountLoadAwarePlacementControls}: validates
 * weight, max-load pressure, and max-load box-count constraints.
 */
public class WeightPressureCountLoadAwarePlacementUtility extends AbstractLoadWeightPlacementUtility {

	public WeightPressureCountLoadAwarePlacementUtility(Stack stack) {
		super(stack);
	}

	@Override
	public long calculateSupporteeLoad(BoxStackValue sv,
			int minX, int minY, int minZ, int maxX, int maxY) {
		long weight = 0;
		int z = minZ + sv.getDz();
		int stackSize = stack.size();
		for (int i = 0; i < stackSize; i++) {
			reliefWeights[i] = 0;
		}

		for (int k = 0; k < pointSupportees.size(); k++) {
			Placement candidate = pointSupportees.get(k);
			if (candidate.getAbsoluteZ() != z) {
				continue;
			}
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}

			long area = LoadPlacementUtility.overlapArea(minX, minY, maxX, maxY, candidate);
			long candidateWeight = candidate.getWeight();
			for (PlacementLoad pl : candidate.getSupportees()) {
				candidateWeight += pl.getWeight();
			}
			long effectiveWeight = (candidateWeight * area) / (area + candidate.getSupportedArea());

			if (sv.isMaxLoadPressure()) {
				if (sv.getMaxLoadPressure() * (double) area < (double) effectiveWeight) {
					return -1;
				}
			}

			calculateRelifWeight(candidate, effectiveWeight);
			weight += effectiveWeight;
		}

		if (sv.isMaxLoadWeight() && weight > sv.getMaxLoadWeight()) {
			return -1;
		}
		return weight + sv.getBox().getWeight();
	}

	@Override
	public boolean populateSupporters(BoxStackValue sv,
			int minX, int minY, int minZ, int maxX, int maxY) {
		placementSupporters.clear();
		int z = minZ - 1;
		for (int k = 0; k < pointSupporters.size(); k++) {
			Placement candidate = pointSupporters.get(k);
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			if (!candidate.isWithinMaxLoadBoxCount(1)) {
				return false;
			}
			placementSupporters.add(candidate);
		}
		return true;
	}

	/**
	 * Checks whether placing a box on top of {@code candidate} would violate its
	 * max-load box-count constraint, recursively through the support chain.
	 */
	protected boolean isWithinSupporteeBoxCount(Placement candidate, int count,
			PlacementList supportees, int minX, int minY, int maxX, int maxY) {
		BoxStackValue sv = candidate.getStackValue();
		if (sv.isMaxLoadBoxCount() && sv.getMaxLoadBoxCount() > count) {
			return true;
		}
		if (count <= 0) {
			return false;
		}
		count--;
		for (int k = 0; k < supportees.size(); k++) {
			Placement p = supportees.get(k);
			if (!p.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			if (!isWithinSupporteeBoxCount(p, count, supportees, minX, minY, maxX, maxY)) {
				return false;
			}
		}
		return true;
	}
}
