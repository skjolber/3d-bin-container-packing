package com.github.skjolber.packing.api.packager.inputs;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;


/**
 * 
 * Holder for the current inputs. 
 * 
 */

public interface ContainerItemInput {

	int getIndex();
	
	ContainerItem getContainerItem();
	
	int size();
	
	BoxItem get(int index);
	
	boolean remove(BoxItem input, int count);

	int getCount();
	boolean decrementCount(int amount);
	
}
