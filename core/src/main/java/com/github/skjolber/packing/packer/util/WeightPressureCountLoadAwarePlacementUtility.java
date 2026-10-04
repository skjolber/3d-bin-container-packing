package com.github.skjolber.packing.packer.util;

import com.github.skjolber.packing.api.Box;
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
	public double calculateSupporteeLoad(BoxStackValue sv, int minX, int minY, int minZ, int maxX, int maxY) {
		double weight = 0.0;
		int z = minZ + sv.getDz();
		resetReliefWeights();

		for (int k = 0; k < pointSupportees.size(); k++) {
			Placement candidate = pointSupportees.get(k);
			if (candidate.getAbsoluteZ() != z) {
				continue;
			}
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}

			long area = candidate.overlapArea2D(minX, maxX, minY, maxY);
			double candidateWeight = candidate.getWeight() + candidate.getLoadWeight();
			double effectiveWeight = candidateWeight * area / (area + candidate.getSupportedArea());

			if (sv.isMaxLoadPressure()) {
				if (Box.calculatePressure(area, effectiveWeight) > sv.getMaxLoadPressure()) {
					return -1.0;
				}
			}
			if (sv.isMaxLoadBoxCount()) {
				if (!isWithinSupporteeBoxCount(candidate, sv.getMaxLoadBoxCount())) {
					return -1.0;
				}
			}

			calculateRelifWeight(candidate, effectiveWeight);
			weight += effectiveWeight;
		}

		if (sv.isMaxLoadWeight() && weight > sv.getMaxLoadWeight()) {
			return -1.0;
		}
		return weight + sv.getBox().getWeight();
	}

	@Override
	public boolean populateSupporters(BoxStackValue sv, int minX, int minY, int minZ, int maxX, int maxY) {
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
			if (!isWithinMaxLoadBoxCount(candidate, 1)) {
				return false;
			}
			placementSupporters.add(candidate);
		}
		return true;
	}
}
