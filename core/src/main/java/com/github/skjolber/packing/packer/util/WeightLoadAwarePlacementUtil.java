package com.github.skjolber.packing.packer.util;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;

/**
 * Utility for {@code WeightLoadAwarePlacementControls}: validates weight
 * constraints only.  The {@link #populateSupporters} method never rejects
 * (always returns {@code true}) because there are no box-count or
 * identical-only checks in this variant.
 */
public class WeightLoadAwarePlacementUtil extends AbstractLoadWeightPlacementUtility {

	public WeightLoadAwarePlacementUtil(Stack stack) {
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
			placementSupporters.add(candidate);
		}
		return true;
	}
}
