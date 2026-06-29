package com.github.skjolber.packing.packer.util;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;

/**
 * Utility for {@code WeightPressureCountIdenticalLoadAwarePlacementControls}:
 * validates weight, max-load pressure, max-load box-count, and the
 * identical-only stacking restriction.
 */
public class WeightPressureCountIdenticalLoadAwarePlacementUtil
		extends WeightPressureCountLoadAwarePlacementUtil {

	public WeightPressureCountIdenticalLoadAwarePlacementUtil(Stack stack) {
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

			long area = LoadWeightPlacementUtil.overlapArea(minX, minY, maxX, maxY, candidate);
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
			if (sv.isMaxLoadBoxCount()) {
				if (!isWithinSupporteeBoxCount(candidate, sv.getMaxLoadBoxCount(),
						pointSupportees, minX, minY, maxX, maxY)) {
					return -1;
				}
			}
			if (sv.isLoadIdenticalBoxOnly()) {
				if (candidate.getBox() != sv.getBox()) {
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
		Box box = sv.getBox();
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
			if (candidate.getStackValue().isLoadIdenticalBoxOnly()) {
				if (candidate.getStackValue().getBox() != box) {
					return false;
				}
			}
			if (!candidate.isWithinMaxLoadBoxCount(1)) {
				return false;
			}
			placementSupporters.add(candidate);
		}
		return true;
	}
}
