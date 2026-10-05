package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that a box is inserted before the box it rests on, so the order of the placements is not a possible insertion order.
 *
 * @see com.github.skjolber.packing.api.InsertionOrder
 */
public class InsertedBeforeSupporterReason implements ValidatorResultReason {

	private static final int CODE = 30;

	private final Placement placement;
	private final Placement supporter;

	public InsertedBeforeSupporterReason(Placement placement, Placement supporter) {
		this.placement = placement;
		this.supporter = supporter;
	}

	/**
	 * @return the box inserted too early
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * @return the box it rests on, inserted after it
	 */
	public Placement getSupporter() {
		return supporter;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Placement " + placement + " rests on " + supporter + ", which is inserted after it";
	}
}
