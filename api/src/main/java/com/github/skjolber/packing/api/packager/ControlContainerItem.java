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

public class ControlContainerItem extends ContainerItem {

	public ControlContainerItem(Container container, int count) {
		super(container, count);
	}
	
	public ControlContainerItem(ContainerItem containerItem) {
		super(containerItem.getContainer(), containerItem.getCount());
	}

	protected BoxItemControlsBuilderFactory boxItemControlsBuilderFactory;
	protected PointControlsBuilderFactory pointControlsBuilderFactory;

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

	public BoxItemControls createBoxItemControls(Container container, Stack stack, FilteredBoxItems filteredBoxItems, FilteredPoints points, FilteredBoxItemGroups groups) {
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
	
	public PointControls createPointControls(Container container, Stack stack, FilteredBoxItems filteredBoxItems, FilteredPoints points) {
		if(pointControlsBuilderFactory == null) {
			return new DefaultPointControls(points);
		}
		return pointControlsBuilderFactory.createPointControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withBoxItems(filteredBoxItems)
				.withPoints(points)
				.build();
	}
	
	public boolean hasPointControlsBuilderFactory() {
		return pointControlsBuilderFactory != null;
	}
	
	public boolean hasBoxItemControlsBuilderFactory() {
		return boxItemControlsBuilderFactory != null;
	}	
	
	public boolean hasControls() {
		return hasBoxItemControlsBuilderFactory() || hasBoxItemControlsBuilderFactory();
	}
}
