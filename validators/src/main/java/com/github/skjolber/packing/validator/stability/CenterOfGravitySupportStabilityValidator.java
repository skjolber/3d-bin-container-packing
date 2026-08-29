package com.github.skjolber.packing.validator.stability;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.UnstableCenterOfGravityReason;

/**
 * Validates that the centre of gravity of each placement's own box lies within the
 * bounding box of the union of its support contact patches.
 *
 * <p>This check considers only the box itself; weight from boxes stacked above is
 * ignored. For a stricter check that includes the full stack above each placement,
 * use {@link CenterOfGravityStabilityValidator}.
 *
	 * <p>A box is considered stable when its configured centre of mass (CoM) lies
	 * within the axis-aligned bounding box
 * of the union of all support contact patches between this placement and its
 * supporters. A placement with no supporters is considered stable only when it
 * rests on the container floor ({@code z == 0}).
 *
 * @see Placement#getSupporters()
 */
public class CenterOfGravitySupportStabilityValidator implements StabilityValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Adds an {@link UnstableCenterOfGravityReason} for every placement whose own
	 * centre of gravity falls outside the support region of its direct supporters.
	 *
	 * @return {@code true} if every placement is stable under its own weight; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			if(!isPlacementStableSupport(placement)) {
				reasons.add(new UnstableCenterOfGravityReason(placement));
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Determines whether {@code placement} is stable given its current supporters,
	 * considering only the box's own weight (not the stack above).
	 *
	 * <p>A box is considered stable when its configured centre of gravity lies within the
	 * axis-aligned bounding box of the union of all support contact patches.
	 *
	 * @param placement the placement to check
	 * @return {@code true} if the centre of mass is within the support region
	 */
	public static boolean isPlacementStableSupport(Placement placement) {
		List<PlacementLoad> supporters = placement.getSupporters();

		if(supporters.isEmpty()) {
			return placement.getAbsoluteZ() == 0;
		}

		// Fast path: full footprint coverage guarantees the CoM is within the support region.
		BoxStackValue stackValue = placement.getStackValue();
		if(placement.getSupportedArea() >= stackValue.getArea()) {
			return true;
		}

		long centerOfGravity2X = getCenterOfGravity2X(placement, stackValue);
		long centerOfGravity2Y = getCenterOfGravity2Y(placement, stackValue);

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

		return centerOfGravity2X >= 2L * minSupportX && centerOfGravity2X <= 2L * maxSupportX
				&& centerOfGravity2Y >= 2L * minSupportY && centerOfGravity2Y <= 2L * maxSupportY;
	}

	static long getCenterOfGravity2X(Placement placement, BoxStackValue stackValue) {
		int centerOfGravityX = stackValue.getCenterOfGravityX();
		if(centerOfGravityX == -1) {
			return 2L * placement.getAbsoluteX() + stackValue.getDx();
		}
		return 2L * (placement.getAbsoluteX() + centerOfGravityX);
	}

	static long getCenterOfGravity2Y(Placement placement, BoxStackValue stackValue) {
		int centerOfGravityY = stackValue.getCenterOfGravityY();
		if(centerOfGravityY == -1) {
			return 2L * placement.getAbsoluteY() + stackValue.getDy();
		}
		return 2L * (placement.getAbsoluteY() + centerOfGravityY);
	}
}
