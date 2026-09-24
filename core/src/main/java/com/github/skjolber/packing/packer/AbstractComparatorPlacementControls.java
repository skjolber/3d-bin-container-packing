package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;

public abstract class AbstractComparatorPlacementControls extends AbstractPlacementControls {

	protected PlacementComparator placementComparator;
	protected Comparator<BoxItem> boxItemComparator;
	private Placement recyclablePlacement;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, pointControls, pointCalculator, container, stack, order);
		
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	/**
	 * Returns a placement which is not referenced by the stack or by the current
	 * best candidate. Placement controls are created per packing operation, so a
	 * single recycled loser is sufficient to avoid allocating for every point and
	 * rotation considered by the comparator.
	 */
	protected Placement acquirePlacement() {
		Placement placement = recyclablePlacement;
		if(placement == null) {
			return new Placement();
		}

		recyclablePlacement = null;
		placement.clearLoad();
		placement.setProperties(null);
		placement.setIndex(0);
		return placement;
	}

	/**
	 * Selects the better placement and retains the loser for the next candidate.
	 */
	protected Placement selectPlacement(Placement current, Placement candidate) {
		if(current == null) {
			return candidate;
		}
		if(placementComparator.compare(current, candidate) >= 0) {
			recyclablePlacement = candidate;
			return current;
		}

		recyclablePlacement = current;
		return candidate;
	}

}
