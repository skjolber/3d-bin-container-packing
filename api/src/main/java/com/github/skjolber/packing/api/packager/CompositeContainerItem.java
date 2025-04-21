package com.github.skjolber.packing.api.packager;

import java.util.function.Supplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilder;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderSupplier;
import com.github.skjolber.packing.api.packager.BoxItemGroupListenerBuilder.BoxItemGroupListenerBuilderSupplier;
import com.github.skjolber.packing.api.packager.BoxItemListenerBuilder.BoxItemListenerBuilderSupplier;

/**
 * 
 * Container item wrapped with some constraints.
 * 
 */

public class CompositeContainerItem {

	protected final ContainerItem containerItem;
	protected BoxItemGroupListenerBuilderSupplier<?> boxItemGroupListenerBuilderSupplier;
	protected BoxItemListenerBuilderSupplier<?> boxItemListenerBuilderSupplier;
	protected FilteredPointsBuilderSupplier<?> filteredPointsBuilderSupplier;

	public CompositeContainerItem(ContainerItem containerItem) {
		this.containerItem = containerItem;
	}
	
	public void setBoxItemListenerBuilderSupplier(
			BoxItemListenerBuilderSupplier<?> boxItemListenerBuilderSupplier) {
		this.boxItemListenerBuilderSupplier = boxItemListenerBuilderSupplier;
	}
	
	public void setFilteredPointsBuilderSupplier(FilteredPointsBuilderSupplier<?> supplier) {
		this.filteredPointsBuilderSupplier = supplier;
	}

	public BoxItemListenerBuilderSupplier<?> getBoxItemListenerBuilderSupplier() {
		return boxItemListenerBuilderSupplier;
	}
	
	public ContainerItem getContainerItem() {
		return containerItem;
	}
	
	public FilteredPointsBuilderSupplier<?> getFilteredPointsBuilderSupplier() {
		return filteredPointsBuilderSupplier;
	}

	public void setBoxItemGroupListenerBuilderSupplier(
			BoxItemGroupListenerBuilderSupplier<?> boxItemGroupListenerBuilderSupplier) {
		this.boxItemGroupListenerBuilderSupplier = boxItemGroupListenerBuilderSupplier;
	}
	
	public BoxItemGroupListenerBuilderSupplier<?> getBoxItemGroupListenerBuilderSupplier() {
		return boxItemGroupListenerBuilderSupplier;
	}

	public BoxItemGroupListener createBoxItemGroupListener(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredPoints points) {
		if(boxItemGroupListenerBuilderSupplier == null) {
			return NoopBoxItemGroupListener.getInstance();
		}
		return boxItemGroupListenerBuilderSupplier.get()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}

	public BoxItemListener createBoxItemListener(Container container, Stack stack, FilteredBoxItems filteredBoxItems, FilteredPoints points) {
		if(boxItemListenerBuilderSupplier == null) {
			return NoopBoxItemListener.getInstance();
		}
		return boxItemListenerBuilderSupplier.get()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItems(filteredBoxItems)
				.withPoints(points)
				.build();
	}
	
	public FilteredPoints createFilteredPoints(Container container, Stack stack, FilteredPoints points) {
		if(filteredPointsBuilderSupplier == null) {
			return points;
		}
		return filteredPointsBuilderSupplier.get()
				.withContainer(container)
				.withStack(stack)
				.withPoints(points)
				.build();
	}
}
