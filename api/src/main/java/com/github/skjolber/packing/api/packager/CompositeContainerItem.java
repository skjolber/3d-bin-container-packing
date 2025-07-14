package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * 
 * Container item wrapped with some constraints.
 * 
 */

public class CompositeContainerItem {

	protected final ContainerItem containerItem;
	protected BoxItemGroupControlsBuilderFactory boxItemGroupListenerControlsFactory;
	protected BoxItemControlsBuilderFactory boxItemListenerBuilderFactory;

	public CompositeContainerItem(ContainerItem containerItem) {
		this.containerItem = containerItem;
	}
	
	public void setBoxItemControlsBuilderFactory(
			BoxItemControlsBuilderFactory factory) {
		this.boxItemListenerBuilderFactory = factory;
	}
	
	public BoxItemControlsBuilderFactory getBoxItemControlsBuilderFactory() {
		return boxItemListenerBuilderFactory;
	}
	
	public ContainerItem getContainerItem() {
		return containerItem;
	}

	public void setBoxItemGroupControlsBuilderFactory(BoxItemGroupControlsBuilderFactory factory) {
		this.boxItemGroupListenerControlsFactory = factory;
	}
	
	public BoxItemGroupControlsBuilderFactory getBoxItemGroupListenerControlsFactory() {
		return boxItemGroupListenerControlsFactory;
	}

	public BoxItemGroupControls createBoxItemGroupListener(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredPoints points) {
		if(boxItemGroupListenerControlsFactory == null) {
			return new DefaultBoxItemGroupControls(groups);
		}
		return boxItemGroupListenerControlsFactory.createBoxItemGroupControlsBuilder()
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
				.withBoxItems(filteredBoxItems)
				.withPoints(points)
				.build();
	}
	
	public boolean hasBoxItemGroupListenerBuilderFactory() {
		return boxItemGroupListenerControlsFactory != null;
	}
	
}
