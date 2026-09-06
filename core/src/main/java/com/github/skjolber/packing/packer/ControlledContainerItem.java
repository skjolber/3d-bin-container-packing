package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

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
		super(containerItem);
	}
	

}
