package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that the boxes of a box item group are not inserted together: a box of another group is inserted between
 * them (see {@link com.github.skjolber.packing.api.BoxItemGroup}).
 */
public class InterleavedGroupReason implements ValidatorResultReason {

	private static final int CODE = 34;

	private final Placement placement;
	private final Placement other;

	/**
	 * @param placement a box of the group, inserted after {@code other}
	 * @param other a box of another group, inserted after earlier boxes of the group
	 */
	public InterleavedGroupReason(Placement placement, Placement other) {
		this.placement = placement;
		this.other = other;
	}

	/**
	 * @return a box of the group, inserted after the other box
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * @return a box of another group, inserted between boxes of the group
	 */
	public Placement getOther() {
		return other;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Placement " + placement + " is inserted after " + other + " of another box item group, which is inserted between the boxes of its group";
	}
}
