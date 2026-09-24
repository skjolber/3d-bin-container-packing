package com.github.skjolber.packing.validator.load.reasons;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that a box of a different type was stacked on top of a placement that
 * requires identical boxes only, as specified by
 * {@link com.github.skjolber.packing.api.BoxStackValue#isLoadIdenticalBoxOnly()}.
 */
public class NonIdenticalLoadBoxReason implements ValidatorResultReason {

	private static final int CODE = 13;

	private final Placement constrainedPlacement;
	private final Placement offendingPlacement;
	private final BoxItem expectedBoxItem;
	private final BoxItem actualBoxItem;

	/**
	 * Creates a reason for an identical-box violation.
	 *
	 * @param constrainedPlacement the placement that requires identical boxes on top
	 * @param offendingPlacement the placement that violated the constraint
	 * @param expectedBoxItem the box item type required (the constrained placement's type)
	 * @param actualBoxItem the box item type found in the offending placement
	 */
	public NonIdenticalLoadBoxReason(Placement constrainedPlacement, Placement offendingPlacement,
			BoxItem expectedBoxItem, BoxItem actualBoxItem) {
		this.constrainedPlacement = constrainedPlacement;
		this.offendingPlacement = offendingPlacement;
		this.expectedBoxItem = expectedBoxItem;
		this.actualBoxItem = actualBoxItem;
	}

	/**
	 * Returns the placement that has the identical-box-only constraint.
	 *
	 * @return the constrained placement
	 */
	public Placement getConstrainedPlacement() {
		return constrainedPlacement;
	}

	/**
	 * Returns the placement that violated the identical-box-only constraint.
	 *
	 * @return the offending placement
	 */
	public Placement getOffendingPlacement() {
		return offendingPlacement;
	}

	/**
	 * Returns the box item type that was expected (same as the constrained placement).
	 *
	 * @return expected box item
	 */
	public BoxItem getExpectedBoxItem() {
		return expectedBoxItem;
	}

	/**
	 * Returns the box item type that was actually found on top.
	 *
	 * @return actual box item
	 */
	public BoxItem getActualBoxItem() {
		return actualBoxItem;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Non-identical box " + offendingPlacement + " stacked on top of " + constrainedPlacement
				+ " which requires identical boxes only";
	}
}
