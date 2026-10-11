package com.github.skjolber.packing.test.example;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * Per-box placement rules for {@linkplain ConstrainedPlacementControls}.
 * <p>
 * A {@linkplain BoxItem} has no user data of its own, but its {@linkplain Box} has properties, so the rule is marked on the box,
 * with {@code Box.newBuilder().withProperty(PlacementConstraint.PROPERTY, PlacementConstraint.GROUND_ONLY)}. It then applies to
 * every box item of that box. To give two box items of the same size different rules, create two boxes (with different ids).
 */
public enum PlacementConstraint {

	/** The box may only rest on placements of boxes with the same box id (the ground is always allowed). */
	SAME_TYPE_STACK,

	/** The box may only be placed on the floor of the container. */
	GROUND_ONLY;

	/** The key of the box property which holds the constraint. */
	public static final String PROPERTY = "placementConstraint";

	/**
	 * @param boxItem a box item
	 * @return the constraint of the box item's box, or null if it has none
	 */
	public static PlacementConstraint of(BoxItem boxItem) {
		return boxItem.getBox().getProperty(PROPERTY);
	}
}
