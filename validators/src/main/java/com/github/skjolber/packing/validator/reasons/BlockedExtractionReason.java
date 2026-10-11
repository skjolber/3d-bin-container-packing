package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that a box cannot be extracted before a box with a later extraction order (see
 * {@link com.github.skjolber.packing.api.BoxItem#withExtractionOrder(int)}): the later box rests on it, or is in its
 * path to the container's opening.
 */
public class BlockedExtractionReason implements ValidatorResultReason {

	private static final int CODE = 33;

	private final Placement placement;
	private final Placement blocking;

	/**
	 * @param placement the box which is extracted first
	 * @param blocking the box which is extracted later, but rests on it or is in its path
	 */
	public BlockedExtractionReason(Placement placement, Placement blocking) {
		this.placement = placement;
		this.blocking = blocking;
	}

	/**
	 * @return the box which is extracted first
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * @return the box which is extracted later, but rests on it or is in its path
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
		return "Placement " + placement + " with extraction order " + placement.getBoxItem().getExtractionOrder() + " is blocked by " + blocking
				+ " with extraction order " + blocking.getBoxItem().getExtractionOrder() + ", which rests on it or is in its path";
	}
}
