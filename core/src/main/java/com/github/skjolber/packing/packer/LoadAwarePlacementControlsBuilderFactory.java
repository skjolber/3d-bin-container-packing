package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.PlacementComparator;

/**
 * Factory that produces {@link LoadAwarePlacementControlsBuilder} instances.
 */
public class LoadAwarePlacementControlsBuilderFactory implements PlacementControlsBuilderFactory {

	protected final PlacementComparator placementComparator;
	protected final Comparator<BoxItem> boxItemComparator;
	protected final boolean requireFullSupport;
	protected final boolean calculateSupport;
	
	public LoadAwarePlacementControlsBuilderFactory(PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator, boolean calculateSupport, boolean requireFullSupport) {
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
		this.requireFullSupport = requireFullSupport;
		this.calculateSupport = calculateSupport;
	}

	@Override
	public PlacementControlsBuilder createPlacementControlsBuilder() {
		return new LoadAwarePlacementControlsBuilder()
				.withPlacementComparator(placementComparator)
				.withBoxItemComparator(boxItemComparator)
				.withStability(calculateSupport, requireFullSupport);
	}
}