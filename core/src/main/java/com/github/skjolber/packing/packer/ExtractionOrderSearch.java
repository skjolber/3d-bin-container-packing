package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;

/**
 * Places the boxes which are extracted last first (see {@link com.github.skjolber.packing.api.BoxItem#withExtractionOrder(int)}), so that the boxes
 * which are extracted earlier are placed after them: between them and the door, or on top of them. Placing them in
 * the order of the search instead would often leave no room for the boxes extracted last, as a box must not be placed
 * where it blocks a box which is extracted earlier.
 * <br>
 * <br>
 * The box items are sorted by descending extraction order (within each container priority). The search starts with
 * the items of the highest extraction order, and widens to the items of the next order when none of them can be
 * placed. It stays wide for the rest of the container (or container priority), so that the remaining boxes of a
 * higher extraction order can still be placed where they fit.
 */
public class ExtractionOrderSearch {

	/** The lowest extraction order searched */
	protected int minExtractionOrder = Integer.MAX_VALUE;
	/** The container priority of the searched items */
	protected int containerPriority;

	/**
	 * @param controls placement controls
	 * @param boxItems box items, sorted by descending extraction order within each container priority
	 * @param end the end index of the items which may be placed (the items of one container priority)
	 * @return the placement, or null if none
	 */
	public Placement getPlacement(PlacementControls controls, BoxItemSource boxItems, int end) {
		int priority = boxItems.get(0).getContainerPriority();
		if(priority != containerPriority) {
			// a new container priority: start with its items of the highest extraction order
			containerPriority = priority;
			minExtractionOrder = Integer.MAX_VALUE;
		}
		while(true) {
			int extractionEnd = getExtractionOrderEnd(boxItems, end, minExtractionOrder);
			if(extractionEnd > 0) {
				Placement placement = controls.getPlacement(0, extractionEnd);
				if(placement != null) {
					return placement;
				}
			}
			if(extractionEnd == end) {
				return null;
			}
			minExtractionOrder = boxItems.get(extractionEnd).getExtractionOrder();
		}
	}

	/**
	 * Start again with the items of the highest extraction order, for example for a new level.
	 */
	public void reset() {
		minExtractionOrder = Integer.MAX_VALUE;
	}

	/**
	 * @return the end index of the items with at least the given extraction order
	 */
	protected static int getExtractionOrderEnd(BoxItemSource boxItems, int end, int minExtractionOrder) {
		int index = 0;
		while(index < end && boxItems.get(index).getExtractionOrder() >= minExtractionOrder) {
			index++;
		}
		return index;
	}
}
