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
	protected BoxItemGroupControlsBuilderFactory boxItemGroupControlsBuilderFactory;
	protected BoxItemControlsBuilderFactory boxItemControlsBuilderFactory;
	protected PointControlsBuilderFactory pointControlsBuilderFactory;

	public CompositeContainerItem(ContainerItem containerItem) {
		this.containerItem = containerItem;
	}
	
	public void setPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory) {
		this.pointControlsBuilderFactory = pointControlsBuilderFactory;
	}
	
	public PointControlsBuilderFactory getPointControlsBuilderFactory() {
		return pointControlsBuilderFactory;
	}
	
	public void setBoxItemControlsBuilderFactory(BoxItemControlsBuilderFactory factory) {
		this.boxItemControlsBuilderFactory = factory;
	}
	
	public BoxItemControlsBuilderFactory getBoxItemControlsBuilderFactory() {
		return boxItemControlsBuilderFactory;
	}
	
	public ContainerItem getContainerItem() {
		return containerItem;
	}

	public void setBoxItemGroupControlsBuilderFactory(BoxItemGroupControlsBuilderFactory factory) {
		this.boxItemGroupControlsBuilderFactory = factory;
	}

	public BoxItemGroupControls createBoxItemGroupControls(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredPoints points) {
		if(boxItemGroupControlsBuilderFactory == null) {
			return new DefaultBoxItemGroupControls(groups);
		}
		return boxItemGroupControlsBuilderFactory.createBoxItemGroupControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withFilteredBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}

	public BoxItemControls createBoxItemControls(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredBoxItems filteredBoxItems, FilteredPoints points) {
		if(boxItemControlsBuilderFactory == null) {
			return new DefaultBoxItemControls(filteredBoxItems);
		}
		return boxItemControlsBuilderFactory.createBoxItemControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withBoxItems(filteredBoxItems)
				.withBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}
	
	public PointControls createPointControls(Container container, Stack stack, FilteredBoxItemGroups groups, FilteredBoxItems filteredBoxItems, FilteredPoints points) {
		if(pointControlsBuilderFactory == null) {
			return new DefaultPointControls(points);
		}
		return pointControlsBuilderFactory.createPointControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withBoxItems(filteredBoxItems)
				.withBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}
	
	public boolean hasBoxItemGroupListenerBuilderFactory() {
		return boxItemGroupControlsBuilderFactory != null;
	}
	
	public boolean hasPointControlsBuilderFactory() {
		return pointControlsBuilderFactory != null;
	}
	
	public boolean hasBoxItemControlsBuilderFactory() {
		return boxItemControlsBuilderFactory != null;
	}	
	
}
