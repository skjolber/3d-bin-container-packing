package com.github.skjolber.packing.api.packager;

import java.util.function.Supplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.NoopPlacementFilter;
import com.github.skjolber.packing.api.ep.PlacementFilter;
import com.github.skjolber.packing.api.ep.PlacementFilterBuilder;

/**
 * 
 * Container item wrapped with some constraints.
 * 
 */

public class CompositeContainerItem {

	protected final ContainerItem containerItem;
	protected Supplier<BoxItemGroupListenerBuilder<?>> boxItemGroupListenerBuilderSupplier;
	protected Supplier<BoxItemListenerBuilder<?>> boxItemListenerBuilderSupplier;
	protected Supplier<PlacementFilterBuilder<?>> placementFilterBuilderSupplier;

	public CompositeContainerItem(ContainerItem containerItem) {
		this.containerItem = containerItem;
	}
	
	public void setBoxItemListenerBuilderSupplier(
			Supplier<BoxItemListenerBuilder<?>> boxItemListenerBuilderSupplier) {
		this.boxItemListenerBuilderSupplier = boxItemListenerBuilderSupplier;
	}
	
	public void setPlacementFilterBuilderSupplier(Supplier<PlacementFilterBuilder<?>> pointListenerBuilderSupplier) {
		this.placementFilterBuilderSupplier = pointListenerBuilderSupplier;
	}

	public Supplier<BoxItemListenerBuilder<?>> getBoxItemListenerBuilderSupplier() {
		return boxItemListenerBuilderSupplier;
	}
	
	public ContainerItem getContainerItem() {
		return containerItem;
	}
	
	public Supplier<PlacementFilterBuilder<?>> getPlacementFilterBuilderSupplier() {
		return placementFilterBuilderSupplier;
	}

	public void setBoxItemGroupListenerBuilderSupplier(
			Supplier<BoxItemGroupListenerBuilder<?>> boxItemGroupListenerBuilderSupplier) {
		this.boxItemGroupListenerBuilderSupplier = boxItemGroupListenerBuilderSupplier;
	}
	
	public Supplier<BoxItemGroupListenerBuilder<?>> getBoxItemGroupListenerBuilderSupplier() {
		return boxItemGroupListenerBuilderSupplier;
	}

	public BoxItemListener createBoxItemListener(Container container, Stack stack, FilteredBoxItems filteredBoxItems) {
		if(boxItemListenerBuilderSupplier == null) {
			return NoopBoxItemListener.getInstance();
		}
		return boxItemListenerBuilderSupplier.get().withContainer(container).withStack(stack).withFilteredBoxItems(filteredBoxItems).build();
	}
	
	public PlacementFilter createPlacementFilter(Container container, Stack stack, ExtremePoints extremePoints) {
		if(placementFilterBuilderSupplier == null) {
			return NoopPlacementFilter.getInstance();
		}
		return placementFilterBuilderSupplier.get().withContainer(container).withStack(stack).withExtremePoints(extremePoints).build();
	}
}
