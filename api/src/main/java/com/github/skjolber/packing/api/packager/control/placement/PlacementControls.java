package com.github.skjolber.packing.api.packager.control.placement;

import java.util.List;

import com.github.skjolber.packing.api.Placement;

public interface PlacementControls {

	/**
	 * Get the next placement, selected among the box items in the range. The range is the box items of the current group when
	 * packing box item groups, otherwise the box items which may be placed in the container now.
	 * <p>
	 * The indexes are local indexes into the {@link com.github.skjolber.packing.api.packager.BoxItemSource} which the controls
	 * were built for: the range is {@code offset} (inclusive) to {@code offset + length} (exclusive), and the indexes change as
	 * box items are removed from the source.
	 * <p>
	 * To honor point controls (filters), take the candidate points of each box item from
	 * {@link com.github.skjolber.packing.api.packager.control.point.PointControls#getPoints(com.github.skjolber.packing.api.BoxItem)
	 * PointControls.getPoints(boxItem)}, not directly from the point calculator: point controls have no effect on controls which
	 * do not consult them.
	 * 
	 * @param offset local index of the first box item to consider
	 * @param length the number of box items to consider
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
	 * @param placements placements
	 */
	
	default void undo(List<Placement> placements) {
		// no-op by default
	}
	
}
