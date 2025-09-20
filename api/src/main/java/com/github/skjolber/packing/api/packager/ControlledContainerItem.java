package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.PointSource;

/**
 * 
 * Container item wrapped with some controls.
 * 
 */

public class ControlledContainerItem extends ContainerItem {

	public ControlledContainerItem(Container container, int count) {
		super(container, count);
	}
	
	public ControlledContainerItem(ContainerItem containerItem) {
		super(containerItem.getContainer(), containerItem.getCount());
	}

	protected ManifestControlsBuilderFactory manifestControlsBuilderFactory;
	protected PointControlsBuilderFactory pointControlsBuilderFactory;

	public void setPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory) {
		this.pointControlsBuilderFactory = pointControlsBuilderFactory;
	}
	
	public PointControlsBuilderFactory getPointControlsBuilderFactory() {
		return pointControlsBuilderFactory;
	}
	
	public void setBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory factory) {
		this.manifestControlsBuilderFactory = factory;
	}
	
	public ManifestControlsBuilderFactory getBoxItemControlsBuilderFactory() {
		return manifestControlsBuilderFactory;
	}

	public ManifestControls createBoxItemControls(Container container, Stack stack, BoxItemSource filteredBoxItems, PointSource points, BoxItemGroupSource groups) {
		if(manifestControlsBuilderFactory == null) {
			return new DefaultManifestControls(filteredBoxItems);
		}
		return manifestControlsBuilderFactory.createBoxItemControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withBoxItems(filteredBoxItems)
				.withBoxItemGroups(groups)
				.withPoints(points)
				.build();
	}
	
	public PointControls createPointControls(Container container, Stack stack, BoxItemSource filteredBoxItems, PointSource points) {
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
		return manifestControlsBuilderFactory != null;
	}	
	
	public boolean hasControls() {
		return hasBoxItemControlsBuilderFactory() || hasBoxItemControlsBuilderFactory();
	}
}
