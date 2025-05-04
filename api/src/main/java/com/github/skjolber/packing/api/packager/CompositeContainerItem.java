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
	protected BoxItemGroupControlsBuilderFactory boxItemGroupListenerBuilderFactory;
	protected BoxItemControlsBuilderFactory boxItemListenerBuilderFactory;

	public CompositeContainerItem(ContainerItem containerItem) {
		this.containerItem = containerItem;
	}
	
	public void setBoxItemListenerBuilderFactory(
			BoxItemControlsBuilderFactory boxItemListenerBuilderSupplier) {
		this.boxItemListenerBuilderFactory = boxItemListenerBuilderSupplier;
	}
	
	public BoxItemControlsBuilderFactory getBoxItemListenerBuilderFactory() {
		return boxItemListenerBuilderFactory;
	}
	
	public ContainerItem getContainerItem() {
		return containerItem;
	}

	public void setBoxItemGroupListenerBuilderSupplier(
			BoxItemGroupControlsBuilderFactory factory) {
		this.boxItemGroupListenerBuilderFactory = factory;
	}
	
	public BoxItemGroupControlsBuilderFactory getBoxItemGroupListenerBuilderSupplier() {
		return boxItemGroupListenerBuilderFactory;
	}

	public BoxItemGroupControls createBoxItemGroupListener(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredPoints points) {
		if(boxItemGroupListenerBuilderFactory == null) {
			return new DefaultBoxItemGroupControls(points);
		}
		return boxItemGroupListenerBuilderFactory.createBoxItemGroupControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}

	public BoxItemControls createBoxItemListener(Container container, Stack stack, FilteredBoxItems filteredBoxItems, FilteredPoints points) {
		if(boxItemListenerBuilderFactory == null) {
			return new DefaultBoxItemControls(filteredBoxItems, points);
		}
		return boxItemListenerBuilderFactory.createBoxItemControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItems(filteredBoxItems)
				.withPoints(points)
				.build();
	}
	
	public boolean hasBoxItemGroupListenerBuilderFactory() {
		return boxItemGroupListenerBuilderFactory != null;
	}
	
}
