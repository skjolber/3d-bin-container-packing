package com.github.skjolber.packing.validator.stability.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.CenterOfGravityStabilityValidator;

/**
 * Indicates that the effective centre of gravity of a placement's entire vertical
 * stack (this box plus all boxes above it, weighted proportionally) lies outside
 * the bounding box of its support contact patches, meaning the stack would topple.
 *
 * @see CenterOfGravityStabilityValidator
 */
public class UnstableStackCenterOfGravityReason implements ValidatorResultReason {

	private static final int CODE = 22;

	private final Placement placement;

	/**
	 * Creates a reason for a stack centre-of-gravity stability violation.
	 *
	 * @param placement the placement whose stack centre of gravity falls outside the support region
	 */
	public UnstableStackCenterOfGravityReason(Placement placement) {
		this.placement = placement;
	}

	/**
	 * Returns the placement whose stack centre of gravity falls outside its support region.
	 *
	 * @return the violating placement
	 */
	public Placement getPlacement() {
		return placement;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Stack centre of gravity of placement " + placement + " lies outside the support region";
	}
}
