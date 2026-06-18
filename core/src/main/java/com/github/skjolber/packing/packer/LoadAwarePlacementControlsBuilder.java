package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;

/**
 * Builder for {@link LoadAwarePlacementControls}.
 */
public class LoadAwarePlacementControlsBuilder implements PlacementControlsBuilder<Placement> {

	protected BoxItemSource boxItems;
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;
	protected Comparator<Placement> placementComparator;
	protected Comparator<BoxItem> boxItemComparator;
	
	protected boolean maxLoadWeight;
	protected boolean maxLoadPressure;
	protected boolean maxLoadBoxCount;
	protected boolean loadIdenticalBox;
	
	protected boolean fullSupport;
	protected boolean calculateSupport;

	public LoadAwarePlacementControlsBuilder withPlacementComparator(Comparator<Placement> placementComparator) {
		this.placementComparator = placementComparator;
		return this;
	}

	public LoadAwarePlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withPointCalculator(PointCalculator pointCalculator) {
		this.pointCalculator = pointCalculator;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withBoxItems(BoxItemSource boxItems) {
		this.boxItems = boxItems;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withStack(Stack stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withOrder(Order order) {
		this.order = order;
		return this;
	}

	@Override
	public PlacementControlsBuilder<Placement> withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount) {
		// Load awareness is always active in this implementation
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;

		return this;
	}
	
	@Override
	public PlacementControlsBuilder<Placement> withLoadIdenticalBox(boolean loadIdenticalBox) {
		this.loadIdenticalBox = loadIdenticalBox;
		return this;
	}


	@Override
	public PlacementControlsBuilder<Placement> withStability(boolean calculateSupport, boolean fullSupport) {
		this.fullSupport = fullSupport;
		this.calculateSupport = calculateSupport;
		return this;
	}
	
	@Override
	public PlacementControls<Placement> build() {
		if(maxLoadWeight || maxLoadPressure || maxLoadBoxCount || loadIdenticalBox) {
			boolean maxLoadWeightOnly = maxLoadWeight && !maxLoadPressure && !maxLoadBoxCount;
			
			if(maxLoadWeightOnly) {
				return new WeightLoadAwarePlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator, fullSupport);
			}
			
			if(!loadIdenticalBox) {
				return new WeightPressureCountLoadAwarePlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator, fullSupport);
			}

			return new WeightPressureCountIdenticalLoadAwarePlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator, fullSupport);
		}
		
		
		if(fullSupport) {
			// Full placement support only
			return new FullSupportPlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		}
		
		if(calculateSupport) {
			// Calculate placement support so to use the value in comparisons
			return new SupportPlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		}

		return new ComparatorPlacementControls(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
	}

}