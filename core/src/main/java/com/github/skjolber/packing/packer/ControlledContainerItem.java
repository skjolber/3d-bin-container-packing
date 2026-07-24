package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;

/**
 * 
 * Container item wrapped with some controls.
 * 
 */

public class ControlledContainerItem extends ContainerItem {

	protected ManifestControlsBuilderFactory manifestControlsBuilderFactory;
	protected PointControlsBuilderFactory pointControlsBuilderFactory;
	protected List<Point> initialPoints; 

	public ControlledContainerItem(Container container, int count) {
		super(container, count);
	}
	
	public ControlledContainerItem(ContainerItem containerItem) {
		super(containerItem.getContainer(), containerItem.getCount());
	}
	

}
