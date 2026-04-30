package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.manifest.DefaultManifestControls;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointSource;

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
