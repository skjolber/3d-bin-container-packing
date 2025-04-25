package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderFactory;

/**
 * 
 * Container item wrapped with some constraints.
 * 
 */

public class CompositeContainerItem {

	protected final ContainerItem containerItem;
	protected BoxItemGroupListenerBuilderFactory boxItemGroupListenerBuilderFactory;
	protected BoxItemListenerBuilderFactory boxItemListenerBuilderFactory;
	protected FilteredPointsBuilderFactory filteredPointsBuilderFactory;

	public CompositeContainerItem(ContainerItem containerItem) {
		this.containerItem = containerItem;
	}
	
	public void setBoxItemListenerBuilderFactory(
			BoxItemListenerBuilderFactory boxItemListenerBuilderSupplier) {
		this.boxItemListenerBuilderFactory = boxItemListenerBuilderSupplier;
	}
	
	public void setFilteredPointsBuilderFactory(FilteredPointsBuilderFactory supplier) {
		this.filteredPointsBuilderFactory = supplier;
	}

	public BoxItemListenerBuilderFactory getBoxItemListenerBuilderFactory() {
		return boxItemListenerBuilderFactory;
	}
	
	public ContainerItem getContainerItem() {
		return containerItem;
	}
	
	public FilteredPointsBuilderFactory getFilteredPointsBuilderFactory() {
		return filteredPointsBuilderFactory;
	}

	public void setBoxItemGroupListenerBuilderSupplier(
			BoxItemGroupListenerBuilderFactory factory) {
		this.boxItemGroupListenerBuilderFactory = factory;
	}
	
	public BoxItemGroupListenerBuilderFactory getBoxItemGroupListenerBuilderSupplier() {
		return boxItemGroupListenerBuilderFactory;
	}

	public BoxItemGroupListener createBoxItemGroupListener(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredPoints points) {
		if(boxItemGroupListenerBuilderFactory == null) {
			return NoopBoxItemGroupListener.getInstance();
		}
		return boxItemGroupListenerBuilderFactory.createBoxItemGroupListenerBuilder()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}

	public BoxItemListener createBoxItemListener(Container container, Stack stack, FilteredBoxItems filteredBoxItems, FilteredPoints points) {
		if(boxItemListenerBuilderFactory == null) {
			return NoopBoxItemListener.getInstance();
		}
		return boxItemListenerBuilderFactory.createBoxItemListenerBuilder()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItems(filteredBoxItems)
				.withPoints(points)
				.build();
	}
	
	public FilteredPoints createFilteredPoints(Container container, Stack stack, FilteredPoints points) {
		if(filteredPointsBuilderFactory == null) {
			return points;
		}
		return filteredPointsBuilderFactory.createFilteredPointsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withPoints(points)
				.build();
	}
}
