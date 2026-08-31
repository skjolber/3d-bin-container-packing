package com.github.skjolber.packing.validator.stability;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.UnstableStackCenterOfGravityReason;

/**
 * Validates that the effective centre of gravity of each placement's entire vertical
 * stack — this box plus all boxes above it, weighted proportionally by their shared
 * support area — lies within the bounding box of the union of its support contact patches.
 *
 * <p>This is stricter than {@link CenterOfGravitySupportStabilityValidator} because it
 * accounts for the off-centre weight contribution of every box stacked above. A heavy
 * overhanging box on top can shift the effective centre of gravity outside the support
 * region even when the bottom box's own centre of gravity is within bounds.
 *
 * <p>A placement with no supporters is considered stable only when it rests on the
 * container floor ({@code z == 0}).
 *
 * @see Placement#getSupporters()
 */
public class CenterOfGravityStabilityValidator implements StabilityValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Adds an {@link UnstableStackCenterOfGravityReason} for every placement whose
	 * stack centre of gravity falls outside the support region of its direct supporters.
	 *
	 * @return {@code true} if every placement's stack is stable; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			if(!isPlacementStable(placement)) {
				reasons.add(new UnstableStackCenterOfGravityReason(placement));
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Determines whether {@code placement} is stable considering both its supporters
	 * and the weight of every box stacked above it (its supportees, recursively).
	 *
	 * <p>The effective centre of mass (CoM) is computed as the weighted average of
	 * the configured centres of gravity of this box and all boxes in its supportee sub-tree.
	 * When a box above is shared between multiple supporters (split load), its
	 * contribution to this sub-tree is scaled by
	 * {@code overlapArea / supportee.supportedArea}, matching the same proportion
	 * used during load propagation.
	 *
	 * <p>The effective CoM is tested against the axis-aligned bounding box of the
	 * union of all support contact patches.
	 *
	 * @param placement the placement to check
	 * @return {@code true} if the effective centre of mass of the stack lies within
	 *         the support region
	 */
	public static boolean isPlacementStable(Placement placement) {
		List<PlacementLoad> supporters = placement.getSupporters();

		if(supporters.isEmpty()) {
			return placement.getAbsoluteZ() == 0;
		}

// Full footprint coverage guarantees stability only when no boxes above can shift the combined CoM.
		BoxStackValue stackValue = placement.getStackValue();
		if(placement.getSupportees().isEmpty() && placement.getSupportedArea() == stackValue.getArea()) {
			return true;
		}

		int minSupportX = Integer.MAX_VALUE;
		int maxSupportX = Integer.MIN_VALUE;
		int minSupportY = Integer.MAX_VALUE;
		int maxSupportY = Integer.MIN_VALUE;

		for(PlacementLoad supporterLink : supporters) {
			Placement supporter = supporterLink.getPlacement();

			int overlapMinX = Math.max(placement.getAbsoluteX(), supporter.getAbsoluteX());
			int overlapMaxX = Math.min(placement.getAbsoluteEndX(), supporter.getAbsoluteEndX());
			int overlapMinY = Math.max(placement.getAbsoluteY(), supporter.getAbsoluteY());
			int overlapMaxY = Math.min(placement.getAbsoluteEndY(), supporter.getAbsoluteEndY());

			if(overlapMinX < minSupportX) minSupportX = overlapMinX;
			if(overlapMaxX > maxSupportX) maxSupportX = overlapMaxX;
			if(overlapMinY < minSupportY) minSupportY = overlapMinY;
			if(overlapMaxY > maxSupportY) maxSupportY = overlapMaxY;
		}

		// Accumulate weighted CoM of this box + the proportional share of every
		// box above it. Values are scaled by 1000 to survive proportional division.
		long[] stack = accumulateStackCenterOfMass(placement, 1000L);
		long totalWeight = stack[0];
		if(totalWeight == 0) {
			// A zero-weight stack has no weighted centre, so use this box's CoG.
			long centerOfGravity2X = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2X(placement, stackValue);
			long centerOfGravity2Y = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2Y(placement, stackValue);
			return centerOfGravity2X >= 2L * minSupportX && centerOfGravity2X <= 2L * maxSupportX
					&& centerOfGravity2Y >= 2L * minSupportY && centerOfGravity2Y <= 2L * maxSupportY;
		}

		// The share scale cancels in the weighted average. Coordinates remain doubled.
		long centerOfGravity2X = stack[1] / totalWeight;
		long centerOfGravity2Y = stack[2] / totalWeight;

		return centerOfGravity2X >= 2L * minSupportX && centerOfGravity2X <= 2L * maxSupportX
				&& centerOfGravity2Y >= 2L * minSupportY && centerOfGravity2Y <= 2L * maxSupportY;
	}
	
	protected static long[] accumulateStackCenterOfMass(Placement placement, long share) {
		BoxStackValue stackValue = placement.getStackValue();
		long w = (long) placement.getWeight() * share;
		long centerOfGravity2X = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2X(placement, stackValue);
		long centerOfGravity2Y = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2Y(placement, stackValue);

		long totalWeight  = w;
		long weightedComX = w * centerOfGravity2X;
		long weightedComY = w * centerOfGravity2Y;

		for(PlacementLoad supporteeLink : placement.getSupportees()) {
			Placement supportee = supporteeLink.getPlacement();
			long supporteeArea = supportee.getSupportedArea();
			if(supporteeArea == 0) {
				continue;
			}

			long overlapMinX = Math.max(placement.getAbsoluteX(), supportee.getAbsoluteX());
			long overlapMaxX = Math.min(placement.getAbsoluteEndX(), supportee.getAbsoluteEndX());
			long overlapMinY = Math.max(placement.getAbsoluteY(), supportee.getAbsoluteY());
			long overlapMaxY = Math.min(placement.getAbsoluteEndY(), supportee.getAbsoluteEndY());

			if(overlapMinX > overlapMaxX || overlapMinY > overlapMaxY) {
				continue;
			}

			long overlapArea = (overlapMaxX - overlapMinX + 1) * (overlapMaxY - overlapMinY + 1);
			long supporteeShare = (share * overlapArea) / supporteeArea;
			if(supporteeShare == 0) {
				continue;
			}

			long[] sub = accumulateStackCenterOfMass(supportee, supporteeShare);
			totalWeight  += sub[0];
			weightedComX += sub[1];
			weightedComY += sub[2];
		}

		return new long[] { totalWeight, weightedComX, weightedComY };
	}
}
