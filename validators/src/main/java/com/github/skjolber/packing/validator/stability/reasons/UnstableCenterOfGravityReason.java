package com.github.skjolber.packing.validator.stability.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.CenterOfGravitySupportStabilityValidator;

/**
 * Indicates that the centre of gravity of a placement (considering only the box itself,
 * not the stack above) lies outside the bounding box of its support contact patches,
 * meaning the box would topple under its own weight.
 *
 * @see CenterOfGravitySupportStabilityValidator
 */
public class UnstableCenterOfGravityReason implements ValidatorResultReason {

	private static final int CODE = 21;

	private final Placement placement;

	/**
	 * Creates a reason for a centre-of-gravity stability violation based on the box's own weight.
	 *
	 * @param placement the placement whose centre of gravity falls outside the support region
	 */
	public UnstableCenterOfGravityReason(Placement placement) {
		this.placement = placement;
	}

	/**
	 * Returns the placement whose centre of gravity falls outside its support region.
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
		return "Centre of gravity of placement " + placement + " lies outside the support region";
	}
}
