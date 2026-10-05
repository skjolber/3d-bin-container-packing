package com.github.skjolber.packing.packer.util;

import java.util.IdentityHashMap;
import java.util.Map;

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
public class WeightPressureCountIdenticalLoadAwarePlacementUtility extends WeightPressureCountLoadAwarePlacementUtility {

	/** The box of all boxes resting on the current candidate (placed under boxes already there), or null if none. */
	protected Box carriedBox;
	/** Whether the boxes resting on the current candidate are not all the same box. */
	protected boolean carriedMixed;

	public WeightPressureCountIdenticalLoadAwarePlacementUtility(Stack stack) {
		super(stack);
	}

	/**
	 * Add the boxes of {@code placement} and everything resting on it to {@link #carriedBox}.
	 *
	 * @return false if they are not all the same box
	 */
	private boolean collectCarriedBoxes(Placement placement) {
		Box box = placement.getBox();
		if(carriedBox == null) {
			carriedBox = box;
		} else if(carriedBox != box) {
			return false;
		}
		for (PlacementLoad load : placement.getSupportees()) {
			if(!collectCarriedBoxes(load.getPlacement())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public double calculateSupporteeLoad(BoxStackValue sv, int minX, int minY, int minZ, int maxX, int maxY) {
		double weight = 0.0;
		int z = minZ + sv.getDz();
		resetReliefWeights();
		supporteeHeight = 0;
		Map<Placement, Integer> heights = null;
		carriedBox = null;
		carriedMixed = false;

		for (int k = 0; k < pointSupportees.size(); k++) {
			Placement candidate = pointSupportees.get(k);
			if (candidate.getAbsoluteZ() != z) {
				continue;
			}
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}

			long area = candidate.overlapArea2D(minX, maxX, minY, maxY);
			double effectiveWeight = addSupporteeShare(candidate, area);

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
			if (sv.isLoadIdenticalBoxOnly()) {
				if (candidate.getBox() != sv.getBox()) {
					return -1.0;
				}
			}

			// placed under boxes which are already there: the boxes below carry them too
			if(heights == null) {
				heights = new IdentityHashMap<>();
			}
			supporteeHeight = Math.max(supporteeHeight, getStackHeight(candidate, heights));
			if(!collectCarriedBoxes(candidate)) {
				carriedMixed = true;
			}
			weight += effectiveWeight;
		}

		if (sv.isMaxLoadWeight() && weight > sv.getMaxLoadWeight()) {
			return -1.0;
		}
		return weight + sv.getBox().getWeight();
	}

	@Override
	public boolean populateSupporters(BoxStackValue sv, int minX, int minY, int minZ, int maxX, int maxY) {
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
				Box candidateBox = candidate.getStackValue().getBox();
				// the boxes resting on the new box rest on the candidate too
				if (candidateBox != box || carriedMixed || (carriedBox != null && carriedBox != candidateBox)) {
					return false;
				}
			}
			if (!isWithinMaxLoadBoxCount(candidate, 1 + supporteeHeight)) {
				return false;
			}
			placementSupporters.add(candidate);
		}
		return true;
	}
}
