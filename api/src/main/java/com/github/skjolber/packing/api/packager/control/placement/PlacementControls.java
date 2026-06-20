package com.github.skjolber.packing.api.packager.control.placement;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;

public interface PlacementControls {

	/**
	 * Get the next placement.
	 * 
	 * @param offset start offset
	 * @param length length
	 * @return the next placement, or null if not available.
	 */
	
	Placement getPlacement(int offset, int length);

	/**
	 * Called after a placement has been accepted and added to the stack.
	 * Implementations may use this hook to update load-graph relationships
	 * (i.e., call {@link Placement#addLoad}) between the newly placed box
	 * and its direct supporters.
	 * <p>
	 * The default implementation is a no-op.
	 *
	 * @param placement the placement that was just accepted
	 */
	default void accepted(Placement placement) {
		// no-op by default
	}

	/**
	 * 
	 * Notify box cannot be fitted, even it was previously accepted; usually because
	 * fitting the whole group was not possible.
	 * 
	 * @param boxItems {@linkplain BoxItem}
	 */
	
	default void undo(List<Placement> boxItems) {
		// no-op by default
	}
	
}
