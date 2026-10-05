package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that a box is inserted after a box in its path from the container's opening, so the order of the placements
 * is not a possible insertion order.
 *
 * @see com.github.skjolber.packing.api.InsertionOrder
 */
public class BlockedInsertionReason implements ValidatorResultReason {

	private static final int CODE = 31;

	private final Placement placement;
	private final Placement blocking;

	public BlockedInsertionReason(Placement placement, Placement blocking) {
		this.placement = placement;
		this.blocking = blocking;
	}

	/**
	 * @return the box whose path is blocked
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * @return the box in its path, inserted before it
	 */
	public Placement getBlocking() {
		return blocking;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Placement " + placement + " is blocked by " + blocking + ", which is inserted before it";
	}
}
